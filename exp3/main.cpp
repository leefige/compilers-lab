/**
 * By Yifei Li
 * */
#define LOG
#include <map>
#include <string>
#include <stack>

#include <llvm/IR/CFG.h>
#include <llvm/IR/InstVisitor.h>
#include <llvm/IR/Instruction.h>
#include <llvm/IRReader/IRReader.h>
#include <llvm/Support/SourceMgr.h>
#include <llvm/Support/raw_ostream.h>
#include <llvm/ADT/SCCIterator.h>
#include <z3++.h>

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
class Z3Walker : public InstVisitor<Z3Walker> {
private:
  std::map<std::string, std::vector<z3::expr>> predicate_map;
  z3::context ctx;
  z3::solver solver;

  // gen 32-bit val
  z3::expr genBVConst(std::string name) {
    return ctx.bv_const(name.c_str(), 32);  
  }

  void parseBinary(const BinaryOperator &I, z3::expr& a, z3::expr& b, z3::expr& r) {
    auto op1 = I.llvm::User::getOperand(0);
    auto op2 = I.llvm::User::getOperand(1);
    a = genBVConst(getName(*op1));
    b = genBVConst(getName(*op2));
    r = genBVConst(getName(I));
  }

public:
  Z3Walker() : ctx(), solver(ctx) {
    std::cout << "new z3walker" << std::endl;
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
    std::cout << "<Func> " << getName(F) << ": ";
    solver.push();

//    std::cout << "ret type: ";
//    F.getReturnType()->print(std::cout);
    // parse args
    for (auto ait = F.arg_begin(); ait != F.arg_end(); ait++) {
      Argument* arg = &(*ait);
      auto argname = getName(*arg);
      std::cout << "; arg " << argname;
      z3::expr arg_ = genBVConst(argname);
      // intra-proc, no need of arg val, because u dont know it at all
      //solver.add(argname == arg->va)
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
    std::cout << "<Solver> " << solver << std::endl;
    solver.pop();
  }

  void visitBasicBlock(BasicBlock &B) {
    std::cout << "  <BB> " << getName(B) << std::endl;
    for (auto iit = B.begin(); iit != B.end(); iit++) {
      this->visit(*iit);
    }
  }

  void visitAdd(BinaryOperator &I) {
    std::cout << "    visit add" << std::endl;
    for (auto i = I.op_begin(); i != I.op_end(); i++) {
      std::cout << "\top: " << *i;
    }
    std::cout << std::endl;
    auto op1 = I.llvm::User::getOperand(0);
    auto op2 = I.llvm::User::getOperand(1);
    z3::expr a = genBVConst(getName(*op1));
    z3::expr b = genBVConst(getName(*op2));
    //solver.add(a == op1->va)
//    solver.add()
    // the Instruction itself is the ret val
    z3::expr r = genBVConst(getName(I));
    solver.add(r == a + b);
  }

  void visitSub(BinaryOperator &I) {
    std::cout << "    visit sub" << std::endl;
    auto op1 = I.llvm::User::getOperand(0);
    auto op2 = I.llvm::User::getOperand(1);
    z3::expr a = genBVConst(getName(*op1));
    z3::expr b = genBVConst(getName(*op2));
    z3::expr r = genBVConst(getName(I));
    solver.add(r == a - b);
  }
  
  void visitMul(BinaryOperator &I) {
    std::cout << "    visit mul" << std::endl;
    auto op1 = I.llvm::User::getOperand(0);
    auto op2 = I.llvm::User::getOperand(1);
    z3::expr a = genBVConst(getName(*op1));
    z3::expr b = genBVConst(getName(*op2));
    z3::expr r = genBVConst(getName(I));
    solver.add(r == a * b);
  }

  void visitShl(BinaryOperator &I) {
    std::cout << "    visit shl" << std::endl;
    auto op1 = I.llvm::User::getOperand(0);
    auto op2 = I.llvm::User::getOperand(1);
    z3::expr a = genBVConst(getName(*op1));
    z3::expr b = genBVConst(getName(*op2));
    z3::expr r = genBVConst(getName(I));
    solver.add(r == z3::shl(a, b));
  }

  void visitLShr(BinaryOperator &I) {
    std::cout << "    visit lshr" << std::endl;
    auto op1 = I.llvm::User::getOperand(0);
    auto op2 = I.llvm::User::getOperand(1);
    z3::expr a = genBVConst(getName(*op1));
    z3::expr b = genBVConst(getName(*op2));
    z3::expr r = genBVConst(getName(I));
    solver.add(r == z3::lshr(a, b));
  }

  void visitAShr(BinaryOperator &I) {
    std::cout << "    visit ashr" << std::endl;
    auto op1 = I.llvm::User::getOperand(0);
    auto op2 = I.llvm::User::getOperand(1);
    z3::expr a = genBVConst(getName(*op1));
    z3::expr b = genBVConst(getName(*op2));
    z3::expr r = genBVConst(getName(I));
    solver.add(r == z3::ashr(a, b));
  }
  void visitAnd(BinaryOperator &I) {
  
  }
  void visitOr(BinaryOperator &I) {
  
  }
  void visitXor(BinaryOperator &I) {
  
  }
  void visitICmp(ICmpInst &I) { 
    std::cout << "    visit icmp" << std::endl;
//    solver.add()
  }

  void visitBranchInst(BranchInst &I) {
  
    std::cout << "    visit br"<< std::endl;
  }
  void visitPHINode(PHINode &I) {
  
    std::cout << "    visit phi" << std::endl;
  }

  // Call checkAndReport here.
  void visitGetElementPtrInst(GetElementPtrInst &I) {
    std::cout << "    visit gep" << std::endl;
    checkAndReport(solver, I);
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

  printf("Now walking the module...\n");
  Z3Walker().visitModule(*module);

  return 0;
}
