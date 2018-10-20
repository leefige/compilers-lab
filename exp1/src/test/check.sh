#!/usr/bin/env bash
echo "checking $1 $2 ..."

suf=$1.$2

if [ $2 == "tf" ]; then
    suf=$1
fi

diff $suf $suf.out;
if [ $? -eq 0 ]; then
    echo "AC"
    rm $suf
else
    echo "fail"
fi
