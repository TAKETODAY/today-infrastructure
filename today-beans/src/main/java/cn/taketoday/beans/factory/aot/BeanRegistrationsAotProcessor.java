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

package cn.taketoday.beans.factory.aot;

import java.util.LinkedHashMap;

import cn.taketoday.beans.factory.aot.BeanRegistrationsAotContribution.Registration;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.lang.Nullable;

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
    var registrations = new LinkedHashMap<BeanRegistrationKey, Registration>();

    for (String beanName : beanFactory.getBeanDefinitionNames()) {
      RegisteredBean registeredBean = RegisteredBean.of(beanFactory, beanName);
      BeanDefinitionMethodGenerator beanDefinitionMethodGenerator =
              beanDefinitionMethodGeneratorFactory.getBeanDefinitionMethodGenerator(registeredBean);
      if (beanDefinitionMethodGenerator != null) {
        registrations.put(new BeanRegistrationKey(beanName, registeredBean.getBeanClass()),
                new Registration(beanDefinitionMethodGenerator, beanFactory.getAliases(beanName)));
      }
    }

    if (registrations.isEmpty()) {
      return null;
    }
    return new BeanRegistrationsAotContribution(registrations);
  }

}
