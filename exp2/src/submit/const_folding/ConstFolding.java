package submit.const_folding;

import flow.ConstantProp;
import flow.Flow;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.*;
import joeq.Main.Helper;
import submit.Modifier;

import java.util.HashMap;
import java.util.Map;

public class ConstFolding implements Modifier {
    public Map<jq_Method, Map<Integer, ConstantProp.ConstantPropTable>> allIn;
    boolean isForward;
    private ConstReplacer replacer = new ConstReplacer();


    public void getDataFlowResult(Flow.Analysis analysis) {
        allIn = new HashMap<jq_Method, Map<Integer, ConstantProp.ConstantPropTable>>(((ConstDetect)analysis).finalIn);
        isForward = analysis.isForward();
    }

    public void visitCFG(ControlFlowGraph cfg) {
        Map<Integer, ConstantProp.ConstantPropTable> in = allIn.get(cfg.getMethod());
        QuadIterator qit = new QuadIterator(cfg, isForward);

        // replace const
        while (qit.hasNext()) {
            Quad q = qit.next();
            ConstantProp.ConstantPropTable table = in.get(q.getID());
            replacer.registerIn(table);
            Helper.runPass(q, replacer);
        }
    }

    public static class ConstReplacer extends QuadVisitor.EmptyVisitor {
        ConstantProp.ConstantPropTable val;

        public void registerIn(ConstantProp.ConstantPropTable o) {
            val = o;
        }

        @Override
        public void visitMove(Quad q) {
            Operand op = Operator.Move.getSrc(q);
            String key = Operator.Move.getDest(q).getRegister().toString();
            if (isRegConst(op)) {
                String reg = regName(op);
                System.out.println("Set move const: " + q.getID());
                Operator.Move.setSrc(q, toConstOprand(val.get(reg).getConst()));
            }
        }

        @Override
        public void visitBinary(Quad q) {
            Operand op1 = Operator.Binary.getSrc1(q);
            Operand op2 = Operator.Binary.getSrc2(q);
            String key = Operator.Binary.getDest(q).getRegister().toString();
            Operator opr = q.getOperator();

            // since constprop only parses ADD
            if (opr == Operator.Binary.ADD_I.INSTANCE) {
                if (isRegConst(op1)) {
                    String reg = regName(op1);
                    Operator.Binary.setSrc1(q, toConstOprand(val.get(reg).getConst()));
                    System.out.println("Set binary const: " + q.getID());

                }
                if (isRegConst(op2)) {
                    String reg = regName(op2);
                    Operator.Binary.setSrc2(q, toConstOprand(val.get(reg).getConst()));
                    System.out.println("Set binary const: " + q.getID());

                }
            }
        }

        @Override
        public void visitUnary(Quad q) {
            Operand op = Operator.Unary.getSrc(q);
            String key = Operator.Unary.getDest(q).getRegister().toString();
            Operator opr = q.getOperator();

            if (opr == Operator.Unary.NEG_I.INSTANCE) {
                if (isRegConst(op)) {
                    String reg = regName(op);
                    Operator.Unary.setSrc(q, toConstOprand(val.get(reg).getConst()));
                    System.out.println("Set unary const: " + q.getID());

                }
            }
        }

        private boolean isConst(Operand op) {
            return (op instanceof Operand.IConstOperand) ||
                    (op instanceof Operand.RegisterOperand &&
                            val.get(((Operand.RegisterOperand) op).getRegister().toString()).isConst());
        }

        private String regName(Operand op) {
            return ((Operand.RegisterOperand) op).getRegister().toString();
        }

        private boolean isRegConst(Operand op) {
            return (op instanceof Operand.RegisterOperand &&
                            val.get(((Operand.RegisterOperand) op).getRegister().toString()).isConst());
        }

        private Operand toConstOprand(int val) {
            return new Operand.IConstOperand(val);
        }
    }
}
