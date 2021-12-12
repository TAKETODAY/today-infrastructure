/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.aop.proxy.ProxyUtils;
import cn.taketoday.beans.factory.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionHolder;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanDefinitionRegistryPostProcessor;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.BeanNameGenerator;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.DependenciesBeanPostProcessor;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.SingletonBeanRegistry;
import cn.taketoday.context.annotation.ConfigurationClassEnhancer.EnhancedConfiguration;
import cn.taketoday.context.aware.ImportAware;
import cn.taketoday.context.loader.ClassPathBeanDefinitionScanner;
import cn.taketoday.context.loader.DefinitionLoadingContext;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.PriorityOrdered;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Required;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;

/**
 * {@link BeanFactoryPostProcessor} used for bootstrapping processing of
 * {@link Configuration @Configuration} classes.
 *
 * <p>This post processor is priority-ordered as it is important that any
 * {@link Component @Component} methods declared in {@code @Configuration} classes have
 * their corresponding bean definitions registered before any other
 * {@code BeanFactoryPostProcessor} executes.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/7 21:36
 */
public class ConfigurationClassPostProcessor
        implements BeanDefinitionRegistryPostProcessor, PriorityOrdered, BeanClassLoaderAware {

  private static final String IMPORT_REGISTRY_BEAN_NAME =
          ConfigurationClassPostProcessor.class.getName() + ".importRegistry";

  private final Logger logger = LoggerFactory.getLogger(getClass());
  public static final AnnotationBeanNameGenerator IMPORT_BEAN_NAME_GENERATOR =
          FullyQualifiedAnnotationBeanNameGenerator.INSTANCE;

  private final DefinitionLoadingContext loadingContext;

  private final Set<Integer> registriesPostProcessed = new HashSet<>();

  private final Set<Integer> factoriesPostProcessed = new HashSet<>();

  @Nullable
  private ConfigurationClassBeanDefinitionReader reader;

  private boolean localBeanNameGeneratorSet = false;

  /* Using fully qualified class names as default bean names by default. */
  private BeanNameGenerator importBeanNameGenerator = IMPORT_BEAN_NAME_GENERATOR;

  private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

  public ConfigurationClassPostProcessor(@Required DefinitionLoadingContext loadingContext) {
    this.loadingContext = loadingContext;
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;  // within PriorityOrdered
  }

  /**
   * Set the {@link ProblemReporter} to use.
   * <p>Used to register any problems detected with {@link Configuration} or {@link Component}
   * declarations. For instance, an @Component method marked as {@code final} is illegal
   * and would be reported as a problem. Defaults to {@link FailFastProblemReporter}.
   */
  public void setProblemReporter(@Nullable ProblemReporter problemReporter) {
    loadingContext.setProblemReporter(problemReporter);
  }

  /**
   * Set the {@link BeanNameGenerator} to be used when triggering component scanning
   * from {@link Configuration} classes and when registering {@link Import}'ed
   * configuration classes. The default is a standard {@link AnnotationBeanNameGenerator}
   * for scanned components (compatible with the default in {@link ClassPathBeanDefinitionScanner})
   * and a variant thereof for imported configuration classes (using unique fully-qualified
   * class names instead of standard component overriding).
   * <p>Note that this strategy does <em>not</em> apply to {@link Bean} methods.
   * <p>This setter is typically only appropriate when configuring the post-processor as a
   * standalone bean definition in XML, e.g. not using the dedicated {@code AnnotationConfig*}
   * application contexts or the {@code <context:annotation-config>} element. Any bean name
   * generator specified against the application context will take precedence over any set here.
   *
   * @see cn.taketoday.context.StandardApplicationContext#setBeanNameGenerator(BeanNameGenerator)
   * @see AnnotationConfigUtils#CONFIGURATION_BEAN_NAME_GENERATOR
   */
  public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
    Assert.notNull(beanNameGenerator, "BeanNameGenerator must not be null");
    this.localBeanNameGeneratorSet = true;
    this.importBeanNameGenerator = beanNameGenerator;
    loadingContext.setBeanNameGenerator(beanNameGenerator);
  }

  @Override
  public void setBeanClassLoader(ClassLoader beanClassLoader) {
    this.beanClassLoader = beanClassLoader;
  }

  /**
   * Derive further bean definitions from the configuration classes in the registry.
   */
  @Override
  public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
    int registryId = System.identityHashCode(registry);
    if (this.registriesPostProcessed.contains(registryId)) {
      throw new IllegalStateException(
              "postProcessBeanDefinitionRegistry already called on this post-processor against " + registry);
    }
    if (this.factoriesPostProcessed.contains(registryId)) {
      throw new IllegalStateException(
              "postProcessBeanFactory already called on this post-processor against " + registry);
    }
    this.registriesPostProcessed.add(registryId);

    processConfigBeanDefinitions(registry);
  }

  /**
   * Prepare the Configuration classes for servicing bean requests at runtime
   * by replacing them with CGLIB-enhanced subclasses.
   */
  @Override
  public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
    int factoryId = System.identityHashCode(beanFactory);
    if (this.factoriesPostProcessed.contains(factoryId)) {
      throw new IllegalStateException(
              "postProcessBeanFactory already called on this post-processor against " + beanFactory);
    }
    this.factoriesPostProcessed.add(factoryId);
    if (!this.registriesPostProcessed.contains(factoryId)) {
      // BeanDefinitionRegistryPostProcessor hook apparently not supported...
      // Simply call processConfigurationClasses lazily at this point then.
      processConfigBeanDefinitions((BeanDefinitionRegistry) beanFactory);
    }

    enhanceConfigurationClasses(beanFactory);
    beanFactory.addBeanPostProcessor(new ImportAwareBeanPostProcessor(beanFactory));
  }

  /**
   * Build and validate a configuration model based on the registry of
   * {@link Configuration} classes.
   */
  public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {
    List<BeanDefinitionHolder> configCandidates = new ArrayList<>();
    String[] candidateNames = registry.getBeanDefinitionNames();

    for (String beanName : candidateNames) {
      BeanDefinition beanDef = BeanFactoryUtils.getBeanDefinition(registry, beanName);
      if (beanDef.getAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE) != null) {
        if (logger.isDebugEnabled()) {
          logger.debug("Bean definition has already been processed as a configuration class: " + beanDef);
        }
      }
      else if (ConfigurationClassUtils.checkConfigurationClassCandidate(beanDef, loadingContext)) {
        configCandidates.add(new BeanDefinitionHolder(beanDef, beanName));
      }
    }

    // Return immediately if no @Configuration classes were found
    if (configCandidates.isEmpty()) {
      return;
    }

    // Sort by previously determined @Order value, if applicable
    configCandidates.sort((bd1, bd2) -> {
      int i1 = ConfigurationClassUtils.getOrder(bd1.getBeanDefinition());
      int i2 = ConfigurationClassUtils.getOrder(bd2.getBeanDefinition());
      return Integer.compare(i1, i2);
    });

    // Detect any custom bean name generation strategy supplied through the enclosing application context
    SingletonBeanRegistry sbr = null;
    if (registry instanceof SingletonBeanRegistry) {
      sbr = (SingletonBeanRegistry) registry;
      if (!this.localBeanNameGeneratorSet) {
        BeanNameGenerator generator = (BeanNameGenerator) sbr.getSingleton(
                AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR);
        if (generator != null) {
          this.importBeanNameGenerator = generator;
        }
      }
    }

    // Parse each @Configuration class
    ConfigurationClassParser parser = new ConfigurationClassParser(loadingContext);

    Set<BeanDefinitionHolder> candidates = new LinkedHashSet<>(configCandidates);
    Set<ConfigurationClass> alreadyParsed = new HashSet<>(configCandidates.size());
    do {
      parser.parse(candidates);
      parser.validate();

      Set<ConfigurationClass> configClasses = new LinkedHashSet<>(parser.getConfigurationClasses());
      configClasses.removeAll(alreadyParsed);

      // Read the model and create bean definitions based on its content
      if (this.reader == null) {
        this.reader = new ConfigurationClassBeanDefinitionReader(
                loadingContext, this.importBeanNameGenerator, parser.getImportRegistry());
      }
      this.reader.loadBeanDefinitions(configClasses);
      alreadyParsed.addAll(configClasses);

      candidates.clear();
      if (registry.getBeanDefinitionCount() > candidateNames.length) {
        String[] newCandidateNames = registry.getBeanDefinitionNames();
        Set<String> oldCandidateNames = new HashSet<>(Arrays.asList(candidateNames));
        Set<String> alreadyParsedClasses = new HashSet<>();
        for (ConfigurationClass configurationClass : alreadyParsed) {
          alreadyParsedClasses.add(configurationClass.getMetadata().getClassName());
        }
        for (String candidateName : newCandidateNames) {
          if (!oldCandidateNames.contains(candidateName)) {
            BeanDefinition bd = registry.getBeanDefinition(candidateName);
            if (ConfigurationClassUtils.checkConfigurationClassCandidate(bd, loadingContext)
                    && !alreadyParsedClasses.contains(bd.getBeanClassName())) {
              candidates.add(new BeanDefinitionHolder(bd, candidateName));
            }
          }
        }
        candidateNames = newCandidateNames;
      }
    }
    while (!candidates.isEmpty());

    // Register the ImportRegistry as a bean in order to support ImportAware @Configuration classes
    if (sbr != null && !sbr.containsSingleton(IMPORT_REGISTRY_BEAN_NAME)) {
      sbr.registerSingleton(IMPORT_REGISTRY_BEAN_NAME, parser.getImportRegistry());
    }

    loadingContext.clearCache();
  }

  /**
   * Post-processes a BeanFactory in search of Configuration class BeanDefinitions;
   * any candidates are then enhanced by a {@link ConfigurationClassEnhancer}.
   * Candidate status is determined by BeanDefinition attribute metadata.
   *
   * @see ConfigurationClassEnhancer
   */
  public void enhanceConfigurationClasses(ConfigurableBeanFactory beanFactory) {
    Map<String, BeanDefinition> configBeanDefs = new LinkedHashMap<>();
    for (String beanName : beanFactory.getBeanDefinitionNames()) {
      BeanDefinition beanDef = BeanFactoryUtils.getBeanDefinition(beanFactory, beanName);
      Object configClassAttr = beanDef.getAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE);

      if (!beanDef.hasBeanClass()) {
        AnnotationMetadata annotationMetadata = null;
        MethodMetadata methodMetadata = null;
        if (beanDef instanceof AnnotatedBeanDefinition annotatedBeanDefinition) {
          annotationMetadata = annotatedBeanDefinition.getMetadata();
          methodMetadata = annotatedBeanDefinition.getFactoryMethodMetadata();
        }

        if (configClassAttr != null || methodMetadata != null) {
          // Configuration class (full or lite) or a configuration-derived @Component method
          // -> eagerly resolve bean class at this point, unless it's a 'lite' configuration
          // or component class without @Component methods.
          boolean liteConfigurationCandidateWithoutBeanMethods =
                  (ConfigurationClassUtils.CONFIGURATION_CLASS_LITE.equals(configClassAttr) &&
                          annotationMetadata != null && !ConfigurationClassUtils.hasComponentMethods(annotationMetadata));
          if (!liteConfigurationCandidateWithoutBeanMethods) {
            try {
              beanDef.resolveBeanClass(this.beanClassLoader);
            }
            catch (Throwable ex) {
              throw new IllegalStateException(
                      "Cannot load configuration class: " + beanDef.getBeanClassName(), ex);
            }
          }
        }
      }
      if (ConfigurationClassUtils.CONFIGURATION_CLASS_FULL.equals(configClassAttr)) {
        if (logger.isInfoEnabled() && beanFactory.containsSingleton(beanName)) {
          logger.info("Cannot enhance @Configuration bean definition '" + beanName +
                  "' since its singleton instance has been created too early. The typical cause " +
                  "is a non-static @Component method with a BeanDefinitionRegistryPostProcessor " +
                  "return type: Consider declaring such methods as 'static'.");
        }
        configBeanDefs.put(beanName, beanDef);
      }
    }
    if (configBeanDefs.isEmpty()) {
      // nothing to enhance -> return immediately
      return;
    }

    ConfigurationClassEnhancer enhancer = new ConfigurationClassEnhancer();
    for (Map.Entry<String, BeanDefinition> entry : configBeanDefs.entrySet()) {
      BeanDefinition beanDef = entry.getValue();
      // If a @Configuration class gets proxied, always proxy the target class
      beanDef.setAttribute(ProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
      // Set enhanced subclass of the user-specified bean class
      Class<?> configClass = beanDef.getBeanClass();
      Class<?> enhancedClass = enhancer.enhance(configClass, this.beanClassLoader);
      if (configClass != enhancedClass) {
        if (logger.isTraceEnabled()) {
          logger.trace(String.format("Replacing bean definition '%s' existing class '%s' with " +
                  "enhanced class '%s'", entry.getKey(), configClass.getName(), enhancedClass.getName()));
        }
        beanDef.setBeanClass(enhancedClass);
      }
    }
  }

  private record ImportAwareBeanPostProcessor(BeanFactory beanFactory)
          implements DependenciesBeanPostProcessor, InitializationBeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
      if (bean instanceof ImportAware importAware) {
        ImportRegistry ir = beanFactory.getBean(IMPORT_REGISTRY_BEAN_NAME, ImportRegistry.class);
        AnnotationMetadata importingClass = ir.getImportingClassFor(ClassUtils.getUserClass(bean).getName());
        if (importingClass != null) {
          importAware.setImportMetadata(importingClass);
        }
      }
      return bean;
    }

    @Override
    public void postProcessDependencies(Object bean, BeanDefinition definition) {
      // postProcessProperties method attempts to autowire other configuration beans.
      if (bean instanceof EnhancedConfiguration enhancedConfiguration) {
        enhancedConfiguration.setBeanFactory(this.beanFactory);
      }
    }
  }

}
