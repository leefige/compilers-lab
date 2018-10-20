package submit;

// some useful things to import. add any additional imports you need.
import joeq.Compiler.Quad.*;
import flow.Flow;

import java.util.*;

/**
 * Skeleton class for implementing the Flow.Solver interface.
 */
public class MySolver implements Flow.Solver {

    protected Flow.Analysis analysis;

    /**
     * Sets the analysis.  When visitCFG is called, it will
     * perform this analysis on a given CFG.
     *
     * @param analyzer The analysis to run
     */
    public void registerAnalysis(Flow.Analysis analyzer) {
        this.analysis = analyzer;
    }

    /**
     * Runs the solver over a given control flow graph.  Prior
     * to calling this, an analysis must be registered using
     * registerAnalysis
     *
     * @param cfg The control flow graph to analyze.
     */
    public void visitCFG(ControlFlowGraph cfg) {

        // this needs to come first.
        analysis.preprocess(cfg);

        /***********************
         * Your code goes here *
         ***********************/
        final boolean isForward = analysis.isForward();

        // iterate all quads and try to find all exit quads
        Set<Quad> exitQuads = new HashSet<Quad>();
        Set<Quad> entryQuads = new HashSet<Quad>();
        QuadIterator qit_ex = new QuadIterator(cfg);

        Flow.DataflowObject topVal = analysis.newTempVar();
        topVal.setToTop();

        while (qit_ex.hasNext()) {
            Quad curQuad = qit_ex.next();
            if (qit_ex.predecessors1().contains(null)) {
                entryQuads.add(curQuad);
            }
            if (qit_ex.successors1().contains(null)) {
                exitQuads.add(curQuad);
            }
            // set init val
            if (isForward) {
                analysis.setOut(curQuad, topVal);
            } else {
                analysis.setIn(curQuad, topVal);
            }
        }

        // analyze
        boolean done = false;
        // if not done, do a new pass
        while (!done) {
            done = true;
            QuadIterator qit = new QuadIterator(cfg, isForward);

            // iterate all the quads
            if (isForward) {
                // forward
                while (qit.hasNext()) {
                    Quad curQuad = qit.next();
                    Iterator<Quad> predIt = qit.predecessors();
                    Flow.DataflowObject newIn = analysis.newTempVar();
                    while (predIt.hasNext()) {
                        Quad predQuad = predIt.next();
                        if (predQuad != null) {
                            newIn.meetWith(analysis.getOut(predQuad));
                        } else {
                            newIn.meetWith(analysis.getEntry());
                        }
                    }
                    analysis.setIn(curQuad, newIn);

                    Flow.DataflowObject oldOut = analysis.getOut(curQuad);
                    analysis.processQuad(curQuad);
                    Flow.DataflowObject newOut = analysis.getOut(curQuad);

                    if (!newOut.equals(oldOut)) {
                        done = false;
                    }
                }
            } else {
                // backward
                while (qit.hasPrevious()) {
                    Quad curQuad = qit.previous();
                    Iterator<Quad> succIt = qit.successors();
                    Flow.DataflowObject newOut = analysis.newTempVar();
                    while (succIt.hasNext()) {
                        Quad succQuad = succIt.next();
                        if (succQuad != null) {
                            newOut.meetWith(analysis.getIn(succQuad));
                        } else {
                            newOut.meetWith(analysis.getExit());
                        }
                    }
                    analysis.setOut(curQuad, newOut);

                    Flow.DataflowObject oldIn = analysis.getIn(curQuad);
                    analysis.processQuad(curQuad);
                    Flow.DataflowObject newIn = analysis.getIn(curQuad);

                    if (!newIn.equals(oldIn)) {
                        done = false;
                    }
                }
            }
        }

        // set entry/exit after iterations
        if (isForward) {
            Flow.DataflowObject finOut = analysis.newTempVar();
            finOut.setToTop();
            for (Quad qd : exitQuads) {
                finOut.meetWith(analysis.getOut(qd));
            }
            analysis.setExit(finOut);
        } else {
            Flow.DataflowObject finIn = analysis.newTempVar();
            finIn.setToTop();
            for (Quad qd : entryQuads) {
                finIn.meetWith(analysis.getIn(qd));
            }
            analysis.setEntry(finIn);
        }

        // this needs to come last.
        analysis.postprocess(cfg);
    }
}
