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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Predicate;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.support.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.context.annotation.ConfigurationCondition.ConfigurationPhase;
import cn.taketoday.context.annotation.DeferredImportSelector.Group;
import cn.taketoday.context.loader.BootstrapContext;
import cn.taketoday.context.loader.ImportSelector;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.NestedIOException;
import cn.taketoday.core.OrderComparator;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.env.CompositePropertySource;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.EncodedResource;
import cn.taketoday.core.io.PropertySourceFactory;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourcePropertySource;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.core.type.StandardAnnotationMetadata;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.filter.AssignableTypeFilter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Parses a {@link Configuration} class definition, populating a collection of
 * {@link ConfigurationClass} objects (parsing a single Configuration class may result in
 * any number of ConfigurationClass objects because one Configuration class may import
 * another using the {@link Import} annotation).
 *
 * <p>This class helps separate the concern of parsing the structure of a Configuration
 * class from the concern of registering BeanDefinition objects based on the content of
 * that model (with the exception of {@code @ComponentScan} annotations which need to be
 * registered immediately).
 *
 * <p>This ASM-based implementation avoids reflection and eager class loading in order to
 * interoperate effectively with lazy class loading in a Framework ApplicationContext.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @see ConfigurationClassBeanDefinitionReader
 * @since 4.0
 */
class ConfigurationClassParser {

  private static final Predicate<String> DEFAULT_EXCLUSION_FILTER = className ->
          className.startsWith("java.lang.annotation.")
                  || className.startsWith("cn.taketoday.lang.");

  private static final Comparator<DeferredImportSelectorHolder> DEFERRED_IMPORT_COMPARATOR =
          (o1, o2) -> AnnotationAwareOrderComparator.INSTANCE.compare(o1.getImportSelector(), o2.getImportSelector());

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final ComponentScanAnnotationParser componentScanParser;

  private final Map<ConfigurationClass, ConfigurationClass> configurationClasses = new LinkedHashMap<>();

  private final Map<String, ConfigurationClass> knownSuperclasses = new HashMap<>();

  private final List<String> propertySourceNames = new ArrayList<>();

  private final ImportStack importStack = new ImportStack();

  private final DeferredImportSelectorHandler deferredImportSelectorHandler = new DeferredImportSelectorHandler();

  private final SourceClass objectSourceClass = new SourceClass(Object.class);

  private final BootstrapContext bootstrapContext;

  /**
   * Create a new {@link ConfigurationClassParser} instance that will be used
   * to populate the set of configuration classes.
   */
  public ConfigurationClassParser(BootstrapContext bootstrapContext) {
    this.bootstrapContext = bootstrapContext;
    this.componentScanParser = new ComponentScanAnnotationParser(bootstrapContext);
  }

  public void parse(Set<BeanDefinition> configCandidates) {
    for (BeanDefinition definition : configCandidates) {
      try {
        if (definition instanceof AnnotatedBeanDefinition) {
          parse(((AnnotatedBeanDefinition) definition).getMetadata(), definition.getBeanName());
        }
        else if (definition.hasBeanClass()) {
          parse(definition.getBeanClass(), definition.getBeanName());
        }
        else {
          parse(definition.getBeanClassName(), definition.getBeanName());
        }
      }
      catch (BeanDefinitionStoreException ex) {
        throw ex;
      }
      catch (Throwable ex) {
        throw new BeanDefinitionStoreException(
                "Failed to parse configuration class [" + definition.getBeanClassName() + "]", ex);
      }
    }

    this.deferredImportSelectorHandler.process();
  }

  protected final void parse(@Nullable String className, String beanName) throws IOException {
    Assert.notNull(className, "No bean class name for configuration class bean definition");
    MetadataReader reader = bootstrapContext.getMetadataReader(className);
    processConfigurationClass(new ConfigurationClass(reader, beanName), DEFAULT_EXCLUSION_FILTER);
  }

  protected final void parse(Class<?> clazz, String beanName) throws IOException {
    processConfigurationClass(new ConfigurationClass(clazz, beanName), DEFAULT_EXCLUSION_FILTER);
  }

  protected final void parse(AnnotationMetadata metadata, String beanName) throws IOException {
    processConfigurationClass(new ConfigurationClass(metadata, beanName), DEFAULT_EXCLUSION_FILTER);
  }

  /**
   * Validate each {@link ConfigurationClass} object.
   *
   * @see ConfigurationClass#validate
   */
  public void validate() {
    ProblemReporter problemReporter = bootstrapContext.getProblemReporter();
    for (ConfigurationClass configClass : this.configurationClasses.keySet()) {
      configClass.validate(problemReporter);
    }
  }

  public Set<ConfigurationClass> getConfigurationClasses() {
    return this.configurationClasses.keySet();
  }

  protected void processConfigurationClass(ConfigurationClass configClass, Predicate<String> filter) throws IOException {
    if (bootstrapContext.passCondition(
            configClass.getMetadata(), ConfigurationPhase.PARSE_CONFIGURATION)) {
      ConfigurationClass existingClass = this.configurationClasses.get(configClass);
      if (existingClass != null) {
        if (configClass.isImported()) {
          if (existingClass.isImported()) {
            existingClass.mergeImportedBy(configClass);
          }
          // Otherwise ignore new imported config class; existing non-imported class overrides it.
          return;
        }
        else {
          // Explicit bean definition found, probably replacing an import.
          // Let's remove the old one and go with the new one.
          this.configurationClasses.remove(configClass);
          this.knownSuperclasses.values().removeIf(configClass::equals);
        }
      }

      // Recursively process the configuration class and its superclass hierarchy.
      SourceClass sourceClass = asSourceClass(configClass, filter);
      do {
        sourceClass = doProcessConfigurationClass(configClass, sourceClass, filter);
      }
      while (sourceClass != null);

      this.configurationClasses.put(configClass, configClass);
    }
  }

  /**
   * Apply processing and build a complete {@link ConfigurationClass} by reading the
   * annotations, members and methods from the source class. This method can be called
   * multiple times as relevant sources are discovered.
   *
   * @param configClass the configuration class being build
   * @param sourceClass a source class
   * @return the superclass, or {@code null} if none found or previously processed
   */
  @Nullable
  protected final SourceClass doProcessConfigurationClass(
          ConfigurationClass configClass, SourceClass sourceClass, Predicate<String> filter) throws IOException {

    if (configClass.getMetadata().isAnnotated(Component.class.getName())) {
      // Recursively process any member (nested) classes first
      processMemberClasses(configClass, sourceClass, filter);
    }

    // Process any @PropertySource annotations
    Environment environment = bootstrapContext.getEnvironment();
    for (MergedAnnotation<PropertySource> propertySource : repeatable(
            sourceClass.getMetadata().getAnnotations(), PropertySource.class, PropertySources.class)) {
      if (environment instanceof ConfigurableEnvironment) {
        processPropertySource(propertySource);
      }
      else {
        logger.info("Ignoring @PropertySource annotation on [" + sourceClass.getMetadata().getClassName() +
                "]. Reason: Environment must implement ConfigurableEnvironment");
      }
    }

    // Process any @ComponentScan annotations

    Set<MergedAnnotation<ComponentScan>> componentScans = repeatable(
            sourceClass.getMetadata().getAnnotations(), ComponentScan.class, ComponentScans.class);

    if (!componentScans.isEmpty()
            && bootstrapContext.passCondition(sourceClass.getMetadata(), ConfigurationPhase.REGISTER_BEAN)) {
      for (MergedAnnotation<ComponentScan> componentScan : componentScans) {
        // The config class is annotated with @ComponentScan -> perform the scan immediately
        Set<BeanDefinition> scannedBeanDefinitions =
                componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());
        // Check the set of scanned definitions for any further config classes and parse recursively if needed
        for (BeanDefinition definition : scannedBeanDefinitions) {
          if (ConfigurationClassUtils.checkConfigurationClassCandidate(definition, bootstrapContext)) {
            parse(definition.getBeanClassName(), definition.getBeanName());
          }
        }
      }
    }

    // Process any @Import annotations
    processImports(configClass, sourceClass, getImports(sourceClass), filter, true);

    // Process individual @Component methods
    Set<MethodMetadata> beanMethods = retrieveComponentMethodMetadata(sourceClass);
    for (MethodMetadata methodMetadata : beanMethods) {
      configClass.addMethod(new ComponentMethod(methodMetadata, configClass));
    }

    // Process default methods on interfaces
    processInterfaces(configClass, sourceClass);

    // Process superclass, if any
    if (sourceClass.getMetadata().hasSuperClass()) {
      String superclass = sourceClass.getMetadata().getSuperClassName();
      if (superclass != null && !superclass.startsWith("java")
              && !this.knownSuperclasses.containsKey(superclass)) {
        this.knownSuperclasses.put(superclass, configClass);
        // Superclass found, return its annotation metadata and recurse
        return sourceClass.getSuperClass();
      }
    }

    // No superclass -> processing is complete
    return null;
  }

  /**
   * Register member (nested) classes that happen to be configuration classes themselves.
   */
  private void processMemberClasses(
          ConfigurationClass configClass, SourceClass sourceClass,
          Predicate<String> filter) throws IOException {
    ProblemReporter problemReporter = bootstrapContext.getProblemReporter();
    Collection<SourceClass> memberClasses = sourceClass.getMemberClasses();
    if (!memberClasses.isEmpty()) {
      List<SourceClass> candidates = new ArrayList<>(memberClasses.size());
      for (SourceClass memberClass : memberClasses) {
        if (ConfigurationClassUtils.isConfigurationCandidate(memberClass.getMetadata()) &&
                !memberClass.getMetadata().getClassName().equals(configClass.getMetadata().getClassName())) {
          candidates.add(memberClass);
        }
      }
      OrderComparator.sort(candidates);
      for (SourceClass candidate : candidates) {
        if (this.importStack.contains(configClass)) {
          problemReporter.error(new CircularImportProblem(configClass, this.importStack));
        }
        else {
          this.importStack.push(configClass);
          try {
            processConfigurationClass(candidate.asConfigClass(configClass), filter);
          }
          finally {
            this.importStack.pop();
          }
        }
      }
    }
  }

  /**
   * Register default methods on interfaces implemented by the configuration class.
   */
  private void processInterfaces(ConfigurationClass configClass, SourceClass sourceClass) throws IOException {
    for (SourceClass ifc : sourceClass.getInterfaces()) {
      Set<MethodMetadata> beanMethods = retrieveComponentMethodMetadata(ifc);
      for (MethodMetadata methodMetadata : beanMethods) {
        if (!methodMetadata.isAbstract()) {
          // A default method or other concrete method on a Java 8+ interface...
          configClass.addMethod(new ComponentMethod(methodMetadata, configClass));
        }
      }
      processInterfaces(configClass, ifc);
    }
  }

  /**
   * Retrieve the metadata for all <code>@Component</code> methods.
   */
  private Set<MethodMetadata> retrieveComponentMethodMetadata(SourceClass sourceClass) {
    AnnotationMetadata original = sourceClass.getMetadata();
    Set<MethodMetadata> componentMethods = original.getAnnotatedMethods(Component.class.getName());
    if (componentMethods.size() > 1 && original instanceof StandardAnnotationMetadata) {
      // Try reading the class file via ASM for deterministic declaration order...
      // Unfortunately, the JVM's standard reflection returns methods in arbitrary
      // order, even between different runs of the same application on the same JVM.
      try {
        AnnotationMetadata asm = bootstrapContext.getAnnotationMetadata(original.getClassName());
        Set<MethodMetadata> asmMethods = asm.getAnnotatedMethods(Component.class.getName());
        if (asmMethods.size() >= componentMethods.size()) {
          LinkedHashSet<MethodMetadata> selectedMethods = new LinkedHashSet<>(asmMethods.size());
          for (MethodMetadata asmMethod : asmMethods) {
            for (MethodMetadata beanMethod : componentMethods) {
              if (beanMethod.getMethodName().equals(asmMethod.getMethodName())) {
                selectedMethods.add(beanMethod);
                break;
              }
            }
          }
          if (selectedMethods.size() == componentMethods.size()) {
            // All reflection-detected methods found in ASM method set -> proceed
            componentMethods = selectedMethods;
          }
        }
      }
      catch (Exception ex) {
        logger.debug("Failed to read class file via ASM for determining @Component method order", ex);
        // No worries, let's continue with the reflection metadata we started with...
      }
    }
    return componentMethods;
  }

  /**
   * Process the given <code>@PropertySource</code> annotation metadata.
   *
   * @param propertySource metadata for the <code>@PropertySource</code> annotation found
   */
  private void processPropertySource(MergedAnnotation<PropertySource> propertySource) {
    String name = propertySource.getString("name");
    if (!StringUtils.hasText(name)) {
      name = null;
    }
    String encoding = propertySource.getString("encoding");
    if (!StringUtils.hasText(encoding)) {
      encoding = null;
    }
    String[] locations = propertySource.getStringArray("value");
    Assert.isTrue(locations.length > 0, "At least one @PropertySource(value) location is required");

    Class<? extends PropertySourceFactory> factoryClass = propertySource.getClass("factory");
    PropertySourceFactory factory =
            factoryClass == PropertySourceFactory.class
            ? bootstrapContext.getPropertySourceFactory() : bootstrapContext.instantiate(factoryClass);

    for (String location : locations) {
      try {
        String resolvedLocation = bootstrapContext.evaluateExpression(location);
        Resource resource = bootstrapContext.getResource(resolvedLocation);
        addPropertySource(factory.createPropertySource(name, new EncodedResource(resource, encoding)));
      }
      catch (IllegalArgumentException | FileNotFoundException | UnknownHostException | SocketException ex) {
        // Placeholders not resolvable or resource not found when trying to open it
        if (propertySource.getBoolean("ignoreResourceNotFound")) {
          if (logger.isInfoEnabled()) {
            logger.info("Properties location [{}] not resolvable: {}", location, ex.getMessage());
          }
        }
        else {
          throw ExceptionUtils.sneakyThrow(ex);
        }
      }
      catch (IOException e) {
        throw ExceptionUtils.sneakyThrow(e);
      }
    }
  }

  private void addPropertySource(cn.taketoday.core.env.PropertySource<?> propertySource) {
    String name = propertySource.getName();
    Environment environment = bootstrapContext.getEnvironment();
    cn.taketoday.core.env.PropertySources propertySources = ((ConfigurableEnvironment) environment).getPropertySources();

    if (this.propertySourceNames.contains(name)) {
      // We've already added a version, we need to extend it
      cn.taketoday.core.env.PropertySource<?> existing = propertySources.get(name);
      if (existing != null) {
        cn.taketoday.core.env.PropertySource<?> newSource
                = propertySource instanceof ResourcePropertySource
                  ? ((ResourcePropertySource) propertySource).withResourceName()
                  : propertySource;

        if (existing instanceof CompositePropertySource) {
          ((CompositePropertySource) existing).addFirstPropertySource(newSource);
        }
        else {
          if (existing instanceof ResourcePropertySource) {
            existing = ((ResourcePropertySource) existing).withResourceName();
          }
          CompositePropertySource composite = new CompositePropertySource(name);
          composite.addPropertySource(newSource);
          composite.addPropertySource(existing);
          propertySources.replace(name, composite);
        }
        return;
      }
    }

    if (this.propertySourceNames.isEmpty()) {
      propertySources.addLast(propertySource);
    }
    else {
      String firstProcessed = this.propertySourceNames.get(this.propertySourceNames.size() - 1);
      propertySources.addBefore(firstProcessed, propertySource);
    }
    this.propertySourceNames.add(name);
  }

  static <A extends Annotation, C extends Annotation> Set<MergedAnnotation<A>> repeatable(
          MergedAnnotations annotations, Class<A> annotationType, Class<C> con) {
    return repeatable(annotations, annotationType, con, MergedAnnotation.VALUE);
  }

  static <A extends Annotation, C extends Annotation> Set<MergedAnnotation<A>> repeatable(
          MergedAnnotations annotations, Class<A> annotationType, Class<C> container, String attributeName) {

    LinkedHashSet<MergedAnnotation<A>> result = new LinkedHashSet<>();
    // Direct annotation present?
    addAttributesIfNotNull(result, annotations.get(annotationType));
    MergedAnnotation<C> annotation = annotations.get(container);

    if (annotation.isPresent()) {
      // repeatable exist
      MergedAnnotation<A>[] repeatable = annotation.getAnnotationArray(attributeName, annotationType);
      for (MergedAnnotation<A> mergedAnnotation : repeatable) {
        addAttributesIfNotNull(result, mergedAnnotation);
      }
    }
    // Return merged result
    return result;
  }

  private static <A extends Annotation> void addAttributesIfNotNull(
          Set<MergedAnnotation<A>> result, MergedAnnotation<A> propertySource) {

    if (propertySource.isPresent()) {
      result.add(propertySource);
    }
  }

  /**
   * Returns {@code @Import} class, considering all meta-annotations.
   */
  private Set<SourceClass> getImports(SourceClass sourceClass) throws IOException {
    Set<SourceClass> imports = new LinkedHashSet<>();
    Set<SourceClass> visited = new LinkedHashSet<>();
    collectImports(sourceClass, imports, visited);
    return imports;
  }

  /**
   * Recursively collect all declared {@code @Import} values. Unlike most
   * meta-annotations it is valid to have several {@code @Import}s declared with
   * different values; the usual process of returning values from the first
   * meta-annotation on a class is not sufficient.
   * <p>For example, it is common for a {@code @Configuration} class to declare direct
   * {@code @Import}s in addition to meta-imports originating from an {@code @Enable}
   * annotation.
   *
   * @param sourceClass the class to search
   * @param imports the imports collected so far
   * @param visited used to track visited classes to prevent infinite recursion
   * @throws IOException if there is any problem reading metadata from the named class
   */
  private void collectImports(SourceClass sourceClass, Set<SourceClass> imports, Set<SourceClass> visited)
          throws IOException {

    if (visited.add(sourceClass)) {
      for (SourceClass annotation : sourceClass.getAnnotationsAsSourceClass()) {
        String annName = annotation.getMetadata().getClassName();
        if (!annName.equals(Import.class.getName())) {
          collectImports(annotation, imports, visited);
        }
      }
      imports.addAll(sourceClass.getAnnotationAttributes(Import.class.getName(), "value"));
    }
  }

  private void processImports(
          ConfigurationClass configClass,
          SourceClass currentSourceClass,
          Collection<SourceClass> importCandidates,
          Predicate<String> exclusionFilter, boolean checkForCircularImports
  ) {

    if (importCandidates.isEmpty()) {
      return;
    }

    if (checkForCircularImports && isChainedImportOnStack(configClass)) {
      bootstrapContext.reportError(new CircularImportProblem(configClass, this.importStack));
    }
    else {
      this.importStack.push(configClass);
      try {
        for (SourceClass candidate : importCandidates) {
          if (candidate.isAssignable(ImportSelector.class)) {
            // Candidate class is an ImportSelector -> delegate to it to determine imports
            Class<?> candidateClass = candidate.loadClass();
            ImportSelector selector = ParserStrategyUtils.instantiateClass(
                    candidateClass, ImportSelector.class, bootstrapContext);
            Predicate<String> selectorFilter = selector.getExclusionFilter();
            if (selectorFilter != null) {
              exclusionFilter = exclusionFilter.or(selectorFilter);
            }
            if (selector instanceof DeferredImportSelector deferredSelector) {
              this.deferredImportSelectorHandler.handle(configClass, deferredSelector);
            }
            else {
              String[] importClassNames = selector.selectImports(currentSourceClass.getMetadata());
              Collection<SourceClass> importSourceClasses = asSourceClasses(importClassNames, exclusionFilter);
              processImports(configClass, currentSourceClass, importSourceClasses, exclusionFilter, false);
            }
          }
          else if (candidate.isAssignable(ImportBeanDefinitionRegistrar.class)) {
            // Candidate class is an ImportBeanDefinitionRegistrar ->
            // delegate to it to register additional bean definitions
            Class<?> candidateClass = candidate.loadClass();
            ImportBeanDefinitionRegistrar registrar =
                    ParserStrategyUtils.instantiateClass(
                            candidateClass, ImportBeanDefinitionRegistrar.class, bootstrapContext);
            configClass.addImportBeanDefinitionRegistrar(registrar, currentSourceClass.getMetadata());
          }
          else {
            // Candidate class not an ImportSelector or ImportBeanDefinitionRegistrar ->
            // process it as an @Configuration class
            this.importStack.registerImport(
                    currentSourceClass.getMetadata(), candidate.getMetadata().getClassName());
            processConfigurationClass(candidate.asConfigClass(configClass), exclusionFilter);
          }
        }
      }
      catch (BeanDefinitionStoreException ex) {
        throw ex;
      }
      catch (Throwable ex) {
        throw new BeanDefinitionStoreException(
                "Failed to process import candidates for configuration class [" +
                        configClass.getMetadata().getClassName() + "]", ex);
      }
      finally {
        this.importStack.pop();
      }
    }
  }

  private boolean isChainedImportOnStack(ConfigurationClass configClass) {
    if (this.importStack.contains(configClass)) {
      String configClassName = configClass.getMetadata().getClassName();
      AnnotationMetadata importingClass = this.importStack.getImportingClassFor(configClassName);
      while (importingClass != null) {
        if (configClassName.equals(importingClass.getClassName())) {
          return true;
        }
        importingClass = this.importStack.getImportingClassFor(importingClass.getClassName());
      }
    }
    return false;
  }

  ImportRegistry getImportRegistry() {
    return this.importStack;
  }

  /**
   * Factory method to obtain a {@link SourceClass} from a {@link ConfigurationClass}.
   */
  private SourceClass asSourceClass(ConfigurationClass configurationClass, Predicate<String> filter) throws IOException {
    AnnotationMetadata metadata = configurationClass.getMetadata();
    if (metadata instanceof StandardAnnotationMetadata) {
      return asSourceClass(((StandardAnnotationMetadata) metadata).getIntrospectedClass(), filter);
    }
    return asSourceClass(metadata.getClassName(), filter);
  }

  /**
   * Factory method to obtain a {@link SourceClass} from a {@link Class}.
   */
  SourceClass asSourceClass(@Nullable Class<?> classType, Predicate<String> filter) throws IOException {
    if (classType == null || filter.test(classType.getName())) {
      return this.objectSourceClass;
    }
    try {
      // Sanity test that we can reflectively read annotations,
      // including Class attributes; if not -> fall back to ASM
      for (Annotation ann : classType.getDeclaredAnnotations()) {
        AnnotationUtils.validateAnnotation(ann);
      }
      return new SourceClass(classType);
    }
    catch (Throwable ex) {
      // Enforce ASM via class name resolution
      return asSourceClass(classType.getName(), filter);
    }
  }

  /**
   * Factory method to obtain a {@link SourceClass} collection from class names.
   */
  private Collection<SourceClass> asSourceClasses(String[] classNames, Predicate<String> filter) throws IOException {
    ArrayList<SourceClass> annotatedClasses = new ArrayList<>(classNames.length);
    for (String className : classNames) {
      annotatedClasses.add(asSourceClass(className, filter));
    }
    return annotatedClasses;
  }

  /**
   * Factory method to obtain a {@link SourceClass} from a class name.
   */
  SourceClass asSourceClass(@Nullable String className, Predicate<String> filter) throws IOException {
    if (className == null || filter.test(className)) {
      return this.objectSourceClass;
    }
    if (className.startsWith("java")) {
      // Never use ASM for core java types
      try {
        return new SourceClass(ClassUtils.forName(className, bootstrapContext.getClassLoader()));
      }
      catch (ClassNotFoundException ex) {
        throw new NestedIOException("Failed to load class [" + className + "]", ex);
      }
    }
    return new SourceClass(bootstrapContext.getMetadataReader(className));
  }

  @SuppressWarnings("serial")
  private static class ImportStack extends ArrayDeque<ConfigurationClass> implements ImportRegistry {

    private final MultiValueMap<String, AnnotationMetadata> imports = MultiValueMap.fromLinkedHashMap();

    public void registerImport(AnnotationMetadata importingClass, String importedClass) {
      this.imports.add(importedClass, importingClass);
    }

    @Override
    @Nullable
    public AnnotationMetadata getImportingClassFor(String importedClass) {
      return CollectionUtils.lastElement(this.imports.get(importedClass));
    }

    @Override
    public void removeImportingClass(String importingClass) {
      for (List<AnnotationMetadata> list : this.imports.values()) {
        for (Iterator<AnnotationMetadata> iterator = list.iterator(); iterator.hasNext(); ) {
          if (iterator.next().getClassName().equals(importingClass)) {
            iterator.remove();
            break;
          }
        }
      }
    }

    /**
     * Given a stack containing (in order)
     * <ul>
     * <li>com.acme.Foo</li>
     * <li>com.acme.Bar</li>
     * <li>com.acme.Baz</li>
     * </ul>
     * return "[Foo->Bar->Baz]".
     */
    @Override
    public String toString() {
      StringJoiner joiner = new StringJoiner("->", "[", "]");
      for (ConfigurationClass configurationClass : this) {
        joiner.add(configurationClass.getSimpleName());
      }
      return joiner.toString();
    }
  }

  private class DeferredImportSelectorHandler {

    @Nullable
    private ArrayList<DeferredImportSelectorHolder> deferredImportSelectors = new ArrayList<>();

    /**
     * Handle the specified {@link DeferredImportSelector}. If deferred import
     * selectors are being collected, this registers this instance to the list. If
     * they are being processed, the {@link DeferredImportSelector} is also processed
     * immediately according to its {@link DeferredImportSelector.Group}.
     *
     * @param configClass the source configuration class
     * @param importSelector the selector to handle
     */
    public void handle(ConfigurationClass configClass, DeferredImportSelector importSelector) {
      DeferredImportSelectorHolder holder = new DeferredImportSelectorHolder(configClass, importSelector);
      if (this.deferredImportSelectors == null) {
        DeferredImportSelectorGroupingHandler handler = new DeferredImportSelectorGroupingHandler();
        handler.register(holder);
        handler.processGroupImports();
      }
      else {
        this.deferredImportSelectors.add(holder);
      }
    }

    public void process() {
      List<DeferredImportSelectorHolder> deferredImports = this.deferredImportSelectors;
      this.deferredImportSelectors = null;
      try {
        if (deferredImports != null) {
          DeferredImportSelectorGroupingHandler handler = new DeferredImportSelectorGroupingHandler();
          deferredImports.sort(DEFERRED_IMPORT_COMPARATOR);

          for (DeferredImportSelectorHolder deferredImport : deferredImports) {
            handler.register(deferredImport);
          }

          handler.processGroupImports();
        }
      }
      finally {
        this.deferredImportSelectors = new ArrayList<>();
      }
    }
  }

  private class DeferredImportSelectorGroupingHandler {

    private final Map<Object, DeferredImportSelectorGrouping> groupings = new LinkedHashMap<>();
    private final Map<AnnotationMetadata, ConfigurationClass> configurationClasses = new HashMap<>();

    public void register(DeferredImportSelectorHolder deferredImport) {
      Class<? extends Group> group = deferredImport.getImportSelector().getImportGroup();
      DeferredImportSelectorGrouping grouping = groupings.computeIfAbsent(
              group != null ? group : deferredImport,
              key -> new DeferredImportSelectorGrouping(createGroup(group))
      );
      grouping.add(deferredImport);
      configurationClasses.put(deferredImport.getConfigurationClass().getMetadata(),
              deferredImport.getConfigurationClass());
    }

    public void processGroupImports() {
      for (DeferredImportSelectorGrouping grouping : groupings.values()) {
        Predicate<String> exclusionFilter = grouping.getCandidateFilter();
        for (Group.Entry entry : grouping.getImports()) {
          ConfigurationClass configurationClass = configurationClasses.get(entry.metadata());
          try {
            processImports(configurationClass, asSourceClass(configurationClass, exclusionFilter),
                    Collections.singleton(asSourceClass(entry.importClassName(), exclusionFilter)),
                    exclusionFilter, false);
          }
          catch (BeanDefinitionStoreException ex) {
            throw ex;
          }
          catch (Throwable ex) {
            throw new BeanDefinitionStoreException(
                    "Failed to process import candidates for configuration class [" +
                            configurationClass.getMetadata().getClassName() + "]", ex);
          }
        }
      }
    }

    private Group createGroup(@Nullable Class<? extends Group> type) {
      Class<? extends Group> effectiveType = type != null ? type : DefaultDeferredImportSelectorGroup.class;
      return ParserStrategyUtils.instantiateClass(effectiveType, Group.class, bootstrapContext);
    }
  }

  private static class DeferredImportSelectorHolder {

    private final ConfigurationClass configurationClass;
    private final DeferredImportSelector importSelector;

    public DeferredImportSelectorHolder(ConfigurationClass configClass, DeferredImportSelector selector) {
      this.configurationClass = configClass;
      this.importSelector = selector;
    }

    public ConfigurationClass getConfigurationClass() {
      return this.configurationClass;
    }

    public DeferredImportSelector getImportSelector() {
      return this.importSelector;
    }
  }

  private static class DeferredImportSelectorGrouping {

    private final DeferredImportSelector.Group group;
    private final List<DeferredImportSelectorHolder> deferredImports = new ArrayList<>();

    DeferredImportSelectorGrouping(Group group) {
      this.group = group;
    }

    public void add(DeferredImportSelectorHolder deferredImport) {
      this.deferredImports.add(deferredImport);
    }

    /**
     * Return the imports defined by the group.
     *
     * @return each import with its associated configuration class
     */
    public Iterable<Group.Entry> getImports() {
      for (DeferredImportSelectorHolder deferredImport : this.deferredImports) {
        this.group.process(deferredImport.getConfigurationClass().getMetadata(),
                deferredImport.getImportSelector());
      }
      return this.group.selectImports();
    }

    public Predicate<String> getCandidateFilter() {
      Predicate<String> mergedFilter = DEFAULT_EXCLUSION_FILTER;
      for (DeferredImportSelectorHolder deferredImport : this.deferredImports) {
        Predicate<String> selectorFilter = deferredImport.getImportSelector().getExclusionFilter();
        if (selectorFilter != null) {
          mergedFilter = mergedFilter.or(selectorFilter);
        }
      }
      return mergedFilter;
    }
  }

  private static class DefaultDeferredImportSelectorGroup implements Group {

    private final ArrayList<Entry> imports = new ArrayList<>();

    @Override
    public void process(AnnotationMetadata metadata, DeferredImportSelector selector) {
      for (String importClassName : selector.selectImports(metadata)) {
        imports.add(new Entry(metadata, importClassName));
      }
    }

    @Override
    public Iterable<Entry> selectImports() {
      return this.imports;
    }
  }

  /**
   * Simple wrapper that allows annotated source classes to be dealt with
   * in a uniform manner, regardless of how they are loaded.
   */
  private class SourceClass implements Ordered {

    private final Object source;  // Class or MetadataReader
    private final AnnotationMetadata metadata;

    public SourceClass(Object source) {
      this.source = source;
      if (source instanceof Class) {
        this.metadata = AnnotationMetadata.introspect((Class<?>) source);
      }
      else {
        this.metadata = ((MetadataReader) source).getAnnotationMetadata();
      }
    }

    public final AnnotationMetadata getMetadata() {
      return this.metadata;
    }

    @Override
    public int getOrder() {
      Integer order = ConfigurationClassUtils.getOrder(this.metadata);
      return (order != null ? order : Ordered.LOWEST_PRECEDENCE);
    }

    public Class<?> loadClass() throws ClassNotFoundException {
      if (this.source instanceof Class) {
        return (Class<?>) this.source;
      }
      String className = ((MetadataReader) this.source).getClassMetadata().getClassName();
      return ClassUtils.forName(className, bootstrapContext.getClassLoader());
    }

    public boolean isAssignable(Class<?> clazz) throws IOException {
      if (this.source instanceof Class) {
        return clazz.isAssignableFrom((Class<?>) this.source);
      }
      return new AssignableTypeFilter(clazz)
              .match((MetadataReader) this.source, bootstrapContext.getMetadataReaderFactory());
    }

    public ConfigurationClass asConfigClass(ConfigurationClass importedBy) {
      if (this.source instanceof Class) {
        return new ConfigurationClass((Class<?>) this.source, importedBy);
      }
      return new ConfigurationClass((MetadataReader) this.source, importedBy);
    }

    public Collection<SourceClass> getMemberClasses() throws IOException {
      Object sourceToProcess = this.source;
      if (sourceToProcess instanceof Class<?> sourceClass) {
        try {
          Class<?>[] declaredClasses = sourceClass.getDeclaredClasses();
          List<SourceClass> members = new ArrayList<>(declaredClasses.length);
          for (Class<?> declaredClass : declaredClasses) {
            members.add(asSourceClass(declaredClass, DEFAULT_EXCLUSION_FILTER));
          }
          return members;
        }
        catch (NoClassDefFoundError err) {
          // getDeclaredClasses() failed because of non-resolvable dependencies
          // -> fall back to ASM below
          sourceToProcess = bootstrapContext.getMetadataReader(sourceClass.getName());
        }
      }

      // ASM-based resolution - safe for non-resolvable classes as well
      MetadataReader sourceReader = (MetadataReader) sourceToProcess;
      String[] memberClassNames = sourceReader.getClassMetadata().getMemberClassNames();
      ArrayList<SourceClass> members = new ArrayList<>(memberClassNames.length);
      for (String memberClassName : memberClassNames) {
        try {
          members.add(asSourceClass(memberClassName, DEFAULT_EXCLUSION_FILTER));
        }
        catch (IOException ex) {
          // Let's skip it if it's not resolvable - we're just looking for candidates
          if (logger.isDebugEnabled()) {
            logger.debug("Failed to resolve member class [{}] - not considering it as a configuration class candidate",
                    memberClassName);
          }
        }
      }
      return members;
    }

    public SourceClass getSuperClass() throws IOException {
      if (this.source instanceof Class) {
        return asSourceClass(((Class<?>) this.source).getSuperclass(), DEFAULT_EXCLUSION_FILTER);
      }
      return asSourceClass(
              ((MetadataReader) this.source).getClassMetadata().getSuperClassName(), DEFAULT_EXCLUSION_FILTER);
    }

    public Set<SourceClass> getInterfaces() throws IOException {
      Set<SourceClass> result = new LinkedHashSet<>();
      if (this.source instanceof Class<?> sourceClass) {
        for (Class<?> ifcClass : sourceClass.getInterfaces()) {
          result.add(asSourceClass(ifcClass, DEFAULT_EXCLUSION_FILTER));
        }
      }
      else {
        for (String className : this.metadata.getInterfaceNames()) {
          result.add(asSourceClass(className, DEFAULT_EXCLUSION_FILTER));
        }
      }
      return result;
    }

    public Set<SourceClass> getAnnotationsAsSourceClass() {
      Set<SourceClass> result = new LinkedHashSet<>();
      if (this.source instanceof Class<?> sourceClass) {
        for (Annotation ann : sourceClass.getDeclaredAnnotations()) {
          Class<?> annType = ann.annotationType();
          if (!annType.getName().startsWith("java")) {
            try {
              result.add(asSourceClass(annType, DEFAULT_EXCLUSION_FILTER));
            }
            catch (Throwable ex) {
              // An annotation not present on the classpath is being ignored
              // by the JVM's class loading -> ignore here as well.
            }
          }
        }
      }
      else {
        for (String className : this.metadata.getAnnotationTypes()) {
          if (!className.startsWith("java")) {
            try {
              result.add(getRelated(className));
            }
            catch (Throwable ex) {
              // An annotation not present on the classpath is being ignored
              // by the JVM's class loading -> ignore here as well.
            }
          }
        }
      }
      return result;
    }

    public Collection<SourceClass> getAnnotationAttributes(String annType, String attribute) throws IOException {
      Map<String, Object> annotationAttributes = this.metadata.getAnnotationAttributes(annType, true);
      if (annotationAttributes == null || !annotationAttributes.containsKey(attribute)) {
        return Collections.emptySet();
      }
      String[] classNames = (String[]) annotationAttributes.get(attribute);
      Set<SourceClass> result = new LinkedHashSet<>();
      for (String className : classNames) {
        result.add(getRelated(className));
      }
      return result;
    }

    private SourceClass getRelated(String className) throws IOException {
      if (this.source instanceof Class) {
        try {
          Class<?> clazz = ClassUtils.forName(className, ((Class<?>) this.source).getClassLoader());
          return asSourceClass(clazz, DEFAULT_EXCLUSION_FILTER);
        }
        catch (ClassNotFoundException ex) {
          // Ignore -> fall back to ASM next, except for core java types.
          if (className.startsWith("java")) {
            throw new NestedIOException("Failed to load class [" + className + "]", ex);
          }
          return new SourceClass(bootstrapContext.getMetadataReader(className));
        }
      }
      return asSourceClass(className, DEFAULT_EXCLUSION_FILTER);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return this == other
              || (other instanceof SourceClass && this.metadata.getClassName().equals(((SourceClass) other).metadata.getClassName()));
    }

    @Override
    public int hashCode() {
      return this.metadata.getClassName().hashCode();
    }

    @Override
    public String toString() {
      return this.metadata.getClassName();
    }
  }

  /**
   * {@link Problem} registered upon detection of a circular {@link Import}.
   */
  private static class CircularImportProblem extends Problem {

    public CircularImportProblem(ConfigurationClass attemptedImport, Deque<ConfigurationClass> importStack) {
      super(String.format("A circular @Import has been detected: " +
                              "Illegal attempt by @Configuration class '%s' to import class '%s' as '%s' is " +
                              "already present in the current import stack %s", importStack.element().getSimpleName(),
                      attemptedImport.getSimpleName(), attemptedImport.getSimpleName(), importStack),
              new Location(importStack.element().getResource(), attemptedImport.getMetadata()));
    }
  }

}
