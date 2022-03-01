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
package cn.taketoday.context.support;

import java.util.function.Supplier;

import cn.taketoday.beans.factory.BeanNamePopulator;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionCustomizer;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.DependencyResolvingStrategies;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.support.StandardDependenciesBeanPostProcessor;
import cn.taketoday.context.AnnotationConfigRegistry;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotatedBeanDefinitionReader;
import cn.taketoday.context.annotation.AnnotationBeanNamePopulator;
import cn.taketoday.context.annotation.AnnotationConfigUtils;
import cn.taketoday.context.annotation.AnnotationScopeMetadataResolver;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.FullyQualifiedAnnotationBeanNamePopulator;
import cn.taketoday.context.annotation.PropsDependenciesBeanPostProcessor;
import cn.taketoday.context.annotation.PropsDependencyResolver;
import cn.taketoday.context.loader.BootstrapContext;
import cn.taketoday.context.loader.ClassPathBeanDefinitionScanner;
import cn.taketoday.context.loader.ScopeMetadataResolver;
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

  private AnnotatedBeanDefinitionReader reader;
  private ClassPathBeanDefinitionScanner scanner;

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
  // Implementation of AbstractApplicationContext
  //---------------------------------------------------------------------

  @Override
  protected void registerBeanPostProcessors(ConfigurableBeanFactory beanFactory) {
    super.registerBeanPostProcessors(beanFactory);

    // register DI bean post processors
    beanFactory.addBeanPostProcessor(new PropsDependenciesBeanPostProcessor(this));

    Object bean = beanFactory.getBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME);

    if (bean instanceof StandardDependenciesBeanPostProcessor autowiredPostProcessor) {
      addDependencyStrategies(autowiredPostProcessor);
    }
  }

  private void addDependencyStrategies(StandardDependenciesBeanPostProcessor autowiredPostProcessor) {
    DependencyResolvingStrategies strategies = autowiredPostProcessor.getResolvingStrategies();

    PropsDependencyResolver strategy = new PropsDependencyResolver(this);
    strategy.setOrder(2);
    strategies.addStrategies(strategy);
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
    reader().registerBean(components);
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
    scanner().scan(basePackages);
  }

  /**
   * Provide a custom {@link BeanNamePopulator} for use with {@link AnnotatedBeanDefinitionReader}
   * and/or {@link BootstrapContext}, if any.
   * <p>Default is {@link AnnotationBeanNamePopulator}.
   * <p>Any call to this method must occur prior to calls to {@link #register(Class...)}
   * and/or {@link #scan(String...)}.
   *
   * @see AnnotationBeanNamePopulator
   * @see FullyQualifiedAnnotationBeanNamePopulator
   * @see BootstrapContext#setBeanNamePopulator(BeanNamePopulator)
   * @see AnnotatedBeanDefinitionReader#setBeanNamePopulator(BeanNamePopulator)
   */
  public void setBeanNamePopulator(BeanNamePopulator beanNamePopulator) {
    Assert.notNull(beanNamePopulator, "BeanNamePopulator is required");

    reader().setBeanNamePopulator(beanNamePopulator);
    scanner().setBeanNamePopulator(beanNamePopulator);
    obtainBootstrapContext().setBeanNamePopulator(beanNamePopulator);

    getBeanFactory().registerSingleton(
            AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, beanNamePopulator);
  }

  /**
   * Set the {@link ScopeMetadataResolver} to use for registered component classes.
   * <p>The default is an {@link AnnotationScopeMetadataResolver}.
   * <p>Any call to this method must occur prior to calls to {@link #register(Class...)}
   * and/or {@link #scan(String...)}.
   */
  public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
    reader().setScopeMetadataResolver(scopeMetadataResolver);
    scanner().setScopeMetadataResolver(scopeMetadataResolver);
    obtainBootstrapContext().setScopeMetadataResolver(scopeMetadataResolver);
  }

  /**
   * Propagate the given custom {@code Environment} to the underlying
   * {@link AnnotatedBeanDefinitionReader} and {@link ClassPathBeanDefinitionScanner}.
   */
  @Override
  public void setEnvironment(ConfigurableEnvironment environment) {
    super.setEnvironment(environment);
    scanner().setEnvironment(environment);
  }

  //---------------------------------------------------------------------
  // Adapt superclass registerBean calls to AnnotatedBeanDefinitionReader
  //---------------------------------------------------------------------

  @Override
  public <T> void registerBean(@Nullable String beanName, Class<T> beanClass,
          @Nullable Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {
    reader().registerBean(beanClass, beanName, supplier, customizers);
  }

  ClassPathBeanDefinitionScanner scanner() {
    if (scanner == null) {
      scanner = new ClassPathBeanDefinitionScanner(this);
    }
    return scanner;
  }

  AnnotatedBeanDefinitionReader reader() {
    if (reader == null) {
      reader = new AnnotatedBeanDefinitionReader(this, beanFactory);
    }
    return reader;
  }

}
