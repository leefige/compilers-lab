package submit.global_common;

import flow.Flow;
import joeq.Compiler.Quad.ControlFlowGraph;
import submit.Modifier;

public class GlobCommEliminate implements Modifier {
    public void visitCFG(ControlFlowGraph cfg) {

    }

    public boolean runAgain() {
        return false;
    }

    public void getDataFlowResult(Flow.Analysis analysis) {

    }
}
