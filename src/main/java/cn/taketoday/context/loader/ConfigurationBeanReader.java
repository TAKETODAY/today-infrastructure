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

package cn.taketoday.context.loader;

import cn.taketoday.beans.factory.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.DefaultAnnotatedBeanDefinition;
import cn.taketoday.beans.factory.DefaultBeanDefinition;
import cn.taketoday.context.annotation.BeanDefinitionBuilder;
import cn.taketoday.context.annotation.ComponentScan;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.aware.ImportAware;
import cn.taketoday.context.event.ApplicationListener;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.env.CompositePropertySource;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.io.DefaultPropertySourceFactory;
import cn.taketoday.core.io.EncodedResource;
import cn.taketoday.core.io.PropertySourceFactory;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourcePropertySource;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Configuration;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author TODAY 2021/10/16 23:17
 * @see cn.taketoday.lang.Configuration
 * @see cn.taketoday.context.annotation.Import
 * @see cn.taketoday.context.annotation.MissingBean
 * @see cn.taketoday.context.annotation.MissingComponent
 * @see cn.taketoday.context.loader.ImportSelector
 * @see cn.taketoday.context.event.ApplicationListener
 * @see cn.taketoday.context.loader.BeanDefinitionImporter
 * @since 4.0
 */
public class ConfigurationBeanReader implements BeanFactoryPostProcessor {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationBeanReader.class);

  private final DefinitionLoadingContext context;

  private final HashSet<Class<?>> importedClass = new HashSet<>();

  private PropertySourceFactory propertySourceFactory;

  public ConfigurationBeanReader(DefinitionLoadingContext context) {
    this.context = context;
  }

  @Override
  public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
    loadConfigurationBeans();
    processMissingBean();
  }

  private void processMissingBean() {
    MissingBeanRegistry missingBeanRegistry = context.getMissingBeanRegistry();
    System.out.println(missingBeanRegistry);
  }

  /**
   * Resolve bean from a class which annotated with @{@link Configuration}
   */
  public void loadConfigurationBeans() {
    log.debug("Loading Configuration Beans");
    for (BeanDefinition definition : context.getRegistry()) {
      MetadataReader metadataReader = getMetadataReader(definition);

      // @Configuration
      processConfiguration(metadataReader, definition);
      // @Import
      processImport(metadataReader, definition);
      // @ComponentScan
      processComponentScan(metadataReader, definition);
    }
  }

  private void processConfiguration(MetadataReader metadataReader, BeanDefinition definition) {
    if (hasAnnotation(metadataReader, Configuration.class)) {
      // @Configuration bean
      AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
      loadConfigurationBeans(definition, annotationMetadata);
    }
  }

  protected boolean hasAnnotation(MetadataReader metadataReader, Class<?> annType) {
    return metadataReader.getAnnotationMetadata().hasAnnotation(annType.getName());
  }

  private void processComponentScan(MetadataReader metadataReader, BeanDefinition definition) {
    if (hasAnnotation(metadataReader, ComponentScan.class)) {

      ScanningBeanDefinitionReader scanningReader = new ScanningBeanDefinitionReader(context);

      AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
      AnnotationAttributes[] annotations = annotationMetadata.getAnnotations().getAttributes(ComponentScan.class);

      LinkedHashSet<String> basePackages = new LinkedHashSet<>();
      LinkedHashSet<String> patternLocations = new LinkedHashSet<>();
      LinkedHashSet<Class<? extends BeanDefinitionLoadingStrategy>> loadingStrategies = new LinkedHashSet<>();

      for (AnnotationAttributes annotation : annotations) {
        CollectionUtils.addAll(basePackages, annotation.getStringArray(Constant.VALUE));
        CollectionUtils.addAll(patternLocations, annotation.getStringArray("patternLocations"));
        CollectionUtils.addAll(loadingStrategies, annotation.getClassArray("loadingStrategies"));
      }

      scanningReader.addLoadingStrategies(loadingStrategies);

      if (CollectionUtils.isNotEmpty(basePackages)) {
        scanningReader.scanPackages(basePackages.toArray(Constant.EMPTY_STRING_ARRAY));
      }

      if (ObjectUtils.isNotEmpty(patternLocations)) {
        scanningReader.scan(patternLocations.toArray(Constant.EMPTY_STRING_ARRAY));
      }
    }

  }

  public void importing(Class<?> component) {
    BeanDefinition defaults = BeanDefinitionBuilder.defaults(component);
    loadConfigurationBeans(defaults, getMetadataReader(defaults).getAnnotationMetadata());
  }

  private void loadConfigurationBeans(BeanDefinition config, AnnotationMetadata importMetadata) {
    // process local declared methods first

    Set<MethodMetadata> annotatedMissingBeanMethods = importMetadata.getAnnotatedMethods(MissingBean.class.getName());
    for (MethodMetadata missingBeanMethod : annotatedMissingBeanMethods) {
      context.detectMissingBean(missingBeanMethod);
    }

    Set<MethodMetadata> annotatedMethods = importMetadata.getAnnotatedMethods(Component.class.getName());
    for (MethodMetadata beanMethod : annotatedMethods) {
      // pass the condition
      if (context.passCondition(beanMethod)) {

        String defaultBeanName = beanMethod.getMethodName();
        String declaringBeanName = config.getName();

        BeanDefinitionBuilder builder = context.createBuilder();

        builder.declaringName(declaringBeanName);
        builder.beanClassName(beanMethod.getReturnTypeName());

        AnnotationAttributes[] components = beanMethod.getAnnotations().getAttributes(Component.class);
        builder.build(defaultBeanName, components, (component, definition) -> {
          register(definition);
        });
      }
    }
  }

  static class ConfigBeanDefinition extends DefaultBeanDefinition implements AnnotatedBeanDefinition {
    BeanDefinition declaringDef;
    MethodMetadata componentMethod;

    private final AnnotationMetadata annotationMetadata;

    ConfigBeanDefinition(AnnotationMetadata annotationMetadata) {
      this.annotationMetadata = annotationMetadata;
    }

    @Override
    public AnnotationMetadata getMetadata() {
      return annotationMetadata;
    }

    @Nullable
    @Override
    public MethodMetadata getFactoryMethodMetadata() {
      return componentMethod;
    }
  }

  private AnnotationMetadata getAnnotationMetadata(BeanDefinition definition) {
    if (definition instanceof AnnotatedBeanDefinition) {
      return ((AnnotatedBeanDefinition) definition).getMetadata();
    }
    return getMetadataReader(definition).getAnnotationMetadata();
  }

  public void register(BeanDefinition definition) {
    context.registerBeanDefinition(definition);

    AnnotationMetadata annotationMetadata = getAnnotationMetadata(definition);
    processImport(annotationMetadata, definition);

    if (annotationMetadata.isAnnotated(Configuration.class.getName())) {
      loadConfigurationBeans(definition, annotationMetadata); //  scan config bean
    }
  }

  /**
   * Create {@link ImportSelector} ,or {@link BeanDefinitionImporter},
   * {@link ApplicationListener} object
   *
   * @param target Must be {@link ImportSelector} ,or {@link BeanDefinitionImporter}
   * @return {@link ImportSelector} object
   */
  @SuppressWarnings("unchecked")
  protected final <T> T createImporter(AnnotationMetadata importMetadata, Class<?> target) {
    T importer = (T) context.instantiate(target);
    if (importer instanceof ImportAware) {
      ((ImportAware) importer).setImportMetadata(importMetadata);
    }
    return importer;
  }

  protected void processImport(MetadataReader metadataReader, BeanDefinition annotated) {
    AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
    processImport(annotationMetadata, annotated);
  }

  protected void processImport(AnnotationMetadata annotationMetadata, BeanDefinition annotated) {
    AnnotationAttributes[] attributes = annotationMetadata.getAnnotations().getAttributes(Import.class);
    for (AnnotationAttributes attr : attributes) {
      for (Class<?> importClass : attr.getClassArray(Constant.VALUE)) {
        if (!importedClass.contains(importClass)) {
          doImport(annotated, annotationMetadata, importClass);
          importedClass.add(importClass);
        }
      }
    }
  }

  @NonNull
  private MetadataReader getMetadataReader(BeanDefinition annotated) {
    try {
      MetadataReaderFactory metadataFactory = context.getMetadataReaderFactory();
      if (annotated instanceof DefaultBeanDefinition) {
        Object source = ((DefaultBeanDefinition) annotated).getSource();
        if (source instanceof Resource) {
          return metadataFactory.getMetadataReader((Resource) source);
        }
        else {
          return metadataFactory.getMetadataReader(annotated.getBeanClassName());
        }
      }
      else {
        return metadataFactory.getMetadataReader(annotated.getBeanClassName());
      }
    }
    catch (IOException e) {
      throw ExceptionUtils.sneakyThrow(e);
    }
  }

  @NonNull
  private MetadataReader getMetadataReader(String className) {
    try {
      MetadataReaderFactory metadataFactory = context.getMetadataReaderFactory();
      return metadataFactory.getMetadataReader(className);
    }
    catch (IOException e) {
      throw ExceptionUtils.sneakyThrow(e);
    }
  }

  /**
   * Select import
   *
   * @param from Target {@link BeanDefinition}
   * @since 2.1.7
   */
  protected void doImport(BeanDefinition from, AnnotationMetadata importMetadata, Class<?> importClass) {
    log.debug("Importing: [{}]", importClass);

    BeanDefinition importDef = BeanDefinitionBuilder.defaults(importClass);
    importDef.setAttribute(ImportAware.ImportAnnotatedMetadata, importMetadata); // @since 3.0
    register(importDef);
    loadConfigurationBeans(importDef, importMetadata); // scan config bean

    // use import selector to select bean to register
    if (ImportSelector.class.isAssignableFrom(importClass)) {
      String[] imports = this.<ImportSelector>createImporter(importMetadata, importClass)
              .selectImports(importMetadata, context);
      if (ObjectUtils.isNotEmpty(imports)) {
        for (String select : imports) {
          MetadataReader metadataReader = getMetadataReader(select);
          AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
          DefaultAnnotatedBeanDefinition definition = new DefaultAnnotatedBeanDefinition(annotationMetadata);
          String beanName = context.createBeanName(select);
          definition.setName(beanName);
          register(definition);
        }
      }
    }
    // for BeanDefinitionImporter to imports beans
    if (BeanDefinitionImporter.class.isAssignableFrom(importClass)) {
      this.<BeanDefinitionImporter>createImporter(importMetadata, importClass)
              .registerBeanDefinitions(importMetadata, context);
    }
    if (ApplicationListener.class.isAssignableFrom(importClass)) {
      context.addApplicationListener(this.createImporter(importMetadata, importClass));
    }
  }


  /**
   * Process the given <code>@PropertySource</code> annotation metadata.
   *
   * @param propertySource metadata for the <code>@PropertySource</code> annotation found
   * @throws IOException if loading a property source failed
   */
  private void processPropertySource(AnnotationAttributes propertySource) throws IOException {
    String name = propertySource.getString("name");
    if (StringUtils.isNotEmpty(name)) {
      name = null;
    }
    String encoding = propertySource.getString("encoding");
    if (StringUtils.isNotEmpty(encoding)) {
      encoding = null;
    }
    String[] locations = propertySource.getStringArray("value");
    Assert.isTrue(locations.length > 0, "At least one @PropertySource(value) location is required");

    Class<? extends PropertySourceFactory> factoryClass = propertySource.getClass("factory");
    PropertySourceFactory factory = factoryClass == PropertySourceFactory.class
            ? getPropertySourceFactory() : context.instantiate(factoryClass);

    for (String location : locations) {
      try {
        String resolvedLocation = context.evaluateExpression(location);
        Resource resource = context.getResource(resolvedLocation);
        addPropertySource(factory.createPropertySource(name, new EncodedResource(resource, encoding)));
      }
      catch (IllegalArgumentException | FileNotFoundException | UnknownHostException | SocketException ex) {
        // Placeholders not resolvable or resource not found when trying to open it
        if (propertySource.getBoolean("ignoreResourceNotFound")) {
          if (log.isInfoEnabled()) {
            log.info("Properties location [" + location + "] not resolvable: " + ex.getMessage());
          }
        }
        else {
          throw ex;
        }
      }
    }
  }

  private final ArrayList<String> propertySourceNames = new ArrayList<>();

  private void addPropertySource(PropertySource<?> propertySource) {
    String name = propertySource.getName();
    Environment environment = context.getApplicationContext().getEnvironment();
    PropertySources propertySources = ((ConfigurableEnvironment) environment).getPropertySources();

    if (this.propertySourceNames.contains(name)) {
      // We've already added a version, we need to extend it
      PropertySource<?> existing = propertySources.get(name);
      if (existing != null) {
        PropertySource<?> newSource = (propertySource instanceof ResourcePropertySource ?
                ((ResourcePropertySource) propertySource).withResourceName() : propertySource);
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

  /**
   * setting default PropertySourceFactory
   *
   * @param propertySourceFactory PropertySourceFactory
   */
  public void setPropertySourceFactory(@Nullable PropertySourceFactory propertySourceFactory) {
    this.propertySourceFactory = propertySourceFactory;
  }

  public PropertySourceFactory getPropertySourceFactory() {
    if (propertySourceFactory == null) {
      propertySourceFactory = new DefaultPropertySourceFactory();
    }
    return propertySourceFactory;
  }


}
