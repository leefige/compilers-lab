package submit;

import flow.Flow;
import flow.Liveness;
import joeq.Class.jq_Reference;
import joeq.Compiler.Quad.*;
import joeq.Compiler.Quad.Operand.IConstOperand;
import joeq.Compiler.Quad.Operand.AConstOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operand.ConditionOperand;

import joeq.Main.Helper;

import java.util.*;

public class NonNull implements Flow.Analysis {

    public static class SingleVar implements Flow.DataflowObject {
        public boolean checked;

        public SingleVar() {
            setToTop();
        }

        public void setToTop() {
            checked = true;
        }

        public void setToBottom() {
            checked = false;
        }

        public void meetWith(Flow.DataflowObject o) {
            SingleVar a = (SingleVar) o;
            checked = checked && a.checked;
        }

        public void copy(Flow.DataflowObject o) {
            SingleVar a = (SingleVar) o;
            checked = a.checked;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof SingleVar) {
                SingleVar a = (SingleVar) o;
                return checked == a.checked;
            }
            return false;
        }

        // same as Boolean
        @Override
        public int hashCode() {
            return checked ? 1231 : 1237;
        }

        @Override
        public String toString() {
            if (checked) {
                return "Checked";
            } else {
                return "Unknown";
            }
        }

        public void reDefine() {
            checked = false;
        }

        public void check() {
            checked = true;
        }
    }

    public static class CheckTable implements Flow.DataflowObject {
        /* 'core' is used to keep track of which variables we need to
         * track */
        private static Set<String> core = new HashSet<String>();
        private SortedMap<String, SingleVar> map;

        public CheckTable() {
            map = new TreeMap<String, SingleVar>();
            for (String key : core) {
                map.put(key, new SingleVar());
            }
        }

        public static void reset() {
            core.clear();
        }

        public static void register(String key) {
            core.add(key);
        }

        public void setToTop() {
            for (SingleVar lattice : map.values()) {
                lattice.setToTop();
            }
        }

        public void setToBottom() {
            for (SingleVar lattice : map.values()) {
                lattice.setToBottom();
            }
        }

        public void meetWith(Flow.DataflowObject o) {
            CheckTable a = (CheckTable) o;
            for (Map.Entry<String, SingleVar> e : a.map.entrySet()) {
                SingleVar mine = map.get(e.getKey());
                mine.meetWith(e.getValue());
            }
        }

        public void copy(Flow.DataflowObject o) {
            CheckTable a = (CheckTable) o;
            for (Map.Entry<String, SingleVar> e : a.map.entrySet()) {
                SingleVar mine = map.get(e.getKey());
                mine.copy(e.getValue());
            }
        }

        @Override
        public String toString() {
            return map.toString();
        }

        public SingleVar get(String key) {
            return map.get(key);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof CheckTable) {
                return map.equals(((CheckTable) o).map);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return map.hashCode();
        }

        public boolean isChecked(String v) {
            return map.get(v).checked;
        }

        public void killVar(String v) {
            SingleVar var = map.get(v);
            var.reDefine();
        }

        public void checkVar(String v) {
            SingleVar var = map.get(v);
            var.check();
        }
    }

    final public static int LEVEL_NORMAL = 0;
    final public static int LEVEL_HIGH = 1;
    private int level;
    private CheckTable[] in, out;
    private CheckTable entry, exit;
    private TransferFunction transferfn;

    public NonNull() {
        level = LEVEL_NORMAL;
        transferfn = new TransferFunction(level);
    }

    public NonNull(int lv) {
        switch (lv) {
            case LEVEL_HIGH:
                level = lv;
                break;
            case LEVEL_NORMAL:
            default:
                level = LEVEL_NORMAL;
                break;
        }
        transferfn = new TransferFunction(level);
    }

    public void preprocess(ControlFlowGraph cfg) {
//        System.out.println("Method: " + cfg.getMethod().getName().toString());
        System.out.print(cfg.getMethod().getName().toString());
        /* Generate initial conditions. */
        QuadIterator qit = new QuadIterator(cfg);
        int max = 0;
        while (qit.hasNext()) {
            int x = qit.next().getID();
            if (x > max) max = x;
        }
        max += 1;
        in = new CheckTable[max];
        out = new CheckTable[max];
        qit = new QuadIterator(cfg);

        CheckTable.reset();

        /* Arguments are always there. */
        int numargs = cfg.getMethod().getParamTypes().length;
        for (int i = 0; i < numargs; i++) {
            CheckTable.register("R" + i);
        }

        while (qit.hasNext()) {
            Quad q = qit.next();
            for (RegisterOperand def : q.getDefinedRegisters()) {
                CheckTable.register(def.getRegister().toString());
            }
            for (RegisterOperand use : q.getUsedRegisters()) {
                CheckTable.register(use.getRegister().toString());
            }
        }

        entry = new CheckTable();
        entry.setToBottom();

        exit = new CheckTable();
        transferfn.val = new CheckTable();
        for (int i = 0; i < in.length; i++) {
            in[i] = new CheckTable();
            out[i] = new CheckTable();
        }

//        System.out.println("Initialization completed. VarSet.core: " + CheckTable.core);
    }

    public void postprocess(ControlFlowGraph cfg) {
//        System.out.println("entry: " + entry.toString());
//        for (int i = 0; i < in.length; i++) {
//            System.out.println(i + " in:  " + in[i].toString());
//            System.out.println(i + " out: " + out[i].toString());
//        }
//        System.out.println("exit: " + exit.toString());
        Set<Integer> redundant = new TreeSet<Integer>();
        QuadIterator qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            Quad q = qit.next();
            Operator oprt = q.getOperator();
            if (oprt instanceof Operator.NullCheck) {
                String reg = q.getUsedRegisters().get(0).getRegister().toString();
                if (in[q.getID()].isChecked(reg)) {
                    redundant.add(q.getID());
                }
            }
        }
//        System.out.println(redundant);
        for (Integer qid : redundant) {
            System.out.print(" " + qid);
        }
        System.out.println();
    }

    /* Is this a forward dataflow analysis? */
    public boolean isForward() {
        return true;
    }

    public Flow.DataflowObject getEntry() {
        Flow.DataflowObject result = newTempVar();
        result.copy(entry);
        return result;
    }

    /* Routines for interacting with dataflow values. */

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
        return new CheckTable();
    }

    public void processQuad(Quad q) {
        transferfn.val.copy(in[q.getID()]);
        Helper.runPass(q, transferfn);
        out[q.getID()].copy(transferfn.val);
    }

    /* Actually perform the transfer operation on the relevant
     * quad. */


    /* The QuadVisitor that actually does the computation */
    public static class TransferFunction extends QuadVisitor.EmptyVisitor {
        CheckTable val;
        int checkLevel;

        TransferFunction(int lv) {
            checkLevel = lv;
        }

        @Override
        public void visitQuad(Quad q) {
            if (checkLevel == NonNull.LEVEL_NORMAL) {
                if (q.getOperator() instanceof Operator.NullCheck) {
                    return;
                }
            } else {
                Operator oprt = q.getOperator();
                if (oprt instanceof Operator.NullCheck ||
                        oprt instanceof Operator.IntIfCmp ||
                        oprt instanceof Operator.Move ||
                        oprt instanceof Operator.Unary) {
                    return;
                }
            }

            for (RegisterOperand def : q.getDefinedRegisters()) {
                String key = def.getRegister().toString();
                //kill all the definitions with respect to the assigned register
                val.killVar(key);
            }
        }

        @Override
        public void visitNullCheck(Quad q) {
//            System.out.println("Call visitNullCheck: " + q.getID());
            for (RegisterOperand target : q.getUsedRegisters()) {
                String reg = target.getRegister().toString();
                // has been checked
                val.checkVar(reg);
            }
        }

        /**COND for IntIfCmp
         * SHIT JOEQ DOC!!!
         * Cond:
         *  0 for EQ
         *  1 for NE
         * */
//        @Override
//        public void visitIntIfCmp(Quad q) {
//            final byte EQ = 0;
//            final byte NE = 1;
//            if (checkLevel != NonNull.LEVEL_NORMAL) {
//                Operand s1 = Operator.IntIfCmp.getSrc1(q);
//                Operand s2 = Operator.IntIfCmp.getSrc2(q);
//
//                // only do this when at least one of opr is reg
//                if (s1 instanceof RegisterOperand && !(s2 instanceof RegisterOperand) ||
//                        s2 instanceof RegisterOperand && !(s1 instanceof RegisterOperand)) {
//                    byte cond = Operator.IntIfCmp.getCond(q).getCondition();
//                    Operand reg = s1 instanceof RegisterOperand ? s1 : s2;
//                    Operand other = s1 instanceof RegisterOperand ? s2 : s1;
//                    String regName = ((RegisterOperand) reg).getRegister().toString();
//                    // now ensure reg is register oprd
//                    if (cond == EQ) {
//                        if (isChecked(other)) {
//                            val.checkVar(regName);
//                        }
//                    } else if (cond == NE) {
//                        if (!isChecked(other)) {
//                            val.checkVar(regName);
//                        }
//                    }
//                } else if (s1 instanceof RegisterOperand && s2 instanceof RegisterOperand) {
//                    RegisterOperand r1 = (RegisterOperand) s1;
//                    RegisterOperand r2 = (RegisterOperand) s2;
//                    String reg1 = r1.getRegister().toString();
//                    String reg2 = r2.getRegister().toString();
//                    byte cond = Operator.IntIfCmp.getCond(q).getCondition();
//                    if (cond == EQ) {
//                        if (isChecked(r1)) {
//                            val.checkVar(reg2);
//                        }
//                        if (isChecked(r2)) {
//                            val.checkVar(reg1);
//                        }
//                    } else if (cond == NE) {
//                        if (!isChecked(r1)) {
//                            val.checkVar(reg2);
//                        }
//                        if (!isChecked(r2)) {
//                            val.checkVar(reg1);
//                        }
//                    }
//
//                }
//            }
//        }

        @Override
        public void visitMove(Quad q) {
            if (checkLevel != NonNull.LEVEL_NORMAL) {
                Operand src = Operator.Move.getSrc(q);
                String dst = Operator.Move.getDest(q).getRegister().toString();
                if (isChecked(src)){
                    val.checkVar(dst);
                } else {
                    val.killVar(dst);
                }
            }
        }

        @Override
        public void visitUnary(Quad q) {
            if (checkLevel != NonNull.LEVEL_NORMAL) {
                String dst = Operator.Unary.getDest(q).getRegister().toString();
                Operand src = Operator.Unary.getSrc(q);
                if (isChecked(src)){
                    val.checkVar(dst);
                } else {
                    val.killVar(dst);
                }
            }
        }

        private boolean isChecked(Operand op) {
            return (op instanceof Operand.IConstOperand ||
                    op instanceof Operand.AConstOperand &&
                            ((AConstOperand) op).getType() != jq_Reference.jq_NullType.NULL_TYPE) ||
                    (op instanceof RegisterOperand &&
                            val.isChecked(((RegisterOperand) op).getRegister().toString()));
        }
    }
}
