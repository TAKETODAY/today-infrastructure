/*
 * Copyright 2017 - 2023 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.validation.beanvalidation;

import org.aopalliance.aop.Advice;

import java.lang.annotation.Annotation;

import cn.taketoday.aop.Pointcut;
import cn.taketoday.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.aop.support.annotation.AnnotationMatchingPointcut;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.config.BeanPostProcessor;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.validation.annotation.Validated;
import cn.taketoday.validation.method.MethodValidationException;
import cn.taketoday.validation.method.MethodValidationResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * A convenient {@link BeanPostProcessor} implementation that delegates to a
 * JSR-303 provider for performing method-level validation on annotated methods.
 *
 * <p>Applicable methods have JSR-303 constraint annotations on their parameters
 * and/or on their return value (in the latter case specified at the method level,
 * typically as inline annotation), e.g.:
 *
 * <pre>{@code
 * public @NotNull Object myValidMethod(@NotNull String arg1, @Max(10) int arg2) {
 *
 * }
 * }</pre>
 *
 * <p>In case of validation errors, the interceptor can raise
 * {@link ConstraintViolationException}, or adapt the violations to
 * {@link MethodValidationResult} and raise {@link MethodValidationException}.
 *
 * <p>Target classes with such annotated methods need to be annotated with Framework's
 * {@link Validated} annotation at the type level, for their methods to be searched for
 * inline constraint annotations. Validation groups can be specified through {@code @Validated}
 * as well. By default, JSR-303 will validate against its default group only.
 *
 * <p>this functionality requires a Bean Validation 1.1+ provider.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MethodValidationInterceptor
 * @see jakarta.validation.executable.ExecutableValidator
 * @since 4.0
 */
@SuppressWarnings("serial")
public class MethodValidationPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor
        implements InitializingBean {

  private Class<? extends Annotation> validatedAnnotationType = Validated.class;

  @Nullable
  private Validator validator;

  private boolean adaptConstraintViolations;

  /**
   * Set the 'validated' annotation type.
   * The default validated annotation type is the {@link Validated} annotation.
   * <p>This setter property exists so that developers can provide their own
   * (non-Framework-specific) annotation type to indicate that a class is supposed
   * to be validated in the sense of applying method validation.
   *
   * @param validatedAnnotationType the desired annotation type
   */
  public void setValidatedAnnotationType(Class<? extends Annotation> validatedAnnotationType) {
    Assert.notNull(validatedAnnotationType, "'validatedAnnotationType' is required");
    this.validatedAnnotationType = validatedAnnotationType;
  }

  /**
   * Set the JSR-303 Validator to delegate to for validating methods.
   * <p>Default is the default ValidatorFactory's default Validator.
   */
  public void setValidator(Validator validator) {
    // Unwrap to the native Validator with forExecutables support
    if (validator instanceof LocalValidatorFactoryBean) {
      this.validator = ((LocalValidatorFactoryBean) validator).getValidator();
    }
    else if (validator instanceof InfraValidatorAdapter) {
      this.validator = validator.unwrap(Validator.class);
    }
    else {
      this.validator = validator;
    }
  }

  /**
   * Set the JSR-303 ValidatorFactory to delegate to for validating methods,
   * using its default Validator.
   * <p>Default is the default ValidatorFactory's default Validator.
   *
   * @see ValidatorFactory#getValidator()
   */
  public void setValidatorFactory(ValidatorFactory validatorFactory) {
    this.validator = validatorFactory.getValidator();
  }

  /**
   * Whether to adapt {@link ConstraintViolation}s to {@link MethodValidationResult}.
   * <p>By default {@code false} in which case
   * {@link jakarta.validation.ConstraintViolationException} is raised in case of
   * violations. When set to {@code true}, {@link MethodValidationException}
   * is raised instead with the method validation results.
   */
  public void setAdaptConstraintViolations(boolean adaptViolations) {
    this.adaptConstraintViolations = adaptViolations;
  }

  @Override
  public void afterPropertiesSet() {
    Pointcut pointcut = new AnnotationMatchingPointcut(validatedAnnotationType, true);
    this.advisor = new DefaultPointcutAdvisor(pointcut, createMethodValidationAdvice(validator));
  }

  /**
   * Create AOP advice for method validation purposes, to be applied
   * with a pointcut for the specified 'validated' annotation.
   *
   * @param validator the JSR-303 Validator to delegate to
   * @return the interceptor to use (typically, but not necessarily,
   * a {@link MethodValidationInterceptor} or subclass thereof)
   */
  protected Advice createMethodValidationAdvice(@Nullable Validator validator) {
    return validator != null
           ? new MethodValidationInterceptor(validator, adaptConstraintViolations)
           : new MethodValidationInterceptor(adaptConstraintViolations);
  }

}
