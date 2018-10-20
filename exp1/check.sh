#!/bin/sh

echo "building..."
rm -r build > /dev/null
make > /dev/null

echo "testing..."
tpe="flow.ConstantProp"
tar="test.$1"
file="$1.$2"

if [ $2 = "lv" ] ; then
    tpe="flow.Liveness"
elif [ $2 = "rd" ] ; then
    tpe="submit.ReachingDefs"
elif [ $2 = "tf" ] ; then
    tpe="submit.Faintness"
    tar="submit.$1"
    file="$1"
fi

rm res/$file > /dev/null 2>&1
./run.sh flow.Flow submit.MySolver $tpe $tar > res/$file

diff res/$file src/test/$file.out;
if [ $? -eq 0 ]; then
    echo "AC"
else
    echo "fail"
    rm res/$1.$2
fi

