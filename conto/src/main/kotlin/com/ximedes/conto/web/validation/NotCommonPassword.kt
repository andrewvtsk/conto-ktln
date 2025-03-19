package com.ximedes.conto.web.validation

import com.ximedes.conto.core.service.UserService
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import javax.validation.Constraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import kotlin.reflect.KClass

/**
 * A field-level validation annotation that checks if
 * a String is present in the list of common passwords.
 *
 * This is just the annotation definition. The actual validation is
 * done by [NotCommonPasswordValidator]
 */
@Target(AnnotationTarget.PROPERTY_GETTER)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = [NotCommonPasswordValidator::class])
annotation class NotCommonPassword(
    val message: String = "The chosen password is too common",
    val groups: Array<KClass<out Any>> = [],
    val payload: Array<KClass<out Any>> = []
)


/**
 * Validator that checks if a given String is a common
 * password.
 */
class NotCommonPasswordValidator(private val userService: UserService) :
    ConstraintValidator<NotCommonPassword?, String?> {
    override fun isValid(password: String?, context: ConstraintValidatorContext): Boolean {
        return !userService.isCommonPassword(password)
    }

}