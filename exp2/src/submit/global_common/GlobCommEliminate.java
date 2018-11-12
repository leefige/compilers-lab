package submit.global_common;

import flow.Flow;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.*;
import submit.Modifier;

import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

public class GlobCommEliminate implements Modifier {
    private boolean runAgain = false;
    public Map<jq_Method, Map<Integer, GlobCommDetect.ExpSet>> allIn;

    public boolean runAgain() {
        return runAgain;
    }

    public void getDataFlowResult(Flow.Analysis analysis) {
        allIn = new HashMap<jq_Method, Map<Integer, GlobCommDetect.ExpSet>>(((GlobCommDetect)analysis).result);
    }

    public void visitCFG(ControlFlowGraph cfg) {
//        Map<Integer, GlobCommDetect.ExpSet> inMap = allIn.get(cfg.getMethod());
//
//        // count redundant expressions
//        Map<GlobCommDetect.Exprsn, Integer> expCnt = new HashMap<GlobCommDetect.Exprsn, Integer>();
//        for (GlobCommDetect.ExpSet expSet : inMap.values()) {
//            for (GlobCommDetect.Exprsn exp : expSet.getSet()) {
//                if (!expCnt.containsKey(exp)) {
//                    expCnt.put(exp, 0);
//                } else {
//                    expCnt.put(exp, expCnt.get(exp) + 1);
//                }
//            }
//        }



    }

    public class ExprReplacer extends BasicBlockVisitor.EmptyVisitor {
//        ConstantProp.ConstantPropTable val;
//
//        public void registerIn(ConstantProp.ConstantPropTable o) {
//            val = o;
//        }

        @Override
        public void visitBasicBlock(BasicBlock bb) {
            ListIterator<Quad> qit = bb.iterator();
            while (qit.hasNext()) {
                Quad q = qit.next();
                Operator opr = q.getOperator();

                // only consider binary
                if (opr instanceof Operator.Binary) {
                    GlobCommDetect.Exprsn exp = GlobCommDetect.Exprsn.getExp(q);

                }
            }
        }
    }
}
