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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.annotation.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.annotation.DisableDependencyInjection;
import cn.taketoday.beans.factory.annotation.EnableDependencyInjection;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinitionHolder;
import cn.taketoday.beans.factory.support.AbstractBeanDefinitionReader;
import cn.taketoday.beans.factory.support.BeanDefinitionReader;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.BeanNameGenerator;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.context.annotation.ConfigurationCondition.ConfigurationPhase;
import cn.taketoday.context.loader.BootstrapContext;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.core.type.StandardAnnotationMetadata;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.NonNull;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Reads a given fully-populated set of ConfigurationClass instances, registering bean
 * definitions with the given {@link BeanDefinitionRegistry} based on its contents.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @see ConfigurationClassParser
 * @since 4.0
 */
class ConfigurationClassBeanDefinitionReader {
  private static final Logger logger = LoggerFactory.getLogger(ConfigurationClassBeanDefinitionReader.class);

  private final ImportRegistry importRegistry;
  private final BeanNameGenerator importBeanNameGenerator;
  private final BootstrapContext bootstrapContext;

  /**
   * Create a new {@link ConfigurationClassBeanDefinitionReader} instance
   * that will be used to populate the given {@link BeanDefinitionRegistry}.
   */
  ConfigurationClassBeanDefinitionReader(
          BootstrapContext bootstrapContext,
          BeanNameGenerator beanNameGenerator, ImportRegistry importRegistry) {

    this.bootstrapContext = bootstrapContext;
    this.importRegistry = importRegistry;
    this.importBeanNameGenerator = beanNameGenerator;
  }

  /**
   * Read {@code configurationModel}, registering bean definitions
   * with the registry based on its contents.
   */
  public void loadBeanDefinitions(Set<ConfigurationClass> configurationModel) {
    TrackedConditionEvaluator trackedConditionEvaluator = new TrackedConditionEvaluator();
    for (ConfigurationClass configClass : configurationModel) {
      loadBeanDefinitionsForConfigurationClass(configClass, trackedConditionEvaluator);
    }
  }

  /**
   * Read a particular {@link ConfigurationClass}, registering bean definitions
   * for the class itself and all of its {@link Component} methods.
   */
  private void loadBeanDefinitionsForConfigurationClass(
          ConfigurationClass configClass, TrackedConditionEvaluator trackedConditionEvaluator) {

    if (trackedConditionEvaluator.shouldSkip(configClass)) {
      String beanName = configClass.getBeanName();
      // TODO annotated with both @Component and @ConditionalOnMissingBean，condition matching error
      if (StringUtils.isNotEmpty(beanName) && bootstrapContext.containsBeanDefinition(beanName)) {
        bootstrapContext.removeBeanDefinition(beanName);
      }
      this.importRegistry.removeImportingClass(configClass.getMetadata().getClassName());
      return;
    }

    if (configClass.isImported()) {
      registerBeanDefinitionForImportedConfigurationClass(configClass);
    }
    for (ComponentMethod componentMethod : configClass.getMethods()) {
      loadBeanDefinitionsForComponentMethod(componentMethod);
    }

    loadBeanDefinitionsFromImportedResources(configClass.getImportedResources());
    loadBeanDefinitionsFromRegistrars(configClass.getImportBeanDefinitionRegistrars());
  }

  /**
   * Register the {@link Configuration} class itself as a bean definition.
   */
  private void registerBeanDefinitionForImportedConfigurationClass(ConfigurationClass configClass) {
    AnnotationMetadata metadata = configClass.getMetadata();
    AnnotatedGenericBeanDefinition configBeanDef = new AnnotatedGenericBeanDefinition(metadata);

    String configBeanName = importBeanNameGenerator.generateBeanName(configBeanDef, bootstrapContext.getRegistry());

    AnnotationConfigUtils.processCommonDefinitionAnnotations(configBeanDef);

    bootstrapContext.registerBeanDefinition(configBeanName, configBeanDef);
    configClass.setBeanName(configBeanName);

    if (logger.isTraceEnabled()) {
      logger.trace("Registered bean definition for imported class '{}'", configBeanName);
    }
  }

  /**
   * Read the given {@link ComponentMethod}, registering bean definitions
   * with the BeanDefinitionRegistry based on its contents.
   */
  private void loadBeanDefinitionsForComponentMethod(ComponentMethod componentMethod) {
    ConfigurationClass configClass = componentMethod.getConfigurationClass();
    MethodMetadata metadata = componentMethod.getMetadata();
    String methodName = metadata.getMethodName();

    // Do we need to mark the bean as skipped by its condition?
    if (bootstrapContext.shouldSkip(metadata, ConfigurationPhase.REGISTER_BEAN)) {
      configClass.skippedComponentMethods.add(methodName);
      return;
    }
    if (configClass.skippedComponentMethods.contains(methodName)) {
      return;
    }
    MergedAnnotations annotations = metadata.getAnnotations();
    MergedAnnotation<Component> component = annotations.get(Component.class);
    Assert.state(component.isPresent(), "No @Component annotation attributes");

    // Consider name and any aliases
    List<String> names = new ArrayList<>(Arrays.asList(component.getStringArray("name")));
    String beanName = (!names.isEmpty() ? names.remove(0) : methodName);

    // Has this effectively been overridden before (e.g. via XML)?
    if (isOverriddenByExistingDefinition(componentMethod, beanName)) {
      if (beanName.equals(componentMethod.getConfigurationClass().getBeanName())) {
        throw new BeanDefinitionStoreException(componentMethod.getConfigurationClass().getResource().toString(),
                beanName, "Bean name derived from @Component method '" + componentMethod.getMetadata().getMethodName() +
                "' clashes with bean name for containing configuration class; please make those names unique!");
      }
      return;
    }

    AnnotationMetadata annotationMetadata = configClass.getMetadata();
    ConfigurationClassBeanDefinition beanDef = new ConfigurationClassBeanDefinition(configClass, metadata, beanName);
    beanDef.setSource(configClass.getResource());
    beanDef.setResource(configClass.getResource());

    boolean disableDependencyInjectionAll = annotationMetadata.isAnnotated(
            DisableAllDependencyInjection.class.getName());
    boolean enableDependencyInjection = isEnableDependencyInjection(annotations, disableDependencyInjectionAll);

    beanDef.setBeanName(beanName);
    if (!names.isEmpty()) {
      beanDef.setAliases(names.toArray(Constant.EMPTY_STRING_ARRAY));
    }
    beanDef.setEnableDependencyInjection(enableDependencyInjection);

    if (metadata.isStatic()) {
      // static @Component method
      if (annotationMetadata instanceof StandardAnnotationMetadata) {
        beanDef.setBeanClass(((StandardAnnotationMetadata) annotationMetadata).getIntrospectedClass());
      }
      else {
        beanDef.setBeanClassName(annotationMetadata.getClassName());
      }
      beanDef.setUniqueFactoryMethodName(methodName);
    }
    else {
      // instance @Component method
      beanDef.setFactoryBeanName(configClass.getBeanName());
      beanDef.setUniqueFactoryMethodName(methodName);
    }

    AnnotationConfigUtils.processCommonDefinitionAnnotations(beanDef);

    if (!component.getBoolean("autowireCandidate")) {
      beanDef.setAutowireCandidate(false);
    }

    String[] initMethodName = component.getStringArray("initMethods");
    if (ObjectUtils.isNotEmpty(initMethodName)) {
      beanDef.setInitMethodNames(initMethodName);
    }

    String destroyMethodName = component.getString("destroyMethod");
    beanDef.setDestroyMethodName(destroyMethodName);

    // Consider scoping
    ScopedProxyMode proxyMode = ScopedProxyMode.NO;
    MergedAnnotation<Scope> scope = annotations.get(Scope.class);
    if (scope.isPresent()) {
      beanDef.setScope(scope.getString("value"));
      proxyMode = scope.getEnum("proxyMode", ScopedProxyMode.class);
      if (proxyMode == ScopedProxyMode.DEFAULT) {
        proxyMode = ScopedProxyMode.NO;
      }
    }

    // Replace the original bean definition with the target one, if necessary
    BeanDefinition beanDefToRegister;
    if (proxyMode != ScopedProxyMode.NO) {
      BeanDefinitionHolder proxyDef = ScopedProxyCreator.createScopedProxy(
              new BeanDefinitionHolder(beanDef, beanName), bootstrapContext.getRegistry(), proxyMode == ScopedProxyMode.TARGET_CLASS);
      beanDefToRegister = new ConfigurationClassBeanDefinition(
              (RootBeanDefinition) proxyDef.getBeanDefinition(), configClass, metadata, beanName);
    }
    else {
      beanDefToRegister = beanDef;
    }

    // Replace the original bean definition with the target one, if necessary
    if (logger.isTraceEnabled()) {
      logger.trace("Registering bean definition for @Component method {}.{}()",
              annotationMetadata.getClassName(), beanName);
    }

    bootstrapContext.registerBeanDefinition(beanDefToRegister);
  }

  private boolean isEnableDependencyInjection(MergedAnnotations annotations, boolean disableDependencyInjectionAll) {
    return annotations.isPresent(EnableDependencyInjection.class)
            || !(disableDependencyInjectionAll || annotations.isPresent(DisableDependencyInjection.class));
  }

  protected boolean isOverriddenByExistingDefinition(ComponentMethod componentMethod, String beanName) {
    BeanDefinition existingBeanDef = bootstrapContext.getBeanDefinition(beanName);
    if (existingBeanDef == null) {
      return false;
    }

    // Is the existing bean definition one that was created from a configuration class?
    // -> allow the current bean method to override, since both are at second-pass level.
    // However, if the bean method is an overloaded case on the same configuration class,
    // preserve the existing bean definition.
    if (existingBeanDef instanceof ConfigurationClassBeanDefinition ccbd) {
      if (ccbd.getMetadata().getClassName().equals(
              componentMethod.getConfigurationClass().getMetadata().getClassName())) {
        if (ccbd.getFactoryMethodMetadata().getMethodName().equals(ccbd.getFactoryMethodName())) {
          ccbd.setNonUniqueFactoryMethodName(ccbd.getFactoryMethodMetadata().getMethodName());
        }
        return true;
      }
      else {
        return false;
      }
    }

    // A bean definition resulting from a component scan can be silently overridden
    // by an @Component method
    if (existingBeanDef instanceof ScannedGenericBeanDefinition) {
      return false;
    }

    // Has the existing bean definition bean marked as a framework-generated bean?
    // -> allow the current bean method to override it, since it is application-level
    if (existingBeanDef.getRole() > BeanDefinition.ROLE_APPLICATION) {
      return false;
    }

    // At this point, it's a top-level override (probably XML), just having been parsed
    // before configuration class processing kicks in...
    if (!bootstrapContext.getRegistry().isAllowBeanDefinitionOverriding()) {
      throw new BeanDefinitionStoreException(componentMethod.getConfigurationClass().getResource().toString(),
              beanName, "@Component definition illegally overridden by existing bean definition: " + existingBeanDef);
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Skipping bean definition for {}: a definition for bean '{}' " +
                      "already exists. This top-level bean definition is considered as an override.",
              componentMethod, beanName);
    }
    return true;
  }

  private void loadBeanDefinitionsFromImportedResources(
          Map<String, Class<? extends BeanDefinitionReader>> importedResources) {

    HashMap<Class<?>, BeanDefinitionReader> readerInstanceCache = new HashMap<>();

    for (Map.Entry<String, Class<? extends BeanDefinitionReader>> entry : importedResources.entrySet()) {
      String resource = entry.getKey();
      Class<? extends BeanDefinitionReader> readerClass = entry.getValue();
      // Default reader selection necessary?
      if (BeanDefinitionReader.class == readerClass) {
        // Primarily ".xml" files but for any other extension as well
        readerClass = XmlBeanDefinitionReader.class;
      }

      BeanDefinitionReader reader = readerInstanceCache.get(readerClass);
      if (reader == null) {
        try {
          // Instantiate the specified BeanDefinitionReader
          reader = readerClass.getConstructor(BeanDefinitionRegistry.class).newInstance(bootstrapContext.getRegistry());
          // Delegate the current ResourceLoader to it if possible
          if (reader instanceof AbstractBeanDefinitionReader abdr) {
            abdr.setEnvironment(bootstrapContext.getEnvironment());
            abdr.setResourceLoader(bootstrapContext.getResourceLoader());
          }
          readerInstanceCache.put(readerClass, reader);
        }
        catch (Throwable ex) {
          throw new IllegalStateException(
                  "Could not instantiate BeanDefinitionReader class [" + readerClass.getName() + "]");
        }
      }

      reader.loadBeanDefinitions(resource);
    }

  }

  private void loadBeanDefinitionsFromRegistrars(Map<ImportBeanDefinitionRegistrar, AnnotationMetadata> registrars) {
    for (Map.Entry<ImportBeanDefinitionRegistrar, AnnotationMetadata> entry : registrars.entrySet()) {
      entry.getKey().registerBeanDefinitions(entry.getValue(), bootstrapContext);
    }
  }

  /**
   * Evaluate {@code @Conditional} annotations, tracking results and taking into
   * account 'imported by'.
   */
  private class TrackedConditionEvaluator {

    private final HashMap<ConfigurationClass, Boolean> skipped = new HashMap<>();

    public boolean shouldSkip(ConfigurationClass configClass) {
      Boolean skip = this.skipped.get(configClass);
      if (skip == null) {
        if (configClass.isImported()) {
          boolean allSkipped = true;
          for (ConfigurationClass importedBy : configClass.getImportedBy()) {
            if (!shouldSkip(importedBy)) {
              allSkipped = false;
              break;
            }
          }
          if (allSkipped) {
            // The config classes that imported this one were all skipped, therefore we are skipped...
            skip = true;
          }
        }
        if (skip == null) {
          skip = bootstrapContext.shouldSkip(configClass.getMetadata(), ConfigurationPhase.REGISTER_BEAN);
        }
        this.skipped.put(configClass, skip);
      }
      return skip;
    }
  }

  /**
   * {@link RootBeanDefinition} marker subclass used to signify that a bean definition
   * was created from a configuration class as opposed to any other configuration source.
   * Used in bean overriding cases where it's necessary to determine whether the bean
   * definition was created externally.
   */
  private static class ConfigurationClassBeanDefinition extends RootBeanDefinition
          implements AnnotatedBeanDefinition {

    private final AnnotationMetadata annotationMetadata;

    private final MethodMetadata factoryMethodMetadata;

    private final String derivedBeanName;

    public ConfigurationClassBeanDefinition(
            ConfigurationClass configClass, MethodMetadata beanMethodMetadata, String derivedBeanName) {

      this.annotationMetadata = configClass.getMetadata();
      this.factoryMethodMetadata = beanMethodMetadata;
      this.derivedBeanName = derivedBeanName;
      setResource(configClass.getResource());
      setLenientConstructorResolution(false);
    }

    public ConfigurationClassBeanDefinition(RootBeanDefinition original,
            ConfigurationClass configClass, MethodMetadata beanMethodMetadata, String derivedBeanName) {
      super(original);
      this.annotationMetadata = configClass.getMetadata();
      this.factoryMethodMetadata = beanMethodMetadata;
      this.derivedBeanName = derivedBeanName;
    }

    private ConfigurationClassBeanDefinition(ConfigurationClassBeanDefinition original) {
      super(original);
      this.annotationMetadata = original.annotationMetadata;
      this.factoryMethodMetadata = original.factoryMethodMetadata;
      this.derivedBeanName = original.derivedBeanName;
    }

    @Override
    public AnnotationMetadata getMetadata() {
      return this.annotationMetadata;
    }

    @Override
    @NonNull
    public MethodMetadata getFactoryMethodMetadata() {
      return this.factoryMethodMetadata;
    }

    @Override
    public boolean isFactoryMethod(Method candidate) {
      return super.isFactoryMethod(candidate)
              && BeanAnnotationHelper.isBeanAnnotated(candidate)
              && BeanAnnotationHelper.determineBeanNameFor(candidate).equals(derivedBeanName);
    }

    @Override
    public ConfigurationClassBeanDefinition cloneBeanDefinition() {
      return new ConfigurationClassBeanDefinition(this);
    }
  }

}
