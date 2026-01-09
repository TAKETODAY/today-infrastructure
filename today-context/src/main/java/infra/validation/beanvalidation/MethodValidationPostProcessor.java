/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.validation.beanvalidation;

import org.aopalliance.aop.Advice;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

import infra.aop.Pointcut;
import infra.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import infra.aop.support.DefaultPointcutAdvisor;
import infra.aop.support.annotation.AnnotationMatchingPointcut;
import infra.beans.factory.InitializingBean;
import infra.beans.factory.config.BeanPostProcessor;
import infra.lang.Assert;
import infra.validation.annotation.Validated;
import infra.validation.method.MethodValidationException;
import infra.validation.method.MethodValidationResult;
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
   * Set a lazily initialized Validator to delegate to for validating methods.
   *
   * @see #setValidator
   * @since 5.0
   */
  public void setValidatorProvider(Supplier<Validator> validatorProvider) {
    this.validator = new SuppliedValidator(validatorProvider);
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
