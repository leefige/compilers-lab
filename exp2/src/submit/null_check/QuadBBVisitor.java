package submit.null_check;

import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.BasicBlockVisitor;
import joeq.Compiler.Quad.Quad;

import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

public class QuadBBVisitor extends BasicBlockVisitor.EmptyVisitor {
    public Map<Integer, Integer> quadBBMap;

    public QuadBBVisitor() {
        quadBBMap = new TreeMap<Integer, Integer>();
    }

    @Override
    public void visitBasicBlock(BasicBlock bb) {
        int bbid = bb.getID();
        ListIterator<Quad> qit = bb.iterator();
        while (qit.hasNext()) {
            Quad q = qit.next();
            int qid = q.getID();
            assert !quadBBMap.containsKey(qid);
            quadBBMap.put(qid, bbid);
        }
    }
}
