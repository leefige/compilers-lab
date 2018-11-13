# Design Introduction

Yifei Li, 2015010062

> Describe your design for extra credit in this file.
> 
> Give a high level description on each of your optimization techniques. We don't expect too much details. Usually less than 5 sentences should be enough to describe each optimization. But make sure we understand what you are doing.

## 0. Optimization Framework

The process of optimization is divided into 2 steps:
1. Use data flow analysis to calculate some data flow characteristics;
2. Use a *modifier* to change / add / eliminate Basic Blocks / Quads.

So I introduce `OptDriver` as a driver for the optimization, and a new *interface* `Modifier` which is used to modify 
the control flow graph.

The `OptDriver` firstly call an instance of `Flow::Analysis` to calculate data flow, and then call an instance of 
corresponding `Modifier` to get the result of previous calculation and modify the `cfg`.

Each pair of `Analysis` and `Modifier` is packaged in a package in `submit`, and the `Analysis` is often a subclass of 
an existing class.

In addition, the driver support this cycle to run more than one times, and the `Modifier` will decide whether to stop.

## 1. Constant Propagation & Constant Folding

The *Modifier* `ConstFolding` firstly use the `in` value of each quad to detect whether the registers used in this quad 
are constants. 
If so, it will change the `Register Operand` into an `IConst Operand`. 

Then it will iterate the `cfg` again, and check whether the 2 operands of an `ADD` quad are both `IConst Operand`. 
If so, it will transfer this quad into a `MOVE` quad and directly calculate the sum of the 2 constants.

## 2. Fainted Variable Elimination

The *Modifier* `FaintEliminate` use the `out` value to check whether the register defined in one quad is fainted 
after this quad. If so, we don't need to defined this register, and it will remove this quad from `cfg`.

What's more, I improve the rule for `NULL_CHECK`. Since we know it will not define any register and the register it 
checks is actually used in other quads, I just don't set the register used in `NULL_CHECK` to fainted. And in `Modifier`, 
if the register checked (used) in `NULL_CHECK`is actually fainted, we just remove this `NULL_CHEKC` quad.

This process may iterate for several times until no change is made in one cycle.