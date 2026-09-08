#include <jni.h>
#include <android/log.h>

#include <media/NdkMediaCodec.h>
#include <media/NdkMediaExtractor.h>
#include <media/NdkMediaFormat.h>
#include <media/NdkMediaError.h>

#include <cstdint>
#include <cstddef>
#include <cstring>
#include <cmath>
#include <algorithm>
#include <limits>

#include <sys/time.h>
#include <unistd.h>

#define LOG_TAG "WaveformNative"

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)

static constexpr int TARGET_BINS = 120;
static constexpr int64_t MAX_WALL_TIME_MS = 45000;

static constexpr int32_t PCM_ENCODING_DEFAULT = 1;
static constexpr int32_t PCM_ENCODING_16BIT   = 2;
static constexpr int32_t PCM_ENCODING_8BIT    = 3;
static constexpr int32_t PCM_ENCODING_FLOAT   = 4;
static constexpr int32_t PCM_ENCODING_24BIT   = 5;
static constexpr int32_t PCM_ENCODING_32BIT   = 6;

static constexpr int MAX_SAMPLES_PER_BUFFER = 1000000;

static int64_t nowMs() {
    timeval tv{};
    gettimeofday(&tv, nullptr);
    return static_cast<int64_t>(tv.tv_sec) * 1000LL +
           static_cast<int64_t>(tv.tv_usec) / 1000LL;
}

static bool safeRange(size_t offset, size_t size, size_t total) {
    if (offset > total) {
        return false;
    }
    return size <= total - offset;
}

static const char* encodingName(int32_t encoding) {
    switch (encoding) {
        case PCM_ENCODING_8BIT:
            return "PCM_8BIT";
        case PCM_ENCODING_16BIT:
            return "PCM_16BIT";
        case PCM_ENCODING_FLOAT:
            return "PCM_FLOAT";
        case PCM_ENCODING_24BIT:
            return "PCM_24BIT_PACKED";
        case PCM_ENCODING_32BIT:
            return "PCM_32BIT";
        case PCM_ENCODING_DEFAULT:
            return "PCM_DEFAULT";
        default:
            return "UNKNOWN";
    }
}

static int bytesPerSampleForEncoding(int32_t encoding) {
    switch (encoding) {
        case PCM_ENCODING_8BIT:
            return 1;

        case PCM_ENCODING_16BIT:
            return 2;

        case PCM_ENCODING_FLOAT:
            return 4;

        case PCM_ENCODING_24BIT:
            return 3;

        case PCM_ENCODING_32BIT:
            return 4;

        case PCM_ENCODING_DEFAULT:
        default:
            return 2;
    }
}

static bool getPcmEncodingFromFormat(
        AMediaFormat* format,
        int32_t* encoding) {

    if (!format || !encoding) {
        return false;
    }

    const char* keys[] = {
            "pcm-encoding",
            "key-pcm-encoding",
            "pcmEncoding",
            "key-pcmEncoding"
    };

    for (const char* key : keys) {
        int32_t value = 0;

        if (AMediaFormat_getInt32(format, key, &value)) {
            if (value >= PCM_ENCODING_DEFAULT &&
                value <= PCM_ENCODING_32BIT) {

                *encoding = value;
                return true;
            }
        }
    }

    return false;
}

static AMediaFormat* buildDecoderFormat(
        AMediaFormat* source,
        bool request16Bit) {

    if (!source) {
        return nullptr;
    }

    AMediaFormat* dst = AMediaFormat_new();

    if (!dst) {
        LOGW("AMediaFormat_new() failed while building decoder format");
        return nullptr;
    }

    const char* mime = nullptr;

    if (!AMediaFormat_getString(
            source,
            AMEDIAFORMAT_KEY_MIME,
            &mime) ||
        !mime) {

        LOGW("Source format has no MIME");
        AMediaFormat_delete(dst);
        return nullptr;
    }

    AMediaFormat_setString(
            dst,
            AMEDIAFORMAT_KEY_MIME,
            mime
    );

    int32_t sampleRate = 0;

    if (AMediaFormat_getInt32(
            source,
            AMEDIAFORMAT_KEY_SAMPLE_RATE,
            &sampleRate)) {

        AMediaFormat_setInt32(
                dst,
                AMEDIAFORMAT_KEY_SAMPLE_RATE,
                sampleRate
        );
    }

    int32_t channelCount = 0;

    if (AMediaFormat_getInt32(
            source,
            AMEDIAFORMAT_KEY_CHANNEL_COUNT,
            &channelCount)) {

        AMediaFormat_setInt32(
                dst,
                AMEDIAFORMAT_KEY_CHANNEL_COUNT,
                channelCount
        );
    }

    int32_t maxInputSize = 0;

    if (AMediaFormat_getInt32(
            source,
            AMEDIAFORMAT_KEY_MAX_INPUT_SIZE,
            &maxInputSize)) {

        AMediaFormat_setInt32(
                dst,
                AMEDIAFORMAT_KEY_MAX_INPUT_SIZE,
                maxInputSize
        );
    }

    if (request16Bit) {
        AMediaFormat_setInt32(
                dst,
                "pcm-encoding",
                PCM_ENCODING_16BIT
        );
    }

    return dst;
}

static void logBufferDiagnostics(
        const uint8_t* data,
        size_t size,
        int32_t encoding) {

    if (!data || size == 0) {
        LOGI("PCM diagnostic: empty buffer");
        return;
    }

    const size_t hexBytes = std::min<size_t>(size, 24);

    char hex[24 * 3 + 1]{};
    size_t pos = 0;

    for (size_t i = 0; i < hexBytes && pos + 3 < sizeof(hex); ++i) {
        int written = snprintf(
                hex + pos,
                sizeof(hex) - pos,
                "%02X ",
                static_cast<unsigned int>(data[i]));

        if (written <= 0) {
            break;
        }

        pos += static_cast<size_t>(written);
    }

    LOGI(
            "PCM diagnostic encoding=%s bps=%d bytes=%zu hex=%s",
            encodingName(encoding),
            bytesPerSampleForEncoding(encoding),
            size,
            hex);

    const int bps = bytesPerSampleForEncoding(encoding);

    if (bps <= 0 || size < static_cast<size_t>(bps)) {
        return;
    }

    const size_t count = std::min<size_t>(
            size / static_cast<size_t>(bps),
            6);

    if (encoding == PCM_ENCODING_FLOAT) {
        char values[512]{};
        size_t p = 0;

        for (size_t i = 0; i < count; ++i) {
            float value = 0.0f;

            memcpy(
                    &value,
                    data + i * 4,
                    sizeof(value));

            int written = snprintf(
                    values + p,
                    sizeof(values) - p,
                    "%.5f ",
                    static_cast<double>(value));

            if (written <= 0 ||
                static_cast<size_t>(written) >= sizeof(values) - p) {
                break;
            }

            p += static_cast<size_t>(written);
        }

        LOGI("PCM diagnostic float values: %s", values);
    } else if (encoding == PCM_ENCODING_16BIT) {
        char values[512]{};
        size_t p = 0;

        for (size_t i = 0; i < count; ++i) {
            int16_t value = 0;

            memcpy(
                    &value,
                    data + i * 2,
                    sizeof(value));

            int written = snprintf(
                    values + p,
                    sizeof(values) - p,
                    "%d ",
                    static_cast<int>(value));

            if (written <= 0 ||
                static_cast<size_t>(written) >= sizeof(values) - p) {
                break;
            }

            p += static_cast<size_t>(written);
        }

        LOGI("PCM diagnostic int16 values: %s", values);
    }
}

static int32_t detect16OrFloat(
        const uint8_t* data,
        size_t size) {

    if (!data || size < 8) {
        return PCM_ENCODING_16BIT;
    }

    const bool divisibleBy4 = (size % 4) == 0;

    const size_t floatCount = std::min<size_t>(
            size / 4,
            4096);

    const size_t int16Count = std::min<size_t>(
            size / 2,
            8192);

    if (floatCount < 2 || int16Count < 4) {
        return PCM_ENCODING_16BIT;
    }

    size_t finiteFloat = 0;
    size_t plausibleFloat = 0;
    size_t normalFloat = 0;
    double floatAbsSum = 0.0;

    for (size_t i = 0; i < floatCount; ++i) {
        float value = 0.0f;

        memcpy(
                &value,
                data + i * 4,
                sizeof(value));

        if (!std::isfinite(value)) {
            continue;
        }

        ++finiteFloat;

        const double a = std::fabs(
                static_cast<double>(value));

        if (a <= 1.0) {
            ++plausibleFloat;
        }

        if (a > 1e-7 && a <= 1.5) {
            ++normalFloat;
        }

        if (a < 2.0) {
            floatAbsSum += a;
        }
    }

    double floatScore = 0.0;

    const double finiteRatio =
            static_cast<double>(finiteFloat) /
            static_cast<double>(floatCount);

    const double plausibleRatio =
            static_cast<double>(plausibleFloat) /
            static_cast<double>(floatCount);

    const double normalRatio =
            static_cast<double>(normalFloat) /
            static_cast<double>(floatCount);

    floatScore += finiteRatio * 0.30;
    floatScore += plausibleRatio * 0.45;
    floatScore += normalRatio * 0.25;

    if (finiteRatio > 0.98 &&
        plausibleRatio > 0.90 &&
        normalRatio > 0.50) {

        floatScore += 0.35;
    }

    double int16AbsSum = 0.0;
    size_t int16NearFullScale = 0;
    size_t int16NonZero = 0;

    for (size_t i = 0; i < int16Count; ++i) {
        int16_t sample = 0;

        memcpy(
                &sample,
                data + i * 2,
                sizeof(sample));

        const int value = static_cast<int>(sample);
        const int absValue = std::abs(value);

        int16AbsSum += static_cast<double>(absValue);

        if (absValue > 30000) {
            ++int16NearFullScale;
        }

        if (value != 0) {
            ++int16NonZero;
        }
    }

    const double int16MeanAbs =
            int16AbsSum /
            static_cast<double>(int16Count);

    const double int16NearFullRatio =
            static_cast<double>(int16NearFullScale) /
            static_cast<double>(int16Count);

    const double int16NonZeroRatio =
            static_cast<double>(int16NonZero) /
            static_cast<double>(int16Count);

    double int16Score = 0.50;

    if (int16NonZeroRatio > 0.05) {
        int16Score += 0.15;
    }

    if (int16MeanAbs > 100.0 &&
        int16MeanAbs < 30000.0) {

        int16Score += 0.15;
    }

    if (int16NearFullRatio < 0.30) {
        int16Score += 0.10;
    }

    if (divisibleBy4) {
        floatScore += 0.05;
    }

    LOGI(
            "PCM heuristic: float finite=%.3f plausible=%.3f normal=%.3f "
            "score=%.3f | int16 meanAbs=%.1f nearFull=%.3f "
            "nonZero=%.3f score=%.3f",
            finiteRatio,
            plausibleRatio,
            normalRatio,
            floatScore,
            int16MeanAbs,
            int16NearFullRatio,
            int16NonZeroRatio,
            int16Score);

    if (floatScore >= 0.82 &&
        plausibleRatio >= 0.80 &&
        finiteRatio >= 0.95) {

        return PCM_ENCODING_FLOAT;
    }

    return PCM_ENCODING_16BIT;
}

static int32_t detectEncodingFromBuffer(
        const uint8_t* data,
        size_t size,
        int32_t currentEncoding) {

    if (!data || size == 0) {
        return currentEncoding;
    }

    if (currentEncoding == PCM_ENCODING_8BIT ||
        currentEncoding == PCM_ENCODING_24BIT ||
        currentEncoding == PCM_ENCODING_32BIT) {

        return currentEncoding;
    }

    return detect16OrFloat(data, size);
}

static void accumulatePcm8(
        const uint8_t* ptr,
        int count,
        double* acc) {

    if (!ptr || !acc || count <= 0) {
        return;
    }

    for (int i = 0; i < count; ++i) {
        int8_t sample = 0;

        memcpy(
                &sample,
                ptr + i,
                sizeof(sample));
        double v =
                (static_cast<double>(sample) - 128.0) *
                256.0;

        *acc += v * v;
    }
}

static void accumulatePcm16(
        const uint8_t* ptr,
        int count,
        double* acc) {

    if (!ptr || !acc || count <= 0) {
        return;
    }

    for (int i = 0; i < count; ++i) {
        int16_t sample = 0;

        memcpy(
                &sample,
                ptr + static_cast<size_t>(i) * 2,
                sizeof(sample));
        int sampleInt = static_cast<int>(sample);

        sampleInt = std::max(
                -32000,
                std::min(32000, sampleInt));

        const double v =
                static_cast<double>(sampleInt);

        *acc += v * v;
    }
}

static void accumulatePcmFloat(
        const uint8_t* ptr,
        int count,
        double* acc) {

    if (!ptr || !acc || count <= 0) {
        return;
    }

    for (int i = 0; i < count; ++i) {
        float sample = 0.0f;

        memcpy(
                &sample,
                ptr + static_cast<size_t>(i) * 4,
                sizeof(sample));

        if (!std::isfinite(sample)) {
            continue;
        }

        if (sample > 1.0f) {
            sample = 1.0f;
        } else if (sample < -1.0f) {
            sample = -1.0f;
        }

        const double v =
                static_cast<double>(sample) * 32768.0;

        *acc += v * v;
    }
}

static void accumulatePcm32(
        const uint8_t* ptr,
        int count,
        double* acc) {

    if (!ptr || !acc || count <= 0) {
        return;
    }

    for (int i = 0; i < count; ++i) {
        int32_t sample = 0;

        memcpy(
                &sample,
                ptr + static_cast<size_t>(i) * 4,
                sizeof(sample));
        const double v =
                static_cast<double>(sample) / 65536.0;

        *acc += v * v;
    }
}

static void accumulatePcm24(
        const uint8_t* ptr,
        int count,
        double* acc) {

    if (!ptr || !acc || count <= 0) {
        return;
    }

    for (int i = 0; i < count; ++i) {
        const uint8_t* p =
                ptr + static_cast<size_t>(i) * 3;

        uint8_t bytes[3]{};

        memcpy(
                bytes,
                p,
                3);
        int32_t sample =
                static_cast<int32_t>(bytes[0]) |
                (static_cast<int32_t>(bytes[1]) << 8) |
                (static_cast<int32_t>(bytes[2]) << 16);

        if (sample & 0x00800000) {
            sample |= static_cast<int32_t>(0xFF000000);
        }
        const double v =
                static_cast<double>(sample) / 256.0;

        *acc += v * v;
    }
}

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_blueplayer_core_player_WaveformExtractor_extractNative(
        JNIEnv* env,
        jobject,
        jint fd,
        jlong offset,
        jlong length,
        jlong durationUs) {

    if (!env) {
        return nullptr;
    }

    jfloatArray empty =
            env->NewFloatArray(0);

    if (!empty) {
        return nullptr;
    }

    if (fd < 0) {
        LOGE("Invalid fd=%d", fd);
        return empty;
    }

    if (offset < 0 ||
        length <= 0 ||
        durationUs <= 0) {

        LOGE(
                "Invalid arguments fd=%d offset=%lld length=%lld durationUs=%lld",
                fd,
                static_cast<long long>(offset),
                static_cast<long long>(length),
                static_cast<long long>(durationUs));

        return empty;
    }

    AMediaExtractor* extractor =
            AMediaExtractor_new();

    if (!extractor) {
        LOGE("AMediaExtractor_new failed");
        return empty;
    }

    media_status_t st =
            AMediaExtractor_setDataSourceFd(
                    extractor,
                    fd,
                    static_cast<off64_t>(offset),
                    static_cast<off64_t>(length));

    if (st != AMEDIA_OK) {
        LOGE(
                "setDataSourceFd failed: %d",
                static_cast<int>(st));

        AMediaExtractor_delete(extractor);
        return empty;
    }

    ssize_t trackIndex = -1;
    AMediaFormat* sourceFormat = nullptr;

    const size_t trackCount =
            AMediaExtractor_getTrackCount(extractor);

    for (size_t i = 0; i < trackCount; ++i) {
        AMediaFormat* f =
                AMediaExtractor_getTrackFormat(
                        extractor,
                        i);

        if (!f) {
            continue;
        }

        const char* trackMime = nullptr;

        if (AMediaFormat_getString(
                f,
                AMEDIAFORMAT_KEY_MIME,
                &trackMime) &&
            trackMime &&
            strncmp(trackMime, "audio/", 6) == 0) {

            trackIndex =
                    static_cast<ssize_t>(i);

            sourceFormat = f;
            break;
        }

        AMediaFormat_delete(f);
    }

    if (trackIndex < 0 ||
        !sourceFormat) {

        LOGE("No audio track found");

        if (sourceFormat) {
            AMediaFormat_delete(sourceFormat);
        }

        AMediaExtractor_delete(extractor);
        return empty;
    }

    const char* mime = nullptr;

    if (!AMediaFormat_getString(
            sourceFormat,
            AMEDIAFORMAT_KEY_MIME,
            &mime) ||
        !mime) {

        LOGE("Audio track has no MIME");

        AMediaFormat_delete(sourceFormat);
        AMediaExtractor_delete(extractor);
        return empty;
    }

    LOGI(
            "Selected audio track=%zd mime=%s",
            trackIndex,
            mime);
    if (!AMediaExtractor_selectTrack(
            extractor,
            static_cast<size_t>(trackIndex))) {

        LOGE("selectTrack failed");

        AMediaFormat_delete(sourceFormat);
        AMediaExtractor_delete(extractor);
        return empty;
    }

    AMediaFormat* requestedFormat =
            buildDecoderFormat(
                    sourceFormat,
                    true);

    AMediaCodec* codec =
            AMediaCodec_createDecoderByType(mime);

    if (!codec) {
        LOGE(
                "createDecoderByType failed for mime=%s",
                mime);

        if (requestedFormat) {
            AMediaFormat_delete(requestedFormat);
        }

        AMediaFormat_delete(sourceFormat);
        AMediaExtractor_delete(extractor);
        return empty;
    }

    bool configured = false;

    if (requestedFormat) {
        st = AMediaCodec_configure(
                codec,
                requestedFormat,
                nullptr,
                nullptr,
                0);

        if (st == AMEDIA_OK) {
            configured = true;

            LOGI(
                    "Codec configured using private PCM16 request format");
        } else {
            LOGW(
                    "PCM16 requested configure failed: %d; "
                    "recreating codec and falling back to original format",
                    static_cast<int>(st));
        }

        AMediaFormat_delete(requestedFormat);
        requestedFormat = nullptr;
    }

    if (!configured) {
        AMediaCodec_delete(codec);
        codec = nullptr;

        codec =
                AMediaCodec_createDecoderByType(mime);

        if (!codec) {
            LOGE(
                    "Fallback createDecoderByType failed for mime=%s",
                    mime);

            AMediaFormat_delete(sourceFormat);
            AMediaExtractor_delete(extractor);
            return empty;
        }

        st = AMediaCodec_configure(
                codec,
                sourceFormat,
                nullptr,
                nullptr,
                0);

        if (st != AMEDIA_OK) {
            LOGE(
                    "Fallback configure(original format) failed: %d",
                    static_cast<int>(st));

            AMediaCodec_delete(codec);
            AMediaFormat_delete(sourceFormat);
            AMediaExtractor_delete(extractor);
            return empty;
        }

        LOGI(
                "Codec configured using untouched extractor format");
    }

    st = AMediaCodec_start(codec);

    if (st != AMEDIA_OK) {
        LOGE(
                "AMediaCodec_start failed: %d",
                static_cast<int>(st));

        AMediaCodec_delete(codec);
        AMediaFormat_delete(sourceFormat);
        AMediaExtractor_delete(extractor);
        return empty;
    }

    double sumSq[TARGET_BINS]{};
    int64_t cnt[TARGET_BINS]{};

    int32_t pcmEncoding =
            PCM_ENCODING_16BIT;

    int bytesPerSample = 2;

    bool encodingKnownFromFormat = false;

    bool needDiagnosticDump = false;

    int64_t outputBufferCounter = 0;
    int64_t nextHeuristicCheck = 0;

    bool inputDone = false;
    bool outputDone = false;

    const int64_t startTime =
            nowMs();

    while (!outputDone) {

        if (nowMs() - startTime >
            MAX_WALL_TIME_MS) {

            LOGW(
                    "45 second wall-time limit reached");

            break;
        }

        if (!inputDone) {

            ssize_t inIdx =
                    AMediaCodec_dequeueInputBuffer(
                            codec,
                            10000);

            if (inIdx >= 0) {

                size_t inputCapacity = 0;

                uint8_t* inputBuffer =
                        AMediaCodec_getInputBuffer(
                                codec,
                                static_cast<size_t>(inIdx),
                                &inputCapacity);

                if (!inputBuffer ||
                    inputCapacity == 0) {

                    LOGE(
                            "getInputBuffer returned null/empty");

                    AMediaCodec_queueInputBuffer(
                            codec,
                            static_cast<size_t>(inIdx),
                            0,
                            0,
                            0,
                            AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM);

                    inputDone = true;

                } else {

                    ssize_t sampleSize =
                            AMediaExtractor_readSampleData(
                                    extractor,
                                    inputBuffer,
                                    inputCapacity);

                    if (sampleSize < 0) {
                        media_status_t queueStatus =
                                AMediaCodec_queueInputBuffer(
                                        codec,
                                        static_cast<size_t>(inIdx),
                                        0,
                                        0,
                                        0,
                                        AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM);

                        if (queueStatus != AMEDIA_OK) {
                            LOGE(
                                    "queue EOS failed: %d",
                                    static_cast<int>(queueStatus));
                        }

                        inputDone = true;

                    } else {

                        if (static_cast<size_t>(sampleSize) >
                            inputCapacity) {

                            LOGE(
                                    "Extractor returned oversized sample");

                            AMediaCodec_queueInputBuffer(
                                    codec,
                                    static_cast<size_t>(inIdx),
                                    0,
                                    0,
                                    0,
                                    AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM);

                            inputDone = true;

                        } else {

                            int64_t sampleTime =
                                    AMediaExtractor_getSampleTime(
                                            extractor);

                            media_status_t queueStatus =
                                    AMediaCodec_queueInputBuffer(
                                            codec,
                                            static_cast<size_t>(inIdx),
                                            0,
                                            static_cast<size_t>(sampleSize),
                                            sampleTime,
                                            0);

                            if (queueStatus != AMEDIA_OK) {
                                LOGE(
                                        "queueInputBuffer failed: %d",
                                        static_cast<int>(queueStatus));

                                outputDone = true;
                            } else {
                                AMediaExtractor_advance(
                                        extractor);
                            }
                        }
                    }
                }

            } else if (
                    inIdx < AMEDIACODEC_INFO_TRY_AGAIN_LATER) {

                LOGE(
                        "dequeueInputBuffer failed: %zd",
                        inIdx);

                outputDone = true;
            }
        }

        AMediaCodecBufferInfo info{};

        ssize_t outIdx =
                AMediaCodec_dequeueOutputBuffer(
                        codec,
                        &info,
                        10000);

        if (outIdx ==
            AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED) {

            AMediaFormat* outFormat =
                    AMediaCodec_getOutputFormat(codec);

            LOGI(
                    "OUTPUT_FORMAT_CHANGED received");

            encodingKnownFromFormat = false;

            if (outFormat) {

                int32_t reportedEncoding = 0;

                if (getPcmEncodingFromFormat(
                        outFormat,
                        &reportedEncoding)) {

                    pcmEncoding =
                            reportedEncoding;

                    bytesPerSample =
                            bytesPerSampleForEncoding(
                                    pcmEncoding);

                    encodingKnownFromFormat = true;

                    LOGI(
                            "Output format reports encoding=%s (%d), "
                            "bytesPerSample=%d",
                            encodingName(pcmEncoding),
                            pcmEncoding,
                            bytesPerSample);

                } else {

                    LOGW(
                            "Output format contains no pcm-encoding; "
                            "enabling raw-buffer heuristic");

                    pcmEncoding =
                            PCM_ENCODING_16BIT;

                    bytesPerSample = 2;
                }

                LOGI(
                        "Output format: %s",
                        "format object received");

                AMediaFormat_delete(outFormat);

            } else {

                LOGW(
                        "getOutputFormat returned null after "
                        "OUTPUT_FORMAT_CHANGED");

                pcmEncoding =
                        PCM_ENCODING_16BIT;

                bytesPerSample = 2;
            }

            needDiagnosticDump = true;

            nextHeuristicCheck =
                    outputBufferCounter;

            continue;

        } else if (
                outIdx ==
                AMEDIACODEC_INFO_OUTPUT_BUFFERS_CHANGED) {

            LOGI(
                    "OUTPUT_BUFFERS_CHANGED received");

            continue;

        } else if (outIdx >= 0) {

            ++outputBufferCounter;

            const bool eos =
                    (info.flags &
                     AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM) != 0;

            if (info.presentationTimeUs < 0) {

                LOGW(
                        "Skipping output buffer with negative "
                        "PTS=%lld size=%d",
                        static_cast<long long>(
                                info.presentationTimeUs),
                        info.size);

                AMediaCodec_releaseOutputBuffer(
                        codec,
                        static_cast<size_t>(outIdx),
                        false);

                if (eos) {
                    outputDone = true;
                }

                continue;
            }

            if (info.size <= 0) {

                AMediaCodec_releaseOutputBuffer(
                        codec,
                        static_cast<size_t>(outIdx),
                        false);

                if (eos) {
                    outputDone = true;
                }

                continue;
            }

            size_t outSize = 0;

            uint8_t* outBuffer =
                    AMediaCodec_getOutputBuffer(
                            codec,
                            static_cast<size_t>(outIdx),
                            &outSize);

            if (!outBuffer) {

                LOGW(
                        "getOutputBuffer returned null index=%zd",
                        outIdx);

                AMediaCodec_releaseOutputBuffer(
                        codec,
                        static_cast<size_t>(outIdx),
                        false);

                if (eos) {
                    outputDone = true;
                }

                continue;
            }

            if (info.offset < 0 ||
                info.size <= 0) {

                LOGW(
                        "Invalid output metadata offset=%d size=%d",
                        info.offset,
                        info.size);

                AMediaCodec_releaseOutputBuffer(
                        codec,
                        static_cast<size_t>(outIdx),
                        false);

                if (eos) {
                    outputDone = true;
                }

                continue;
            }

            const size_t bufferOffset =
                    static_cast<size_t>(info.offset);

            const size_t bufferSize =
                    static_cast<size_t>(info.size);

            if (!safeRange(
                    bufferOffset,
                    bufferSize,
                    outSize)) {

                LOGE(
                        "Output buffer range invalid: "
                        "offset=%zu size=%zu outSize=%zu",
                        bufferOffset,
                        bufferSize,
                        outSize);

                AMediaCodec_releaseOutputBuffer(
                        codec,
                        static_cast<size_t>(outIdx),
                        false);

                if (eos) {
                    outputDone = true;
                }

                continue;
            }

            const uint8_t* pcm =
                    outBuffer + bufferOffset;

            const bool periodicCheck =
                    !encodingKnownFromFormat &&
                    outputBufferCounter >= nextHeuristicCheck;

            if (needDiagnosticDump ||
                periodicCheck) {

                int32_t detected =
                        detectEncodingFromBuffer(
                                pcm,
                                bufferSize,
                                pcmEncoding);

                logBufferDiagnostics(
                        pcm,
                        bufferSize,
                        detected);

                if (!encodingKnownFromFormat &&
                    detected != pcmEncoding) {

                    LOGI(
                            "RAW PCM heuristic changed encoding "
                            "%s -> %s",
                            encodingName(pcmEncoding),
                            encodingName(detected));

                    pcmEncoding = detected;

                    bytesPerSample =
                            bytesPerSampleForEncoding(
                                    pcmEncoding);

                    LOGI(
                            "Detected encoding=%s (%d), "
                            "bytesPerSample=%d",
                            encodingName(pcmEncoding),
                            pcmEncoding,
                            bytesPerSample);
                } else {
                    LOGI(
                            "Detected encoding=%s (%d), "
                            "bytesPerSample=%d",
                            encodingName(pcmEncoding),
                            pcmEncoding,
                            bytesPerSample);
                }

                needDiagnosticDump = false;
                nextHeuristicCheck =
                        outputBufferCounter + 64;
            }
            bytesPerSample =
                    bytesPerSampleForEncoding(
                            pcmEncoding);

            if (bytesPerSample <= 0) {
                bytesPerSample = 2;
                pcmEncoding = PCM_ENCODING_16BIT;
            }

            size_t sampleCount =
                    bufferSize /
                    static_cast<size_t>(bytesPerSample);

            if (sampleCount >
                static_cast<size_t>(
                        MAX_SAMPLES_PER_BUFFER)) {

                sampleCount =
                        static_cast<size_t>(
                                MAX_SAMPLES_PER_BUFFER);
            }

            if (sampleCount > 0) {

                int bin =
                        static_cast<int>(
                                (static_cast<double>(
                                         info.presentationTimeUs) /
                                 static_cast<double>(
                                         durationUs)) *
                                static_cast<double>(
                                        TARGET_BINS));

                if (bin < 0) {
                    bin = 0;
                }

                if (bin >= TARGET_BINS) {
                    bin = TARGET_BINS - 1;
                }

                const size_t bytesUsed =
                        sampleCount *
                        static_cast<size_t>(bytesPerSample);

                if (bytesUsed <= bufferSize) {

                    double acc = 0.0;

                    const int count =
                            static_cast<int>(
                                    sampleCount);

                    switch (pcmEncoding) {

                        case PCM_ENCODING_FLOAT:
                            accumulatePcmFloat(
                                    pcm,
                                    count,
                                    &acc);
                            break;

                        case PCM_ENCODING_8BIT:
                            accumulatePcm8(
                                    pcm,
                                    count,
                                    &acc);
                            break;

                        case PCM_ENCODING_24BIT:
                            accumulatePcm24(
                                    pcm,
                                    count,
                                    &acc);
                            break;

                        case PCM_ENCODING_32BIT:
                            accumulatePcm32(
                                    pcm,
                                    count,
                                    &acc);
                            break;

                        case PCM_ENCODING_16BIT:
                        case PCM_ENCODING_DEFAULT:
                        default:
                            accumulatePcm16(
                                    pcm,
                                    count,
                                    &acc);
                            break;
                    }

                    if (std::isfinite(acc) &&
                        acc >= 0.0) {

                        sumSq[bin] += acc;
                        cnt[bin] +=
                                static_cast<int64_t>(
                                        sampleCount);
                    }
                }
            }

            AMediaCodec_releaseOutputBuffer(
                    codec,
                    static_cast<size_t>(outIdx),
                    false);

            if (eos) {
                outputDone = true;
            }

        } else if (
                outIdx <
                AMEDIACODEC_INFO_TRY_AGAIN_LATER) {

            LOGE(
                    "dequeueOutputBuffer failed: %zd",
                    outIdx);

            outputDone = true;
        }
    }
    media_status_t stopStatus =
            AMediaCodec_stop(codec);

    if (stopStatus != AMEDIA_OK) {
        LOGW(
                "AMediaCodec_stop returned %d",
                static_cast<int>(stopStatus));
    }

    AMediaCodec_delete(codec);
    codec = nullptr;

    AMediaFormat_delete(sourceFormat);
    sourceFormat = nullptr;

    AMediaExtractor_delete(extractor);
    extractor = nullptr;

    float vals[TARGET_BINS]{};

    for (int i = 0; i < TARGET_BINS; ++i) {

        if (cnt[i] > 0 &&
            std::isfinite(sumSq[i]) &&
            sumSq[i] >= 0.0) {

            const double rms =
                    std::sqrt(
                            sumSq[i] /
                            static_cast<double>(
                                    cnt[i]));

            if (std::isfinite(rms) &&
                rms >= 0.0) {

                vals[i] =
                        static_cast<float>(rms);

            } else {
                vals[i] = 0.0f;
            }

        } else {
            vals[i] = 0.0f;
        }
    }

    for (int i = 0; i < TARGET_BINS; ++i) {

        if (vals[i] == 0.0f) {

            int l = i - 1;
            int r = i + 1;

            while (l >= 0 &&
                   vals[l] == 0.0f) {
                --l;
            }

            while (r < TARGET_BINS &&
                   vals[r] == 0.0f) {
                ++r;
            }

            if (l >= 0 &&
                r < TARGET_BINS) {

                vals[i] =
                        (vals[l] + vals[r]) /
                        2.0f;

            } else if (l >= 0) {

                vals[i] = vals[l];

            } else if (r < TARGET_BINS) {

                vals[i] = vals[r];
            }
        }
    }

    float smooth[TARGET_BINS]{};

    for (int i = 0; i < TARGET_BINS; ++i) {

        const float prev =
                (i > 0)
                ? vals[i - 1]
                : vals[i];

        const float next =
                (i < TARGET_BINS - 1)
                ? vals[i + 1]
                : vals[i];

        smooth[i] =
                (prev +
                 vals[i] * 2.0f +
                 next) /
                4.0f;

        if (!std::isfinite(smooth[i]) ||
            smooth[i] < 0.0f) {

            smooth[i] = 0.0f;
        }
    }

    float globalMax = 1.0f;

    for (int i = 0; i < TARGET_BINS; ++i) {

        if (std::isfinite(smooth[i]) &&
            smooth[i] > globalMax) {

            globalMax = smooth[i];
        }
    }

    if (!std::isfinite(globalMax) ||
        globalMax < 1e-6f) {

        globalMax = 1.0f;
    }

    for (int i = 0; i < TARGET_BINS; ++i) {

        float v =
                smooth[i] /
                globalMax;

        if (!std::isfinite(v)) {
            v = 0.0f;
        }

        v =
                std::max(
                        0.0f,
                        std::min(
                                1.0f,
                                v));

        vals[i] =
                powf(v, 0.75f);

        if (!std::isfinite(vals[i])) {
            vals[i] = 0.0f;
        }
    }

    jfloatArray result =
            env->NewFloatArray(TARGET_BINS);

    if (!result) {
        LOGE(
                "NewFloatArray(%d) failed",
                TARGET_BINS);

        return empty;
    }

    env->SetFloatArrayRegion(
            result,
            0,
            TARGET_BINS,
            vals);

    return result;
}
