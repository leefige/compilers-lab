; ModuleID = 'foo.ll'
source_filename = "foo.ll"

@a = external global i32, align 4
@arr = external global [128 x i32], align 16

define i32 @foo(i32) {
  %2 = icmp eq i32 %0, 3
  br i1 %2, label %3, label %6

; <label>:3:                                      ; preds = %1
  %4 = add nsw i32 1, 2
  %5 = sub nsw i32 %4, %0
  br label %12

; <label>:6:                                      ; preds = %1
  %7 = load i32, i32* @a, align 4
  %8 = mul nsw i32 %0, %7
  %9 = sdiv i32 10, %0
  %10 = shl i32 2, %0
  %11 = ashr i32 16, %0
  br label %12

; <label>:12:                                     ; preds = %6, %3
  %.0 = phi i32 [ %5, %3 ], [ %8, %6 ]
  ret i32 %.0
}

define i32 @foo1(i32) {
  %2 = alloca [64 x i32], align 16
  %3 = load i32, i32* getelementptr inbounds ([128 x i32], [128 x i32]* @arr, i64 0, i64 31), align 4
  %4 = sext i32 %0 to i64
  %5 = getelementptr inbounds [64 x i32], [64 x i32]* %2, i64 0, i64 %4
  %6 = load i32, i32* %5, align 4
  %7 = add nsw i32 %6, %3
  store i32 %7, i32* %5, align 4
  %8 = and i32 16, %0
  %9 = sext i32 %8 to i64
  %10 = getelementptr inbounds [64 x i32], [64 x i32]* %2, i64 0, i64 %9
  %11 = load i32, i32* %10, align 4
  ret i32 %11
}

define void @bar(i32*) {
  %2 = load i32, i32* %0, align 4
  %3 = shl i32 %2, 4
  %4 = load i32, i32* %0, align 4
  %5 = and i32 32, %4
  %6 = or i32 8, %3
  %7 = load i32, i32* %0, align 4
  %8 = xor i32 %7, %6
  store i32 %8, i32* %0, align 4
  ret void
}

define i32 @foobar() {
  %1 = alloca i32, align 4
  %2 = load i32, i32* @a, align 4
  %3 = call i32 @foo(i32 %2)
  store i32 %3, i32* %1, align 4
  %4 = load i32, i32* %1, align 4
  %5 = and i32 %4, 32
  %6 = call i32 @foo1(i32 %5)
  %7 = load i32, i32* %1, align 4
  %8 = xor i32 %7, %6
  store i32 %8, i32* %1, align 4
  call void @bar(i32* %1)
  %9 = load i32, i32* %1, align 4
  ret i32 %9
}
