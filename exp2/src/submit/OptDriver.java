package submit;

import flow.Flow;
import joeq.Class.jq_Class;
import joeq.Main.Helper;

public class OptDriver {

    private Flow.Solver solver;
    private Flow.Analysis analysis;
    private Modifier modifier;

    public void registerSolver(Flow.Solver sol) {
        solver = sol;
    }

    public void registerAnalysis(Flow.Analysis ana) {
        analysis = ana;
    }

    public void registerOptimizer(Modifier opt) {
        modifier = opt;
    }

    public void run(jq_Class clazz) {
        // register the analysis with the solver.
        solver.registerAnalysis(analysis);
        do {
            Helper.runPass(clazz, solver);
            // get result of data flow analysis
            modifier.getDataFlowResult(analysis);
            Helper.runPass(clazz, modifier);
        } while (modifier.runAgain());
    }
}
