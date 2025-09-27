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

package infra.context.properties;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import infra.aot.generate.GenerationContext;
import infra.beans.factory.aot.BeanFactoryInitializationAotContribution;
import infra.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import infra.beans.factory.aot.BeanFactoryInitializationCode;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.properties.bind.BindMethod;
import infra.context.properties.bind.Bindable;
import infra.context.properties.bind.BindableRuntimeHintsRegistrar;
import infra.util.ClassUtils;

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
  @Nullable
  public ConfigurationPropertiesReflectionHintsContribution processAheadOfTime(ConfigurableBeanFactory beanFactory) {
    var beanNames = beanFactory.getBeanNamesForAnnotation(ConfigurationProperties.class);
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
