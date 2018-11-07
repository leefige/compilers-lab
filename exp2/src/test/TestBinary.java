package test;

import joeq.Compiler.Quad.Operator;

import java.util.Set;
import java.util.TreeSet;

public class TestBinary {
    int binary(int a) {
        Integer aa = new Integer(a);
        int b = 3 + 1;
        Integer bb = new Integer(b);
        bb *= aa;
        aa *= bb;
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
}
