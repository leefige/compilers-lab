; ModuleID = 'little.ll'
source_filename = "little.ll"

@arr = external global [1024 x i32], align 16

define i32 @test(i32) {
  %2 = sext i32 %0 to i64
  %3 = getelementptr inbounds [1024 x i32], [1024 x i32]* @arr, i64 0, i64 %2
  %4 = load i32, i32* %3, align 4
  ret i32 %4
}
