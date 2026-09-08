cmake_minimum_required(VERSION 3.22.1)
project(bassdsp)

add_library(bassdsp SHARED bass_dsp.cpp)

target_compile_options(bassdsp PRIVATE -O3 -fno-math-errno)

find_library(log-lib log)

target_link_libraries(bassdsp ${log-lib})