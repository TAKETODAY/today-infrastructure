/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.aop.aspectj.annotation;

import java.lang.reflect.Field;

import infra.aot.generate.GenerationContext;
import infra.aot.hint.MemberCategory;
import infra.beans.factory.aot.BeanRegistrationAotContribution;
import infra.beans.factory.aot.BeanRegistrationAotProcessor;
import infra.beans.factory.aot.BeanRegistrationCode;
import infra.beans.factory.support.RegisteredBean;
import infra.lang.Nullable;
import infra.util.ClassUtils;

/**
 * An AOT {@link BeanRegistrationAotProcessor} that detects the presence of
 * classes compiled with AspectJ and adds the related required field hints.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class AspectJAdvisorBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

  private static final String AJC_MAGIC = "ajc$";

  private static final boolean aspectjPresent = ClassUtils.isPresent("org.aspectj.lang.annotation.Pointcut",
          AspectJAdvisorBeanRegistrationAotProcessor.class.getClassLoader());

  @Override
  public @Nullable BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
    if (aspectjPresent) {
      Class<?> beanClass = registeredBean.getBeanClass();
      if (compiledByAjc(beanClass)) {
        return new AspectJAdvisorContribution(beanClass);
      }
    }
    return null;
  }

  private static boolean compiledByAjc(Class<?> clazz) {
    for (Field field : clazz.getDeclaredFields()) {
      if (field.getName().startsWith(AJC_MAGIC)) {
        return true;
      }
    }
    return false;
  }

  private static class AspectJAdvisorContribution implements BeanRegistrationAotContribution {

    private final Class<?> beanClass;

    public AspectJAdvisorContribution(Class<?> beanClass) {
      this.beanClass = beanClass;
    }

    @Override
    public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
      generationContext.getRuntimeHints().reflection().registerType(this.beanClass, MemberCategory.ACCESS_DECLARED_FIELDS);
    }
  }

}
