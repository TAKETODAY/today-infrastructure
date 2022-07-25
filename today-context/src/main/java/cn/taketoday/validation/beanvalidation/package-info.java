/**
 * Support classes for integrating a JSR-303 Bean Validation provider
 * (such as Hibernate Validator) into a Framework ApplicationContext
 * and in particular with Framework's data binding and validation APIs.
 *
 * <p>The central class is {@link
 * cn.taketoday.validation.beanvalidation.LocalValidatorFactoryBean}
 * which defines a shared ValidatorFactory/Validator setup for availability
 * to other Framework components.
 */
@NonNullApi
@NonNullFields
package cn.taketoday.validation.beanvalidation;

import cn.taketoday.lang.NonNullApi;
import cn.taketoday.lang.NonNullFields;
