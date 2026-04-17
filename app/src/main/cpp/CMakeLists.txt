cmake_minimum_required(VERSION 3.22.1)

project("rhythmtrainer")

find_package(oboe REQUIRED CONFIG)

add_library(rhythmtrainer SHARED
    RhythmEngine.cpp
    RhythmEngineJNI.cpp
)

target_include_directories(rhythmtrainer PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${oboe_INCLUDE_DIRS}
)

target_link_libraries(rhythmtrainer
    oboe::oboe
    log
    c++_shared
)