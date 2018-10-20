package submit;

class TestFaintness {
    /**
     * In this method all variables are faint because the final value is never used.
     * Sample out is at src/test/Faintness.out
     */
    void test1() {
        int x = 2;
        int y = x + 2;
        int z = x + y;
        return;
    }

    /**
     * Write your test cases here. Create as many methods as you want.
     * Run the test from root dir using
     * ./run.sh flow.Flow submit.MySolver submit.Faintness submit.TestFaintness
     */
    /*
      void test2() {
      }
      ...
    */

    // faint: y, z
    // only use x as ret
    int foo() {
        int x = 1;
        int y = x + 2;
        int z = x + y;
        return x;
    }

    // faint: z
    // ret: y = x + 2, z of no use
    int bar() {
        int x = 1;
        int y = x + 2;
        int z = x + y;
        return y;
    }

    // faint: -
    int foobar() {
        int x = 1;
        int y = x + 2;
        int z = x + y;
        return z;
    }

    // faint: x, y
    // no ret
    void param_no_ret(int x) {
        int y = x;
    }

    // faint: y
    // only use x as ret
    int param_ret(int x) {
        int y = x + 2;
        return x;
    }

    // faint: seems no faint var
    // however, due to optimization,
    // AND use constant instead of t holding 3
    // thus t is deduced to fainted var
    int select(int c) {
        int r;
        if (c > 0) {
            r = 1;
        } else {
            int t = 3;
            int w = 1 & t;
            int k = -w;
            r = -k;
        }
        return r;
    }

    // faint: -
    // every var is used though some in branches
    int switchcase(int c) {
        int r = 0;
        int w = -1;
        switch (c) {
            case 1:
                r = 1;
                break;
            case 2:
                r = 8;
            case 3:
                r = 27;
                w = 0;
                break;
            default:
                w = 2;
        }
        return w * r;
    }

    // faint: r0, xx
    // res only use r2 and related vars: r1, p
    public static void main (String [] args) {
        TestFaintness t = new TestFaintness();
        int r0 = t.bar();
        t.test1();
        t.foobar();
        int r1 = t.switchcase(9);
        int r2;
        if (r1 > 0) {
            r2 = t.param_ret(3);
        } else {
            int p = r1 * 2;
            r2 = r1 + p * r1;
        }
        int xx = r0 & 2;
        System.out.println("Result is: " + t.select(r2));
    }

}
