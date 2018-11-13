package submit.faint;

import flow.Flow;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.*;
import joeq.Main.Helper;

import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

public class FaintEliminate implements submit.Modifier {

    private boolean changed = false;

    public Map<jq_Method, Map<Integer, FaintDetect.VarSet>> finalOut;

    private FaintKiller killer = new FaintKiller();

    public boolean runAgain() {
        return changed;
    }

    public void getDataFlowResult(Flow.Analysis analysis) {
        changed = false;
        finalOut = new HashMap<jq_Method, Map<Integer, FaintDetect.VarSet>>(((FaintDetect)analysis).finalOut);
    }

    public void visitCFG(ControlFlowGraph cfg) {
        Map<Integer, FaintDetect.VarSet> out = finalOut.get(cfg.getMethod());
        killer.registerOut(out);
        System.out.print("Killed fainted defs in " + cfg.getMethod().toString() + " : ");
        Helper.runPass(cfg, killer);
        System.out.println();
    }

    public class FaintKiller extends BasicBlockVisitor.EmptyVisitor {

        private Map<Integer, FaintDetect.VarSet> out;

        public void registerOut(Map<Integer, FaintDetect.VarSet> o) {
            out = o;
        }

        @Override
        public void visitBasicBlock(BasicBlock bb) {
            ListIterator<Quad> qit = bb.iterator();
            while (qit.hasNext()) {
                Quad q = qit.next();
                FaintDetect.VarSet faints = out.get(q.getID());
                // T-1 is always fainted
                if (!(q.getOperator() instanceof Operator.NullCheck)) {
                    for (Operand.RegisterOperand def : q.getDefinedRegisters()) {
                        if (faints.isFaint(def.getRegister().toString())) {
                            System.out.print(" " + q.getID());
                            qit.remove();
                            changed = true;
                            break;
                        }
                    }
                } else {
                    for (Operand.RegisterOperand use : q.getUsedRegisters()) {
                        if (faints.isFaint(use.getRegister().toString())) {
                            System.out.print(" " + q.getID());
                            qit.remove();
                            changed = true;
                            break;
                        }
                    }
                }
            }
        }
    }
}
