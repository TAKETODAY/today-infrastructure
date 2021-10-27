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

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.beans.factory.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.DefaultBeanDefinition;
import cn.taketoday.context.annotation.BeanDefinitionBuilder;
import cn.taketoday.context.annotation.ComponentScan;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.aware.ImportAware;
import cn.taketoday.context.event.ApplicationListener;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Configuration;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.ObjectUtils;

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
    try {
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
    catch (IOException e) {
      throw ExceptionUtils.sneakyThrow(e);
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
    Set<MethodMetadata> annotatedMethods = importMetadata.getAnnotatedMethods(Component.class.getName());
    for (MethodMetadata beanMethod : annotatedMethods) {
      // pass the condition
      if (context.passCondition(beanMethod)) {
        context.detectMissingBean(beanMethod);

        String defaultBeanName = beanMethod.getMethodName();
        String declaringBeanName = config.getName();

        BeanDefinitionBuilder builder = context.createBuilder();

        builder.declaringName(declaringBeanName);
        builder.beanClassName(beanMethod.getReturnTypeName());

        AnnotationAttributes[] components = beanMethod.getAnnotations().getAttributes(Component.class);

//        DefaultAnnotatedBeanDefinition def = new DefaultAnnotatedBeanDefinition();

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

  public void register(BeanDefinition definition) {
    try {
      context.registerBeanDefinition(definition);
      MetadataReader metadataReader = getMetadataReader(definition);

      processImport(metadataReader, definition);

      AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
      if (annotationMetadata.isAnnotated(Configuration.class.getName())) {
        loadConfigurationBeans(definition, annotationMetadata); //  scan config bean
      }
    }
    catch (IOException e) {
      throw ExceptionUtils.sneakyThrow(e);
    }
  }

  /**
   * Create {@link ImportSelector} ,or {@link BeanDefinitionImporter},
   * {@link ApplicationListener} object
   *
   * @param target Must be {@link ImportSelector} ,or {@link BeanDefinitionImporter}
   * @return {@link ImportSelector} object
   */
  protected final <T> T createImporter(AnnotationMetadata importMetadata, Class<T> target) {
    T importer = context.instantiate(target);
    if (importer instanceof ImportAware) {
      ((ImportAware) importer).setImportMetadata(importMetadata);
    }
    return importer;
  }

  protected void processImport(MetadataReader metadataReader, BeanDefinition annotated) throws IOException {
    AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
    AnnotationAttributes[] attributes = annotationMetadata.getAnnotations().getAttributes(Import.class);

    for (AnnotationAttributes attr : attributes) {
      for (Class<?> importClass : attr.getClassArray(Constant.VALUE)) {
        if (!importedClass.contains(importClass)) {
          doImport(annotated, annotationMetadata, importClass);
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

  /**
   * Select import
   *
   * @param from Target {@link BeanDefinition}
   * @param importMetadata
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
      String[] imports = createImporter(importMetadata, ImportSelector.class)
              .selectImports(importMetadata, context);
      if (ObjectUtils.isNotEmpty(imports)) {
        for (String select : imports) {
          Class<Object> beanClass = ClassUtils.load(select);
          if (beanClass == null) {
            throw new ConfigurationException("Bean class not in class-path: " + select);
          }
          register(BeanDefinitionBuilder.defaults(beanClass));
        }
      }
    }
    // for BeanDefinitionImporter to imports beans
    if (BeanDefinitionImporter.class.isAssignableFrom(importClass)) {
      createImporter(importMetadata, BeanDefinitionImporter.class)
              .registerBeanDefinitions(importMetadata, context);
    }
    if (ApplicationListener.class.isAssignableFrom(importClass)) {
      context.addApplicationListener(createImporter(importMetadata, ApplicationListener.class));
    }
  }

}
