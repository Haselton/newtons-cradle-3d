cmake_minimum_required(VERSION 3.18.1)
project(cradle_physics LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(NEWTON_BUILD_CORE_ONLY ON CACHE BOOL "" FORCE)
set(NEWTON_BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(NEWTON_BUILD_PROFILER OFF CACHE BOOL "" FORCE)
set(NEWTON_WITH_SSE_PLUGIN OFF CACHE BOOL "" FORCE)
set(NEWTON_WITH_SSE4_PLUGIN OFF CACHE BOOL "" FORCE)
set(NEWTON_WITH_AVX_PLUGIN OFF CACHE BOOL "" FORCE)
set(NEWTON_WITH_AVX2_PLUGIN OFF CACHE BOOL "" FORCE)

set(NEWTON_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/newton-dynamics/newton-3.14")
add_subdirectory("${NEWTON_ROOT}/sdk" "${CMAKE_CURRENT_BINARY_DIR}/newton-sdk")

add_library(cradle_physics SHARED cradle_physics.cpp)
target_include_directories(cradle_physics PRIVATE "${NEWTON_ROOT}/sdk/dgNewton")
target_compile_definitions(cradle_physics PRIVATE _NEWTON_STATIC_LIB _POSIX_VER DG_SCALAR_VECTOR_CLASS)

# Newton 3.14 predates modern Android/NDK platform detection. Apply its
# portable POSIX/scalar configuration to the SDK targets as well.
target_compile_definitions(dgCore PUBLIC _POSIX_VER DG_SCALAR_VECTOR_CLASS)
target_compile_definitions(newton PUBLIC _POSIX_VER DG_SCALAR_VECTOR_CLASS)
target_compile_definitions(dgPhysics PUBLIC _POSIX_VER DG_SCALAR_VECTOR_CLASS)
target_link_libraries(cradle_physics PRIVATE newton android log)
