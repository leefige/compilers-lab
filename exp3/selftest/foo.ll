@a = external global i32, align 4
@arr = external global [128 x i32], align 16

; Function Attrs: noinline nounwind optnone uwtable
define i32 @foo__(i32, i32) #0 {
  %3 = alloca i32, align 4
  %4 = alloca i32, align 4
  %5 = alloca [128 x i32], align 16
  %6 = alloca i32, align 4
  store i32 %0, i32* %3, align 4
  store i32 %1, i32* %4, align 4
  %7 = load i32, i32* %3, align 4
  %8 = icmp sgt i32 %7, 0
  br i1 %8, label %9, label %35

; <label>:9:                                      ; preds = %2
  %10 = load i32, i32* %4, align 4
  %11 = load i32, i32* %4, align 4
  %12 = xor i32 %11, %10
  store i32 %12, i32* %4, align 4
  %13 = load i32, i32* %4, align 4
  %14 = load i32, i32* %3, align 4
  %15 = add nsw i32 %13, %14
  store i32 %15, i32* %4, align 4
  %16 = load i32, i32* %4, align 4
  %17 = shl i32 %16, 1
  store i32 %17, i32* %4, align 4
  %18 = load i32, i32* %4, align 4
  %19 = icmp slt i32 %18, 64
  br i1 %19, label %20, label %30

; <label>:20:                                     ; preds = %9
  %21 = load i32, i32* %4, align 4
  %22 = sext i32 %21 to i64
  %23 = getelementptr inbounds [128 x i32], [128 x i32]* %5, i64 0, i64 %22
  %24 = load i32, i32* %23, align 4
  %25 = load i32, i32* %3, align 4
  %26 = add nsw i32 %24, %25
  store i32 %26, i32* %6, align 4
  %27 = load i32, i32* %6, align 4
  %28 = load i32, i32* %4, align 4
  %29 = xor i32 %28, %27
  store i32 %29, i32* %4, align 4
  br label %34

; <label>:30:                                     ; preds = %9
  %31 = load i32, i32* %3, align 4
  %32 = load i32, i32* %4, align 4
  %33 = or i32 %32, %31
  store i32 %33, i32* %4, align 4
  br label %34

; <label>:34:                                     ; preds = %30, %20
  br label %39

; <label>:35:                                     ; preds = %2
  %36 = load i32, i32* %3, align 4
  %37 = load i32, i32* %4, align 4
  %38 = and i32 %37, %36
  store i32 %38, i32* %4, align 4
  br label %39

; <label>:39:                                     ; preds = %35, %34
  %40 = load i32, i32* %4, align 4
  ret i32 %40
}

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

; Function Attrs: noinline nounwind optnone uwtable
define i32 @foo1(i32) #0 {
  %2 = alloca i32, align 4
  %3 = alloca [64 x i32], align 16
  store i32 %0, i32* %2, align 4
  %4 = load i32, i32* getelementptr inbounds ([128 x i32], [128 x i32]* @arr, i64 0, i64 31), align 4
  %5 = load i32, i32* %2, align 4
  %6 = sext i32 %5 to i64
  %7 = getelementptr inbounds [64 x i32], [64 x i32]* %3, i64 0, i64 %6
  %8 = load i32, i32* %7, align 4
  %9 = add nsw i32 %8, %4
  store i32 %9, i32* %7, align 4
  %10 = load i32, i32* %2, align 4
  %11 = sext i32 %10 to i64
  %12 = getelementptr inbounds [64 x i32], [64 x i32]* %3, i64 0, i64 %11
  %13 = load i32, i32* %12, align 4
  ret i32 %13
}

; Function Attrs: noinline nounwind optnone uwtable
define void @bar(i32*) #0 {
  %2 = alloca i32*, align 8
  %3 = alloca i32, align 4
  %4 = alloca i32, align 4
  store i32* %0, i32** %2, align 8
  %5 = load i32*, i32** %2, align 8
  %6 = load i32, i32* %5, align 4
  %7 = shl i32 %6, 4
  store i32 %7, i32* %3, align 4
  store i32 32, i32* %4, align 4
  %8 = load i32, i32* %4, align 4
  %9 = load i32*, i32** %2, align 8
  %10 = load i32, i32* %9, align 4
  %11 = and i32 %8, %10
  store i32 %11, i32* %4, align 4
  %12 = load i32, i32* %3, align 4
  %13 = or i32 8, %12
  store i32 %13, i32* %4, align 4
  %14 = load i32*, i32** %2, align 8
  %15 = load i32, i32* %14, align 4
  %16 = load i32, i32* %4, align 4
  %17 = xor i32 %15, %16
  store i32 %17, i32* %3, align 4
  %18 = load i32, i32* %3, align 4
  %19 = load i32*, i32** %2, align 8
  store i32 %18, i32* %19, align 4
  ret void
}

; Function Attrs: noinline nounwind optnone uwtable
define i32 @foobar() #0 {
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

