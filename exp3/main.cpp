/**
 * By Yifei Li
 * */
#define LOG
#include <map>
#include <string>
#include <stack>
#include <set>
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
  std::map<std::string, std::vector<z3::expr>> predicate_map;
  z3::context ctx;
  z3::solver solver;

  Function* current_fun;
  // args
  z3::expr_vector arg_evec;
  z3::sort_vector arg_svec;
  std::set<std::string> arg_names; 

  void astInit(Function* func) {
    current_fun = func;
    std::string ast_name = getName(*current_fun);
    predicate_map.insert(
        std::pair<std::string, std::vector<z3::expr> >(ast_name, std::vector<z3::expr>())
    );
    this->getArgInfo(func);
  }

  void astAdd(const z3::expr& exp) {
//    debug << "in ast add\n";
    std::string ast_name = getName(*current_fun);
    auto vec = predicate_map[ast_name];
    vec.push_back(exp);
//    debug << "want to insert back to map\n";
    predicate_map[ast_name] = vec;
//    debug << "ast added: " << exp << std::endl;
  }

  z3::expr getAst(Function* func) {
    std::string name = getName(*func);
    auto vec = predicate_map[name];
    assert(!vec.empty());
    auto ast = *vec.begin();
    for (auto eit = vec.begin() + 1; eit != vec.end(); eit++) {
      ast = ast && *eit;
    }
    //ast = ast.simplify();
    return ast;
  }

  z3::func_decl gen_func(Value* var, unsigned n) {
    return z3::function(getName(*var), arg_svec, ctx.bv_sort(n));
  }

  z3::expr gen_i(Value* var, unsigned n) {
    if (ConstantInt* CI = dyn_cast<ConstantInt>(var)) {
      // var indeed is a ConstantInt, we can use CI here
      return ctx.bv_val(CI->getSExtValue(), n);
    }
    else {
      // var was not actually a ConstantInt
      std::string reg = getName(*var);
      // if this is arg
      if (arg_names.count(reg)) {
        return ctx.bv_const(reg.c_str(), n);
      } 
      // or this is a func of arg
      else {
        z3::func_decl r = gen_func(var, n);
        return z3::expr(r(arg_evec));
      }
    }
  }

  // gen 32-bit const
  z3::expr gen_i32(Value* var) {
    return gen_i(var, 32);
  }

  // gen 1-bit const
  z3::expr gen_i1(Value* var) {
    return gen_i(var, 1);
  }

  // gen 64-bit const
  z3::expr gen_i64(Value* var) {
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

  void getArgInfo(Function* F) {
    while (!arg_evec.empty()) {
      arg_evec.pop_back();
    }
    while (!arg_svec.empty()) {
      arg_svec.pop_back();
    }
    arg_names.clear();

    for (auto ait = F->arg_begin(); ait != F->arg_end(); ait++) {
      Argument* arg = &(*ait);
      arg_names.insert(getName(*ait));
      z3::sort st = getSort(arg);
      arg_svec.push_back(st);
      unsigned size = st.bv_size();
      arg_evec.push_back(ctx.bv_const(getName(*arg).c_str(), size));
    }
    if (arg_evec.size() == 0) {
      arg_evec.push_back(ctx.bv_const("dummy", 32));
      arg_svec.push_back(ctx.bv_sort(32));
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
    auto op1 = I.getOperand(0);
    auto op2 = I.getOperand(1);
    z3::expr a = gen_i32(op1);
    z3::expr b = gen_i32(op2);
    // the Instruction itself is the ret val
    z3::expr r = gen_i32(&I);
    astAdd(z3::forall(arg_evec, r == a + b));
  }

  void visitSub(BinaryOperator &I) {
    debug << "    visit sub" << std::endl;
    auto op1 = I.getOperand(0);
    auto op2 = I.getOperand(1);
    z3::expr a = gen_i32(op1);
    z3::expr b = gen_i32(op2);
    z3::expr r = gen_i32(&I);
    astAdd(z3::forall(arg_evec, r == a - b));
  }
  
  void visitMul(BinaryOperator &I) {
    debug << "    visit mul" << std::endl;
    auto op1 = I.getOperand(0);
    auto op2 = I.getOperand(1);
    z3::expr a = gen_i32(op1);
    z3::expr b = gen_i32(op2);
    z3::expr r = gen_i32(&I);
    astAdd(z3::forall(arg_evec, r == a * b));
  }

  void visitShl(BinaryOperator &I) {
    debug << "    visit shl" << std::endl;
    auto op1 = I.getOperand(0);
    auto op2 = I.getOperand(1);
    z3::expr a = gen_i32(op1);
    z3::expr b = gen_i32(op2);
    z3::expr r = gen_i32(&I);
//    debug << "shl gen_ed" << std::endl;
    z3::expr ex = (r == z3::shl(a, b));
//    debug << "2nd: " << ex << std::endl;
    z3::expr all = z3::forall(arg_evec, ex);
//    debug << "all: " << all << std::endl;
    astAdd(all);
  }

  void visitLShr(BinaryOperator &I) {
    debug << "    visit lshr" << std::endl;
    auto op1 = I.getOperand(0);
    auto op2 = I.getOperand(1);
    z3::expr a = gen_i32(op1);
    z3::expr b = gen_i32(op2);
    z3::expr r = gen_i32(&I);
    astAdd(z3::forall(arg_evec, r == z3::lshr(a, b)));
  }

  void visitAShr(BinaryOperator &I) {
    debug << "    visit ashr" << std::endl;
    auto op1 = I.getOperand(0);
    auto op2 = I.getOperand(1);
    z3::expr a = gen_i32(op1);
    z3::expr b = gen_i32(op2);
    z3::expr r = gen_i32(&I);
    astAdd(z3::forall(arg_evec, r == z3::ashr(a, b)));
  }

  void visitAnd(BinaryOperator &I) {
    debug << "    visit and" << std::endl;
    auto op1 = I.getOperand(0);
    auto op2 = I.getOperand(1);
    z3::expr a = gen_i32(op1);
    z3::expr b = gen_i32(op2);
    z3::expr r = gen_i32(&I);
//    astAdd((
//        r == z3::ite(
//          ((a == i1_true()) && (b == i1_true())),
//          i1_true(), i1_false())
//        );
    z3::expr ex = (r == (a & b));
//    debug << "ex: " << ex << "\n";
    z3::expr all = z3::forall(arg_evec, ex);
//    debug << "all:" << all << "\n";
    astAdd(all);
  }

  void visitOr(BinaryOperator &I) {
    debug << "    visit or" << std::endl;
    auto op1 = I.getOperand(0);
    auto op2 = I.getOperand(1);
    z3::expr a = gen_i32(op1);
    z3::expr b = gen_i32(op2);
    z3::expr r = gen_i32(&I);
    astAdd(z3::forall(arg_evec, r == (a | b)));
  }

  void visitXor(BinaryOperator &I) {
    debug << "    visit xor" << std::endl;
    auto op1 = I.getOperand(0);
    auto op2 = I.getOperand(1);
    z3::expr a = gen_i32(op1);
    z3::expr b = gen_i32(op2);
    z3::expr r = gen_i32(&I);
    astAdd(z3::forall(arg_evec, r == (a ^ b)));
  }

  void visitICmp(ICmpInst &I) { 
    debug << "    visit icmp" << std::endl;
    auto op1 = I.getOperand(0);
    auto op2 = I.getOperand(1);
    z3::expr a = gen_i32(op1);
    z3::expr b = gen_i32(op2);
    // note: r here is bv_1
    z3::expr r = gen_i1(&I);
    
    auto cond = I.getPredicate();
    switch (cond) {
      case CmpInst::ICMP_EQ :  ///< equal 
        astAdd(z3::forall(arg_evec, r == z3::ite((a == b), i1_true(), i1_false()))/*simp*/);
        break;
      case CmpInst::ICMP_NE :  ///< not equal
        astAdd(z3::forall(arg_evec, r == z3::ite((a != b), i1_true(), i1_false()))/*simp*/);
        break;
      case CmpInst::ICMP_UGT:  ///< unsigned greater than
        astAdd(z3::forall(arg_evec, r == z3::ite(z3::ugt(a, b), i1_true(), i1_false()))/*simp*/);
        break;
      case CmpInst::ICMP_UGE:  ///< unsigned greater or equal
        astAdd(z3::forall(arg_evec, r == z3::ite(z3::uge(a, b), i1_true(), i1_false()))/*simp*/);
        break;
      case CmpInst::ICMP_ULT:  ///< unsigned less than
        astAdd(z3::forall(arg_evec, r == z3::ite(z3::ult(a, b), i1_true(), i1_false()))/*simp*/);
        break;
      case CmpInst::ICMP_ULE:  ///< unsigned less or equal
        astAdd(z3::forall(arg_evec, r == z3::ite(z3::ule(a, b), i1_true(), i1_false()))/*simp*/);
        break;
      case CmpInst::ICMP_SGT:  ///< signed greater than
        astAdd(z3::forall(arg_evec, r == z3::ite((a > b), i1_true(), i1_false()))/*simp*/);
        break;
      case CmpInst::ICMP_SGE:  ///< signed greater or equal
        astAdd(z3::forall(arg_evec, r == z3::ite((a >= b), i1_true(), i1_false()))/*simp*/);
        break;
      case CmpInst::ICMP_SLT:  ///< signed less than
        debug << "in slt" << std::endl;
        astAdd(z3::forall(arg_evec, r == z3::ite((a < b), i1_true(), i1_false()))/*simp*/);
        break;
      case CmpInst::ICMP_SLE:  ///< signed less or equal
        astAdd(z3::forall(arg_evec, r == z3::ite((a <= b), i1_true(), i1_false()))/*simp*/);
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
      z3::expr cd = gen_i1(cond);
      z3::expr tr = gen_i1(tar_tr);
      z3::expr fls = gen_i1(tar_fls);

      astAdd(z3::forall(arg_evec, tr == cd));
      astAdd(z3::forall(arg_evec, fls == z3::ite(
              (cd == i1_true()), i1_false(), i1_true()
            )));
    } else {
      auto tar = I.getOperand(0);     // true
      z3::expr tr = gen_i1(tar);
      astAdd(z3::forall(arg_evec, tr == i1_true()));
    }
  }

  void visitPHINode(PHINode &I) { 
    debug << "    visit phi" << std::endl;
    unsigned cnt = I.getNumIncomingValues();
    for (unsigned i = 0; i != cnt; i++) {
      auto val = I.getIncomingValue(i);
      auto bb = I.getIncomingBlock(i);
      z3::expr v = gen_i32(val);
      z3::expr b = gen_i1(bb);
      z3::expr dst = gen_i32(&I);
      astAdd(z3::forall(arg_evec, 
            z3::implies(
              (b == i1_true()), (dst == v)
            )));
    }
  }

  void visitSExtInst(SExtInst & I) {
    debug << "    visit sext" << std::endl;
    auto srcv = I.getOperand(0);
    z3::expr src = gen_i32(srcv);
    z3::expr dst = gen_i64(&I);
    astAdd(z3::forall(arg_evec, dst == z3::sext(src, 32)));
  }
  
  void visitZExtInst(ZExtInst & I) {
    debug << "    visit zext" << std::endl;
    auto srcv = I.getOperand(0);
    z3::expr src = gen_i32(srcv);
    z3::expr dst = gen_i64(&I);
    astAdd(z3::forall(arg_evec, dst == z3::zext(src, 32)));
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
          z3::expr ptr = gen_i64(ptrOp);
          z3::expr lb = ctx.bv_val(0, 64);
          z3::expr ub = ctx.bv_val(size, 64); 
          z3::expr inbounds = z3::forall(arg_evec, (ptr >= lb && ptr < ub));

          solver.push();
//          z3::expr check = (getAst(I.getFunction()) && !inbounds)
//#ifdef NDEBUG
//            .simplify()
//#endif
//          ;
          for (z3::expr e: predicate_map[getName(*current_fun)]) {
            solver.add(e);
          }
          solver.add(!inbounds); 
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

