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

package infra.context.aot;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import infra.aot.generate.GenerationContext;
import infra.beans.factory.aot.AotException;
import infra.beans.factory.aot.AotProcessingException;
import infra.beans.factory.aot.AotServices;
import infra.beans.factory.aot.BeanFactoryInitializationAotContribution;
import infra.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import infra.beans.factory.aot.BeanFactoryInitializationCode;
import infra.beans.factory.support.StandardBeanFactory;

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
