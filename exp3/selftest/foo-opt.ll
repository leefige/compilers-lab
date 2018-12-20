; ModuleID = 'foo.ll'
source_filename = "foo.ll"

@a = external global i32, align 4

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
