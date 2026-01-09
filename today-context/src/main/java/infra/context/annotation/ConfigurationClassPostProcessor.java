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

package infra.context.annotation;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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

import infra.aop.framework.autoproxy.AutoProxyUtils;
import infra.aot.generate.AccessControl;
import infra.aot.generate.GeneratedClass;
import infra.aot.generate.GeneratedMethod;
import infra.aot.generate.GeneratedMethods;
import infra.aot.generate.GenerationContext;
import infra.aot.generate.MethodReference;
import infra.aot.generate.MethodReference.ArgumentCodeGenerator;
import infra.aot.hint.ExecutableMode;
import infra.aot.hint.MemberCategory;
import infra.aot.hint.ReflectionHints;
import infra.aot.hint.ResourceHints;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.TypeReference;
import infra.beans.BeanUtils;
import infra.beans.PropertyValues;
import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.BeanDefinitionStoreException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanRegistrar;
import infra.beans.factory.DependenciesBeanPostProcessor;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.annotation.AnnotatedBeanDefinition;
import infra.beans.factory.aot.AotServices;
import infra.beans.factory.aot.BeanFactoryInitializationAotContribution;
import infra.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import infra.beans.factory.aot.BeanFactoryInitializationCode;
import infra.beans.factory.aot.BeanRegistrationAotContribution;
import infra.beans.factory.aot.BeanRegistrationAotProcessor;
import infra.beans.factory.aot.BeanRegistrationCode;
import infra.beans.factory.aot.BeanRegistrationCodeFragments;
import infra.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
import infra.beans.factory.aot.InstanceSupplierCodeGenerator;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanDefinitionCustomizer;
import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.config.BeanFactoryPostProcessor;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.config.SingletonBeanRegistry;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import infra.beans.factory.support.BeanNameGenerator;
import infra.beans.factory.support.BeanRegistryAdapter;
import infra.beans.factory.support.RegisteredBean;
import infra.beans.factory.support.RegisteredBean.InstantiationDescriptor;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.BootstrapContext;
import infra.context.BootstrapContextAware;
import infra.context.annotation.ConfigurationClassEnhancer.EnhancedConfiguration;
import infra.core.Ordered;
import infra.core.PriorityOrdered;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.io.ClassPathResource;
import infra.core.io.PatternResourceLoader;
import infra.core.io.PropertySourceDescriptor;
import infra.core.io.PropertySourceProcessor;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.core.type.AnnotationMetadata;
import infra.core.type.MethodMetadata;
import infra.core.type.classreading.CachingMetadataReaderFactory;
import infra.core.type.classreading.MetadataReaderFactory;
import infra.javapoet.ClassName;
import infra.javapoet.CodeBlock;
import infra.javapoet.MethodSpec;
import infra.javapoet.NameAllocator;
import infra.javapoet.ParameterizedTypeName;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.stereotype.Component;
import infra.util.ClassUtils;
import infra.util.CollectionUtils;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.util.ObjectUtils;
import infra.util.ReflectionUtils;
import infra.util.StringUtils;

import static infra.context.annotation.ConfigurationClassUtils.CONFIGURATION_CLASS_LITE;

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

  private final MultiValueMap<String, BeanRegistrar> beanRegistrars = new LinkedMultiValueMap<>();

  @Nullable
  private ConfigurationClassBeanDefinitionReader reader;

  private boolean localBeanNameGeneratorSet = false;

  /* Using fully qualified class names as default bean names by default. */
  private BeanNameGenerator importBeanNameGenerator = IMPORT_BEAN_NAME_GENERATOR;

  @Nullable
  private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

  @Nullable
  private List<PropertySourceDescriptor> propertySourceDescriptors;

  public ConfigurationClassPostProcessor() {
  }

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
    this.importBeanNameGenerator = beanNameGenerator;
    obtainBootstrapContext().setBeanNameGenerator(beanNameGenerator);
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
      this.bootstrapContext = BootstrapContext.obtain(beanFactory);
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
  @SuppressWarnings("NullAway")
  public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableBeanFactory beanFactory) {
    boolean hasPropertySourceDescriptors = CollectionUtils.isNotEmpty(this.propertySourceDescriptors);
    boolean hasImportRegistry = beanFactory.containsBean(IMPORT_REGISTRY_BEAN_NAME);
    boolean hasBeanRegistrars = !this.beanRegistrars.isEmpty();
    if (hasPropertySourceDescriptors || hasImportRegistry || hasBeanRegistrars) {
      return (generationContext, code) -> {
        if (hasPropertySourceDescriptors) {
          new PropertySourcesAotContribution(this.propertySourceDescriptors, this::resolvePropertySourceLocation)
                  .applyTo(generationContext, code);
        }
        if (hasImportRegistry) {
          new ImportAwareAotContribution(beanFactory).applyTo(generationContext, code);
        }
        if (hasBeanRegistrars) {
          new BeanRegistrarAotContribution(this.beanRegistrars, beanFactory).applyTo(generationContext, code);
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
      var configGenerator = (BeanNameGenerator) sbr.getSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR);
      if (configGenerator != null) {
        if (this.localBeanNameGeneratorSet) {
          if (configGenerator instanceof ConfigurationBeanNameGenerator
                  && configGenerator != this.importBeanNameGenerator) {
            throw new IllegalStateException("Context-level ConfigurationBeanNameGenerator [" +
                    configGenerator + "] must not be overridden with processor-level generator [" +
                    this.importBeanNameGenerator + "]");
          }
        }
        else {
          this.importBeanNameGenerator = configGenerator;
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
        this.reader = new ConfigurationClassBeanDefinitionReader(bootstrapContext, importBeanNameGenerator, parser.importRegistry);
      }
      reader.loadBeanDefinitions(configClasses);
      for (ConfigurationClass configClass : configClasses) {
        beanRegistrars.putAll(configClass.beanRegistrars);
      }
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
          throw new BeanDefinitionStoreException(
                  "Cannot enhance @Configuration bean definition '%s' since it is not stored in an AbstractBeanDefinition subclass"
                          .formatted(beanName));
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
    @Nullable
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

    private static final String BEAN_NAME = "infra.context.annotation.internalImportAwareAotProcessor";

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

    private final Function<String, @Nullable Resource> resourceResolver;

    PropertySourcesAotContribution(List<PropertySourceDescriptor> descriptors,
            Function<String, @Nullable Resource> resourceResolver) {
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

      InstantiationDescriptor instantiationDescriptor = proxyInstantiationDescriptor(
              generationContext.getRuntimeHints(), this.registeredBean.resolveInstantiationDescriptor());
      return new InstanceSupplierCodeGenerator(generationContext,
              beanRegistrationCode.getClassName(), beanRegistrationCode.getMethods(), allowDirectSupplierShortcut)
              .generateCode(this.registeredBean, instantiationDescriptor);
    }

    private InstantiationDescriptor proxyInstantiationDescriptor(RuntimeHints runtimeHints, InstantiationDescriptor instantiationDescriptor) {
      Executable userExecutable = instantiationDescriptor.executable();
      if (userExecutable instanceof Constructor<?> userConstructor) {
        try {
          runtimeHints.reflection().registerType(userConstructor.getDeclaringClass());
          Constructor<?> constructor = this.proxyClass.getConstructor(userExecutable.getParameterTypes());
          return new InstantiationDescriptor(constructor);
        }
        catch (NoSuchMethodException ex) {
          throw new IllegalStateException("No matching constructor found on proxy " + this.proxyClass, ex);
        }
      }
      return instantiationDescriptor;
    }

  }

  private static class BeanRegistrarAotContribution implements BeanFactoryInitializationAotContribution {

    private static final String CUSTOMIZER_MAP_VARIABLE = "customizers";

    private static final String ENVIRONMENT_VARIABLE = "environment";

    private final ConfigurableBeanFactory beanFactory;

    private final MultiValueMap<String, BeanRegistrar> beanRegistrars;

    private final AotServices<BeanRegistrationAotProcessor> aotProcessors;

    public BeanRegistrarAotContribution(MultiValueMap<String, BeanRegistrar> beanRegistrars, ConfigurableBeanFactory beanFactory) {
      this.beanFactory = beanFactory;
      this.beanRegistrars = beanRegistrars;
      this.aotProcessors = AotServices.factoriesAndBeans(this.beanFactory).load(BeanRegistrationAotProcessor.class);
    }

    @Override
    public void applyTo(GenerationContext generationContext, BeanFactoryInitializationCode beanFactoryInitializationCode) {
      GeneratedMethod generatedMethod = beanFactoryInitializationCode.getMethods().add(
              "applyBeanRegistrars", builder -> this.generateApplyBeanRegistrarsMethod(
                      builder, generationContext, beanFactoryInitializationCode.getClassName()));
      beanFactoryInitializationCode.addInitializer(generatedMethod.toMethodReference());
    }

    private void generateApplyBeanRegistrarsMethod(MethodSpec.Builder method, GenerationContext generationContext, ClassName className) {
      ReflectionHints reflectionHints = generationContext.getRuntimeHints().reflection();
      method.addJavadoc("Apply bean registrars.");
      method.addModifiers(Modifier.PRIVATE);
      method.addParameter(BeanFactory.class, BeanFactoryInitializationCode.BEAN_FACTORY_VARIABLE);
      method.addParameter(Environment.class, ENVIRONMENT_VARIABLE);
      method.addCode(generateCustomizerMap());

      for (String name : this.beanFactory.getBeanDefinitionNames()) {
        BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition(name);
        if (beanDefinition.getSource() instanceof Class<?> sourceClass
                && BeanRegistrar.class.isAssignableFrom(sourceClass)) {

          for (BeanRegistrationAotProcessor aotProcessor : this.aotProcessors) {
            BeanRegistrationAotContribution contribution =
                    aotProcessor.processAheadOfTime(RegisteredBean.of(this.beanFactory, name));
            if (contribution != null) {
              contribution.applyTo(generationContext,
                      new UnsupportedBeanRegistrationCode(name, aotProcessor.getClass()));
            }
          }
          if (beanDefinition instanceof RootBeanDefinition rootBeanDefinition) {
            if (rootBeanDefinition.getPreferredConstructors() != null) {
              for (Constructor<?> constructor : rootBeanDefinition.getPreferredConstructors()) {
                reflectionHints.registerConstructor(constructor, ExecutableMode.INVOKE);
              }
            }
            if (ObjectUtils.isNotEmpty(rootBeanDefinition.getInitMethodNames())) {
              method.addCode(generateInitDestroyMethods(name, rootBeanDefinition,
                      rootBeanDefinition.getInitMethodNames(), "setInitMethodNames", reflectionHints));
            }
            if (ObjectUtils.isNotEmpty(rootBeanDefinition.getDestroyMethodNames())) {
              method.addCode(generateInitDestroyMethods(name, rootBeanDefinition,
                      rootBeanDefinition.getDestroyMethodNames(), "setDestroyMethodNames", reflectionHints));
            }
            checkUnsupportedFeatures(rootBeanDefinition);
          }
        }
      }
      method.addCode(generateRegisterCode(className, generationContext));
    }

    private void checkUnsupportedFeatures(AbstractBeanDefinition beanDefinition) {
      if (ObjectUtils.isNotEmpty(beanDefinition.getFactoryBeanName())) {
        throw new UnsupportedOperationException("AOT post processing of the factory bean name is not supported yet with BeanRegistrar");
      }
      if (beanDefinition.hasConstructorArgumentValues()) {
        throw new UnsupportedOperationException("AOT post processing of argument values is not supported yet with BeanRegistrar");
      }
      if (!beanDefinition.getQualifiers().isEmpty()) {
        throw new UnsupportedOperationException("AOT post processing of qualifiers is not supported yet with BeanRegistrar");
      }
    }

    private CodeBlock generateCustomizerMap() {
      var code = CodeBlock.builder();
      code.addStatement("$T<$T, $T> $L = new $T<>()", MultiValueMap.class, String.class, BeanDefinitionCustomizer.class,
              CUSTOMIZER_MAP_VARIABLE, LinkedMultiValueMap.class);
      return code.build();
    }

    private CodeBlock generateRegisterCode(ClassName className, GenerationContext generationContext) {
      var code = CodeBlock.builder();
      CodeBlock.Builder metadataReaderFactoryCode = null;
      NameAllocator nameAllocator = new NameAllocator();
      for (var beanRegistrarEntry : this.beanRegistrars.entrySet()) {
        for (BeanRegistrar beanRegistrar : beanRegistrarEntry.getValue()) {
          String beanRegistrarName = nameAllocator.newName(StringUtils.uncapitalize(beanRegistrar.getClass().getSimpleName()));
          Constructor<?> constructor = BeanUtils.obtainConstructor(beanRegistrar.getClass());
          boolean visible = isVisible(constructor, className);
          if (visible) {
            code.addStatement("$T $L = new $T()", beanRegistrar.getClass(), beanRegistrarName, beanRegistrar.getClass());
          }
          else {
            try {
              Class<?> configClass = ClassUtils.forName(beanRegistrarEntry.getKey(), beanRegistrar.getClass().getClassLoader());
              GeneratedClass generatedClass = generationContext.getGeneratedClasses()
                      .getOrAddForFeatureComponent("BeanRegistrars", configClass, type ->
                              type.addJavadoc("Bean registrars for {@link $T}.", configClass)
                                      .addModifiers(Modifier.PUBLIC));
              GeneratedMethod generatedMethod = generatedClass.getMethods().add(
                      "get" + beanRegistrar.getClass().getSimpleName(),
                      method -> method
                              .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                              .returns(BeanRegistrar.class)
                              .addStatement("return new $T()", beanRegistrar.getClass()));
              code.addStatement("$T $L = $L", BeanRegistrar.class, beanRegistrarName,
                      generatedMethod.toMethodReference().toInvokeCodeBlock(ArgumentCodeGenerator.none()));
            }
            catch (ClassNotFoundException ex) {
              throw new IllegalStateException(ex);
            }
          }
          if (beanRegistrar instanceof ImportAware) {
            if (metadataReaderFactoryCode == null) {
              metadataReaderFactoryCode = CodeBlock.builder();
              metadataReaderFactoryCode.addStatement("$T metadataReaderFactory = new $T()",
                      MetadataReaderFactory.class, CachingMetadataReaderFactory.class);
            }
            CodeBlock setImportMetadataCode;
            if (visible) {
              setImportMetadataCode = CodeBlock.builder()
                      .addStatement("$L.setImportMetadata(metadataReaderFactory.getMetadataReader($S).getAnnotationMetadata())",
                              beanRegistrarName, beanRegistrarEntry.getKey()).build();
            }
            else {
              setImportMetadataCode = CodeBlock.builder()
                      .addStatement("(($T)$L).setImportMetadata(metadataReaderFactory.getMetadataReader($S).getAnnotationMetadata())",
                              ImportAware.class, beanRegistrarName, beanRegistrarEntry.getKey()).build();
            }
            code.beginControlFlow("try")
                    .add(setImportMetadataCode)
                    .nextControlFlow("catch ($T ex)", IOException.class)
                    .addStatement("throw new $T(\"Failed to read metadata for '$L'\", ex)",
                            IllegalStateException.class, beanRegistrarEntry.getKey())
                    .endControlFlow();
          }
          code.addStatement("$L.register(new $T(($T)$L, $L, $L, $L.getClass(), $L), $L)", beanRegistrarName,
                  BeanRegistryAdapter.class, BeanDefinitionRegistry.class, BeanFactoryInitializationCode.BEAN_FACTORY_VARIABLE,
                  BeanFactoryInitializationCode.BEAN_FACTORY_VARIABLE, ENVIRONMENT_VARIABLE, beanRegistrarName,
                  CUSTOMIZER_MAP_VARIABLE, ENVIRONMENT_VARIABLE);
        }
      }
      return (metadataReaderFactoryCode == null ? code.build() : metadataReaderFactoryCode.add(code.build()).build());
    }

    private boolean isVisible(Constructor<?> ctor, ClassName className) {
      AccessControl classAccessControl = AccessControl.forClass(ctor.getDeclaringClass());
      AccessControl memberAccessControl = AccessControl.forMember(ctor);
      AccessControl.Visibility visibility = AccessControl.lowest(classAccessControl, memberAccessControl).getVisibility();
      return (visibility == AccessControl.Visibility.PUBLIC || (visibility != AccessControl.Visibility.PRIVATE &&
              ctor.getDeclaringClass().getPackageName().equals(className.packageName())));
    }

    private CodeBlock generateInitDestroyMethods(String beanName, AbstractBeanDefinition beanDefinition,
            String[] methodNames, String method, ReflectionHints reflectionHints) {

      var code = CodeBlock.builder();
      // For Publisher-based destroy methods
      reflectionHints.registerType(TypeReference.of("org.reactivestreams.Publisher"));
      Class<?> beanType = ClassUtils.getUserClass(beanDefinition.getResolvableType().toClass());
      Arrays.stream(methodNames).forEach(methodName -> addInitDestroyHint(beanType, methodName, reflectionHints));
      CodeBlock arguments = Arrays.stream(methodNames)
              .map(name -> CodeBlock.of("$S", name))
              .collect(CodeBlock.joining(", "));

      code.addStatement("$L.add($S, $L -> (($T)$L).$L($L))", CUSTOMIZER_MAP_VARIABLE, beanName, "bd",
              AbstractBeanDefinition.class, "bd", method, arguments);
      return code.build();
    }

    // Inspired from BeanDefinitionPropertiesCodeGenerator#addInitDestroyHint
    private static void addInitDestroyHint(Class<?> beanUserClass, String methodName, ReflectionHints reflectionHints) {
      Class<?> methodDeclaringClass = beanUserClass;

      // Parse fully-qualified method name if necessary.
      int indexOfDot = methodName.lastIndexOf('.');
      if (indexOfDot > 0) {
        String className = methodName.substring(0, indexOfDot);
        methodName = methodName.substring(indexOfDot + 1);
        if (!beanUserClass.getName().equals(className)) {
          try {
            methodDeclaringClass = ClassUtils.forName(className, beanUserClass.getClassLoader());
          }
          catch (Throwable ex) {
            throw new IllegalStateException("Failed to load Class [%s] from ClassLoader [%s]"
                    .formatted(className, beanUserClass.getClassLoader()), ex);
          }
        }
      }

      Method method = ReflectionUtils.findMethod(methodDeclaringClass, methodName);
      if (method != null) {
        reflectionHints.registerMethod(method, ExecutableMode.INVOKE);
        Method publiclyAccessibleMethod = ReflectionUtils.getPubliclyAccessibleMethodIfPossible(method, beanUserClass);
        if (!publiclyAccessibleMethod.equals(method)) {
          reflectionHints.registerMethod(publiclyAccessibleMethod, ExecutableMode.INVOKE);
        }
      }
    }

    static class UnsupportedBeanRegistrationCode implements BeanRegistrationCode {

      private final String message;

      public UnsupportedBeanRegistrationCode(String beanName, Class<?> aotProcessorClass) {
        this.message = "Code generation attempted for bean %s by the AOT Processor %s is not supported with BeanRegistrar yet"
                .formatted(beanName, aotProcessorClass);
      }

      @Override
      public ClassName getClassName() {
        throw new UnsupportedOperationException(this.message);
      }

      @Override
      public GeneratedMethods getMethods() {
        throw new UnsupportedOperationException(this.message);
      }

      @Override
      public void addInstancePostProcessor(MethodReference methodReference) {
        throw new UnsupportedOperationException(this.message);
      }
    }
  }

}
