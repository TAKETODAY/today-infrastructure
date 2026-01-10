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

package infra.beans.factory.aot;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.RegisteredBean;

/**
 * {@link BeanFactoryInitializationAotProcessor} that contributes code to
 * register beans.
 *
 * @author Phillip Webb
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class BeanRegistrationsAotProcessor implements BeanFactoryInitializationAotProcessor {

  @Override
  @Nullable
  public BeanRegistrationsAotContribution processAheadOfTime(ConfigurableBeanFactory beanFactory) {
    var beanDefinitionMethodGeneratorFactory = new BeanDefinitionMethodGeneratorFactory(beanFactory);
    var registrations = new ArrayList<BeanRegistrationsAotContribution.Registration>();

    for (String beanName : beanFactory.getBeanDefinitionNames()) {
      RegisteredBean registeredBean = RegisteredBean.of(beanFactory, beanName);
      BeanDefinitionMethodGenerator beanDefinitionMethodGenerator =
              beanDefinitionMethodGeneratorFactory.getBeanDefinitionMethodGenerator(registeredBean);
      if (beanDefinitionMethodGenerator != null) {
        registrations.add(new BeanRegistrationsAotContribution.Registration(registeredBean, beanDefinitionMethodGenerator, beanFactory.getAliases(beanName)));
      }
    }

    if (registrations.isEmpty()) {
      return null;
    }
    return new BeanRegistrationsAotContribution(registrations);
  }

}
