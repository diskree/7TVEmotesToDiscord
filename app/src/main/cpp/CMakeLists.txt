# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.18.1)

# Declares and names the project.

project("gifsicle")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

include_directories(gifsicle/include)
add_library( # Sets the name of the library.
        gifsicle

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        gifsicle/src/clp.c
        gifsicle/src/fmalloc.c
        gifsicle/src/giffunc.c
        gifsicle/src/gifread.c
        gifsicle/src/gifunopt.c
        gifsicle/src/gifsicle.h
        gifsicle/src/kcolor.h
        gifsicle/src/merge.c
        gifsicle/src/optimize.c
        gifsicle/src/quantize.c
        gifsicle/src/support.c
        gifsicle/src/xform.c
        gifsicle/src/gifsicle.c
        gifsicle/include/lcdf/clp.h
        gifsicle/include/lcdf/inttypes.h
        gifsicle/include/lcdfgif/gif.h
        gifsicle/include/lcdfgif/gifx.h
)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        gifsicle

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})