; ModuleID = 'foo.ll'
source_filename = "foo.ll"

@a = external global i32, align 4

define i32 @foo(i32) {
  %2 = icmp eq i32 %0, 3
  br i1 %2, label %3, label %5

; <label>:3:                                      ; preds = %1
  %4 = add nsw i32 1, 2
  br label %8

; <label>:5:                                      ; preds = %1
  %6 = load i32, i32* @a, align 4
  %7 = mul nsw i32 %0, %6
  br label %8

; <label>:8:                                      ; preds = %5, %3
  %.0 = phi i32 [ %4, %3 ], [ %7, %5 ]
  ret i32 %.0
}
