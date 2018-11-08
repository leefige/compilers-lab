package submit.const_folding;

import flow.ConstantProp;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.QuadIterator;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ConstDetect extends ConstantProp {
    public Map<jq_Method, Map<Integer, ConstantPropTable>> finalIn = new HashMap<jq_Method, Map<Integer, ConstantPropTable>>();

    @Override
    public void postprocess(ControlFlowGraph cfg) {
        Map<Integer, ConstantPropTable> curIn = new TreeMap<Integer, ConstantPropTable>();
        QuadIterator qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            Quad q = qit.next();
            int qid = q.getID();
            curIn.put(qid, (ConstantPropTable)getIn(q));
        }
        finalIn.put(cfg.getMethod(), curIn);
    }
}
