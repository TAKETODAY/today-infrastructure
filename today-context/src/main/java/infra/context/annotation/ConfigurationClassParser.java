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

package infra.context.annotation;

import java.io.IOException;
import java.lang.annotation.Annotation;
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
import java.util.function.Predicate;

import infra.beans.factory.BeanDefinitionStoreException;
import infra.beans.factory.BeanRegistrar;
import infra.beans.factory.annotation.AnnotatedBeanDefinition;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.parsing.Location;
import infra.beans.factory.parsing.Problem;
import infra.beans.factory.parsing.ProblemReporter;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.beans.factory.support.BeanDefinitionReader;
import infra.beans.factory.support.BeanNameGenerator;
import infra.context.ApplicationContextException;
import infra.context.BootstrapContext;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.ConfigurationCondition.ConfigurationPhase;
import infra.context.annotation.DeferredImportSelector.Group;
import infra.core.OrderComparator;
import infra.core.Ordered;
import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.core.annotation.AnnotationUtils;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.io.PropertySourceDescriptor;
import infra.core.type.AnnotationMetadata;
import infra.core.type.MethodMetadata;
import infra.core.type.StandardAnnotationMetadata;
import infra.core.type.classreading.MetadataReader;
import infra.core.type.filter.AbstractTypeHierarchyTraversingFilter;
import infra.core.type.filter.AssignableTypeFilter;
import infra.core.type.filter.TypeFilter;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.stereotype.Component;
import infra.util.ClassUtils;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.util.StringUtils;

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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConfigurationClassBeanDefinitionReader
 * @since 4.0
 */
class ConfigurationClassParser {

  private static final Predicate<String> DEFAULT_EXCLUSION_FILTER = className ->
          className.startsWith("java.lang.annotation.")
                  || className.startsWith("infra.lang.");

  private static final Predicate<Condition> REGISTER_BEAN_CONDITION_FILTER = condition ->
          condition instanceof ConfigurationCondition configCondition
                  && ConfigurationPhase.REGISTER_BEAN.equals(configCondition.getConfigurationPhase());

  private static final Comparator<DeferredImportSelectorHolder> DEFERRED_IMPORT_COMPARATOR =
          (o1, o2) -> AnnotationAwareOrderComparator.INSTANCE.compare(o1.importSelector, o2.importSelector);

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final LinkedHashMap<ConfigurationClass, ConfigurationClass> configurationClasses = new LinkedHashMap<>();

  private final MultiValueMap<String, ConfigurationClass> knownSuperclasses = new LinkedMultiValueMap<>();

  private final DeferredImportSelectorHandler deferredImportSelectorHandler = new DeferredImportSelectorHandler();

  private final SourceClass objectSourceClass = new SourceClass(Object.class);

  private final BootstrapContext bootstrapContext;

  @Nullable
  private final PropertySourceRegistry propertySourceRegistry;

  public final ImportRegistry importRegistry = new ImportStack();

  /**
   * Create a new {@link ConfigurationClassParser} instance that will be used
   * to populate the set of configuration classes.
   */
  public ConfigurationClassParser(BootstrapContext bootstrapContext) {
    this.bootstrapContext = bootstrapContext;
    this.propertySourceRegistry = bootstrapContext.getEnvironment() instanceof ConfigurableEnvironment ce ?
            new PropertySourceRegistry(ce, bootstrapContext) : null;
  }

  public void parse(Set<BeanDefinitionHolder> configCandidates) {
    for (BeanDefinitionHolder holder : configCandidates) {
      BeanDefinition bd = holder.getBeanDefinition();
      try {
        ConfigurationClass configClass;
        if (bd instanceof AnnotatedBeanDefinition annotatedBeanDef) {
          configClass = parse(annotatedBeanDef, holder.getBeanName());
        }
        else if (bd instanceof AbstractBeanDefinition abstractBeanDef && abstractBeanDef.hasBeanClass()) {
          configClass = parse(abstractBeanDef.getBeanClass(), holder.getBeanName());
        }
        else {
          configClass = parse(bd.getBeanClassName(), holder.getBeanName());
        }

        // Downgrade to lite (no enhancement) in case of no instance-level @Component methods.
        if (!configClass.hasNonStaticComponentMethods()
                && ConfigurationClassUtils.CONFIGURATION_CLASS_FULL.equals(
                bd.getAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE))) {
          bd.setAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE,
                  ConfigurationClassUtils.CONFIGURATION_CLASS_LITE);
        }
      }
      catch (BeanDefinitionStoreException ex) {
        throw ex;
      }
      catch (Throwable ex) {
        throw new BeanDefinitionStoreException(
                "Failed to parse configuration class [%s]".formatted(bd.getBeanClassName()), ex);
      }
    }

    deferredImportSelectorHandler.process();
  }

  private ConfigurationClass parse(AnnotatedBeanDefinition beanDef, String beanName) {
    ConfigurationClass configClass = new ConfigurationClass(
            beanDef.getMetadata(), beanName, (beanDef instanceof ScannedGenericBeanDefinition));
    processConfigurationClass(configClass, DEFAULT_EXCLUSION_FILTER);
    return configClass;
  }

  private ConfigurationClass parse(Class<?> clazz, String beanName) {
    ConfigurationClass configClass = new ConfigurationClass(clazz, beanName);
    processConfigurationClass(configClass, DEFAULT_EXCLUSION_FILTER);
    return configClass;
  }

  final ConfigurationClass parse(@Nullable String className, String beanName) throws IOException {
    Assert.notNull(className, "No bean class name for configuration class bean definition");
    MetadataReader reader = bootstrapContext.getMetadataReader(className);
    ConfigurationClass configClass = new ConfigurationClass(reader, beanName);
    processConfigurationClass(configClass, DEFAULT_EXCLUSION_FILTER);
    return configClass;
  }

  /**
   * Validate each {@link ConfigurationClass} object.
   *
   * @see ConfigurationClass#validate
   */
  public void validate() {
    ProblemReporter problemReporter = bootstrapContext.getProblemReporter();
    for (ConfigurationClass configClass : configurationClasses.keySet()) {
      configClass.validate(problemReporter);
    }
  }

  public Set<ConfigurationClass> getConfigurationClasses() {
    return configurationClasses.keySet();
  }

  List<PropertySourceDescriptor> getPropertySourceDescriptors() {
    return propertySourceRegistry != null ? propertySourceRegistry.getDescriptors()
            : Collections.emptyList();
  }

  protected void processConfigurationClass(ConfigurationClass configClass, Predicate<String> filter) {
    if (bootstrapContext.passCondition(configClass.metadata, ConfigurationPhase.PARSE_CONFIGURATION)) {
      ConfigurationClass existingClass = configurationClasses.get(configClass);
      if (existingClass != null) {
        if (configClass.isImported()) {
          if (existingClass.isImported()) {
            existingClass.mergeImportedBy(configClass);
          }
          // Otherwise ignore new imported config class; existing non-imported class overrides it.
          return;
        }
        else if (configClass.scanned) {
          String beanName = configClass.beanName;
          if (StringUtils.isNotEmpty(beanName) && bootstrapContext.containsBeanDefinition(beanName)) {
            bootstrapContext.removeBeanDefinition(beanName);
          }
          // An implicitly scanned bean definition should not override an explicit import.
          return;
        }
        else {
          // Explicit bean definition found, probably replacing an import.
          // Let's remove the old one and go with the new one.
          configurationClasses.remove(configClass);
          removeKnownSuperclass(configClass.metadata.getClassName(), false);
        }
      }

      // Recursively process the configuration class and its superclass hierarchy.
      SourceClass sourceClass = null;
      try {
        sourceClass = asSourceClass(configClass, filter);
        do {
          sourceClass = doProcessConfigurationClass(configClass, sourceClass, filter);
        }
        while (sourceClass != null);
      }
      catch (IOException ex) {
        throw new BeanDefinitionStoreException(
                "I/O failure while processing configuration class [%s]".formatted(sourceClass), ex);
      }

      configurationClasses.put(configClass, configClass);
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

    if (configClass.metadata.isAnnotated(Component.class)) {
      // Recursively process any member (nested) classes first
      processMemberClasses(configClass, sourceClass, filter);
    }

    // Process any @PropertySource annotations
    Environment environment = bootstrapContext.getEnvironment();
    MergedAnnotations annotations = sourceClass.metadata.getAnnotations();
    for (var propertySource : sourceClass.metadata.getMergedRepeatableAnnotation(
            PropertySource.class, PropertySources.class, true)) {
      if (this.propertySourceRegistry != null) {
        this.propertySourceRegistry.processPropertySource(propertySource);
      }
      else {
        logger.info("Ignoring @PropertySource annotation on [{}]. Reason: Environment must implement ConfigurableEnvironment",
                sourceClass.metadata.getClassName());
      }
    }

    // Process any @ComponentScan annotations

    var componentScans = sourceClass.metadata.getMergedRepeatableAnnotation(ComponentScan.class, ComponentScans.class,
            false, MergedAnnotation::isDirectlyPresent);

    // Fall back to searching for @ComponentScan meta-annotations (which indirectly
    // includes locally declared composed annotations).
    if (componentScans.isEmpty()) {
      componentScans = sourceClass.metadata.getMergedRepeatableAnnotation(
              ComponentScan.class, ComponentScans.class, false, MergedAnnotation::isMetaPresent);
    }

    if (!componentScans.isEmpty()) {
      List<Condition> registerBeanConditions = collectRegisterBeanConditions(configClass);
      if (!registerBeanConditions.isEmpty()) {
        throw new ApplicationContextException(
                "Component scan for configuration class [%s] could not be used with conditions in REGISTER_BEAN phase: %s"
                        .formatted(configClass.metadata.getClassName(), registerBeanConditions));
      }
      for (MergedAnnotation<ComponentScan> componentScan : componentScans) {
        // The config class is annotated with @ComponentScan -> perform the scan immediately
        Set<BeanDefinitionHolder> scannedBeanDefinitions = parseComponentScan(componentScan, sourceClass.metadata.getClassName());
        // Check the set of scanned definitions for any further config classes and parse recursively if needed
        for (BeanDefinitionHolder holder : scannedBeanDefinitions) {
          BeanDefinition bdCand = holder.getBeanDefinition().getOriginatingBeanDefinition();
          if (bdCand == null) {
            bdCand = holder.getBeanDefinition();
          }
          if (ConfigurationClassUtils.checkConfigurationClassCandidate(bdCand, bootstrapContext)) {
            parse(bdCand.getBeanClassName(), holder.getBeanName());
          }
        }
      }
    }

    // Process any @Import annotations
    processImports(configClass, sourceClass, getImports(sourceClass), filter, true);

    // Process any @ImportResource annotations
    MergedAnnotation<ImportResource> importResource = annotations.get(ImportResource.class);
    if (importResource.isPresent()) {
      String[] resources = importResource.getStringArray("locations");
      Class<? extends BeanDefinitionReader> readerClass = importResource.getClass("reader");
      for (String resource : resources) {
        String resolvedResource = environment.resolveRequiredPlaceholders(resource);
        configClass.addImportedResource(resolvedResource, readerClass);
      }
    }

    // Process individual @Component methods
    Set<MethodMetadata> beanMethods = retrieveComponentMethodMetadata(sourceClass);
    for (MethodMetadata methodMetadata : beanMethods) {
      configClass.addMethod(new ComponentMethod(methodMetadata, configClass));
    }

    // Process default methods on interfaces
    processInterfaces(configClass, sourceClass);

    // Process superclass, if any
    if (sourceClass.metadata.hasSuperClass()) {
      String superclass = sourceClass.metadata.getSuperClassName();
      if (superclass != null && !superclass.startsWith("java")) {
        boolean superclassKnown = this.knownSuperclasses.containsKey(superclass);
        this.knownSuperclasses.add(superclass, configClass);
        if (!superclassKnown) {
          // Superclass found, return its annotation metadata and recurse
          return sourceClass.getSuperClass();
        }
      }
    }

    // No superclass -> processing is complete
    return null;
  }

  /**
   * Register member (nested) classes that happen to be configuration classes themselves.
   */
  private void processMemberClasses(ConfigurationClass configClass, SourceClass sourceClass, Predicate<String> filter)
          throws IOException //
  {
    Collection<SourceClass> memberClasses = sourceClass.getMemberClasses();
    if (!memberClasses.isEmpty()) {
      ArrayList<SourceClass> candidates = new ArrayList<>(memberClasses.size());
      for (SourceClass memberClass : memberClasses) {
        if (ConfigurationClassUtils.isConfigurationCandidate(memberClass.metadata)
                && !memberClass.metadata.getClassName().equals(configClass.metadata.getClassName())) {
          candidates.add(memberClass);
        }
      }
      OrderComparator.sort(candidates);
      for (SourceClass candidate : candidates) {
        if (importRegistry.contains(configClass)) {
          bootstrapContext.reportError(new CircularImportProblem(configClass, importRegistry));
        }
        else {
          importRegistry.push(configClass);
          try {
            processConfigurationClass(candidate.asConfigClass(configClass), filter);
          }
          finally {
            importRegistry.pop();
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
    AnnotationMetadata original = sourceClass.metadata;
    Set<MethodMetadata> componentMethods = original.getAnnotatedMethods(Component.class.getName());
    if (componentMethods.size() > 1 && original instanceof StandardAnnotationMetadata) {
      // Try reading the class file via ASM for deterministic declaration order...
      // Unfortunately, the JVM's standard reflection returns methods in arbitrary
      // order, even between different runs of the same application on the same JVM.
      try {
        AnnotationMetadata asm = bootstrapContext.getAnnotationMetadata(original.getClassName());
        Set<MethodMetadata> asmMethods = asm.getAnnotatedMethods(Component.class.getName());
        if (asmMethods.size() >= componentMethods.size()) {
          LinkedHashSet<MethodMetadata> candidateMethods = new LinkedHashSet<>(componentMethods);
          LinkedHashSet<MethodMetadata> selectedMethods = new LinkedHashSet<>(asmMethods.size());
          for (MethodMetadata asmMethod : asmMethods) {
            for (Iterator<MethodMetadata> it = candidateMethods.iterator(); it.hasNext(); ) {
              MethodMetadata beanMethod = it.next();
              if (beanMethod.getMethodName().equals(asmMethod.getMethodName())) {
                selectedMethods.add(beanMethod);
                it.remove();
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
   * Remove known superclasses for the given removed class, potentially replacing
   * the superclass exposure on a different config class with the same superclass.
   */
  private void removeKnownSuperclass(String removedClass, boolean replace) {
    String replacedSuperclass = null;
    ConfigurationClass replacingClass = null;

    var it = this.knownSuperclasses.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, List<ConfigurationClass>> entry = it.next();
      if (entry.getValue().removeIf(configClass -> configClass.metadata.getClassName().equals(removedClass))) {
        if (entry.getValue().isEmpty()) {
          it.remove();
        }
        else if (replace && replacingClass == null) {
          replacedSuperclass = entry.getKey();
          replacingClass = entry.getValue().get(0);
        }
      }
    }

    if (replacingClass != null) {
      try {
        SourceClass sourceClass = asSourceClass(replacingClass, DEFAULT_EXCLUSION_FILTER).getSuperClass();
        while (!sourceClass.metadata.getClassName().equals(replacedSuperclass)
                && sourceClass.metadata.getSuperClassName() != null) {
          sourceClass = sourceClass.getSuperClass();
        }
        do {
          sourceClass = doProcessConfigurationClass(replacingClass, sourceClass, DEFAULT_EXCLUSION_FILTER);
        }
        while (sourceClass != null);
      }
      catch (IOException ex) {
        throw new BeanDefinitionStoreException(
                "I/O failure while removing configuration class [%s]".formatted(removedClass), ex);
      }
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
   * <p>As of 5.0, {@code @Import} annotations declared on interfaces
   * implemented by the configuration class are also considered. This allows imports to
   * be triggered indirectly via marker interfaces or shared base interfaces.
   *
   * @param sourceClass the class to search
   * @param imports the imports collected so far
   * @param visited used to track visited classes and interfaces to prevent infinite
   * recursion
   * @throws IOException if there is any problem reading metadata from the named class
   */
  private void collectImports(SourceClass sourceClass, Set<SourceClass> imports, Set<SourceClass> visited) throws IOException {
    if (visited.add(sourceClass)) {
      for (SourceClass ifc : sourceClass.getInterfaces()) {
        collectImports(ifc, imports, visited);
      }
      for (SourceClass annotation : sourceClass.getAnnotations()) {
        String annName = annotation.metadata.getClassName();
        if (!annName.equals(Import.class.getName())) {
          collectImports(annotation, imports, visited);
        }
      }
      imports.addAll(sourceClass.getAnnotationAttributes(Import.class.getName(), "value"));
    }
  }

  private void processImports(ConfigurationClass configClass, SourceClass currentSourceClass,
          Collection<SourceClass> importCandidates, Predicate<String> exclusionFilter, boolean checkForCircularImports) {
    if (importCandidates.isEmpty()) {
      return;
    }

    if (checkForCircularImports && isChainedImportOnStack(configClass)) {
      bootstrapContext.reportError(new CircularImportProblem(configClass, importRegistry));
    }
    else {
      importRegistry.push(configClass);
      try {
        for (SourceClass candidate : importCandidates) {
          if (candidate.isAssignable(ImportSelector.class)) {
            // Candidate class is an ImportSelector -> delegate to it to determine imports
            Class<?> candidateClass = candidate.loadClass();
            var selector = bootstrapContext.instantiate(candidateClass, ImportSelector.class);
            Predicate<String> selectorFilter = selector.getExclusionFilter();
            if (selectorFilter != null) {
              exclusionFilter = exclusionFilter.or(selectorFilter);
            }
            if (selector instanceof DeferredImportSelector deferredSelector) {
              deferredImportSelectorHandler.handle(configClass, deferredSelector);
            }
            else {
              String[] importClassNames = selector.selectImports(currentSourceClass.metadata);
              Collection<SourceClass> importSourceClasses = asSourceClasses(importClassNames, exclusionFilter);
              processImports(configClass, currentSourceClass, importSourceClasses, exclusionFilter, false);
            }
          }
          else if (candidate.isAssignable(BeanRegistrar.class)) {
            Class<?> candidateClass = candidate.loadClass();
            BeanRegistrar registrar = bootstrapContext.instantiate(candidateClass, BeanRegistrar.class);

            AnnotationMetadata metadata = currentSourceClass.metadata;
            if (registrar instanceof ImportAware importAware) {
              importAware.setImportMetadata(metadata);
            }

            configClass.addBeanRegistrar(metadata.getClassName(), registrar);
          }
          else if (candidate.isAssignable(ImportBeanDefinitionRegistrar.class)) {
            // Candidate class is an ImportBeanDefinitionRegistrar ->
            // delegate to it to register additional bean definitions
            Class<?> candidateClass = candidate.loadClass();
            var registrar = bootstrapContext.instantiate(candidateClass, ImportBeanDefinitionRegistrar.class);
            configClass.addImportBeanDefinitionRegistrar(registrar, currentSourceClass.metadata);
          }
          else {
            // Candidate class not an ImportSelector or ImportBeanDefinitionRegistrar ->
            // process it as an @Configuration class
            importRegistry.registerImport(
                    currentSourceClass.metadata, candidate.metadata.getClassName());
            processConfigurationClass(candidate.asConfigClass(configClass), exclusionFilter);
          }
        }
      }
      catch (BeanDefinitionStoreException ex) {
        throw ex;
      }
      catch (Throwable ex) {
        throw new BeanDefinitionStoreException("Failed to process import candidates for configuration class [%s]: %s"
                .formatted(configClass.metadata.getClassName(), ex.getMessage()), ex);
      }
      finally {
        importRegistry.pop();
      }
    }
  }

  private boolean isChainedImportOnStack(ConfigurationClass configClass) {
    if (importRegistry.contains(configClass)) {
      String configClassName = configClass.metadata.getClassName();
      AnnotationMetadata importingClass = importRegistry.getImportingClassFor(configClassName);
      while (importingClass != null) {
        if (configClassName.equals(importingClass.getClassName())) {
          return true;
        }
        importingClass = importRegistry.getImportingClassFor(importingClass.getClassName());
      }
    }
    return false;
  }

  /**
   * Factory method to obtain a {@link SourceClass} from a {@link ConfigurationClass}.
   */
  private SourceClass asSourceClass(ConfigurationClass configurationClass, Predicate<String> filter) throws IOException {
    AnnotationMetadata metadata = configurationClass.metadata;
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
      return objectSourceClass;
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
      SourceClass sourceClass = asSourceClass(className, filter);
      if (this.objectSourceClass != sourceClass) {
        annotatedClasses.add(sourceClass);
      }
    }
    return annotatedClasses;
  }

  /**
   * Factory method to obtain a {@link SourceClass} from a class name.
   */
  SourceClass asSourceClass(@Nullable String className, Predicate<String> filter) throws IOException {
    if (className == null || filter.test(className)) {
      return objectSourceClass;
    }
    if (className.startsWith("java")) {
      // Never use ASM for core java types
      try {
        return new SourceClass(ClassUtils.forName(className, bootstrapContext.getClassLoader()));
      }
      catch (ClassNotFoundException ex) {
        throw new IOException("Failed to load class [%s]".formatted(className), ex);
      }
    }
    return new SourceClass(bootstrapContext.getMetadataReader(className));
  }

  private List<Condition> collectRegisterBeanConditions(ConfigurationClass configurationClass) {
    AnnotationMetadata metadata = configurationClass.metadata;
    ConditionEvaluator conditionEvaluator = bootstrapContext.getConditionEvaluator();
    ArrayList<Condition> allConditions = new ArrayList<>(conditionEvaluator.collectConditions(metadata));
    ConfigurationClass enclosingConfigurationClass = getEnclosingConfigurationClass(configurationClass);
    if (enclosingConfigurationClass != null) {
      allConditions.addAll(conditionEvaluator.collectConditions(enclosingConfigurationClass.metadata));
    }
    return allConditions.stream().filter(REGISTER_BEAN_CONDITION_FILTER).toList();
  }

  @Nullable
  private ConfigurationClass getEnclosingConfigurationClass(ConfigurationClass configurationClass) {
    String enclosingClassName = configurationClass.metadata.getEnclosingClassName();
    if (enclosingClassName != null) {
      return configurationClass.importedBy.stream()
              .filter(candidate -> enclosingClassName.equals(candidate.metadata.getClassName()))
              .findFirst()
              .orElse(null);
    }
    return null;
  }

  /**
   * Parse for the @{@link ComponentScan} annotation.
   *
   * @see ClassPathBeanDefinitionScanner#scan(String...)
   * @since 5.0
   */
  Set<BeanDefinitionHolder> parseComponentScan(MergedAnnotation<ComponentScan> componentScan, String declaringClass) {
    var scanner = new ClassPathBeanDefinitionScanner(
            bootstrapContext.getRegistry(), componentScan.getBoolean("useDefaultFilters"),
            bootstrapContext.getEnvironment(), bootstrapContext.getResourceLoader());

    var generatorClass = componentScan.<BeanNameGenerator>getClass("nameGenerator");
    scanner.setBeanNameGenerator(BeanNameGenerator.class == generatorClass
            ? bootstrapContext.getBeanNameGenerator()
            : bootstrapContext.instantiate(generatorClass));

    var scopedProxyMode = componentScan.getEnum("scopedProxy", ScopedProxyMode.class);
    if (scopedProxyMode != ScopedProxyMode.DEFAULT) {
      scanner.setScopedProxyMode(scopedProxyMode);
    }
    else {
      var resolverClass = componentScan.<ScopeMetadataResolver>getClass("scopeResolver");
      if (resolverClass != ScopeMetadataResolver.class) {
        scanner.setScopeMetadataResolver(bootstrapContext.instantiate(resolverClass));
      }
    }

    scanner.setResourcePattern(componentScan.getString("resourcePattern"));

    for (var includeFilter : componentScan.getAnnotationArray("includeFilters", ComponentScan.Filter.class)) {
      List<TypeFilter> typeFilters = TypeFilterUtils.createTypeFiltersFor(includeFilter, bootstrapContext);
      for (TypeFilter typeFilter : typeFilters) {
        scanner.addIncludeFilter(typeFilter);
      }
    }
    for (var excludeFilter : componentScan.getAnnotationArray("excludeFilters", ComponentScan.Filter.class)) {
      List<TypeFilter> typeFilters = TypeFilterUtils.createTypeFiltersFor(excludeFilter, bootstrapContext);
      for (TypeFilter typeFilter : typeFilters) {
        scanner.addExcludeFilter(typeFilter);
      }
    }

    boolean lazyInit = componentScan.getBoolean("lazyInit");
    if (lazyInit) {
      scanner.getBeanDefinitionDefaults().setLazyInit(true);
    }

    LinkedHashSet<String> basePackages = new LinkedHashSet<>();
    String[] basePackagesArray = componentScan.getStringArray("basePackages");
    for (String pkg : basePackagesArray) {
      String[] tokenized = StringUtils.tokenizeToStringArray(
              bootstrapContext.evaluateExpression(pkg), ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
      Collections.addAll(basePackages, tokenized);
    }
    for (Class<?> clazz : componentScan.getClassArray("basePackageClasses")) {
      basePackages.add(ClassUtils.getPackageName(clazz));
    }
    if (basePackages.isEmpty()) {
      basePackages.add(ClassUtils.getPackageName(declaringClass));
    }

    scanner.addExcludeFilter(new AbstractTypeHierarchyTraversingFilter(false, false) {

      @Override
      protected boolean matchClassName(String className) {
        return declaringClass.equals(className);
      }
    });

    scanner.setMetadataReaderFactory(bootstrapContext.getMetadataReaderFactory());
    return scanner.collectHolders(StringUtils.toStringArray(basePackages));
  }

  private final class DeferredImportSelectorHandler {

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
      if (deferredImportSelectors == null) {
        DeferredImportSelectorGroupingHandler handler = new DeferredImportSelectorGroupingHandler();
        handler.register(holder);
        handler.processGroupImports();
      }
      else {
        deferredImportSelectors.add(holder);
      }
    }

    public void process() {
      List<DeferredImportSelectorHolder> deferredImports = deferredImportSelectors;
      this.deferredImportSelectors = null;
      try {
        if (deferredImports != null) {
          var handler = new DeferredImportSelectorGroupingHandler();
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

  private final class DeferredImportSelectorGroupingHandler {

    private final HashMap<AnnotationMetadata, ConfigurationClass> configurationClasses = new HashMap<>();

    private final LinkedHashMap<Object, DeferredImportSelectorGrouping> groupings = new LinkedHashMap<>();

    public void register(DeferredImportSelectorHolder deferredImport) {
      var group = deferredImport.importSelector.getImportGroup();
      var grouping = groupings.computeIfAbsent(
              group != null ? group : deferredImport,
              key -> new DeferredImportSelectorGrouping(createGroup(group))
      );
      grouping.add(deferredImport);
      configurationClasses.put(deferredImport.configurationClass.metadata,
              deferredImport.configurationClass);
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
            throw new BeanDefinitionStoreException("Failed to process import candidates for configuration class [%s]"
                    .formatted(configurationClass.metadata.getClassName()), ex);
          }
        }
      }
    }

    private Group createGroup(@Nullable Class<? extends Group> type) {
      Class<? extends Group> effectiveType = type != null ? type : DefaultDeferredImportSelectorGroup.class;
      return bootstrapContext.instantiate(effectiveType, Group.class);
    }
  }

  private static final class DeferredImportSelectorHolder {

    public final ConfigurationClass configurationClass;

    public final DeferredImportSelector importSelector;

    public DeferredImportSelectorHolder(ConfigurationClass configClass, DeferredImportSelector selector) {
      this.configurationClass = configClass;
      this.importSelector = selector;
    }
  }

  private static final class DeferredImportSelectorGrouping {

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
      for (DeferredImportSelectorHolder deferredImport : deferredImports) {
        group.process(deferredImport.configurationClass.metadata, deferredImport.importSelector);
      }
      return group.selectImports();
    }

    public Predicate<String> getCandidateFilter() {
      Predicate<String> mergedFilter = DEFAULT_EXCLUSION_FILTER;
      for (DeferredImportSelectorHolder deferredImport : deferredImports) {
        Predicate<String> selectorFilter = deferredImport.importSelector.getExclusionFilter();
        if (selectorFilter != null) {
          mergedFilter = mergedFilter.or(selectorFilter);
        }
      }
      return mergedFilter;
    }
  }

  private static final class DefaultDeferredImportSelectorGroup implements Group {

    private final ArrayList<Entry> imports = new ArrayList<>();

    @Override
    public void process(AnnotationMetadata metadata, DeferredImportSelector selector) {
      for (String importClassName : selector.selectImports(metadata)) {
        imports.add(new Entry(metadata, importClassName));
      }
    }

    @Override
    public Iterable<Entry> selectImports() {
      return imports;
    }
  }

  /**
   * Simple wrapper that allows annotated source classes to be dealt with
   * in a uniform manner, regardless of how they are loaded.
   */
  private final class SourceClass implements Ordered {

    public final Object source;  // Class or MetadataReader

    public final AnnotationMetadata metadata;

    public SourceClass(Object source) {
      this.source = source;
      if (source instanceof Class) {
        this.metadata = AnnotationMetadata.introspect((Class<?>) source);
      }
      else {
        this.metadata = ((MetadataReader) source).getAnnotationMetadata();
      }
    }

    @Override
    public int getOrder() {
      Integer order = ConfigurationClassUtils.getOrder(metadata);
      return (order != null ? order : Ordered.LOWEST_PRECEDENCE);
    }

    public Class<?> loadClass() throws ClassNotFoundException {
      if (source instanceof Class) {
        return (Class<?>) source;
      }
      String className = ((MetadataReader) source).getClassMetadata().getClassName();
      return ClassUtils.forName(className, bootstrapContext.getClassLoader());
    }

    public boolean isAssignable(Class<?> clazz) throws IOException {
      if (source instanceof Class) {
        return clazz.isAssignableFrom((Class<?>) source);
      }
      return new AssignableTypeFilter(clazz)
              .match((MetadataReader) source, bootstrapContext.getMetadataReaderFactory());
    }

    public ConfigurationClass asConfigClass(ConfigurationClass importedBy) {
      if (source instanceof Class) {
        return new ConfigurationClass((Class<?>) source, importedBy);
      }
      return new ConfigurationClass((MetadataReader) source, importedBy);
    }

    public Collection<SourceClass> getMemberClasses() throws IOException {
      Object sourceToProcess = this.source;
      if (sourceToProcess instanceof Class<?> sourceClass) {
        try {
          Class<?>[] declaredClasses = sourceClass.getDeclaredClasses();
          ArrayList<SourceClass> members = new ArrayList<>(declaredClasses.length);
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
          logger.debug("Failed to resolve member class [{}] - not considering it as a configuration class candidate",
                  memberClassName);
        }
      }
      return members;
    }

    public SourceClass getSuperClass() throws IOException {
      if (source instanceof Class) {
        return asSourceClass(((Class<?>) source).getSuperclass(), DEFAULT_EXCLUSION_FILTER);
      }
      return asSourceClass(
              ((MetadataReader) source).getClassMetadata().getSuperClassName(), DEFAULT_EXCLUSION_FILTER);
    }

    public LinkedHashSet<SourceClass> getInterfaces() throws IOException {
      LinkedHashSet<SourceClass> result = new LinkedHashSet<>();
      if (source instanceof Class<?> sourceClass) {
        for (Class<?> ifcClass : sourceClass.getInterfaces()) {
          result.add(asSourceClass(ifcClass, DEFAULT_EXCLUSION_FILTER));
        }
      }
      else {
        for (String className : metadata.getInterfaceNames()) {
          result.add(asSourceClass(className, DEFAULT_EXCLUSION_FILTER));
        }
      }
      return result;
    }

    public Set<SourceClass> getAnnotations() {
      LinkedHashSet<SourceClass> result = new LinkedHashSet<>();
      if (source instanceof Class<?> sourceClass) {
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
        for (String className : metadata.getAnnotationTypes()) {
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
      MergedAnnotation<Annotation> annotation = metadata.getAnnotation(annType);
      if (annotation.isPresent()) {
        String[] classNames = annotation.getValue(attribute, String[].class);
        if (classNames != null) {
          LinkedHashSet<SourceClass> result = new LinkedHashSet<>();
          for (String className : classNames) {
            result.add(getRelated(className));
          }
          return result;
        }
      }
      return Collections.emptySet();
    }

    private SourceClass getRelated(String className) throws IOException {
      if (source instanceof Class) {
        try {
          Class<?> clazz = ClassUtils.forName(className, ((Class<?>) source).getClassLoader());
          return asSourceClass(clazz, DEFAULT_EXCLUSION_FILTER);
        }
        catch (ClassNotFoundException ex) {
          // Ignore -> fall back to ASM next, except for core java types.
          if (className.startsWith("java")) {
            throw new IOException("Failed to load class [%s]".formatted(className), ex);
          }
          return new SourceClass(bootstrapContext.getMetadataReader(className));
        }
      }
      return asSourceClass(className, DEFAULT_EXCLUSION_FILTER);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return this == other
              || (other instanceof SourceClass && metadata.getClassName().equals(((SourceClass) other).metadata.getClassName()));
    }

    @Override
    public int hashCode() {
      return metadata.getClassName().hashCode();
    }

    @Override
    public String toString() {
      return metadata.getClassName();
    }
  }

  /**
   * {@link Problem} registered upon detection of a circular {@link Import}.
   */
  private static final class CircularImportProblem extends Problem {

    public CircularImportProblem(ConfigurationClass attemptedImport, Deque<ConfigurationClass> importStack) {
      super(String.format("A circular @Import has been detected: " +
                              "Illegal attempt by @Configuration class '%s' to import class '%s' as '%s' is " +
                              "already present in the current import stack %s", importStack.element().getSimpleName(),
                      attemptedImport.getSimpleName(), attemptedImport.getSimpleName(), importStack),
              new Location(importStack.element().resource, attemptedImport.metadata));
    }
  }

  final class ImportStack extends ImportRegistry {

    @Override
    public void removeImportingClass(String importingClass) {
      super.removeImportingClass(importingClass);
      removeKnownSuperclass(importingClass, true);
    }

  }

}
