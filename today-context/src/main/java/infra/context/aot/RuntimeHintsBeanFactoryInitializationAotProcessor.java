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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import infra.aot.generate.GenerationContext;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.beans.BeanUtils;
import infra.beans.factory.aot.AotServices;
import infra.beans.factory.aot.BeanFactoryInitializationAotContribution;
import infra.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import infra.beans.factory.aot.BeanFactoryInitializationCode;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.annotation.ImportRuntimeHints;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * {@link BeanFactoryInitializationAotProcessor} implementation that processes
 * {@link RuntimeHintsRegistrar} implementations declared as
 * {@code today.strategies} or using
 * {@link ImportRuntimeHints @ImportRuntimeHints} annotated configuration
 * classes or bean methods.
 *
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class RuntimeHintsBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

  private static final Logger logger = LoggerFactory.getLogger(RuntimeHintsBeanFactoryInitializationAotProcessor.class);

  @Override
  public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableBeanFactory beanFactory) {
    Map<Class<? extends RuntimeHintsRegistrar>, RuntimeHintsRegistrar> registrars = AotServices
            .factories(beanFactory.getBeanClassLoader()).load(RuntimeHintsRegistrar.class).stream()
            .collect(LinkedHashMap::new, (map, item) -> map.put(item.getClass(), item), Map::putAll);
    extractFromBeanFactory(beanFactory)
            .forEach(registrarClass -> registrars.computeIfAbsent(registrarClass, BeanUtils::newInstance));
    return new RuntimeHintsRegistrarContribution(registrars.values(), beanFactory.getBeanClassLoader());
  }

  private Set<Class<? extends RuntimeHintsRegistrar>> extractFromBeanFactory(ConfigurableBeanFactory beanFactory) {
    Set<Class<? extends RuntimeHintsRegistrar>> registrarClasses = new LinkedHashSet<>();
    for (String beanName : beanFactory.getBeanDefinitionNames()) {
      beanFactory.findAllAnnotationsOnBean(beanName, ImportRuntimeHints.class, true)
              .forEach(annotation -> registrarClasses.addAll(extractFromBeanDefinition(beanName, annotation)));
    }
    return registrarClasses;
  }

  private Set<Class<? extends RuntimeHintsRegistrar>> extractFromBeanDefinition(
          String beanName, ImportRuntimeHints annotation) {

    Set<Class<? extends RuntimeHintsRegistrar>> registrars = new LinkedHashSet<>();
    for (Class<? extends RuntimeHintsRegistrar> registrarClass : annotation.value()) {
      if (logger.isTraceEnabled()) {
        logger.trace("Loaded [{}] registrar from annotated bean [{}]",
                registrarClass.getCanonicalName(), beanName);
      }
      registrars.add(registrarClass);
    }
    return registrars;
  }

  static class RuntimeHintsRegistrarContribution implements BeanFactoryInitializationAotContribution {

    private final Iterable<RuntimeHintsRegistrar> registrars;

    @Nullable
    private final ClassLoader beanClassLoader;

    RuntimeHintsRegistrarContribution(Iterable<RuntimeHintsRegistrar> registrars,
            @Nullable ClassLoader beanClassLoader) {
      this.registrars = registrars;
      this.beanClassLoader = beanClassLoader;
    }

    @Override
    public void applyTo(GenerationContext generationContext,
            BeanFactoryInitializationCode beanFactoryInitializationCode) {

      RuntimeHints hints = generationContext.getRuntimeHints();
      this.registrars.forEach(registrar -> {
        if (logger.isTraceEnabled()) {
          logger.trace("Processing RuntimeHints contribution from [{}]",
                  registrar.getClass().getCanonicalName());
        }
        registrar.registerHints(hints, this.beanClassLoader);
      });
    }
  }

}
