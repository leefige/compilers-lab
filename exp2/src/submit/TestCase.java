package submit;

public class TestCase {
    int binary(int a) {
        System.out.println("binary: a=" + a);
        Integer aa = new Integer(a);
        int b = 3 + 1;
        Integer bb = new Integer(b);
        bb += aa;
        aa += bb;
        Integer c = (aa + bb);
        System.out.println(c.toString());
        return c;
    }

    int unary(int a) {
        System.out.println("unary: a=" + a);
        new Integer(a).toString();
        System.out.println(new Integer(-a));
        return a;
    }

    void cmp() {
        Integer set = new Integer(0);
        set = null;
        if (set != null) {
            set += 1;
            set.toString();
        } else {
            set = new Integer(0);
            set = 1;
            set.toString();
        }
        System.out.println("cmp: " + set.toString());
    }

    void cnstfld() {
        int a = 1;
        int b = 2;
        int c = 3;
        int d = a + b;
        int e = a + c;
        int f = a + d;
        int g = f + b;
        System.out.println("const fold: g=" + new Integer(g).toString());
    }

    void common(int b, int c) {
        int a = b + c;
        System.out.println("common : a=" + a);

        if (a > 0) {
            int d = b + c;
            int e = d << 1;
            System.out.println("e=" + e);
        } else {
            int f = b + c;
            int g = f << 4;
            String s = new Integer(g).toString();
            System.out.println("s=" + s);
        }
        int k = b + c;

        for (int i = 0; i < k; i++) {
            System.out.println("i=" + i);
        }
    }

    int faint(int a) {
        System.out.println("faint: a=" + a);
        int b = a + 1;
        int c = b + 1;
        int d = b + c;
        int e = a + c;
        b = a * 8;
        System.out.println("b=" + b);
        return a;
    }

    public static void main(String[] args) {
        TestCase t = new TestCase();
        int a = t.binary(t.unary(7));
        t.cmp();
        new Integer(a).toString();
        t.cnstfld();
        t.common(3, 4);
        System.out.println(t.faint(9));
    }
}
