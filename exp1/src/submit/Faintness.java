package submit;

// some useful things to import. add any additional imports you need.
import joeq.Compiler.Quad.*;
import flow.Flow;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Main.Helper;

import java.util.Set;
import java.util.TreeSet;

/**
 * Skeleton class for implementing a faint variable analysis
 * using the Flow.Analysis interface.
 */
public class Faintness implements Flow.Analysis {

    /**
     * Class for the dataflow objects in the Faintness analysis.
     * You are free to change this class or move it to another file.
     */
    public static class VarSet implements Flow.DataflowObject {
        private Set<String> set;
        public static Set<String> universalSet;
        public VarSet() { set = new TreeSet<String>(); }
        /**
         * Methods from the Flow.DataflowObject interface.
         * See Flow.java for the meaning of these methods.
         * These need to be filled in.
         */
        // top should be the universal set
        public void setToTop() { set = new TreeSet<String>(universalSet); }
        public void setToBottom() { set = new TreeSet<String>(); }

        public void meetWith(Flow.DataflowObject o)
        {
            VarSet a = (VarSet)o;
            // get the intersect
            set.retainAll(a.set);
        }

        public void copy(Flow.DataflowObject o)
        {
            VarSet a = (VarSet) o;
            set = new TreeSet<String>(a.set);
        }


        /**
         * toString() method for the dataflow objects which is used
         * by postprocess() below.  The format of this method must
         * be of the form "[REG0, REG1, REG2, ...]", where each REG is
         * the identifier of a register, and the list of REGs must be sorted.
         * See src/test/TestFaintness.out for example output of the analysis.
         * The output format of your reaching definitions analysis must
         * match this exactly.
         */
        @Override
        public boolean equals(Object o)
        {
            if (o instanceof VarSet)
            {
                VarSet a = (VarSet) o;
                return set.equals(a.set);
            }
            return false;
        }
        @Override
        public int hashCode() {
            return set.hashCode();
        }
        @Override
        public String toString()
        {
            return set.toString();
        }

        public void faintVar(String d, String u) {
            // pass faintness
            if (!set.contains(d)) {
                wakeVar(u);
            }
        }

        public void wakeVar(String v) {
            if (set.contains(v)) {
                set.remove(v);
            }
        }
    }

    /**
     * Dataflow objects for the interior and entry/exit points
     * of the CFG. in[ID] and out[ID] store the entry and exit
     * state for the input and output of the quad with identifier ID.
     *
     * You are free to modify these fields, just make sure to
     * preserve the data printed by postprocess(), which relies on these.
     */
    private VarSet[] in, out;
    private VarSet entry, exit;

    /**
     * This method initializes the datflow framework.
     *
     * @param cfg  The control flow graph we are going to process.
     */
    public void preprocess(ControlFlowGraph cfg) {
        // this line must come first.
        System.out.println("Method: "+cfg.getMethod().getName().toString());

        // get the amount of space we need to allocate for the in/out arrays.
        QuadIterator qit = new QuadIterator(cfg);
        int max = 0;
        while (qit.hasNext()) {
            int id = qit.next().getID();
            if (id > max) 
                max = id;
        }
        max += 1;

        // get all regs appeared
        Set<String> s = new TreeSet<String>();
        VarSet.universalSet = s;

        /* Arguments are always there. */
        int numargs = cfg.getMethod().getParamTypes().length;
        for (int i = 0; i < numargs; i++) {
            s.add("R"+i);
        }

        qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            Quad q = qit.next();
            for (RegisterOperand def : q.getDefinedRegisters()) {
                s.add(def.getRegister().toString());
            }
            for (RegisterOperand use : q.getUsedRegisters()) {
                s.add(use.getRegister().toString());
            }
        }

        // allocate the in and out arrays.
        in = new VarSet[max];
        out = new VarSet[max];

        // initialize the contents of in and out.
        qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            int i = qit.next().getID();
            in[i] = new VarSet();
            out[i] = new VarSet();
        }

        // initialize the entry and exit points.
        entry = new VarSet();
        exit = new VarSet();
        exit.setToTop();

        /************************************************
         * Your remaining initialization code goes here *
         ************************************************/
        transferfn.val = new VarSet();
        System.out.println("Initialization completed.");
//        System.out.println("TOP: " + VarSet.universalSet.toString());
    }

    /**
     * This method is called after the fixpoint is reached.
     * It must print out the dataflow objects associated with
     * the entry, exit, and all interior points of the CFG.
     * Unless you modify in, out, entry, or exit you shouldn't
     * need to change this method.
     *
     * @param cfg  Unused.
     */
    public void postprocess (ControlFlowGraph cfg) {
        System.out.println("entry: " + entry.toString());
        for (int i=1; i<in.length; i++) {
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
    public boolean isForward () { return false; }

    /* Routines for interacting with dataflow values. */

    public Flow.DataflowObject getEntry()
    {
        Flow.DataflowObject result = newTempVar();
        result.copy(entry);
        return result;
    }
    public Flow.DataflowObject getExit()
    {
        Flow.DataflowObject result = newTempVar();
        result.copy(exit);
        return result;
    }
    public Flow.DataflowObject getIn(Quad q)
    {
        Flow.DataflowObject result = newTempVar();
        result.copy(in[q.getID()]);
        return result;
    }
    public Flow.DataflowObject getOut(Quad q)
    {
        Flow.DataflowObject result = newTempVar();
        result.copy(out[q.getID()]);
        return result;
    }
    public void setIn(Quad q, Flow.DataflowObject value)
    {
        in[q.getID()].copy(value);
    }
    public void setOut(Quad q, Flow.DataflowObject value)
    {
        out[q.getID()].copy(value);
    }
    public void setEntry(Flow.DataflowObject value)
    {
        entry.copy(value);
    }
    public void setExit(Flow.DataflowObject value)
    {
        exit.copy(value);
    }

    public Flow.DataflowObject newTempVar() {
        VarSet res = new VarSet();
        res.setToTop();
        return res;
    }

    /* Actually perform the transfer operation on the relevant
     * quad. */

    private TransferFunction transferfn = new TransferFunction ();
    public void processQuad(Quad q) {
        transferfn.val.copy(out[q.getID()]);
        Helper.runPass(q, transferfn);
        in[q.getID()].copy(transferfn.val);
    }

    /* The QuadVisitor that actually does the computation */
    public static class TransferFunction extends QuadVisitor.EmptyVisitor {
        VarSet val;

        @Override
        public void visitMove(Quad q) {
            String dst = Operator.Move.getDest(q).getRegister().toString();
            Operand op = Operator.Move.getSrc(q);
            if (op instanceof RegisterOperand) {
                String use = ((RegisterOperand) op).getRegister().toString();
                val.faintVar(dst, use);
            }
        }

        @Override
        public void visitBinary(Quad q) {
            Operand op1 = Operator.Binary.getSrc1(q);
            Operand op2 = Operator.Binary.getSrc2(q);
            String dst = Operator.Binary.getDest(q).getRegister().toString();

            if (op1 instanceof RegisterOperand) {
                String use = ((RegisterOperand) op1).getRegister().toString();
                val.faintVar(dst, use);
            }

            if (op2 instanceof RegisterOperand) {
                String use = ((RegisterOperand) op2).getRegister().toString();
                val.faintVar(dst, use);
            }
        }

        @Override
        public void visitQuad(Quad q) {
            Operator op = q.getOperator();
            // has been parsed, just let go
            if (!(op instanceof Operator.Move || op instanceof Operator.Binary)) {
                for (RegisterOperand use : q.getUsedRegisters()) {
                    val.wakeVar(use.getRegister().toString());
                }
            }
        }
    }
}
