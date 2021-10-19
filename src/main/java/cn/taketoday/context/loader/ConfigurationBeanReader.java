/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import java.lang.reflect.Method;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.context.annotation.BeanDefinitionBuilder;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.aware.ImportAware;
import cn.taketoday.context.event.ApplicationListener;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Configuration;
import cn.taketoday.lang.Constant;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;

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

  public ConfigurationBeanReader(DefinitionLoadingContext context) {
    this.context = context;
  }

  @Override
  public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
    loadConfigurationBeans();
  }

  /**
   * Resolve bean from a class which annotated with @{@link Configuration}
   */
  public void loadConfigurationBeans() {
    log.debug("Loading Configuration Beans");

    for (BeanDefinition definition : context.getRegistry()) {
      if (definition.isAnnotationPresent(Configuration.class)) {
        // @Configuration bean
        loadConfigurationBeans(definition);
      }
    }
  }

  /**
   * Load {@link Configuration} beans from input bean class
   *
   * @param declaringDef current {@link Configuration} bean
   * @since 2.1.7
   */
  protected void loadConfigurationBeans(BeanDefinition declaringDef) {
    // process local declared methods first
    for (Method method : ReflectionUtils.getDeclaredMethods(declaringDef.getBeanClass())) {
      AnnotationAttributes[] components = AnnotationUtils.getAttributesArray(method, Component.class);
      if (ObjectUtils.isEmpty(components)) {
        // detect missed bean
        context.detectMissingBean(method);
      } // is a Component
      else if (context.passCondition(method)) { // pass the condition
        registerConfigurationBean(declaringDef, method, components);
      }
    }
  }

  /**
   * Create {@link Configuration} bean definition, and register it
   *
   * @param method factory method
   * @param components {@link AnnotationAttributes}
   */
  protected void registerConfigurationBean(
          BeanDefinition declaringDef, Method method, AnnotationAttributes[] components
  ) {
    String defaultBeanName = method.getName(); // @since v2.1.7
    String declaringBeanName = declaringDef.getName(); // @since v2.1.7

    BeanDefinitionBuilder builder = context.createBuilder();
    builder.factoryMethod(method);
    builder.declaringName(declaringBeanName);
    builder.beanClass(method.getReturnType());
    builder.build(defaultBeanName, components, (component, definition) -> {
      register(definition);
    });
  }

  public void register(BeanDefinition definition) {
    context.registerBeanDefinition(definition);
    importAnnotated(definition);
    if (definition.isAnnotationPresent(Configuration.class)) {
      loadConfigurationBeans(definition); //  scan config bean
    }
  }

  /**
   * Create {@link ImportSelector} ,or {@link BeanDefinitionImporter},
   * {@link ApplicationListener} object
   *
   * @param target Must be {@link ImportSelector} ,or {@link BeanDefinitionImporter}
   * @return {@link ImportSelector} object
   */
  protected <T> T createImporter(BeanDefinition importDef, Class<T> target) {
    try {
      Object bean = context.getBean(importDef);
      if (bean instanceof ImportAware) {
        ((ImportAware) bean).setImportBeanDefinition(importDef);
      }
      return target.cast(bean);
    }
    catch (Throwable e) {
      throw new BeanDefinitionStoreException("Can't initialize a target: [" + importDef + "]");
    }
  }

  void importAnnotated(BeanDefinition annotated) {
    for (AnnotationAttributes attr : AnnotationUtils.getAttributesArray(annotated, Import.class)) {
      for (Class<?> importClass : attr.getAttribute(Constant.VALUE, Class[].class)) {
        if (!context.containsBeanDefinition(importClass, true)) {
          doImport(annotated, importClass);
        }
      }
    }
  }

  /**
   * Select import
   *
   * @param annotated Target {@link BeanDefinition}
   * @since 2.1.7
   */
  protected void doImport(BeanDefinition annotated, Class<?> importClass) {
    log.debug("Importing: [{}]", importClass);

    BeanDefinition importDef = BeanDefinitionBuilder.defaults(importClass);
    importDef.setAttribute(ImportAware.ImportAnnotatedMetadata, annotated); // @since 3.0
    register(importDef);
    loadConfigurationBeans(importDef); // scan config bean
    // use import selector to select bean to register
    if (ImportSelector.class.isAssignableFrom(importClass)) {
      String[] imports = createImporter(importDef, ImportSelector.class)
              .selectImports(annotated, context.getRegistry());
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
      createImporter(importDef, BeanDefinitionImporter.class)
              .registerBeanDefinitions(annotated, context.getRegistry());
    }
    if (ApplicationListener.class.isAssignableFrom(importClass)) {
      context.addApplicationListener(createImporter(importDef, ApplicationListener.class));
    }
  }

}
