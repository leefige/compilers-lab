@a = external global i32, align 4

; Function Attrs: noinline nounwind optnone uwtable
define i32 @foo(i32) #0 {
  %2 = alloca i32, align 4
  %3 = alloca i32, align 4
  store i32 %0, i32* %3, align 4
  %4 = load i32, i32* %3, align 4
  %5 = icmp eq i32 %4, 3
  br i1 %5, label %6, label %8

; <label>:6:                                      ; preds = %1
  %7 = load i32, i32* @a, align 4
  store i32 %7, i32* %2, align 4
  br label %10

; <label>:8:                                      ; preds = %1
  %9 = load i32, i32* %3, align 4
  store i32 %9, i32* %2, align 4
  br label %10

; <label>:10:                                     ; preds = %8, %6
  %11 = load i32, i32* %2, align 4
  ret i32 %11
}

