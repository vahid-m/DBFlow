package com.dbflow5.codegen.shared.validation

import com.dbflow5.codegen.shared.ClassModel

class ClassValidator(
    private val fieldValidator: FieldValidator,
) : GroupedValidator<ClassModel>(
    listOf(
        PrimaryValidator(),
        ClassToFieldValidator(fieldValidator)
    )
)
