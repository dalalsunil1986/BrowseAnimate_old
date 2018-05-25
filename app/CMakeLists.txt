# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

cmake_minimum_required(VERSION 3.4.1)

find_library(LIB-LOG log)
find_library(LIB-Z z)
find_library(LIB-ANDROID android)
find_library(LIB-STDC stdc++)

include_directories(libs/include)
include_directories(libs/include/jsoncpp)
include_directories(libs/include/surface)

add_library(libavcodec STATIC IMPORTED)
set_target_properties(libavcodec PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/libs/lib/${ANDROID_ABI}/libavcodec.a)

add_library(libavdevice STATIC IMPORTED)
set_target_properties(libavdevice PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/libs/lib/${ANDROID_ABI}/libavdevice.a)

add_library(libavfilter STATIC IMPORTED)
set_target_properties(libavfilter PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/libs/lib/${ANDROID_ABI}/libavfilter.a)

add_library(libavformat STATIC IMPORTED)
set_target_properties(libavformat PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/libs/lib/${ANDROID_ABI}/libavformat.a)

add_library(libpostproc STATIC IMPORTED)
set_target_properties(libpostproc PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/libs/lib/${ANDROID_ABI}/libpostproc.a)

add_library(libavutil STATIC IMPORTED)
set_target_properties(libavutil PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/libs/lib/${ANDROID_ABI}/libavutil.a)

add_library(libswresample STATIC IMPORTED)
set_target_properties(libswresample PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/libs/lib/${ANDROID_ABI}/libswresample.a)

add_library(libswscale STATIC IMPORTED)
set_target_properties(libswscale PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/libs/lib/${ANDROID_ABI}/libswscale.a)

add_library(libx264 STATIC IMPORTED)
set_target_properties(libx264 PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/libs/lib/${ANDROID_ABI}/libx264.a)

add_library(libfdk-aac STATIC IMPORTED)
set_target_properties(libfdk-aac PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/libs/lib/${ANDROID_ABI}/libfdk-aac.a)

add_library(libmp3lame STATIC IMPORTED)
set_target_properties(libmp3lame PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/libs/lib/${ANDROID_ABI}/libmp3lame.a)

add_library(json STATIC
            src/main/cpp/jsoncpp/json_reader.cpp
            src/main/cpp/jsoncpp/json_value.cpp
            src/main/cpp/jsoncpp/json_writer.cpp)

add_library(ffmpeg SHARED
            src/main/cpp/ffmpeg/ffmpeg.cpp
            src/main/cpp/ffmpeg/ffmpeg_main.cpp
            src/main/cpp/ffmpeg/ffmpeg_buffer.cpp
            src/main/cpp/ffmpeg/ffmpeg_packet.cpp
            src/main/cpp/ffmpeg/ffmpeg_surface.cpp
            src/main/cpp/ffmpeg/encoder/ffmpeg_encoder.cpp
            src/main/cpp/ffmpeg/encoder/ffmpeg_encoder_audio.cpp
            src/main/cpp/ffmpeg/encoder/ffmpeg_encoder_video.cpp
            src/main/cpp/ffmpeg/decoder/ffmpeg_decoder.cpp
            src/main/cpp/ffmpeg/decoder/ffmpeg_decoder_audio.cpp
            src/main/cpp/ffmpeg/decoder/ffmpeg_decoder_video.cpp)

target_link_libraries(ffmpeg ${LIB-LOG} ${LIB-Z} ${LIB-ANDROID} ${LIB-STDC} json libavformat libavcodec libavdevice libavfilter libpostproc libswresample libswscale libavutil libx264 libfdk-aac libmp3lame)