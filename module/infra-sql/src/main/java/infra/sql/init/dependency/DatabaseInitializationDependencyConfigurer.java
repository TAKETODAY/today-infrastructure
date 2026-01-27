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

package infra.sql.init.dependency;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import infra.aot.AotDetector;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanFactoryPostProcessor;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.context.BootstrapContext;
import infra.context.EnvironmentAware;
import infra.context.annotation.Import;
import infra.context.annotation.ImportBeanDefinitionRegistrar;
import infra.core.Ordered;
import infra.core.env.Environment;
import infra.core.type.AnnotationMetadata;
import infra.lang.TodayStrategies;
import infra.util.CollectionUtils;
import infra.util.StringUtils;

import static infra.lang.TodayStrategies.ArgumentResolver;

/**
 * Configures beans that depend upon SQL database initialization with
 * {@link BeanDefinition#getDependsOn() dependencies} upon beans that perform database
 * initialization. Intended for {@link Import import} in configuration classes that define
 * database initialization beans or that define beans that require database initialization
 * to have completed before they are initialized.
 * <p>
 * Beans that initialize a database are identified by {@link DatabaseInitializerDetector
 * DatabaseInitializerDetectors}. Beans that depend upon database initialization are
 * identified by {@link DependsOnDatabaseInitializationDetector
 * DependsOnDatabaseInitializationDetectors}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see DatabaseInitializerDetector
 * @see DependsOnDatabaseInitializationDetector
 * @see DependsOnDatabaseInitialization
 * @since 5.0
 */
public class DatabaseInitializationDependencyConfigurer implements ImportBeanDefinitionRegistrar {

  @Override
  public void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context) {
    String name = DependsOnDatabaseInitializationPostProcessor.class.getName();
    if (!context.containsBeanDefinition(name)) {
      BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(DependsOnDatabaseInitializationPostProcessor.class);
      context.registerBeanDefinition(name, builder.getBeanDefinition());
    }
  }

  /**
   * {@link BeanFactoryPostProcessor} used to configure database initialization
   * dependency relationships.
   */
  static class DependsOnDatabaseInitializationPostProcessor
          implements BeanFactoryPostProcessor, EnvironmentAware, Ordered {

    @SuppressWarnings("NullAway.Init")
    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
      this.environment = environment;
    }

    @Override
    public int getOrder() {
      return 0;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
      if (AotDetector.useGeneratedArtifacts()) {
        return;
      }
      InitializerBeanNames initializerBeanNames = detectInitializerBeanNames(beanFactory);
      if (initializerBeanNames.isEmpty()) {
        return;
      }
      Set<String> previousInitializerBeanNamesBatch = null;
      for (Set<String> initializerBeanNamesBatch : initializerBeanNames.batchedBeanNames()) {
        for (String initializerBeanName : initializerBeanNamesBatch) {
          BeanDefinition beanDefinition = getBeanDefinition(initializerBeanName, beanFactory);
          beanDefinition
                  .setDependsOn(merge(beanDefinition.getDependsOn(), previousInitializerBeanNamesBatch));
        }
        previousInitializerBeanNamesBatch = initializerBeanNamesBatch;
      }
      for (String dependsOnInitializationBeanNames : detectDependsOnInitializationBeanNames(beanFactory)) {
        BeanDefinition beanDefinition = getBeanDefinition(dependsOnInitializationBeanNames, beanFactory);
        beanDefinition.setDependsOn(merge(beanDefinition.getDependsOn(), initializerBeanNames.beanNames()));
      }
    }

    private String @Nullable [] merge(String @Nullable [] source, @Nullable Set<String> additional) {
      if (CollectionUtils.isEmpty(additional)) {
        return source;
      }
      Set<String> result = new LinkedHashSet<>((source != null) ? Arrays.asList(source) : Collections.emptySet());
      result.addAll(additional);
      return StringUtils.toStringArray(result);
    }

    private InitializerBeanNames detectInitializerBeanNames(ConfigurableBeanFactory beanFactory) {
      List<DatabaseInitializerDetector> detectors = getDetectors(beanFactory, DatabaseInitializerDetector.class);
      InitializerBeanNames initializerBeanNames = new InitializerBeanNames();
      for (DatabaseInitializerDetector detector : detectors) {
        for (String beanName : detector.detect(beanFactory)) {
          BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
          beanDefinition.setAttribute(DatabaseInitializerDetector.class.getName(),
                  detector.getClass().getName());
          initializerBeanNames.detected(detector, beanName);
        }
      }
      for (DatabaseInitializerDetector detector : detectors) {
        detector.detectionComplete(beanFactory, initializerBeanNames.beanNames());
      }
      return initializerBeanNames;
    }

    private Collection<String> detectDependsOnInitializationBeanNames(ConfigurableBeanFactory beanFactory) {
      List<DependsOnDatabaseInitializationDetector> detectors = getDetectors(beanFactory,
              DependsOnDatabaseInitializationDetector.class);
      Set<String> beanNames = new HashSet<>();
      for (DependsOnDatabaseInitializationDetector detector : detectors) {
        beanNames.addAll(detector.detect(beanFactory));
      }
      return beanNames;
    }

    private <T> List<T> getDetectors(ConfigurableBeanFactory beanFactory, Class<T> type) {
      ArgumentResolver argumentResolver = ArgumentResolver.of(Environment.class, this.environment);
      return TodayStrategies.forDefaultResourceLocation(beanFactory.getBeanClassLoader())
              .load(type, argumentResolver);
    }

    private static BeanDefinition getBeanDefinition(String beanName, ConfigurableBeanFactory beanFactory) {
      try {
        return beanFactory.getBeanDefinition(beanName);
      }
      catch (NoSuchBeanDefinitionException ex) {
        BeanFactory parentBeanFactory = beanFactory.getParentBeanFactory();
        if (parentBeanFactory instanceof ConfigurableBeanFactory configurableBeanFactory) {
          return getBeanDefinition(beanName, configurableBeanFactory);
        }
        throw ex;
      }
    }

    static class InitializerBeanNames {

      private final Map<DatabaseInitializerDetector, Set<String>> byDetectorBeanNames = new LinkedHashMap<>();

      private final Set<String> beanNames = new LinkedHashSet<>();

      private void detected(DatabaseInitializerDetector detector, String beanName) {
        this.byDetectorBeanNames.computeIfAbsent(detector, (key) -> new LinkedHashSet<>()).add(beanName);
        this.beanNames.add(beanName);
      }

      private boolean isEmpty() {
        return this.beanNames.isEmpty();
      }

      private Iterable<Set<String>> batchedBeanNames() {
        return this.byDetectorBeanNames.values();
      }

      private Set<String> beanNames() {
        return Collections.unmodifiableSet(this.beanNames);
      }

    }

  }

}
