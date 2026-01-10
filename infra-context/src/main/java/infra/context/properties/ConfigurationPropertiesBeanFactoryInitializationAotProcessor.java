/*
 * Copyright 2012-present the original author or authors.
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
