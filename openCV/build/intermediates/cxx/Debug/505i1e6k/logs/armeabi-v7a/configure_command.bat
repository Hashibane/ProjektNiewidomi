@echo off
"X:\\AndroidSDK\\cmake\\3.22.1\\bin\\cmake.exe" ^
  "-HC:\\Users\\hook0\\AndroidStudioProjects\\Cybertech22\\openCV\\libcxx_helper" ^
  "-DCMAKE_SYSTEM_NAME=Android" ^
  "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON" ^
  "-DCMAKE_SYSTEM_VERSION=21" ^
  "-DANDROID_PLATFORM=android-21" ^
  "-DANDROID_ABI=armeabi-v7a" ^
  "-DCMAKE_ANDROID_ARCH_ABI=armeabi-v7a" ^
  "-DANDROID_NDK=X:\\AndroidSDK\\ndk\\25.1.8937393" ^
  "-DCMAKE_ANDROID_NDK=X:\\AndroidSDK\\ndk\\25.1.8937393" ^
  "-DCMAKE_TOOLCHAIN_FILE=X:\\AndroidSDK\\ndk\\25.1.8937393\\build\\cmake\\android.toolchain.cmake" ^
  "-DCMAKE_MAKE_PROGRAM=X:\\AndroidSDK\\cmake\\3.22.1\\bin\\ninja.exe" ^
  "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=C:\\Users\\hook0\\AndroidStudioProjects\\Cybertech22\\openCV\\build\\intermediates\\cxx\\Debug\\505i1e6k\\obj\\armeabi-v7a" ^
  "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=C:\\Users\\hook0\\AndroidStudioProjects\\Cybertech22\\openCV\\build\\intermediates\\cxx\\Debug\\505i1e6k\\obj\\armeabi-v7a" ^
  "-DCMAKE_BUILD_TYPE=Debug" ^
  "-BC:\\Users\\hook0\\AndroidStudioProjects\\Cybertech22\\openCV\\.cxx\\Debug\\505i1e6k\\armeabi-v7a" ^
  -GNinja ^
  "-DANDROID_STL=c++_shared"
