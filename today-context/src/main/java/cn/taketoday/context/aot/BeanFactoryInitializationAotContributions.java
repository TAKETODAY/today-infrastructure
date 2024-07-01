/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.context.aot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.beans.factory.aot.AotException;
import cn.taketoday.beans.factory.aot.AotProcessingException;
import cn.taketoday.beans.factory.aot.AotServices;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotContribution;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationCode;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.lang.Nullable;

/**
 * A collection of {@link BeanFactoryInitializationAotContribution AOT
 * contributions} obtained from {@link BeanFactoryInitializationAotProcessor AOT
 * processors}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class BeanFactoryInitializationAotContributions {

  private final List<BeanFactoryInitializationAotContribution> contributions;

  BeanFactoryInitializationAotContributions(StandardBeanFactory beanFactory) {
    this(beanFactory, AotServices.factoriesAndBeans(beanFactory));
  }

  BeanFactoryInitializationAotContributions(StandardBeanFactory beanFactory,
          AotServices.Loader loader) {
    this.contributions = getContributions(beanFactory, getProcessors(loader));
  }

  private static List<BeanFactoryInitializationAotProcessor> getProcessors(AotServices.Loader loader) {
    List<BeanFactoryInitializationAotProcessor> processors = new ArrayList<>(
            loader.load(BeanFactoryInitializationAotProcessor.class).asList());
    processors.add(new RuntimeHintsBeanFactoryInitializationAotProcessor());
    return Collections.unmodifiableList(processors);
  }

  private List<BeanFactoryInitializationAotContribution> getContributions(
          StandardBeanFactory beanFactory, List<BeanFactoryInitializationAotProcessor> processors) {
    List<BeanFactoryInitializationAotContribution> contributions = new ArrayList<>();
    for (BeanFactoryInitializationAotProcessor processor : processors) {
      BeanFactoryInitializationAotContribution contribution = processAheadOfTime(processor, beanFactory);
      if (contribution != null) {
        contributions.add(contribution);
      }
    }
    return Collections.unmodifiableList(contributions);
  }

  @Nullable
  private BeanFactoryInitializationAotContribution processAheadOfTime(BeanFactoryInitializationAotProcessor processor,
          StandardBeanFactory beanFactory) {

    try {
      return processor.processAheadOfTime(beanFactory);
    }
    catch (AotException ex) {
      throw ex;
    }
    catch (Exception ex) {
      throw new AotProcessingException("Error executing '" +
              processor.getClass().getName() + "': " + ex.getMessage(), ex);
    }
  }

  void applyTo(GenerationContext generationContext,
          BeanFactoryInitializationCode beanFactoryInitializationCode) {
    for (BeanFactoryInitializationAotContribution contribution : this.contributions) {
      contribution.applyTo(generationContext, beanFactoryInitializationCode);
    }
  }

}
