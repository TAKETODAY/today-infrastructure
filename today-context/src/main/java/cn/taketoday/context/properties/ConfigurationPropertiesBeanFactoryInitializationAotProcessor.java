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

package cn.taketoday.context.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotContribution;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationCode;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.properties.bind.BindMethod;
import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.BindableRuntimeHintsRegistrar;
import cn.taketoday.util.ClassUtils;

/**
 * {@link BeanFactoryInitializationAotProcessor} that contributes runtime hints for
 * configuration properties-annotated beans.
 *
 * @author Stephane Nicoll
 * @author Christoph Strobl
 * @author Sebastien Deleuze
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ConfigurationPropertiesBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

  @Override
  public ConfigurationPropertiesReflectionHintsContribution processAheadOfTime(ConfigurableBeanFactory beanFactory) {
    Set<String> beanNames = beanFactory.getBeanNamesForAnnotation(ConfigurationProperties.class);
    List<Bindable<?>> bindables = new ArrayList<>();
    for (String beanName : beanNames) {
      Class<?> beanType = beanFactory.getType(beanName, false);
      if (beanType != null) {
        BindMethod bindMethod = beanFactory.containsBeanDefinition(beanName)
                                ? (BindMethod) beanFactory.getBeanDefinition(beanName).getAttribute(BindMethod.class.getName())
                                : null;
        bindables.add(Bindable.of(ClassUtils.getUserClass(beanType))
                .withBindMethod((bindMethod != null) ? bindMethod : BindMethod.JAVA_BEAN));
      }
    }
    return (!bindables.isEmpty()) ? new ConfigurationPropertiesReflectionHintsContribution(bindables) : null;
  }

  static final class ConfigurationPropertiesReflectionHintsContribution
          implements BeanFactoryInitializationAotContribution {

    private final List<Bindable<?>> bindables;

    private ConfigurationPropertiesReflectionHintsContribution(List<Bindable<?>> bindables) {
      this.bindables = bindables;
    }

    @Override
    public void applyTo(GenerationContext generationContext,
            BeanFactoryInitializationCode beanFactoryInitializationCode) {
      BindableRuntimeHintsRegistrar.forBindables(this.bindables)
              .registerHints(generationContext.getRuntimeHints());
    }

    Iterable<Bindable<?>> getBindables() {
      return this.bindables;
    }

  }

}
