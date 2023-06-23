/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.aop.aspectj.annotation;

import java.util.List;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.aspectj.AbstractAspectJAdvice;
import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.hint.ExecutableMode;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotContribution;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationCode;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

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
  private static class AspectDelegate {

    @Nullable
    private static AspectContribution processAheadOfTime(ConfigurableBeanFactory beanFactory) {
      BeanFactoryAspectJAdvisorsBuilder builder = new BeanFactoryAspectJAdvisorsBuilder(beanFactory);
      List<Advisor> advisors = builder.buildAspectJAdvisors();
      return advisors.isEmpty() ? null : new AspectContribution(advisors);
    }

  }

  private static class AspectContribution implements BeanFactoryInitializationAotContribution {

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
