#! /usr/bin/fish
rm -rf build
mkdir build
cd build
cmake .. -DZ3_DIR="$Z3_DIR" -DLLVM_DIR="$LLVM_DIR/lib/cmake/llvm" -DCMAKE_BUILD_TYPE=Debug
cmake --build .

