
/**
 * Support classes for integrating a JSR-303 Bean Validation provider
 * (such as Hibernate Validator) into a Framework ApplicationContext
 * and in particular with Framework's data binding and validation APIs.
 *
 * <p>The central class is {@link
 * infra.validation.beanvalidation.LocalValidatorFactoryBean}
 * which defines a shared ValidatorFactory/Validator setup for availability
 * to other Framework components.
 */
@NullMarked
package infra.validation.beanvalidation;

import org.jspecify.annotations.NullMarked;
