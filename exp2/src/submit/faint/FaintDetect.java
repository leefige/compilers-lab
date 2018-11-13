package submit.faint;

import flow.Faintness;
import flow.Flow;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.*;

import java.util.*;


/**
 * This is just a similar class to Faintness
 * I have to copy it since the Faintness::VarSet::isFaint() is package-private
 * and thus I cannot know about the set of VarSet from package 'submit'
 * However, a getter should be public
 * What's more, the ctor of Faintness::VarSet is also package-private...
 * I cannot even extend it, so I have to copy it
 *
 * By the way, I also modified the quad visiting method:
 *      if the quad is NULL_CHECK, do not 'use' the reg being checked
 *      (because it's actually not used)
 * in order to perform more optimization
 * */
public class FaintDetect implements Flow.Analysis {

    public Map<jq_Method, Map<Integer, VarSet>> finalOut = new HashMap<jq_Method, Map<Integer, VarSet>>();
    private VarSet[] in, out;
    private VarSet entry, exit;

    /**
     * This method initializes the datflow framework.
     *
     * @param cfg The control flow graph we are going to process.
     */
    public void preprocess(ControlFlowGraph cfg) {
        // this line must come first.
        // get the amount of space we need to allocate for the in/out arrays.
        QuadIterator qit = new QuadIterator(cfg);
        int max = 0;
        while (qit.hasNext()) {
            int id = qit.next().getID();
            if (id > max)
                max = id;
        }
        max += 1;

        // Begin computing the universal set. This needs to be done before
        // any VarSet objects are created.
        Set<String> s = new TreeSet<String>();
        VarSet.universalSet = s;

        /* Arguments are always there. */
        int numargs = cfg.getMethod().getParamTypes().length;
        for (int i = 0; i < numargs; i++) {
            s.add("R" + i);
        }

        qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            Quad q = qit.next();
            for (Operand.RegisterOperand def : q.getDefinedRegisters()) {
                s.add(def.getRegister().toString());
            }
            for (Operand.RegisterOperand use : q.getUsedRegisters()) {
                s.add(use.getRegister().toString());
            }
        }
        // End computing the universal set

        // allocate the in and out arrays.
        in = new VarSet[max];
        out = new VarSet[max];

        // initialize the contents of in and out.
        qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            int id = qit.next().getID();
            in[id] = new VarSet();
            out[id] = new VarSet();
        }

        // initialize the entry and exit points.
        entry = new VarSet();
        exit = new VarSet();

        // Most of my initialization is above (computing the universal set)
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
        Map<Integer, VarSet> curOut = new TreeMap<Integer, VarSet>();
        QuadIterator qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            Quad q = qit.next();
            int qid = q.getID();
            curOut.put(qid, (VarSet) getOut(q));
        }
        finalOut.put(cfg.getMethod(), curOut);
    }

    public void processQuad(Quad q) {
        VarSet val = (VarSet) getOut(q);
        // Move non-faintness over to used registers for move and binary operators
        if (q.getOperator() instanceof Operator.Move || q.getOperator() instanceof Operator.Binary) {
            // Get the defined register (we know there's exactly one)
            Operand.RegisterOperand def = q.getDefinedRegisters().iterator().next();
            boolean defWasFaint = val.isFaint(def.getRegister().toString());
            // Make the defined register faint
            val.setFaint(def.getRegister().toString());

            // If the defined register was not faint, make the used registers not faint
            if (!defWasFaint) {
                for (Operand.RegisterOperand use : q.getUsedRegisters()) {
                    val.setNotFaint(use.getRegister().toString());
                }
            }
        } else if (q.getOperator() instanceof Operator.NullCheck) {
            // Get the defined register (we know there's exactly one: T-1)
            for (Operand.RegisterOperand def : q.getDefinedRegisters()) {
                val.setFaint(def.getRegister().toString());
            }
            // do nothing with used, because we know it will only be checked
        } else {
            // For all other quads behave similarly to liveness analysis
            for (Operand.RegisterOperand def : q.getDefinedRegisters()) {
                val.setFaint(def.getRegister().toString());
            }
            for (Operand.RegisterOperand use : q.getUsedRegisters()) {
                val.setNotFaint(use.getRegister().toString());
            }
        }
        setIn(q, val);
    }

    /**
     * Other methods from the Flow.Analysis interface.
     * See Flow.java for the meaning of these methods.
     * <p>
     * These implementations essentially copied from flow.Livenesss
     */
    public boolean isForward() {
        return false;
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
        return new VarSet();
    }

    /**
     * Almost a copy of Faintness.VarSet
     * Just to avoid some non-public method problem
     */
    public static class VarSet implements Flow.DataflowObject {
        static Set<String> universalSet;
        private Set<String> set;

        /**
         * The default value has all registers faint
         */
        public VarSet() {
            set = new TreeSet<String>(universalSet);
        }

        /**
         * Methods from the Flow.DataflowObject interface.
         * See Flow.java for the meaning of these methods.
         * <p>
         * Most are similar to the methods in flow.Liveness.VarSet
         */
        public void setToTop() {
            set = new TreeSet<String>(universalSet);
        }

        public void setToBottom() {
            set = new TreeSet<String>();
        }

        /**
         * Meet is an intersection
         */
        public void meetWith(Flow.DataflowObject o) {
            VarSet a = (VarSet) o;
            set.retainAll(a.set); // strange name for intersect
        }

        public void copy(Flow.DataflowObject o) {
            VarSet a = (VarSet) o;
            set = new TreeSet<String>(a.set);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof VarSet) {
                VarSet a = (VarSet) o;
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
         * be of the form "[REG0, REG1, REG2, ...]", where each REG is
         * the identifier of a register, and the list of REGs must be sorted.
         * See src/test/TestFaintness.out for example output of the analysis.
         * The output format of your reaching definitions analysis must
         * match this exactly.
         */
        @Override
        public String toString() {
            return set.toString();
        }

        private void setFaint(String v) {
            set.add(v);
        }

        private void setNotFaint(String v) {
            set.remove(v);
        }

        public boolean isFaint(String v) {
            return set.contains(v);
        }
    }


}
