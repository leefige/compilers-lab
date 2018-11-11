package test;

import com.sun.org.apache.xerces.internal.impl.xs.SchemaSymbols;
import joeq.Compiler.Quad.Operator;

import java.util.Set;
import java.util.TreeSet;

public class TestBinary {
    int binary(int a) {
        Integer aa = new Integer(a);
        int b = 3 + 1;
        Integer bb = new Integer(b);
        bb += aa;
        aa += bb;
        Integer c = (aa + bb);
        c.toString();
        return c;
    }

    int unary(int a) {
        new Integer(a).toString();
        new Integer(-a);
        return a;
    }

    void cmp() {
        Integer set = new Integer(0);
        if (set != null) {
            set.toString();
        }
        set.toString();
    }

    void cnstfld() {
        int a = 1;
        int b = 2;
        int c = 3;
        int d = a + b;
        int e = a + c;
        int f = a + d;
        int g = f + b;
        System.out.println(new Integer(g).toString());
    }

    public void common(int b, int c) {
        int a = b + c;
        System.out.println(a);

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

    public static void main() {
        TestBinary t = new TestBinary();
//        int a = t.binary(t.unary(7));
//        t.cmp();
//        new Integer(a).toString();
        t.common(3, 4);
    }
}
