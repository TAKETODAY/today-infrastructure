/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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
package cn.taketoday.context.support;

import java.util.function.Supplier;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinitionCustomizer;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.BeanNameGenerator;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.AnnotationConfigRegistry;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.BootstrapContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotatedBeanDefinitionReader;
import cn.taketoday.context.annotation.AnnotationBeanNameGenerator;
import cn.taketoday.context.annotation.AnnotationConfigUtils;
import cn.taketoday.context.annotation.AnnotationScopeMetadataResolver;
import cn.taketoday.context.annotation.ClassPathBeanDefinitionScanner;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import cn.taketoday.context.annotation.ScopeMetadataResolver;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Standard {@link ApplicationContext}
 *
 * like Spring's AnnotationConfigApplicationContext
 *
 * @author TODAY 2018-09-06 13:47
 */
public class StandardApplicationContext
        extends GenericApplicationContext implements ConfigurableApplicationContext, BeanDefinitionRegistry, AnnotationConfigRegistry {

  private final AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(this);
  private final ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(this);

  /**
   * Default Constructor
   */
  public StandardApplicationContext() { }

  /**
   * Construct with {@link StandardBeanFactory}
   *
   * @param beanFactory {@link StandardBeanFactory} instance
   */
  public StandardApplicationContext(StandardBeanFactory beanFactory) {
    super(beanFactory);
  }

  /**
   * Create a new StandardApplicationContext with the given parent.
   *
   * @param parent the parent application context
   * @see #registerBeanDefinition(String, BeanDefinition)
   * @see #refresh()
   */
  public StandardApplicationContext(@Nullable ApplicationContext parent) {
    setParent(parent);
  }

  /**
   * Create a new StandardApplicationContext with the given StandardBeanFactory.
   *
   * @param beanFactory the StandardBeanFactory instance to use for this context
   * @param parent the parent application context
   * @see #registerBeanDefinition(String, BeanDefinition)
   * @see #refresh()
   */
  public StandardApplicationContext(StandardBeanFactory beanFactory, ApplicationContext parent) {
    this(beanFactory);
    setParent(parent);
  }

  /**
   * Start with given class set
   *
   * @param components one or more component classes,
   * e.g. {@link Configuration @Configuration} classes
   * @see #refresh()
   * @see #register(Class[])
   */
  public StandardApplicationContext(Class<?>... components) {
    register(components);
    refresh();
  }

  /**
   * Start context with given properties location and base scan packages
   *
   * @param basePackages scan classes from packages
   * @see #refresh()
   */
  public StandardApplicationContext(String... basePackages) {
    scan(basePackages);
    refresh();
  }

  //---------------------------------------------------------------------
  // Implementation of AnnotationConfigRegistry
  //---------------------------------------------------------------------

  /**
   * Register one or more component classes to be processed.
   * <p>Note that {@link #refresh()} must be called in order for the context
   * to fully process the new classes.
   *
   * @param components one or more component classes &mdash; for example,
   * {@link Configuration @Configuration} classes
   * @see #scan(String...)
   * @see #refresh()
   */
  @Override
  public void register(Class<?>... components) {
    Assert.notEmpty(components, "At least one component class must be specified");
    reader.register(components);
  }

  /**
   * Perform a scan within the specified base packages.
   * <p>Note that {@link #refresh()} must be called in order for the context
   * to fully process the new classes.
   *
   * @param basePackages the packages to scan for component classes
   * @see #register(Class...)
   * @see #refresh()
   */
  @Override
  public void scan(String... basePackages) {
    Assert.notEmpty(basePackages, "At least one base package must be specified");
    scanner.scan(basePackages);
  }

  /**
   * Provide a custom {@link BeanNameGenerator} for use with {@link AnnotatedBeanDefinitionReader}
   * and/or {@link BootstrapContext}, if any.
   * <p>Default is {@link AnnotationBeanNameGenerator}.
   * <p>Any call to this method must occur prior to calls to {@link #register(Class...)}
   * and/or {@link #scan(String...)}.
   *
   * @see AnnotationBeanNameGenerator
   * @see FullyQualifiedAnnotationBeanNameGenerator
   * @see BootstrapContext#setBeanNameGenerator(BeanNameGenerator)
   * @see AnnotatedBeanDefinitionReader#setBeanNameGenerator(BeanNameGenerator)
   */
  public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
    Assert.notNull(beanNameGenerator, "BeanNameGenerator is required");

    reader.setBeanNameGenerator(beanNameGenerator);
    scanner.setBeanNameGenerator(beanNameGenerator);
    obtainBootstrapContext().setBeanNameGenerator(beanNameGenerator);

    getBeanFactory().registerSingleton(
            AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, beanNameGenerator);
  }

  /**
   * Set the {@link ScopeMetadataResolver} to use for registered component classes.
   * <p>The default is an {@link AnnotationScopeMetadataResolver}.
   * <p>Any call to this method must occur prior to calls to {@link #register(Class...)}
   * and/or {@link #scan(String...)}.
   */
  public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
    reader.setScopeMetadataResolver(scopeMetadataResolver);
    scanner.setScopeMetadataResolver(scopeMetadataResolver);
    obtainBootstrapContext().setScopeMetadataResolver(scopeMetadataResolver);
  }

  /**
   * Propagate the given custom {@code Environment} to the underlying
   * {@link AnnotatedBeanDefinitionReader} and {@link ClassPathBeanDefinitionScanner}.
   */
  @Override
  public void setEnvironment(ConfigurableEnvironment environment) {
    super.setEnvironment(environment);
    reader.setEnvironment(environment);
    scanner.setEnvironment(environment);
  }

  //---------------------------------------------------------------------
  // Adapt superclass registerBean calls to AnnotatedBeanDefinitionReader
  //---------------------------------------------------------------------

  @Override
  public <T> void registerBean(@Nullable String beanName, Class<T> beanClass,
          @Nullable Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {
    reader.registerBean(beanClass, beanName, supplier, customizers);
  }

}
