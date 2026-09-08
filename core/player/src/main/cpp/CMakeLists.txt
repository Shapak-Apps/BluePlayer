cmake_minimum_required(VERSION 3.22.1)
project("waveformextractor")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL Release)
    set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -ffunction-sections -fdata-sections")
    set(CMAKE_SHARED_LINKER_FLAGS_RELEASE "${CMAKE_SHARED_LINKER_FLAGS_RELEASE} -Wl,--gc-sections")
endif()

add_library(waveformextractor SHARED waveform_extractor.cpp)

find_library(log-lib log)
find_library(mediandk-lib mediandk)

target_link_libraries(waveformextractor ${log-lib} ${mediandk-lib})