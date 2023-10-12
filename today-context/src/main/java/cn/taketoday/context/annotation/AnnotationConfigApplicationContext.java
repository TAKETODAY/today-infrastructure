/*
 * Copyright 2017 - 2023 the original author or authors.
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
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.context.support.GenericXmlApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Standalone application context, accepting <em>component classes</em> as input &mdash;
 * in particular {@link Configuration @Configuration}-annotated classes, but also plain
 * {@link cn.taketoday.stereotype.Component @Component} types and JSR-330 compliant
 * classes using {@code jakarta.inject} annotations.
 *
 * <p>Allows for registering classes one by one using {@link #register(Class...)}
 * as well as for classpath scanning using {@link #scan(String...)}.
 *
 * <p>In case of multiple {@code @Configuration} classes, {@link cn.taketoday.stereotype.Component @Component} methods
 * defined in later classes will override those defined in earlier classes. This can
 * be leveraged to deliberately override certain bean definitions via an extra
 * {@code @Configuration} class.
 *
 * <p>See {@link Configuration @Configuration}'s javadoc for usage examples.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #register
 * @see #scan
 * @see AnnotatedBeanDefinitionReader
 * @see ClassPathBeanDefinitionScanner
 * @see GenericXmlApplicationContext
 * @since 2018-09-06 13:47
 */
public class AnnotationConfigApplicationContext extends GenericApplicationContext
        implements ConfigurableApplicationContext, BeanDefinitionRegistry, AnnotationConfigRegistry {

  private final AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(this);

  private final ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(this);

  /**
   * Create a new AnnotationConfigApplicationContext that needs to be populated
   * through {@link #register} calls and then manually {@linkplain #refresh refreshed}.
   */
  public AnnotationConfigApplicationContext() { }

  /**
   * Create a new AnnotationConfigApplicationContext with the given StandardBeanFactory.
   *
   * @param beanFactory the StandardBeanFactory instance to use for this context
   */
  public AnnotationConfigApplicationContext(StandardBeanFactory beanFactory) {
    super(beanFactory);
  }

  /**
   * Create a new AnnotationConfigApplicationContext with the given parent.
   *
   * @param parent the parent application context
   * @see #registerBeanDefinition(String, BeanDefinition)
   * @see #refresh()
   */
  public AnnotationConfigApplicationContext(@Nullable ApplicationContext parent) {
    setParent(parent);
  }

  /**
   * Create a new AnnotationConfigApplicationContext with the given StandardBeanFactory.
   *
   * @param beanFactory the StandardBeanFactory instance to use for this context
   * @param parent the parent application context
   * @see #registerBeanDefinition(String, BeanDefinition)
   * @see #refresh()
   */
  public AnnotationConfigApplicationContext(StandardBeanFactory beanFactory, ApplicationContext parent) {
    this(beanFactory);
    setParent(parent);
  }

  /**
   * Create a new AnnotationConfigApplicationContext, deriving bean definitions
   * from the given component classes and automatically refreshing the context.
   *
   * @param components one or more component classes &mdash; for example,
   * {@link Configuration @Configuration} classes
   * @see #refresh()
   * @see #register(Class[])
   */
  public AnnotationConfigApplicationContext(Class<?>... components) {
    register(components);
    refresh();
  }

  /**
   * Create a new AnnotationConfigApplicationContext, scanning for components
   * in the given packages, registering bean definitions for those components,
   * and automatically refreshing the context.
   *
   * @param basePackages the packages to scan for component classes
   * @see #refresh()
   */
  public AnnotationConfigApplicationContext(String... basePackages) {
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
    getBootstrapContext().setBeanNameGenerator(beanNameGenerator);

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
    getBootstrapContext().setScopeMetadataResolver(scopeMetadataResolver);
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
