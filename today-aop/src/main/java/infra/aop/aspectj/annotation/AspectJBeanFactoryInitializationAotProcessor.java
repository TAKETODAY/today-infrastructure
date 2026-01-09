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
