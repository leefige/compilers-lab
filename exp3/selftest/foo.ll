@a = external global i32, align 4

; Function Attrs: noinline nounwind optnone uwtable
define i32 @foo(i32) #0 {
  %2 = alloca i32, align 4
  %3 = alloca i32, align 4
  %4 = alloca i32, align 4
  %5 = alloca i32, align 4
  %6 = alloca i32, align 4
  %7 = alloca i32, align 4
  store i32 %0, i32* %3, align 4
  %8 = load i32, i32* %3, align 4
  %9 = icmp eq i32 %8, 3
  br i1 %9, label %10, label %15

; <label>:10:                                     ; preds = %1
  store i32 1, i32* %4, align 4
  store i32 2, i32* %5, align 4
  %11 = load i32, i32* %4, align 4
  %12 = load i32, i32* %5, align 4
  %13 = add nsw i32 %11, %12
  store i32 %13, i32* %6, align 4
  %14 = load i32, i32* %6, align 4
  store i32 %14, i32* %2, align 4
  br label %20

; <label>:15:                                     ; preds = %1
  %16 = load i32, i32* %3, align 4
  %17 = load i32, i32* @a, align 4
  %18 = mul nsw i32 %16, %17
  store i32 %18, i32* %7, align 4
  %19 = load i32, i32* %7, align 4
  store i32 %19, i32* %2, align 4
  br label %20

; <label>:20:                                     ; preds = %15, %10
  %21 = load i32, i32* %2, align 4
  ret i32 %21
}

