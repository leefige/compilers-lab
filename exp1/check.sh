#!/bin/sh

echo "building..."
rm -r build > /dev/null
make > /dev/null

echo "testing..."
type="ConstantProp"

if [ $2 = "lv" ] ; then
    type="Liveness"
elif [ $2 = "rd" ] ; then
    type="ReachingDefs"
elif [ $2 = "ft" ] ; then
    type="Faintness"
fi

rm res/$1.$2 > /dev/null 2>&1
./run.sh flow.Flow submit.MySolver flow.$type test.$1 > res/$1.$2

diff res/$1.$2 src/test/$1.$2.out;
if [ $? -eq 0 ]; then
    echo "AC"
else
    echo "fail"
    rm res/$1.$2
fi

