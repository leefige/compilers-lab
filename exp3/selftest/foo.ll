@a = external global i32, align 4

; Function Attrs: noinline nounwind optnone uwtable
define i32 @foo(i32) #0 {
  %2 = alloca i32, align 4
  %3 = alloca i32, align 4
  %4 = alloca i32, align 4
  %5 = alloca i32, align 4
  %6 = alloca i32, align 4
  %7 = alloca i32, align 4
  %8 = alloca i32, align 4
  %9 = alloca i32, align 4
  %10 = alloca i32, align 4
  store i32 %0, i32* %3, align 4
  %11 = load i32, i32* %3, align 4
  %12 = icmp eq i32 %11, 3
  br i1 %12, label %13, label %21

; <label>:13:                                     ; preds = %1
  store i32 1, i32* %4, align 4
  store i32 2, i32* %5, align 4
  %14 = load i32, i32* %4, align 4
  %15 = load i32, i32* %5, align 4
  %16 = add nsw i32 %14, %15
  store i32 %16, i32* %6, align 4
  %17 = load i32, i32* %6, align 4
  %18 = load i32, i32* %3, align 4
  %19 = sub nsw i32 %17, %18
  store i32 %19, i32* %6, align 4
  %20 = load i32, i32* %6, align 4
  store i32 %20, i32* %2, align 4
  br label %32

; <label>:21:                                     ; preds = %1
  %22 = load i32, i32* %3, align 4
  %23 = load i32, i32* @a, align 4
  %24 = mul nsw i32 %22, %23
  store i32 %24, i32* %7, align 4
  %25 = load i32, i32* %3, align 4
  %26 = sdiv i32 10, %25
  store i32 %26, i32* %8, align 4
  %27 = load i32, i32* %3, align 4
  %28 = shl i32 2, %27
  store i32 %28, i32* %9, align 4
  %29 = load i32, i32* %3, align 4
  %30 = ashr i32 16, %29
  store i32 %30, i32* %10, align 4
  %31 = load i32, i32* %7, align 4
  store i32 %31, i32* %2, align 4
  br label %32

; <label>:32:                                     ; preds = %21, %13
  %33 = load i32, i32* %2, align 4
  ret i32 %33
}
