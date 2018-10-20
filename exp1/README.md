# README: 实验1 基于JoeQ的数据流分析

> 最终提交时间: 2018/10/18 21:51

## 0. 这是什么？

这是对本次实验基本过程、一些重点环节和我的工作及感想的简述。

## 1. 实验过程

本实验分为三步，按照下面的顺序完成：

1. 补全MySolver框架，即 Data Flow 算法框架
2. 实现Reaching Definition（一个前向算法）
3. 实现Faintness（一个后向算法），并设计相关测例

## 2. MySolver

这部分主要实现课本上的算法9-25，有以下要点：

1. 注册analysis：由于是框架，需要可扩充不同的分析器，在`registerAnalysis`中为`this.analysis`赋值
2. 确定分析方向：分为前向和后向两类，直接调用analysis的接口`isForward()`即可获取
3. 获取TOP：不同的算法有各自的TOP值，调用analysis的接口`newTempVar()`获取一个变量，再调用其`setToTop()`
4. 初始化：遍历所有Quad，将它们的IN/OUT设为TOP（根据是否为forward判断IN/OUT）
5. 分析过程：在全部Quad的OUT/IN不再变化（收敛）前，重复遍历所有Quad，执行下面操作：
    1. 根据前驱/后继，使用`meetWith()`更新IN/OUT，注意，若前驱/后继有null，说明连接了ENTRY/EXIT，相应用`analysis`的`entry`/`exit`作为其IN/OUT
    2. 获取旧的OUT/IN值
    3. 调用`analysis.processQuad()`处理当前Quad，并获取新的OUT/IN
    4. 比较新旧OUT/IN，若变化，则设置一个标志，说明之后还需继续循环，直到不变时，跳出到5
    5. 最后根据所有与EXIT/ENTRY相连的Quad，使用`meetWith()`更新`exit`/`entry`

## 3. Reaching Definition

这部分主要填充`analysis`的算法框架，是一种前向算法：

1. 设计IN/OUT的变量，继承自 `Flow.DataflowObject`，我称为`DefSet`，类似Liveness，用TreeSet保存各定值点，TOP即为空集，BOTTOM即为全集，meet操作为并集，gen为直接添加进集合，kill则是判断所有被定值变量与当前Quad相同的定值点，把它们remove
2. `preprocess`：在框架基础上，主要添加对BOTTOM全集的初始化，遍历所有Quad找出所有定值点，建立定值点到被定值变量的MAP，用TreeMap保存
3. `postprocess`：不变
4. `entry`, `newTempVar()`：TOP，即空集
5. `TransferFunction`：对所有Quad操作相同，重载`visitQuad()`，若该Quad为定值点，则先kill所有定值相同变量的定值点，再gen该定值点

## 4. Faintness

这部分主要填充`analysis`的算法框架，是一种后向算法：

1. 设计IN/OUT的变量，继承自 `Flow.DataflowObject`，我称为`VarSet`，类似Liveness，用TreeSet保存各变量，TOP即为全集，BOTTOM即为空集，meet操作为交集
2. 定义两个操作：`wakeVar`为直接从Faint变量集合中remove该变量（即该变量活跃，或awake），`faintVar`则是用来传递faint，参数为use和def，若def为faint，则use也为faint，否则`wakeVar(use)`
3. `preprocess`：在框架基础上，主要添加对全集的初始化，遍历所有Quad找出所有变量
4. `postprocess`：不变
5. `exit`, `newTempVar()`：TOP，即全集
6. `TransferFunction`：针对`BINARY`和`MOVE`考虑faint传播，对其他所有Quad操作相同用`visitQuad()`：
    - 重载`visitMove()`和`visitBinary()`，调用`faintVar`，尝试对used变量进行faintness传播
    - 重载`visitQuad()`，just wake up all used registers
7. 编写一些测例，包括了对函数返回值、函数参数、if分支、switch-case和主函数输出字符串的测试。

## 5. 一些便利化的工作

首先，如助教所言，没错ssh输入大量命令很麻烦，因此我配置了`~/.ssh/config`，之后ssh和sftp都只需输入`ssh compiler`或`sftp compiler`即可。

其次，测试是一件重要而麻烦的事，而且手工比对很容易出错，因此程序员很喜欢测试自动化，我也不例外。我编写了简易的测试脚本`check.sh`，用来对输出结果进行比对。

原理很简单，调用`run.sh`，根据输入设定好参数，将输出保存为log，用`diff`比对文件即可。但对于测试的方便性和准确性的提升是巨大的。

另外，为了保证结果为最新的，每次测试将重新make，这是吸取了IDE有时忘记build就测试的教训。

类似的，在IDE环境下我也写了一个测试脚本，对IDE配置做一些修改，运行时将log写入指定文件，执行脚本进行`diff`即可。

## 6. 一点感想

本次实验我动手比较早，虽然有从容的好处，但也因为后续文档和测例都有更新，所以在前期遇到一点困扰，如原始文档中的`Quads.successors()`，不过这些都在文档更新前，我自己通过阅读JoeQ源码，分析出其实使用的是`QuadIterator`得到解决。

另一方面，个人认为最难的部分其实在于一开始动手完成Solver的过程，因为这是一个摸索实验框架的过程，之后填充算法都是在已经明白Solver框架的基础上，会容易很多。而我对Solver的认识有一个重大的飞跃，是从我做了第三次作业得到的，这次作业的9.3.4题颇有难度，我对课本反复研读，对整个Data Flow算法的流程、原理、算法，以及迭代算法与理想解、MOP解的关系都有了更深入的认识，比如遍历顺序其实没有影响，这一点虽然在做实验中似乎没有体现，但在一些细节的思考上心里会更有底一点。当然，在完成作业3后，我对算法9-25更是烂熟于心，再看Solver框架就容易了很多，也顿时理解了analysis中entry/exit变量的含义。
