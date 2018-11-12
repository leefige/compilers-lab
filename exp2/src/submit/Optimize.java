package submit;

import examples.PrintQuads;
import flow.Flow;
import flow.FlowSolver;
import joeq.Class.jq_Class;
import joeq.Interpreter.QuadInterpreter;
import joeq.Main.Driver;
import joeq.Main.Helper;
import submit.const_folding.ConstDetect;
import submit.const_folding.ConstFolding;
import submit.faint.FaintDetect;
import submit.faint.FaintEliminate;
import submit.global_common.GlobCommDetect;
import submit.global_common.GlobCommEliminate;
import submit.null_check.NonNull;
import submit.null_check.NullCheckEliminate;

import java.util.*;

class Optimize {
    /**
     * @param optimizeClasses a list of names of class that should be optimized
     * @param nullCheckOnly   if set to true, disable all optimizations except "remove redundant NULL_CHECKs."
     */
    private static List<jq_Class> optimize(List<String> optimizeClasses, boolean nullCheckOnly) {
        // get an instance of the solver class.
        Flow.Solver solver = new FlowSolver();

        // generate driver
        OptDriver driver = new OptDriver();
        driver.registerSolver(solver);

        // a list of analysis classes.
        Flow.Analysis nonNullAnalysis = new NonNull(NonNull.LEVEL_NORMAL);
        NullCheckEliminate nullCheck = new NullCheckEliminate();

        List<Flow.Analysis> anaList = new ArrayList<Flow.Analysis>();
        List<Modifier> modList = new ArrayList<Modifier>();

        anaList.add(new ConstDetect());
        modList.add(new ConstFolding());
//        optMap.put(new GlobCommDetect(), new GlobCommEliminate());
        anaList.add(new FaintDetect());
        modList.add(new FaintEliminate());

        List<jq_Class> outputs = new ArrayList<jq_Class>();
        for (String className : optimizeClasses) {
            jq_Class clazz = (jq_Class) Helper.load(className);

            // TODO: Remove redundant null checks
            driver.registerAnalysis(nonNullAnalysis);
            driver.registerOptimizer(nullCheck);
            driver.run(clazz);

            if (!nullCheckOnly) {
                // TODO: Run your extra optimizations. (Not required)
                for (int i = 0; i < anaList.size(); i++) {
                    driver.registerAnalysis(anaList.get(i));
                    driver.registerOptimizer(modList.get(i));
                    driver.run(clazz);
                }
            }

            outputs.add(clazz);
        }
        return outputs;
    }

    /**
     * ATTN: DO NOT MODIFY THIS METHOD
     */
    public static void main(String[] args) {
        List<String> optimizeFiles = new ArrayList<String>();
        List<String> runMainParams = new ArrayList<String>();
        String runMainClass = null;
        boolean nullCheckOnly = true, printAfterOpt = false;

        for (int i = 0; i < args.length; i++)
            if (args[i].equals("--main") || args[i].equals("-m"))
                runMainClass = args[++i];
            else if (args[i].equals("--params") || args[i].equals("-p"))
                runMainParams.addAll(Arrays.asList(args[++i].split(",")));
            else if (args[i].equals("--extra") || args[i].equals("-e"))
                nullCheckOnly = false;
            else if (args[i].equals("--print"))
                printAfterOpt = true;
            else
                optimizeFiles.add(args[i]);

        Collection<jq_Class> optimized = Optimize.optimize(optimizeFiles, nullCheckOnly);

        if (printAfterOpt)
            for (jq_Class clazz : optimized)
                PrintQuads.printClass(clazz);

        if (runMainClass != null)
            interpret(runMainClass, runMainParams);
    }

    /**
     * ATTN: DO NOT MODIFY THIS METHOD
     */
    private static void interpret(String mainClass, List<String> params) {
        String[] args = new String[params.size() + 4];
        args[0] = "interpret";
        args[1] = mainClass + ".main";
        args[2] = "{";
        for (int i = 0; i < params.size(); i++)
            args[i + 3] = params.get(i);
        args[params.size() + 3] = "}";

        QuadInterpreter.num_quads = 0;
        QuadInterpreter.num_nullchecks = 0;

        Driver.main(args);
    }
}
