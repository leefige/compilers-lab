package test;

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
}
