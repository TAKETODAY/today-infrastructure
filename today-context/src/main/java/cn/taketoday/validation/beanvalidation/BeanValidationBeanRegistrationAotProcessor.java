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

import java.util.Collection;
import java.util.HashSet;

import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotContribution;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotProcessor;
import cn.taketoday.beans.factory.aot.BeanRegistrationCode;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.NoProviderFoundException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.metadata.BeanDescriptor;
import jakarta.validation.metadata.ConstraintDescriptor;
import jakarta.validation.metadata.ConstructorDescriptor;
import jakarta.validation.metadata.MethodType;
import jakarta.validation.metadata.ParameterDescriptor;
import jakarta.validation.metadata.PropertyDescriptor;

/**
 * AOT {@code BeanRegistrationAotProcessor} that adds additional hints
 * required for {@link ConstraintValidator}s.
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/24 14:06
 */
class BeanValidationBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

  private static final boolean beanValidationPresent = ClassUtils.isPresent(
          "jakarta.validation.Validation", BeanValidationBeanRegistrationAotProcessor.class.getClassLoader());

  private static final Logger logger = LoggerFactory.getLogger(BeanValidationBeanRegistrationAotProcessor.class);

  @Override
  @Nullable
  public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
    if (beanValidationPresent) {
      return BeanValidationDelegate.processAheadOfTime(registeredBean);
    }
    return null;
  }

  /**
   * Inner class to avoid a hard dependency on the Bean Validation API at runtime.
   */
  private static class BeanValidationDelegate {

    @Nullable
    private static final Validator validator = getValidatorIfAvailable();

    @Nullable
    private static Validator getValidatorIfAvailable() {
      try {
        return Validation.buildDefaultValidatorFactory().getValidator();
      }
      catch (NoProviderFoundException ex) {
        logger.info("No Bean Validation provider available - skipping validation constraint hint inference");
        return null;
      }
    }

    @Nullable
    public static BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
      if (validator == null) {
        return null;
      }

      BeanDescriptor descriptor;
      try {
        descriptor = validator.getConstraintsForClass(registeredBean.getBeanClass());
      }
      catch (RuntimeException ex) {
        if (ex instanceof TypeNotPresentException) {
          logger.debug("Skipping validation constraint hint inference for bean " +
                  registeredBean.getBeanName() + " due to a TypeNotPresentException at validator level: " + ex.getMessage());
        }
        else {
          logger.warn("Skipping validation constraint hint inference for bean " +
                  registeredBean.getBeanName(), ex);
        }
        return null;
      }

      var constraintDescriptors = new HashSet<ConstraintDescriptor<?>>();
      for (var methodDescriptor : descriptor.getConstrainedMethods(MethodType.NON_GETTER, MethodType.GETTER)) {
        for (ParameterDescriptor parameterDescriptor : methodDescriptor.getParameterDescriptors()) {
          constraintDescriptors.addAll(parameterDescriptor.getConstraintDescriptors());
        }
      }
      for (ConstructorDescriptor constructorDescriptor : descriptor.getConstrainedConstructors()) {
        for (ParameterDescriptor parameterDescriptor : constructorDescriptor.getParameterDescriptors()) {
          constraintDescriptors.addAll(parameterDescriptor.getConstraintDescriptors());
        }
      }
      for (PropertyDescriptor propertyDescriptor : descriptor.getConstrainedProperties()) {
        constraintDescriptors.addAll(propertyDescriptor.getConstraintDescriptors());
      }
      if (!constraintDescriptors.isEmpty()) {
        return new AotContribution(constraintDescriptors);
      }
      return null;
    }
  }

  private static class AotContribution implements BeanRegistrationAotContribution {

    private final Collection<ConstraintDescriptor<?>> constraintDescriptors;

    public AotContribution(Collection<ConstraintDescriptor<?>> constraintDescriptors) {
      this.constraintDescriptors = constraintDescriptors;
    }

    @Override
    public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
      for (ConstraintDescriptor<?> constraintDescriptor : this.constraintDescriptors) {
        for (Class<?> constraintValidatorClass : constraintDescriptor.getConstraintValidatorClasses()) {
          generationContext.getRuntimeHints().reflection()
                  .registerType(constraintValidatorClass, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
        }
      }
    }
  }

}
