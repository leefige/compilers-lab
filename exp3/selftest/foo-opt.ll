; ModuleID = 'foo.ll'
source_filename = "foo.ll"

@a = external global i32, align 4

define i32 @foo(i32) {
  %2 = icmp eq i32 %0, 3
  br i1 %2, label %3, label %5

; <label>:3:                                      ; preds = %1
  %4 = load i32, i32* @a, align 4
  br label %6

; <label>:5:                                      ; preds = %1
  br label %6

; <label>:6:                                      ; preds = %5, %3
  %.0 = phi i32 [ %4, %3 ], [ %0, %5 ]
  ret i32 %.0
}
