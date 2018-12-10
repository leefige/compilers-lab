#! /usr/bin/fish
for file in (ls test)
    ./build/z3_expr test/$file
end

