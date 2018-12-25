@arr = external global [1024 x i32], align 16

; Function Attrs: noinline nounwind optnone uwtable
define i32 @test(i32) #0 {
  %2 = alloca i32, align 4
  store i32 %0, i32* %2, align 4
  %3 = load i32, i32* %2, align 4
  %4 = sext i32 %3 to i64
  %5 = getelementptr inbounds [1024 x i32], [1024 x i32]* @arr, i64 0, i64 %4
  %6 = load i32, i32* %5, align 4
  ret i32 %6
}
