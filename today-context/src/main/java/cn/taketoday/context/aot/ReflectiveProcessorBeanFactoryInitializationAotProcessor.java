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

package cn.taketoday.context.aot;

import java.util.Arrays;

import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.annotation.Reflective;
import cn.taketoday.aot.hint.annotation.ReflectiveProcessor;
import cn.taketoday.aot.hint.annotation.ReflectiveRuntimeHintsRegistrar;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotContribution;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationCode;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.RegisteredBean;

/**
 * AOT {@code BeanFactoryInitializationAotProcessor} that detects the presence
 * of {@link Reflective @Reflective} on annotated elements of all registered
 * beans and invokes the underlying {@link ReflectiveProcessor} implementations.
 *
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 */
class ReflectiveProcessorBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

  private static final ReflectiveRuntimeHintsRegistrar REGISTRAR = new ReflectiveRuntimeHintsRegistrar();

  @Override
  public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableBeanFactory beanFactory) {
    Class<?>[] beanTypes = Arrays.stream(beanFactory.getBeanDefinitionNames())
            .map(beanName -> RegisteredBean.of(beanFactory, beanName).getBeanClass())
            .toArray(Class<?>[]::new);
    return new ReflectiveProcessorBeanFactoryInitializationAotContribution(beanTypes);
  }

  private static class ReflectiveProcessorBeanFactoryInitializationAotContribution implements BeanFactoryInitializationAotContribution {

    private final Class<?>[] types;

    public ReflectiveProcessorBeanFactoryInitializationAotContribution(Class<?>[] types) {
      this.types = types;
    }

    @Override
    public void applyTo(GenerationContext generationContext, BeanFactoryInitializationCode beanFactoryInitializationCode) {
      RuntimeHints runtimeHints = generationContext.getRuntimeHints();
      REGISTRAR.registerRuntimeHints(runtimeHints, this.types);
    }

  }

}
