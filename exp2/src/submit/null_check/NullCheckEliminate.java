package submit.null_check;

import flow.Flow;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.QuadIterator;
import submit.Modifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class NullCheckEliminate implements Modifier {
    private Map<jq_Method, Set<Integer>> flowResult;
    private boolean isForward;

    public boolean runAgain() {
        return false;
    }

    public void getDataFlowResult(Flow.Analysis analysis) {
        flowResult = new HashMap<jq_Method, Set<Integer>>(((NonNull)analysis).allResult);
        isForward = analysis.isForward();
    }

    public void visitCFG(ControlFlowGraph cfg) {
        jq_Method method = cfg.getMethod();
        Set<Integer> target = flowResult.get(method);
        QuadIterator qit = new QuadIterator(cfg, isForward);
        while (qit.hasNext()) {
            Quad q = qit.next();
            if (target.contains(q.getID())) {
                qit.remove();
            }
        }
    }
}
