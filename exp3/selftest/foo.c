extern int a;
int foo(int i) {
    if (i == 3) {
      int a = 1; 
      int b = 2;
      int c = a + b;
      return c;
    } else {
      int d = i * a;
      return d;
    }
}

