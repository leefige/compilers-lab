extern int a;
int foo(int i) {
    if (i == 3) {
        return a;
    } else {
        return i;
    }
}

