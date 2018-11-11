package submit;

import flow.Flow;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.ControlFlowGraphVisitor;

public interface Modifier extends ControlFlowGraphVisitor {
    public boolean runAgain();
    public void getDataFlowResult(Flow.Analysis analysis);
    void visitCFG(ControlFlowGraph cfg);
}
