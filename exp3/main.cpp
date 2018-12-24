/**
 * By Yifei Li
 * */
#define LOG
#include <map>
#include <string>
#include <stack>
#include <assert.h>

#include <llvm/IR/CFG.h>
#include <llvm/IR/InstVisitor.h>
#include <llvm/IR/Instruction.h>
#include <llvm/IR/DerivedTypes.h>
#include <llvm/IRReader/IRReader.h>
#include <llvm/Support/SourceMgr.h>
#include <llvm/Support/raw_ostream.h>
#include <llvm/ADT/SCCIterator.h>
#include <z3++.h>

//#define NDEBUG

using namespace llvm;

namespace {

// Get unique name of a LLVM node. Applicable to BasicBlock and Instruction.
std::string getName(const Value &Node) {
  if (!Node.getName().empty())
    return Node.getName().str();

  std::string Str;
  raw_string_ostream OS(Str);

  Node.printAsOperand(OS, false);
  return OS.str();
}

// Check
void checkAndReport(z3::solver &solver, const GetElementPtrInst &gep) {
  std::string name = getName(gep);
  std::cout << "Checking with assertions:" << std::endl
            << solver.assertions() << std::endl;
  if (solver.check() == z3::sat)
    std::cout << "GEP " << name << " is potentially out of bound." << std::endl
              << "Model causing out of bound:" << std::endl
              << solver.get_model() << std::endl;
  else
    std::cout << "GEP " << name << " is safe." << std::endl;
}
} // namespace

// ONLY MODIFY THIS CLASS FOR PART 1 & 2!
/* for debug */
namespace {
#ifdef NDEBUG
#define debug 0 && std::cout
#else
#define debug std::cout
#endif
}

class Z3Walker : public InstVisitor<Z3Walker> {
private:
  std::map<std::string, std::vector<z3::ast>> predicate_map;
  z3::context ctx;
  z3::solver solver;

  //std::string ast_name;
  Function* current_fun;
  z3::expr_vector arg_evec;
  z3::sort_vector arg_svec;

  void astInit(Function* func) {
    current_fun = func;
    std::string ast_name = getName(*current_fun);
    predicate_map.insert(
        std::pair<std::string, std::vector<z3::ast> >(ast_name, std::vector<z3::ast>())
    );
    this->getArgVectors(func);
  }

  void astAdd(const z3::ast& exp) {
    std::string ast_name = getName(*current_fun);
    auto vec = predicate_map[ast_name];
    vec.push_back(exp);
    predicate_map[ast_name] = vec;
  }

//  z3::ast getAst(Function* func) {
//    std::string name = getName(*func);
//    auto vec = predicate_map[name];
//    assert(!vec.empty());
//    auto ast = *vec.begin();
//    for (auto eit = vec.begin() + 1; eit != vec.end(); eit++) {
//      ast = ast && *eit;
//    }
//    //ast = ast.simplify();
//    return ast;
//  }

  z3::func_decl gen_func(Value* var, unsigned n) {
    return z3::function(getName(*var), arg_svec, ctx.bv_sort(n));
  }

  z3::ast gen_i(Value* var, unsigned n) {
    if (ConstantInt* CI = dyn_cast<ConstantInt>(var)) {
      // var indeed is a ConstantInt, we can use CI here
      return ctx.bv_val(CI->getSExtValue(), n);
    }
    else {
      // var was not actually a ConstantInt
      return gen_func(var, n);
    }
  }

  // gen 32-bit const
  z3::ast gen_i32(Value* var) {
    return gen_i(var, 32);
  }

  // gen 1-bit const
  z3::ast gen_i1(Value* var) {
    return gen_i(var, 1);
  }

  // gen 64-bit const
  z3::ast gen_i64(Value* var) {
    return gen_i(var, 64);
  }

  // gen true/false
  z3::expr i1_true() {
    return ctx.bv_val(1, 1);
  }
  z3::expr i1_false() {
    return ctx.bv_val(0, 1);
  }

  // get sort
  z3::sort getSort(Value* var) {
    Type* type = var->getType();
    if (type->isIntegerTy()) {
      IntegerType* ty = (IntegerType*)type;
      unsigned width = ty->getBitWidth();
      return ctx.bv_sort(width);
    } else {
      return ctx.bv_sort(32);   // potentially harmful
    }
  }

  void getArgVectors(Function* F) {
    while (!arg_evec.empty()) {
      arg_evec.pop_back();
    }
    while (!arg_svec.empty()) {
      arg_svec.pop_back();
    }

    for (auto ait = F->arg_begin(); ait != F->arg_end(); ait++) {
      Argument* arg = &(*ait);
      z3::sort st = getSort(arg);
      arg_svec.push_back(st);
      unsigned size = st.bv_size();
      arg_evec.push_back(ctx.bv_const(getName(*arg).c_str(), size));
    }
  }

public:
  Z3Walker() : ctx(), solver(ctx), arg_evec(ctx), arg_svec(ctx) {
    debug << "new z3walker" << std::endl;
    current_fun = NULL;
  }

  // Not using InstVisitor::visit due to their sequential order.
  // We want topological order on the Call Graph and CFG.
  void visitModule(Module &M) {
    std::cout << "<Module> " << M.getName().str();
    // iterate functions
    std::cout << ", size is " << M.size() << std::endl;
    for(auto it = M.begin(); it != M.end(); it++) {
      this->visitFunction(*it);
    }
  }

  void visitFunction(Function &F) {
    std::cout << "---------" << std::endl << "<Func> " << getName(F) << ":";
//    solver.push();
    astInit(&F);
    
    // parse args
    unsigned arg_cnt = F.arg_size();
    for (auto ait = F.arg_begin(); ait != F.arg_end(); ait++) {
      Argument* arg = &(*ait);
      auto argname = getName(*arg);
      std::cout << " arg " << argname;
    }
    std::cout << std::endl;

    // use stack to reverse post order
    std::stack<scc_iterator<Function*> > sccs;
    for(scc_iterator<Function*> scci = scc_begin(&F), scce = scc_end(&F); scci != scce; ++scci) {
      sccs.push(scci);
    }
    
    // topological order iteration on BB
    while(!sccs.empty()){
      scc_iterator<Function*> SCCI = sccs.top();
      sccs.pop();
      const std::vector<BasicBlock*> & nextSCC = *SCCI;
      // iterate bbs
      std::vector<BasicBlock*>::const_iterator I = nextSCC.begin(), E = nextSCC.end();
      for(--E,--I; E != I; --E) {
        this->visitBasicBlock(**E);
      }
    }
//    debug << "------------" << std::endl << "<Solver>" << std::endl 
//      << solver << "============" << std::endl;
//    solver.pop();
  }

  void visitBasicBlock(BasicBlock &B) {
    debug << "  <BB> " << getName(B) << std::endl;
    for (auto iit = B.begin(); iit != B.end(); iit++) {
      this->visit(*iit);
    }
  }

  void visitAdd(BinaryOperator &I) {
    debug << "    visit add" << std::endl;
//    for (auto i = I.op_begin(); i != I.op_end(); i++) {
//      std::cout << "\top: " << *i;
//    }
    debug << std::endl;
    auto op1 = I.getOperand(0);
    auto op2 = I.getOperand(1);
    z3::ast a = gen_i32(op1);
    z3::ast b = gen_i32(op2);
    // the Instruction itself is the ret val
    z3::func_decl r = gen_func(&I, 32);
    astAdd(z3::forall(arg_evec, r(arg_evec) == a + b)/*simp*/);
  }

  void visitSub(BinaryOperator &I) {
    debug << "    visit sub" << std::endl;
    auto op1 = I.getOperand(0);
    auto op2 = I.getOperand(1);
    z3::ast a = gen_i32(op1);
    z3::ast b = gen_i32(op2);
    z3::func_decl r = gen_func(&I, 32);
    astAdd(z3::forall(arg_evec, r(arg_evec) == a - b)/*simp*/);
  }
  
  void visitMul(BinaryOperator &I) {
    debug << "    visit mul" << std::endl;
    auto op1 = I.getOperand(0);
    auto op2 = I.getOperand(1);
    z3::ast a = gen_i32(op1);
    z3::ast b = gen_i32(op2);
    z3::func_decl r = gen_func(&I, 32);
    astAdd(z3::forall(arg_evec, r(arg_evec) == a + b)/*simp*/);
    z3::ast r = gen_i32(&I);
    astAdd((r == a * b)/*simp*/);
  }

  void visitShl(BinaryOperator &I) {
    debug << "    visit shl" << std::endl;
    auto op1 = I.getOperand(0);
    auto op2 = I.getOperand(1);
    z3::ast a = gen_i32(op1);
    z3::ast b = gen_i32(op2);
    z3::func_decl r = gen_func(&I, 32);
    astAdd(z3::forall(arg_evec, r(arg_evec) == a + b)/*simp*/);
    z3::ast r = gen_i32(&I);
    astAdd((r == z3::shl(a, b))/*simp*/);
  }

  void visitLShr(BinaryOperator &I) {
    debug << "    visit lshr" << std::endl;
    auto op1 = I.getOperand(0);
    auto op2 = I.getOperand(1);
    z3::ast a = gen_i32(op1);
    z3::ast b = gen_i32(op2);
    z3::func_decl r = gen_func(&I, 32);
    astAdd(z3::forall(arg_evec, r(arg_evec) == a + b)/*simp*/);
    z3::ast r = gen_i32(&I);
    astAdd((r == z3::lshr(a, b))/*simp*/);
  }

  void visitAShr(BinaryOperator &I) {
    debug << "    visit ashr" << std::endl;
    auto op1 = I.getOperand(0);
    auto op2 = I.getOperand(1);
    z3::ast a = gen_i32(op1);
    z3::ast b = gen_i32(op2);
    z3::func_decl r = gen_func(&I, 32);
    astAdd(z3::forall(arg_evec, r(arg_evec) == a + b)/*simp*/);
    z3::ast r = gen_i32(&I);
    astAdd((r == z3::ashr(a, b))/*simp*/);
  }

  void visitAnd(BinaryOperator &I) {
    debug << "    visit and" << std::endl;
    auto op1 = I.getOperand(0);
    auto op2 = I.getOperand(1);
    z3::ast a = gen_i32(op1);
    z3::ast b = gen_i32(op2);
    z3::func_decl r = gen_func(&I, 32);
    astAdd(z3::forall(arg_evec, r(arg_evec) == a + b)/*simp*/);
    z3::ast r = gen_i32(&I);
//    astAdd((
//        r == z3::ite(
//          ((a == i1_true()) && (b == i1_true())),
//          i1_true(), i1_false())
//        );
    astAdd((r == (a & b))/*simp*/);
  }

  void visitOr(BinaryOperator &I) {
    debug << "    visit or" << std::endl;
    auto op1 = I.getOperand(0);
    auto op2 = I.getOperand(1);
    z3::ast a = gen_i32(op1);
    z3::ast b = gen_i32(op2);
    z3::func_decl r = gen_func(&I, 32);
    astAdd(z3::forall(arg_evec, r(arg_evec) == a + b)/*simp*/);
    z3::ast r = gen_i32(&I);
    astAdd((r == (a | b))/*simp*/);
  }

  void visitXor(BinaryOperator &I) {
    debug << "    visit xor" << std::endl;
    auto op1 = I.getOperand(0);
    auto op2 = I.getOperand(1);
    z3::ast a = gen_i32(op1);
    z3::ast b = gen_i32(op2);
    z3::func_decl r = gen_func(&I, 32);
    astAdd(z3::forall(arg_evec, r(arg_evec) == a + b)/*simp*/);
    z3::ast r = gen_i32(&I);
    astAdd((r == (a ^ b))/*simp*/);
  }

  void visitICmp(ICmpInst &I) { 
    debug << "    visit icmp" << std::endl;
    auto op1 = I.getOperand(0);
    auto op2 = I.getOperand(1);
    z3::ast a = gen_i32(op1);
    z3::ast b = gen_i32(op2);
    z3::func_decl r = gen_func(&I, 32);
    astAdd(z3::forall(arg_evec, r(arg_evec) == a + b)/*simp*/);
    // note: r here is bv_1
    z3::ast r = gen_i1(&I);
    
    auto cond = I.getPredicate();
    switch (cond) {
      case CmpInst::ICMP_EQ :  ///< equal 
        astAdd((r == z3::ite((a == b), i1_true(), i1_false()))/*simp*/);
        break;
      case CmpInst::ICMP_NE :  ///< not equal
        astAdd((r == z3::ite((a != b), i1_true(), i1_false()))/*simp*/);
        break;
      case CmpInst::ICMP_UGT:  ///< unsigned greater than
        astAdd((r == z3::ite(z3::ugt(a, b), i1_true(), i1_false()))/*simp*/);
        break;
      case CmpInst::ICMP_UGE:  ///< unsigned greater or equal
        astAdd((r == z3::ite(z3::uge(a, b), i1_true(), i1_false()))/*simp*/);
        break;
      case CmpInst::ICMP_ULT:  ///< unsigned less than
        astAdd((r == z3::ite(z3::ult(a, b), i1_true(), i1_false()))/*simp*/);
        break;
      case CmpInst::ICMP_ULE:  ///< unsigned less or equal
        astAdd((r == z3::ite(z3::ule(a, b), i1_true(), i1_false()))/*simp*/);
        break;
      case CmpInst::ICMP_SGT:  ///< signed greater than
        astAdd((r == z3::ite((a > b), i1_true(), i1_false()))/*simp*/);
        break;
      case CmpInst::ICMP_SGE:  ///< signed greater or equal
        astAdd((r == z3::ite((a >= b), i1_true(), i1_false()))/*simp*/);
        break;
      case CmpInst::ICMP_SLT:  ///< signed less than
        astAdd((r == z3::ite((a < b), i1_true(), i1_false()))/*simp*/);
        break;
      case CmpInst::ICMP_SLE:  ///< signed less or equal
        astAdd(((r == z3::ite((a <= b), i1_true(), i1_false()))/*simp*/));
        break;
      default:
        errs() << "Unsupported ICMP_OP: " << cond << "\n";
        break;
    }  
  }

  void visitBranchInst(BranchInst &I) { 
    debug << "    visit br"<< std::endl;
    // only consider conditional jump
    if (I.isConditional()) {
      auto cond = I.getCondition();
      auto tar_fls = I.getOperand(1); // BB
      auto tar_tr = I.getOperand(2);
      z3::ast cd = gen_i1(cond);
      z3::ast tr = gen_i1(tar_tr);
      z3::ast fls = gen_i1(tar_fls);
      astAdd(tr == cd);
      astAdd(fls == z3::ite(
              (cd == i1_true()), i1_false(), i1_true()
            ));
    } else {
      auto tar = I.getOperand(0);     // true
      z3::ast tr = gen_i1(tar);
      astAdd(tr == i1_true());
    }
  }

  void visitPHINode(PHINode &I) { 
    debug << "    visit phi" << std::endl;
    unsigned cnt = I.getNumIncomingValues();
    for (unsigned i = 0; i != cnt; i++) {
      auto val = I.getIncomingValue(i);
      auto bb = I.getIncomingBlock(i);
      z3::ast v = gen_i32(val);
      z3::ast b = gen_i1(bb);
      z3::ast dst = gen_i32(&I);
      astAdd(z3::implies(
              (b == i1_true()), (dst == v)
            ));
    }
  }

  void visitSExtInst(SExtInst & I) {
    debug << "    visit sext" << std::endl;
    auto srcv = I.getOperand(0);
    z3::ast src = gen_i32(srcv);
    z3::ast dst = gen_i64(&I);
    astAdd(dst == z3::sext(src, 32));
  }
  
  void visitZExtInst(ZExtInst & I) {
    debug << "    visit zext" << std::endl;
    auto srcv = I.getOperand(0);
    z3::ast src = gen_i32(srcv);
    z3::ast dst = gen_i64(&I);
    astAdd(dst == z3::zext(src, 32));
  }

  // Call checkAndReport here.
  void visitGetElementPtrInst(GetElementPtrInst &I) {
    debug << "    visit gep" << std::endl;
    if (I.isInBounds()) {
      if (I.getSourceElementType()->isArrayTy()) {
        ArrayType* type = (ArrayType*) I.getSourceElementType();
        if (type->getArrayElementType()->isIntegerTy()) {
//          std::cout << "===check pushed===" << std::endl;
//          std::cout << solver << std::endl;
          auto size = type->getArrayNumElements(); 
          auto ptrOp = I.getOperand(2);
          z3::ast ptr = gen_i64(ptrOp);
          z3::ast lb = ctx.bv_val(0, 64);
          z3::ast ub = ctx.bv_val(size, 64); 
          z3::ast inbounds = (ptr >= lb && ptr < ub);

          solver.push();
          z3::ast check = (getAst(I.getFunction()) && !inbounds)
#ifdef NDEBUG
            .simplify()
#endif
          ;
          solver.add(check); 
//          std::cout << "bound added" << std::endl << solver << std::endl;
          checkAndReport(solver, I);
          solver.pop();
//          std::cout << "===check popped===" << std::endl;
//          std::cout << solver << std::endl;
        }
      }
    }
  }
};

int main(int argc, char const *argv[]) {
  if (argc < 2) {
    errs() << "Usage: " << argv[0] << " <IR file>\n";
    return 1;
  }

  LLVMContext llvmctx;

  // Parse the input LLVM IR file into a module.
  SMDiagnostic Err;
  auto module = parseIRFile(argv[1], Err, llvmctx);
  if (!module) {
    Err.print(argv[0], errs());
    return 1;
  }

//  printf("Now walking the module...\n");
  Z3Walker().visitModule(*module);

  return 0;
}

