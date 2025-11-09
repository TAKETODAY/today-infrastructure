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

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.aop.Advisor;
import infra.aop.aspectj.AbstractAspectJAdvice;
import infra.aot.generate.GenerationContext;
import infra.aot.hint.ExecutableMode;
import infra.aot.hint.ReflectionHints;
import infra.beans.factory.aot.BeanFactoryInitializationAotContribution;
import infra.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import infra.beans.factory.aot.BeanFactoryInitializationCode;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.util.ClassUtils;

/**
 * {@link BeanFactoryInitializationAotProcessor} implementation responsible for registering
 * hints for AOP advices.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/23 10:55
 */
class AspectJBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

  private static final boolean aspectJPresent = ClassUtils.isPresent(
          "org.aspectj.lang.annotation.Pointcut", AspectJBeanFactoryInitializationAotProcessor.class.getClassLoader());

  @Nullable
  @Override
  public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableBeanFactory beanFactory) {
    if (aspectJPresent) {
      return AspectDelegate.processAheadOfTime(beanFactory);
    }
    return null;
  }

  /**
   * Inner class to avoid a hard dependency on AspectJ at runtime.
   */
  private static final class AspectDelegate {

    @Nullable
    private static AspectContribution processAheadOfTime(ConfigurableBeanFactory beanFactory) {
      BeanFactoryAspectJAdvisorsBuilder builder = new BeanFactoryAspectJAdvisorsBuilder(beanFactory);
      List<Advisor> advisors = builder.buildAspectJAdvisors();
      return advisors.isEmpty() ? null : new AspectContribution(advisors);
    }

  }

  private static final class AspectContribution implements BeanFactoryInitializationAotContribution {

    private final List<Advisor> advisors;

    public AspectContribution(List<Advisor> advisors) {
      this.advisors = advisors;
    }

    @Override
    public void applyTo(GenerationContext generationContext,
            BeanFactoryInitializationCode beanFactoryInitializationCode) {
      ReflectionHints reflectionHints = generationContext.getRuntimeHints().reflection();
      for (Advisor advisor : this.advisors) {
        if (advisor.getAdvice() instanceof AbstractAspectJAdvice aspectJAdvice) {
          reflectionHints.registerMethod(aspectJAdvice.getAspectJAdviceMethod(), ExecutableMode.INVOKE);
        }
      }
    }

  }

}
