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

void bar(int* b) {
  int tmp = *b << 4;
  int aa = 32;
  aa = aa & *b;
  aa = 8 | tmp;
  tmp = *b ^ aa;
  *b = tmp;
}

int foobar() {
  int k = foo(a);
  bar(&k);
  return k;
}

