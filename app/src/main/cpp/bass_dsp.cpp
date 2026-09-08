#include <jni.h>

#include <atomic>
#include <cmath>
#include <new>

namespace {

    constexpr float kPi = 3.14159265358979f;
    constexpr float kCornerHz = 110.0f;
    constexpr float kQ = 0.707f;
    constexpr float kClipThreshold = 0.89f;
    constexpr float kMaxGainDb = 12.0f;
    constexpr int kMaxChannels = 8;
    constexpr int kStages = 2;

    struct Biquad {
        float b0 = 1.f;
        float b1 = 0.f;
        float b2 = 0.f;
        float a1 = 0.f;
        float a2 = 0.f;
    };

    struct StageState {
        float x1 = 0.f;
        float x2 = 0.f;
        float y1 = 0.f;
        float y2 = 0.f;
    };

    struct Dsp {
        Biquad coeff;
        StageState state[kMaxChannels][kStages];
        int channels = 2;
        int sampleRate = 44100;
        float appliedGain = -1.f;
        bool cascade = false;
        std::atomic<float> gainDb{0.f};
    };

    inline float runStage(const Biquad& c, StageState& s, float x) {
        const float y = c.b0 * x + c.b1 * s.x1 + c.b2 * s.x2 - c.a1 * s.y1 - c.a2 * s.y2;
        s.x2 = s.x1;
        s.x1 = x;
        s.y2 = s.y1;
        s.y1 = y;
        return y;
    }

    inline float softClip(float x) {
        const float ax = std::fabs(x);
        if (ax <= kClipThreshold) return x;
        const float over = (ax - kClipThreshold) / (1.f - kClipThreshold);
        const float limited = kClipThreshold + (1.f - kClipThreshold) * std::tanh(over);
        return x < 0.f ? -limited : limited;
    }

    void computeCoeffs(Dsp& d, float gainDb) {
        if (gainDb <= 0.05f) {
            d.coeff = Biquad();
            d.cascade = false;
            return;
        }

        const float a = std::sqrt(std::pow(10.f, gainDb / 20.f));
        const float w0 = 2.f * kPi * kCornerHz / static_cast<float>(d.sampleRate);
        const float cw = std::cos(w0);
        const float sw = std::sin(w0);
        const float alpha = sw / (2.f * kQ);
        const float sq = std::sqrt(a);

        const float b0 = a * ((a + 1.f) + (a - 1.f) * cw + 2.f * sq * alpha);
        const float b1 = 2.f * a * ((a - 1.f) + (a + 1.f) * cw);
        const float b2 = a * ((a + 1.f) + (a - 1.f) * cw - 2.f * sq * alpha);
        const float a0 = (a + 1.f) - (a - 1.f) * cw + 2.f * sq * alpha;
        const float a1 = -2.f * ((a - 1.f) - (a + 1.f) * cw);
        const float a2 = (a + 1.f) - (a - 1.f) * cw - 2.f * sq * alpha;

        d.coeff.b0 = b0 / a0;
        d.coeff.b1 = b1 / a0;
        d.coeff.b2 = b2 / a0;
        d.coeff.a1 = a1 / a0;
        d.coeff.a2 = a2 / a0;
        d.cascade = gainDb > 6.f;
    }

    inline Dsp* fromHandle(jlong h) {
        return reinterpret_cast<Dsp*>(h);
    }

}

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_blueplayer_app_audio_NativeBassProcessor_nativeCreate(
        JNIEnv*, jobject, jint sampleRate, jint channels) {
    Dsp* d = new (std::nothrow) Dsp();
    if (!d) return 0L;
    d->sampleRate = sampleRate > 0 ? sampleRate : 44100;
    d->channels = (channels > 0 && channels <= kMaxChannels) ? channels : 2;
    return reinterpret_cast<jlong>(d);
}

JNIEXPORT void JNICALL
Java_com_blueplayer_app_audio_NativeBassProcessor_nativeDestroy(
        JNIEnv*, jobject, jlong h) {
    delete fromHandle(h);
}

JNIEXPORT void JNICALL
Java_com_blueplayer_app_audio_NativeBassProcessor_nativeSetGain(
        JNIEnv*, jobject, jlong h, jfloat db) {
    Dsp* d = fromHandle(h);
    if (!d) return;
    const float g = db < 0.f ? 0.f : (db > kMaxGainDb ? kMaxGainDb : db);
    d->gainDb.store(g, std::memory_order_relaxed);
}

JNIEXPORT void JNICALL
Java_com_blueplayer_app_audio_NativeBassProcessor_nativeFlush(
        JNIEnv*, jobject, jlong h) {
    Dsp* d = fromHandle(h);
    if (!d) return;
    for (int c = 0; c < kMaxChannels; ++c) {
        for (int s = 0; s < kStages; ++s) {
            d->state[c][s] = StageState();
        }
    }
}

JNIEXPORT void JNICALL
Java_com_blueplayer_app_audio_NativeBassProcessor_nativeProcess(
        JNIEnv* env, jobject, jlong h,
        jshortArray in, jshortArray out, jint samples) {
    Dsp* d = fromHandle(h);
    if (!d || !in || !out || samples <= 0) return;

    jshort* src = env->GetShortArrayElements(in, nullptr);
    jshort* dst = env->GetShortArrayElements(out, nullptr);
    if (!src || !dst) {
        if (src) env->ReleaseShortArrayElements(in, src, JNI_ABORT);
        if (dst) env->ReleaseShortArrayElements(out, dst, JNI_ABORT);
        return;
    }

    const float gain = d->gainDb.load(std::memory_order_relaxed);
    if (gain != d->appliedGain) {
        computeCoeffs(*d, gain);
        d->appliedGain = gain;
    }

    const bool active = gain > 0.05f;
    const int chCount = d->channels;

    for (jint i = 0; i < samples; ++i) {
        float x = static_cast<float>(src[i]) * (1.f / 32768.f);

        if (active) {
            const int c = i % chCount;
            float y = runStage(d->coeff, d->state[c][0], x);
            if (d->cascade) {
                y = runStage(d->coeff, d->state[c][1], y);
            }
            x = y;
        }

        const float clipped = softClip(x);
        float v = clipped * 32767.f;
        if (v > 32767.f) v = 32767.f;
        if (v < -32768.f) v = -32768.f;
        dst[i] = static_cast<jshort>(std::lroundf(v));
    }

    env->ReleaseShortArrayElements(in, src, JNI_ABORT);
    env->ReleaseShortArrayElements(out, dst, 0);
}

}