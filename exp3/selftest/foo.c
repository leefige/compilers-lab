extern int a;
int foo(int i) {
    if (i == 3) {
      int a = 1; 
      int b = 2;
      int c = a + b;
      c = c - i;
      return c;
    } else {
      int d = i * a;
      int e = 10 / i;
      int f = 2 << i;
      int g = 16 >> i;
      return d;
    }
}

