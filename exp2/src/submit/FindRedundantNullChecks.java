package submit;

import flow.Flow;
import flow.FlowSolver;
import joeq.Class.jq_Class;
import joeq.Main.Helper;

import java.util.*;

public class FindRedundantNullChecks {

    /**
     * Main method of FindRedundantNullChecks.
     * This method should print out a list of quad ids of redundant null checks for each function.
     * The format should be "method_name id0 id1 id2", integers for each id separated by spaces.
     *
     * @param _args an array of class names. If "-e" presented, do extra analysing.
     */
    public static void main(String[] _args) {
        List<String> args = new ArrayList<String>(Arrays.asList(_args));
        boolean extra = args.contains("-e");
        int checkLevel;
        if (extra) {
            args.remove("-e");
            checkLevel = NonNull.LEVEL_HIGH;
        } else {
            checkLevel = NonNull.LEVEL_NORMAL;
        }
        // TODO: Fill in this

        // get an instance of the solver class.
        Flow.Solver solver = new FlowSolver();


        // get an instance of the analysis class.
        Flow.Analysis analysis = new NonNull(checkLevel);

        // get the classes we will be visiting.
        jq_Class[] classes = new jq_Class[args.size()];
        for (int i = 0; i < classes.length; i++)
            classes[i] = (jq_Class) Helper.load(args.get(i));

        // register the analysis with the solver.
        solver.registerAnalysis(analysis);

        // visit each of the specified classes with the solver.
        for (int i = 0; i < classes.length; i++) {
//            System.out.println("Now analyzing " + classes[i].getName());
            Helper.runPass(classes[i], solver);
        }
    }
}
