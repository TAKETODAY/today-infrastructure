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

package cn.taketoday.context.annotation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import cn.taketoday.aop.framework.autoproxy.AutoProxyUtils;
import cn.taketoday.aot.generate.GeneratedMethod;
import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.hint.ExecutableMode;
import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.ResourceHints;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.DependenciesBeanPostProcessor;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.annotation.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotContribution;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationCode;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotContribution;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotProcessor;
import cn.taketoday.beans.factory.aot.BeanRegistrationCode;
import cn.taketoday.beans.factory.aot.BeanRegistrationCodeFragments;
import cn.taketoday.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
import cn.taketoday.beans.factory.aot.InstanceSupplierCodeGenerator;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinitionHolder;
import cn.taketoday.beans.factory.config.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.SingletonBeanRegistry;
import cn.taketoday.beans.factory.parsing.FailFastProblemReporter;
import cn.taketoday.beans.factory.parsing.ProblemReporter;
import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import cn.taketoday.beans.factory.support.BeanNameGenerator;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.BootstrapContext;
import cn.taketoday.context.BootstrapContextAware;
import cn.taketoday.context.annotation.ConfigurationClassEnhancer.EnhancedConfiguration;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.PriorityOrdered;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.PropertySourceDescriptor;
import cn.taketoday.core.io.PropertySourceProcessor;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.javapoet.MethodSpec;
import cn.taketoday.javapoet.ParameterizedTypeName;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;

import static cn.taketoday.context.annotation.ConfigurationClassUtils.CONFIGURATION_CLASS_LITE;

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
public class ConfigurationClassPostProcessor implements PriorityOrdered, BeanClassLoaderAware, BootstrapContextAware,
        BeanDefinitionRegistryPostProcessor, BeanRegistrationAotProcessor, BeanFactoryInitializationAotProcessor {

  private static final Logger log = LoggerFactory.getLogger(ConfigurationClassPostProcessor.class);

  private static final String IMPORT_REGISTRY_BEAN_NAME =
          ConfigurationClassPostProcessor.class.getName() + ".importRegistry";

  public static final AnnotationBeanNameGenerator IMPORT_BEAN_NAME_GENERATOR =
          FullyQualifiedAnnotationBeanNameGenerator.INSTANCE;

  @Nullable
  private BootstrapContext bootstrapContext;

  private final Set<Integer> registriesPostProcessed = new HashSet<>();

  private final Set<Integer> factoriesPostProcessed = new HashSet<>();

  @Nullable
  private ConfigurationClassBeanDefinitionReader reader;

  private boolean localBeanNameGeneratorSet = false;

  /* Using fully qualified class names as default bean names by default. */
  private BeanNameGenerator importBeanNameGenerator = IMPORT_BEAN_NAME_GENERATOR;

  @Nullable
  private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

  @Nullable
  private List<PropertySourceDescriptor> propertySourceDescriptors;

  public ConfigurationClassPostProcessor() { }

  public ConfigurationClassPostProcessor(BootstrapContext bootstrapContext) {
    setBootstrapContext(bootstrapContext);
  }

  @Override
  public void setBootstrapContext(BootstrapContext context) {
    Assert.notNull(context, "BootstrapContext is required");
    this.bootstrapContext = context;
  }

  // @since 4.0
  protected final BootstrapContext obtainBootstrapContext() {
    Assert.state(bootstrapContext != null, "BootstrapContext is required");
    return bootstrapContext;
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
    obtainBootstrapContext().setProblemReporter(problemReporter);
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
   * @see AnnotationConfigApplicationContext#setBeanNameGenerator(BeanNameGenerator)
   * @see AnnotationConfigUtils#CONFIGURATION_BEAN_NAME_GENERATOR
   */
  public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
    Assert.notNull(beanNameGenerator, "BeanNameGenerator is required");
    this.localBeanNameGeneratorSet = true;
    obtainBootstrapContext().setBeanNameGenerator(beanNameGenerator);
    this.importBeanNameGenerator = beanNameGenerator;
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
    if (bootstrapContext == null) {
      bootstrapContext = BootstrapContext.from(beanFactory);
    }
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

  @Nullable
  @Override
  public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
    Object configClassAttr = registeredBean.getMergedBeanDefinition()
            .getAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE);
    if (ConfigurationClassUtils.CONFIGURATION_CLASS_FULL.equals(configClassAttr)) {
      return BeanRegistrationAotContribution.withCustomCodeFragments(codeFragments ->
              new ConfigurationClassProxyBeanRegistrationCodeFragments(codeFragments, registeredBean));
    }
    return null;
  }

  @Override
  @Nullable
  public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableBeanFactory beanFactory) {
    boolean hasPropertySourceDescriptors = CollectionUtils.isNotEmpty(this.propertySourceDescriptors);
    boolean hasImportRegistry = beanFactory.containsBean(IMPORT_REGISTRY_BEAN_NAME);
    if (hasPropertySourceDescriptors || hasImportRegistry) {
      return (generationContext, code) -> {
        if (hasPropertySourceDescriptors) {
          new PropertySourcesAotContribution(this.propertySourceDescriptors, this::resolvePropertySourceLocation)
                  .applyTo(generationContext, code);
        }
        if (hasImportRegistry) {
          new ImportAwareAotContribution(beanFactory).applyTo(generationContext, code);
        }
      };
    }
    return null;
  }

  @Nullable
  private Resource resolvePropertySourceLocation(String location) {
    BootstrapContext bootstrapContext = obtainBootstrapContext();
    try {
      String resolvedLocation = bootstrapContext.getEnvironment().resolveRequiredPlaceholders(location);
      return bootstrapContext.getResource(resolvedLocation);
    }
    catch (Exception ex) {
      return null;
    }
  }

  /**
   * Build and validate a configuration model based on the registry of
   * {@link Configuration} classes.
   */
  public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {
    ArrayList<BeanDefinitionHolder> configCandidates = new ArrayList<>();
    String[] candidateNames = registry.getBeanDefinitionNames();
    BootstrapContext bootstrapContext = obtainBootstrapContext();
    for (String beanName : candidateNames) {
      BeanDefinition beanDef = registry.getBeanDefinition(beanName);
      if (beanDef.getAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE) != null) {
        if (log.isDebugEnabled()) {
          log.debug("Bean definition has already been processed as a configuration class: {}", beanDef);
        }
      }
      else if (ConfigurationClassUtils.checkConfigurationClassCandidate(beanDef, bootstrapContext)) {
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
        BeanNameGenerator populator = (BeanNameGenerator) sbr.getSingleton(
                AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR);
        if (populator != null) {
          this.importBeanNameGenerator = populator;
        }
      }
    }

    // Parse each @Configuration class
    var parser = new ConfigurationClassParser(bootstrapContext);

    LinkedHashSet<BeanDefinitionHolder> candidates = new LinkedHashSet<>(configCandidates);
    HashSet<ConfigurationClass> alreadyParsed = new HashSet<>(configCandidates.size());
    do {
      parser.parse(candidates);
      parser.validate();

      Set<ConfigurationClass> configClasses = new LinkedHashSet<>(parser.getConfigurationClasses());
      configClasses.removeAll(alreadyParsed);

      // Read the model and create bean definitions based on its content
      if (reader == null) {
        this.reader = new ConfigurationClassBeanDefinitionReader(
                bootstrapContext, importBeanNameGenerator, parser.importRegistry);
      }
      reader.loadBeanDefinitions(configClasses);
      alreadyParsed.addAll(configClasses);

      candidates.clear();
      if (registry.getBeanDefinitionCount() > candidateNames.length) {
        String[] newCandidateNames = registry.getBeanDefinitionNames();
        HashSet<String> oldCandidateNames = CollectionUtils.newHashSet(candidateNames);
        HashSet<String> alreadyParsedClasses = new HashSet<>();
        for (ConfigurationClass configurationClass : alreadyParsed) {
          alreadyParsedClasses.add(configurationClass.metadata.getClassName());
        }
        for (String candidateName : newCandidateNames) {
          if (!oldCandidateNames.contains(candidateName)) {
            BeanDefinition bd = registry.getBeanDefinition(candidateName);
            if (ConfigurationClassUtils.checkConfigurationClassCandidate(bd, bootstrapContext)
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
      sbr.registerSingleton(IMPORT_REGISTRY_BEAN_NAME, parser.importRegistry);
    }

    // Store the PropertySourceDescriptors to contribute them Ahead-of-time if necessary
    this.propertySourceDescriptors = parser.getPropertySourceDescriptors();

    bootstrapContext.clearCache();
  }

  /**
   * Post-processes a BeanFactory in search of Configuration class BeanDefinitions;
   * any candidates are then enhanced by a {@link ConfigurationClassEnhancer}.
   * Candidate status is determined by BeanDefinition attribute metadata.
   *
   * @see ConfigurationClassEnhancer
   */
  public void enhanceConfigurationClasses(ConfigurableBeanFactory beanFactory) {
    LinkedHashMap<String, AbstractBeanDefinition> configBeanDefs = new LinkedHashMap<>();
    for (String beanName : beanFactory.getBeanDefinitionNames()) {
      BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
      Object configClassAttr = beanDef.getAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE);
      AnnotationMetadata annotationMetadata = null;
      MethodMetadata methodMetadata = null;
      if (beanDef instanceof AnnotatedBeanDefinition annotatedBeanDefinition) {
        annotationMetadata = annotatedBeanDefinition.getMetadata();
        methodMetadata = annotatedBeanDefinition.getFactoryMethodMetadata();
      }
      if ((configClassAttr != null || methodMetadata != null)
              && (beanDef instanceof AbstractBeanDefinition abd) && !abd.hasBeanClass()) {
        // Configuration class (full or lite) or a configuration-derived @Bean method
        // -> eagerly resolve bean class at this point, unless it's a 'lite' configuration
        // or component class without @Bean methods.
        boolean liteConfigurationCandidateWithoutBeanMethods = CONFIGURATION_CLASS_LITE.equals(configClassAttr)
                && (annotationMetadata != null) && !ConfigurationClassUtils.hasComponentMethods(annotationMetadata);
        if (!liteConfigurationCandidateWithoutBeanMethods) {
          try {
            abd.resolveBeanClass(this.beanClassLoader);
          }
          catch (Throwable ex) {
            throw new IllegalStateException(
                    "Cannot load configuration class: " + beanDef.getBeanClassName(), ex);
          }
        }
      }
      if (ConfigurationClassUtils.CONFIGURATION_CLASS_FULL.equals(configClassAttr)) {
        if (!(beanDef instanceof AbstractBeanDefinition abd)) {
          throw new BeanDefinitionStoreException("Cannot enhance @Configuration bean definition '" +
                  beanName + "' since it is not stored in an AbstractBeanDefinition subclass");
        }
        else if (beanFactory.containsSingleton(beanName)) {
          if (log.isWarnEnabled()) {
            log.warn("Cannot enhance @Configuration bean definition '{}' " +
                    "since its singleton instance has been created too early. The typical cause " +
                    "is a non-static @Component method with a BeanDefinitionRegistryPostProcessor " +
                    "return type: Consider declaring such methods as 'static'.", beanName);
          }
        }
        else {
          configBeanDefs.put(beanName, abd);
        }
      }
    }
    if (configBeanDefs.isEmpty()) {
      // nothing to enhance -> return immediately
      return;
    }

    ConfigurationClassEnhancer enhancer = new ConfigurationClassEnhancer();
    for (Map.Entry<String, AbstractBeanDefinition> entry : configBeanDefs.entrySet()) {
      AbstractBeanDefinition beanDef = entry.getValue();
      // If a @Configuration class gets proxied, always proxy the target class
      beanDef.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
      // Set enhanced subclass of the user-specified bean class
      Class<?> configClass = beanDef.getBeanClass();
      Class<?> enhancedClass = enhancer.enhance(configClass, this.beanClassLoader);
      if (configClass != enhancedClass) {
        if (log.isTraceEnabled()) {
          log.trace("Replacing bean definition '{}' existing class '{}' with " +
                  "enhanced class '{}'", entry.getKey(), configClass.getName(), enhancedClass.getName());
        }
        beanDef.setBeanClass(enhancedClass);
      }
    }
  }

  private record ImportAwareBeanPostProcessor(BeanFactory beanFactory)
          implements DependenciesBeanPostProcessor, InitializationBeanPostProcessor, Ordered {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
      if (bean instanceof ImportAware importAware) {
        ImportRegistry registry = beanFactory.getBean(IMPORT_REGISTRY_BEAN_NAME, ImportRegistry.class);
        AnnotationMetadata importingClass = registry.getImportingClassFor(ClassUtils.getUserClass(bean).getName());
        if (importingClass != null) {
          importAware.setImportMetadata(importingClass);
        }
      }
      return bean;
    }

    @Override
    public PropertyValues processDependencies(@Nullable PropertyValues propertyValues, Object bean, String beanName) {
      // postProcessDependencies method attempts to autowire other configuration beans.
      if (bean instanceof EnhancedConfiguration enhancedConfiguration) {
        // FIXME
        enhancedConfiguration.setBeanFactory(this.beanFactory);
      }
      return propertyValues;
    }

    @Override
    public int getOrder() {
      return HIGHEST_PRECEDENCE;
    }
  }

  private static class ImportAwareAotContribution implements BeanFactoryInitializationAotContribution {

    private static final String BEAN_FACTORY_VARIABLE = BeanFactoryInitializationCode.BEAN_FACTORY_VARIABLE;

    private static final ParameterizedTypeName STRING_STRING_MAP =
            ParameterizedTypeName.get(Map.class, String.class, String.class);

    private static final String MAPPINGS_VARIABLE = "mappings";

    private static final String BEAN_DEFINITION_VARIABLE = "beanDefinition";

    private static final String BEAN_NAME = "cn.taketoday.context.annotation.internalImportAwareAotProcessor";

    private final ConfigurableBeanFactory beanFactory;

    public ImportAwareAotContribution(ConfigurableBeanFactory beanFactory) {
      this.beanFactory = beanFactory;
    }

    @Override
    public void applyTo(GenerationContext generationContext,
            BeanFactoryInitializationCode beanFactoryInitializationCode) {

      Map<String, String> mappings = buildImportAwareMappings();
      if (!mappings.isEmpty()) {
        GeneratedMethod generatedMethod = beanFactoryInitializationCode
                .getMethods()
                .add("addImportAwareBeanPostProcessors", method ->
                        generateAddPostProcessorMethod(method, mappings));
        beanFactoryInitializationCode
                .addInitializer(generatedMethod.toMethodReference());
        ResourceHints hints = generationContext.getRuntimeHints().resources();
        mappings.forEach(
                (target, from) -> hints.registerType(TypeReference.of(from)));
      }
    }

    private void generateAddPostProcessorMethod(MethodSpec.Builder method, Map<String, String> mappings) {
      method.addJavadoc("Add ImportAwareBeanPostProcessor to support ImportAware beans.");
      method.addModifiers(Modifier.PRIVATE);
      method.addParameter(StandardBeanFactory.class, BEAN_FACTORY_VARIABLE);
      method.addCode(generateAddPostProcessorCode(mappings));
    }

    private CodeBlock generateAddPostProcessorCode(Map<String, String> mappings) {
      CodeBlock.Builder code = CodeBlock.builder();
      code.addStatement("$T $L = new $T<>()", STRING_STRING_MAP, MAPPINGS_VARIABLE, HashMap.class);
      mappings.forEach((type, from) -> code.addStatement("$L.put($S, $S)", MAPPINGS_VARIABLE, type, from));
      code.addStatement("$T $L = new $T($T.class)", RootBeanDefinition.class, BEAN_DEFINITION_VARIABLE,
              RootBeanDefinition.class, ImportAwareAotBeanPostProcessor.class);
      code.addStatement("$L.setRole($T.ROLE_INFRASTRUCTURE)", BEAN_DEFINITION_VARIABLE, BeanDefinition.class);
      code.addStatement("$L.setInstanceSupplier(() -> new $T($L))", BEAN_DEFINITION_VARIABLE, ImportAwareAotBeanPostProcessor.class, MAPPINGS_VARIABLE);
      code.addStatement("$L.registerBeanDefinition($S, $L)", BEAN_FACTORY_VARIABLE, BEAN_NAME, BEAN_DEFINITION_VARIABLE);
      return code.build();
    }

    private Map<String, String> buildImportAwareMappings() {
      ImportRegistry importRegistry = this.beanFactory
              .getBean(IMPORT_REGISTRY_BEAN_NAME, ImportRegistry.class);
      Map<String, String> mappings = new LinkedHashMap<>();
      for (String name : this.beanFactory.getBeanDefinitionNames()) {
        Class<?> beanType = this.beanFactory.getType(name);
        if (beanType != null && ImportAware.class.isAssignableFrom(beanType)) {
          String target = ClassUtils.getUserClass(beanType).getName();
          AnnotationMetadata from = importRegistry.getImportingClassFor(target);
          if (from != null) {
            mappings.put(target, from.getClassName());
          }
        }
      }
      return mappings;
    }

  }

  private static class PropertySourcesAotContribution implements BeanFactoryInitializationAotContribution {

    private static final String ENVIRONMENT_VARIABLE = "environment";

    private static final String RESOURCE_LOADER_VARIABLE = "resourceLoader";

    private final List<PropertySourceDescriptor> descriptors;

    private final Function<String, Resource> resourceResolver;

    PropertySourcesAotContribution(List<PropertySourceDescriptor> descriptors,
            Function<String, Resource> resourceResolver) {
      this.descriptors = descriptors;
      this.resourceResolver = resourceResolver;
    }

    @Override
    public void applyTo(GenerationContext generationContext, BeanFactoryInitializationCode beanFactoryInitializationCode) {
      registerRuntimeHints(generationContext.getRuntimeHints());
      GeneratedMethod generatedMethod = beanFactoryInitializationCode
              .getMethods()
              .add("processPropertySources", this::generateAddPropertySourceProcessorMethod);
      beanFactoryInitializationCode
              .addInitializer(generatedMethod.toMethodReference());
    }

    private void registerRuntimeHints(RuntimeHints hints) {
      for (PropertySourceDescriptor descriptor : this.descriptors) {
        Class<?> factory = descriptor.propertySourceFactory();
        if (factory != null) {
          hints.reflection().registerType(factory, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
        }
        for (String location : descriptor.locations()) {
          if (location.startsWith(PatternResourceLoader.CLASSPATH_ALL_URL_PREFIX)
                  || (location.startsWith(PatternResourceLoader.CLASSPATH_URL_PREFIX)
                  && (location.contains("*") || location.contains("?")))) {

            if (log.isWarnEnabled()) {
              log.warn("""
                      Runtime hint registration is not supported for the 'classpath*:' \
                      prefix or wildcards in @PropertySource locations. Please manually \
                      register a resource hint for each property source location represented \
                      by '{}'.""", location);
            }
          }
          else {
            Resource resource = this.resourceResolver.apply(location);
            if (resource instanceof ClassPathResource classPathResource && classPathResource.exists()) {
              hints.resources().registerPattern(classPathResource.getPath());
            }
          }
        }
      }
    }

    private void generateAddPropertySourceProcessorMethod(MethodSpec.Builder method) {
      method.addJavadoc("Apply known @PropertySources to the environment.");
      method.addModifiers(Modifier.PRIVATE);
      method.addParameter(ConfigurableEnvironment.class, ENVIRONMENT_VARIABLE);
      method.addParameter(ResourceLoader.class, RESOURCE_LOADER_VARIABLE);
      method.addCode(generateAddPropertySourceProcessorCode());
    }

    private CodeBlock generateAddPropertySourceProcessorCode() {
      CodeBlock.Builder code = CodeBlock.builder();
      String processorVariable = "processor";
      code.addStatement("$T $L = new $T($L, $L)", PropertySourceProcessor.class,
              processorVariable, PropertySourceProcessor.class, ENVIRONMENT_VARIABLE,
              RESOURCE_LOADER_VARIABLE);
      code.beginControlFlow("try");
      for (PropertySourceDescriptor descriptor : this.descriptors) {
        code.addStatement("$L.processPropertySource($L)", processorVariable,
                generatePropertySourceDescriptorCode(descriptor));
      }
      code.nextControlFlow("catch ($T ex)", IOException.class);
      code.addStatement("throw new $T(ex)", UncheckedIOException.class);
      code.endControlFlow();
      return code.build();
    }

    private CodeBlock generatePropertySourceDescriptorCode(PropertySourceDescriptor descriptor) {
      CodeBlock.Builder code = CodeBlock.builder();
      code.add("new $T(", PropertySourceDescriptor.class);
      CodeBlock values = descriptor.locations().stream()
              .map(value -> CodeBlock.of("$S", value)).collect(CodeBlock.joining(", "));
      if (descriptor.name() == null && descriptor.propertySourceFactory() == null
              && descriptor.encoding() == null && !descriptor.ignoreResourceNotFound()) {
        code.add("$L)", values);
      }
      else {
        List<CodeBlock> arguments = new ArrayList<>();
        arguments.add(CodeBlock.of("$T.of($L)", List.class, values));
        arguments.add(CodeBlock.of("$L", descriptor.ignoreResourceNotFound()));
        arguments.add(handleNull(descriptor.name(), () -> CodeBlock.of("$S", descriptor.name())));
        arguments.add(handleNull(descriptor.propertySourceFactory(),
                () -> CodeBlock.of("$T.class", descriptor.propertySourceFactory())));
        arguments.add(handleNull(descriptor.encoding(),
                () -> CodeBlock.of("$S", descriptor.encoding())));
        code.add(CodeBlock.join(arguments, ", "));
        code.add(")");
      }
      return code.build();
    }

    private CodeBlock handleNull(@Nullable Object value, Supplier<CodeBlock> nonNull) {
      if (value == null) {
        return CodeBlock.of("null");
      }
      else {
        return nonNull.get();
      }
    }

  }

  private static class ConfigurationClassProxyBeanRegistrationCodeFragments extends BeanRegistrationCodeFragmentsDecorator {

    private final RegisteredBean registeredBean;

    private final Class<?> proxyClass;

    public ConfigurationClassProxyBeanRegistrationCodeFragments(
            BeanRegistrationCodeFragments codeFragments, RegisteredBean registeredBean) {
      super(codeFragments);
      this.registeredBean = registeredBean;
      this.proxyClass = registeredBean.getBeanType().toClass();
    }

    @Override
    public CodeBlock generateSetBeanDefinitionPropertiesCode(GenerationContext generationContext,
            BeanRegistrationCode beanRegistrationCode, RootBeanDefinition beanDefinition, Predicate<String> attributeFilter) {
      CodeBlock.Builder code = CodeBlock.builder();
      code.add(super.generateSetBeanDefinitionPropertiesCode(generationContext,
              beanRegistrationCode, beanDefinition, attributeFilter));
      code.addStatement("$T.initializeConfigurationClass($T.class)",
              ConfigurationClassUtils.class, ClassUtils.getUserClass(this.proxyClass));
      return code.build();
    }

    @Override
    public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
            BeanRegistrationCode beanRegistrationCode, boolean allowDirectSupplierShortcut) {

      Executable executableToUse = proxyExecutable(generationContext.getRuntimeHints(),
              this.registeredBean.resolveConstructorOrFactoryMethod());
      return new InstanceSupplierCodeGenerator(generationContext,
              beanRegistrationCode.getClassName(), beanRegistrationCode.getMethods(), allowDirectSupplierShortcut)
              .generateCode(this.registeredBean, executableToUse);
    }

    private Executable proxyExecutable(RuntimeHints runtimeHints, Executable userExecutable) {
      if (userExecutable instanceof Constructor<?> userConstructor) {
        try {
          runtimeHints.reflection().registerConstructor(userConstructor, ExecutableMode.INTROSPECT);
          return this.proxyClass.getConstructor(userExecutable.getParameterTypes());
        }
        catch (NoSuchMethodException ex) {
          throw new IllegalStateException("No matching constructor found on proxy " + this.proxyClass, ex);
        }
      }
      return userExecutable;
    }

  }

}
