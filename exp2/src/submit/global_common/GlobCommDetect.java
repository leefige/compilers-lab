package submit.global_common;

// some useful things to import. add any additional imports you need.

import flow.Flow;
import joeq.Compiler.Quad.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;

import java.util.*;

/**
 * Skeleton class for implementing a reaching definition analysis
 * using the Flow.Analysis interface.
 */
public class GlobCommDetect implements Flow.Analysis {

    // expression object
    public static class Exprsn {
        private String op1;
        private String op2;
        private Operator oprt;
        private int qid;

        /**
         * Methods from the Flow.DataflowObject interface.
         * See Flow.java for the meaning of these methods.
         * These need to be filled in.
         */

        public static Exprsn getExp(Quad q) {
            Operator.Binary opr = (Operator.Binary) q.getOperator();
            Operand o1 = Operator.Binary.getSrc1(q);
            Operand o2 = Operator.Binary.getSrc2(q);
            if (! (o1 instanceof Operand.RegisterOperand && o2 instanceof Operand.RegisterOperand)) {
                return null;
            }
            String op1 = ((RegisterOperand) o1).getRegister().toString();
            String op2 = ((RegisterOperand) o2).getRegister().toString();
            return new Exprsn(opr, op1, op2, q.getID());
        }

        private Exprsn(Operator opr, String o1, String o2, int id) {
            oprt = opr;
            op1 = o1;
            op2 = o2;
            qid = id;
        }

        public void copy(Flow.DataflowObject o) {
            Exprsn t = (Exprsn) o;
            op1 = t.op1;
            op2 = t.op2;
            oprt = t.oprt;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Exprsn) {
                Exprsn a = (Exprsn) o;
                return op1.equals(a.op1) && op2.equals(a.op2) &&
                        oprt.equals(a.oprt) && qid == a.qid;
            }
            return false;
        }

        /**
         * toString() method for the dataflow objects which is used
         * by postprocess() below.  The format of this method must
         * be of the form "[ID0, ID1, ID2, ...]", where each ID is
         * the identifier of a quad defining some register, and the
         * list of IDs must be sorted.  See src/test/test.rd.out
         * for example output of the analysis.  The output format of
         * your reaching definitions analysis must match this exactly.
         */
        @Override
        public String toString() {
            return oprt.toString() + ":" + op1 + "," + op2;
        }

        public String getOp1() {
            return op1;
        }

        public String getOp2() {
            return op2;
        }

        public Operator getOprt() {
            return oprt;
        }

        public int getQid() {
            return qid;
        }
    }

    public class ExpSet implements Flow.DataflowObject {
        private Set<Exprsn> set;

        /**
         * Methods from the Flow.DataflowObject interface.
         * See Flow.java for the meaning of these methods.
         * These need to be filled in.
         */
        public ExpSet() {
            set = new TreeSet<Exprsn>();
        }

        public void setToTop() {
            set = new TreeSet<Exprsn>(universalSet);
        }

        public void setToBottom() {
            set = new TreeSet<Exprsn>();
        }

        /**
         * Meet is a intersection
         */
        public void meetWith(Flow.DataflowObject o) {
            ExpSet t = (ExpSet) o;
            this.set.retainAll(t.set);
        }

        public void copy(Flow.DataflowObject o) {
            ExpSet t = (ExpSet) o;
            set = new TreeSet<Exprsn>(t.set);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ExpSet) {
                ExpSet a = (ExpSet) o;
                return set.equals(a.set);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return set.hashCode();
        }

        /**
         * toString() method for the dataflow objects which is used
         * by postprocess() below.  The format of this method must
         * be of the form "[ID0, ID1, ID2, ...]", where each ID is
         * the identifier of a quad defining some register, and the
         * list of IDs must be sorted.  See src/test/test.rd.out
         * for example output of the analysis.  The output format of
         * your reaching definitions analysis must match this exactly.
         */
        @Override
        public String toString() {
            return set.toString();
        }

        public void useExp(Quad q) {
            if (q.getOperator() instanceof Operator.Binary) {
                Exprsn exp = Exprsn.getExp(q);
                if (exp != null) {
                    set.add(exp);
                }
            }
        }

        public void killExp(Exprsn exp) {
            if (exp != null) {
                set.remove(exp);
            }
        }
    }

    /**
     * Class for the dataflow objects in the ReachingDefs analysis.
     * You are free to change this class or move it to another file.
     */
    public static Set<Exprsn> universalSet = new TreeSet<Exprsn>();
    private static HashMap<String, Set<Exprsn>> usedregExpMap;
    /**
     * Dataflow objects for the interior and entry/exit points
     * of the CFG. in[ID] and out[ID] store the entry and exit
     * state for the input and output of the quad with identifier ID.
     * <p>
     * You are free to modify these fields, just make sure to
     * preserve the data printed by postprocess(), which relies on these.
     */
    private ExpSet[] in, out;
    private ExpSet entry, exit;
    private TransferFunction transferfn = new TransferFunction();

    /**
     * This method initializes the datflow framework.
     *
     * @param cfg The control flow graph we are going to process.
     */
    public void preprocess(ControlFlowGraph cfg) {
        // this line must come first.
        System.out.println("Method: " + cfg.getMethod().getName().toString());
        usedregExpMap = new HashMap<String, Set<Exprsn>>();

        // get the amount of space we need to allocate for the in/out arrays.
        QuadIterator qit = new QuadIterator(cfg);
        int max = 0;
        while (qit.hasNext()) {
            int id = qit.next().getID();
            if (id > max)
                max = id;
        }
        max += 1;

        // allocate the in and out arrays.
        in = new ExpSet[max];
        out = new ExpSet[max];

        // initialize the contents of in and out.
        qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            int id = qit.next().getID();
            in[id] = new ExpSet();
            out[id] = new ExpSet();
        }

        qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            Quad q = qit.next();
            if (q.getOperator() instanceof Operator.Binary) {
                Exprsn exp = Exprsn.getExp(q);
                universalSet.add(exp);
                for (RegisterOperand use : q.getUsedRegisters()) {
                    String key = use.getRegister().toString();
                    if (!usedregExpMap.containsKey(key)) {
                        usedregExpMap.put(key, new HashSet<Exprsn>());
                    }
                    Set<Exprsn> myset = usedregExpMap.get(key);
                    myset.add(exp);
                    usedregExpMap.put(key, myset);
                }
            }
        }
        // initialize the entry and exit points.
        transferfn.val = new ExpSet();
        entry = new ExpSet();
        exit = new ExpSet();
    }

    /**
     * This method is called after the fixpoint is reached.
     * It must print out the dataflow objects associated with
     * the entry, exit, and all interior points of the CFG.
     * Unless you modify in, out, entry, or exit you shouldn't
     * need to change this method.
     *
     * @param cfg Unused.
     */
    public void postprocess(ControlFlowGraph cfg) {
        System.out.println("entry: " + entry.toString());
        for (int i = 1; i < in.length; i++) {
            if (in[i] != null) {
                System.out.println(i + " in:  " + in[i].toString());
                System.out.println(i + " out: " + out[i].toString());
            }
        }
        System.out.println("exit: " + exit.toString());
    }

    /**
     * Other methods from the Flow.Analysis interface.
     * See Flow.java for the meaning of these methods.
     * These need to be filled in.
     */
    public boolean isForward() {
        return true;
    }

    public Flow.DataflowObject getEntry() {
        Flow.DataflowObject result = newTempVar();
        result.copy(entry);
        return result;
    }

    public void setEntry(Flow.DataflowObject value) {
        entry.copy(value);
    }

    public Flow.DataflowObject getExit() {
        Flow.DataflowObject result = newTempVar();
        result.copy(exit);
        return result;
    }

    public void setExit(Flow.DataflowObject value) {
        exit.copy(value);
    }

    public Flow.DataflowObject getIn(Quad q) {
        Flow.DataflowObject result = newTempVar();
        result.copy(in[q.getID()]);
        return result;
    }

    public Flow.DataflowObject getOut(Quad q) {
        Flow.DataflowObject result = newTempVar();
        result.copy(out[q.getID()]);
        return result;
    }

    public void setIn(Quad q, Flow.DataflowObject value) {
        in[q.getID()].copy(value);
    }

    public void setOut(Quad q, Flow.DataflowObject value) {
        out[q.getID()].copy(value);
    }

    public Flow.DataflowObject newTempVar() {
        return new ExpSet();
    }

    public void processQuad(Quad q) {
        transferfn.val.copy(in[q.getID()]);
        transferfn.visitQuad(q);
        out[q.getID()].copy(transferfn.val);
    }

    /* The QuadVisitor that actually does the computation */
    public static class TransferFunction extends QuadVisitor.EmptyVisitor {
        ExpSet val;

        @Override
        public void visitQuad(Quad q) {
            for (RegisterOperand def : q.getDefinedRegisters()) {
                String key = def.getRegister().toString();
                //kill all the definitions with respect to the assigned register
                Iterator<Exprsn> iter = usedregExpMap.get(key).iterator();
                while (iter.hasNext())
                    val.killExp(iter.next());
            }
            val.useExp(q);
        }
    }
}
