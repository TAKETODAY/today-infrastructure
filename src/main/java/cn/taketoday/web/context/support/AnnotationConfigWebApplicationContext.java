/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.context.support;

import cn.taketoday.beans.factory.support.DefaultListableBeanFactory;
import cn.taketoday.context.annotation.AnnotatedBeanDefinitionReader;
import cn.taketoday.context.annotation.AnnotationConfigUtils;
import cn.taketoday.context.annotation.ClassPathBeanDefinitionScanner;
import cn.taketoday.util.Assert;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.context.ContextLoader;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.beans.factory.BeanNamePopulator;
import cn.taketoday.context.AnnotationConfigRegistry;
import cn.taketoday.context.loader.ScopeMetadataResolver;
import cn.taketoday.lang.Nullable;

/**
 * {@link cn.taketoday.web.context.WebApplicationContext WebApplicationContext}
 * implementation which accepts <em>component classes</em> as input &mdash; in particular
 * {@link cn.taketoday.context.annotation.Configuration @Configuration}
 * classes, but also plain {@link cn.taketoday.stereotype.Component @Component}
 * classes as well as JSR-330 compliant classes using {@code jakarta.inject} annotations.
 *
 * <p>Allows for registering classes one by one (specifying class names as config
 * locations) as well as via classpath scanning (specifying base packages as config
 * locations).
 *
 * <p>This is essentially the equivalent of
 * {@link cn.taketoday.context.annotation.AnnotationConfigApplicationContext
 * AnnotationConfigApplicationContext} for a web environment. However, in contrast to
 * {@code AnnotationConfigApplicationContext}, this class does not extend
 * {@link cn.taketoday.context.support.GenericApplicationContext
 * GenericApplicationContext} and therefore does not provide some of the convenient
 * {@code registerBean(...)} methods available in a {@code GenericApplicationContext}.
 * If you wish to register annotated <em>component classes</em> with a
 * {@code GenericApplicationContext} in a web environment, you may use a
 * {@code GenericWebApplicationContext} with an
 * {@link cn.taketoday.context.annotation.AnnotatedBeanDefinitionReader
 * AnnotatedBeanDefinitionReader}. See the Javadoc for {@link GenericWebApplicationContext}
 * for details and an example.
 *
 * <p>To make use of this application context, the
 * {@linkplain ContextLoader#CONTEXT_CLASS_PARAM "contextClass"} context-param for
 * ContextLoader and/or "contextClass" init-param for FrameworkServlet must be set to
 * the fully-qualified name of this class.
 *
 * <p>As of Spring 3.1, this class may also be directly instantiated and injected into
 * Spring's {@code DispatcherServlet} or {@code ContextLoaderListener} when using the
 * {@link cn.taketoday.web.WebApplicationInitializer WebApplicationInitializer}
 * code-based alternative to {@code web.xml}. See its Javadoc for details and usage examples.
 *
 * <p>Unlike {@link XmlWebApplicationContext}, no default configuration class locations
 * are assumed. Rather, it is a requirement to set the
 * {@linkplain ContextLoader#CONFIG_LOCATION_PARAM "contextConfigLocation"}
 * context-param for {@link ContextLoader} and/or "contextConfigLocation" init-param for
 * FrameworkServlet.  The param-value may contain both fully-qualified
 * class names and base packages to scan for components. See {@link #loadBeanDefinitions}
 * for exact details on how these locations are processed.
 *
 * <p>As an alternative to setting the "contextConfigLocation" parameter, users may
 * implement an {@link cn.taketoday.context.ApplicationContextInitializer
 * ApplicationContextInitializer} and set the
 * {@linkplain ContextLoader#CONTEXT_INITIALIZER_CLASSES_PARAM "contextInitializerClasses"}
 * context-param / init-param. In such cases, users should favor the {@link #refresh()}
 * and {@link #scan(String...)} methods over the {@link #setConfigLocation(String)}
 * method, which is primarily for use by {@code ContextLoader}.
 *
 * <p>Note: In case of multiple {@code @Configuration} classes, later {@code @Bean}
 * definitions will override ones defined in earlier loaded files. This can be leveraged
 * to deliberately override certain bean definitions via an extra {@code @Configuration}
 * class.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.context.annotation.AnnotationConfigApplicationContext
 * @see cn.taketoday.web.context.support.GenericWebApplicationContext
 * @since 4.0 2022/2/20 17:55
 */
public class AnnotationConfigWebApplicationContext
        extends AbstractRefreshableWebApplicationContext implements AnnotationConfigRegistry {

  @Nullable
  private BeanNamePopulator BeanNamePopulator;

  @Nullable
  private ScopeMetadataResolver scopeMetadataResolver;

  private final Set<Class<?>> componentClasses = new LinkedHashSet<>();

  private final Set<String> basePackages = new LinkedHashSet<>();

  /**
   * Set a custom {@link BeanNamePopulator} for use with {@link AnnotatedBeanDefinitionReader}
   * and/or {@link ClassPathBeanDefinitionScanner}.
   * <p>Default is {@link cn.taketoday.context.annotation.AnnotationBeanNamePopulator}.
   *
   * @see AnnotatedBeanDefinitionReader#setBeanNamePopulator
   * @see ClassPathBeanDefinitionScanner#setBeanNamePopulator
   */
  public void setBeanNamePopulator(@Nullable BeanNamePopulator BeanNamePopulator) {
    this.BeanNamePopulator = BeanNamePopulator;
  }

  /**
   * Return the custom {@link BeanNamePopulator} for use with {@link AnnotatedBeanDefinitionReader}
   * and/or {@link ClassPathBeanDefinitionScanner}, if any.
   */
  @Nullable
  protected BeanNamePopulator getBeanNamePopulator() {
    return this.BeanNamePopulator;
  }

  /**
   * Set a custom {@link ScopeMetadataResolver} for use with {@link AnnotatedBeanDefinitionReader}
   * and/or {@link ClassPathBeanDefinitionScanner}.
   * <p>Default is an {@link cn.taketoday.context.annotation.AnnotationScopeMetadataResolver}.
   *
   * @see AnnotatedBeanDefinitionReader#setScopeMetadataResolver
   * @see ClassPathBeanDefinitionScanner#setScopeMetadataResolver
   */
  public void setScopeMetadataResolver(@Nullable ScopeMetadataResolver scopeMetadataResolver) {
    this.scopeMetadataResolver = scopeMetadataResolver;
  }

  /**
   * Return the custom {@link ScopeMetadataResolver} for use with {@link AnnotatedBeanDefinitionReader}
   * and/or {@link ClassPathBeanDefinitionScanner}, if any.
   */
  @Nullable
  protected ScopeMetadataResolver getScopeMetadataResolver() {
    return this.scopeMetadataResolver;
  }

  /**
   * Register one or more component classes to be processed.
   * <p>Note that {@link #refresh()} must be called in order for the context
   * to fully process the new classes.
   *
   * @param componentClasses one or more component classes,
   * e.g. {@link cn.taketoday.context.annotation.Configuration @Configuration} classes
   * @see #scan(String...)
   * @see #loadBeanDefinitions(DefaultListableBeanFactory)
   * @see #setConfigLocation(String)
   * @see #refresh()
   */
  @Override
  public void register(Class<?>... componentClasses) {
    Assert.notEmpty(componentClasses, "At least one component class must be specified");
    Collections.addAll(this.componentClasses, componentClasses);
  }

  /**
   * Perform a scan within the specified base packages.
   * <p>Note that {@link #refresh()} must be called in order for the context
   * to fully process the new classes.
   *
   * @param basePackages the packages to check for component classes
   * @see #loadBeanDefinitions(DefaultListableBeanFactory)
   * @see #register(Class...)
   * @see #setConfigLocation(String)
   * @see #refresh()
   */
  @Override
  public void scan(String... basePackages) {
    Assert.notEmpty(basePackages, "At least one base package must be specified");
    Collections.addAll(this.basePackages, basePackages);
  }

  /**
   * Register a {@link cn.taketoday.beans.factory.config.BeanDefinition} for
   * any classes specified by {@link #register(Class...)} and scan any packages
   * specified by {@link #scan(String...)}.
   * <p>For any values specified by {@link #setConfigLocation(String)} or
   * {@link #setConfigLocations(String[])}, attempt first to load each location as a
   * class, registering a {@code BeanDefinition} if class loading is successful,
   * and if class loading fails (i.e. a {@code ClassNotFoundException} is raised),
   * assume the value is a package and attempt to scan it for component classes.
   * <p>Enables the default set of annotation configuration post processors, such that
   * {@code @Autowired}, {@code @Required}, and associated annotations can be used.
   * <p>Configuration class bean definitions are registered with generated bean
   * definition names unless the {@code value} attribute is provided to the stereotype
   * annotation.
   *
   * @param beanFactory the bean factory to load bean definitions into
   * @see #register(Class...)
   * @see #scan(String...)
   * @see #setConfigLocation(String)
   * @see #setConfigLocations(String[])
   * @see AnnotatedBeanDefinitionReader
   * @see ClassPathBeanDefinitionScanner
   */
  @Override
  protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) {
    AnnotatedBeanDefinitionReader reader = getAnnotatedBeanDefinitionReader(beanFactory);
    ClassPathBeanDefinitionScanner scanner = getClassPathBeanDefinitionScanner(beanFactory);

    BeanNamePopulator BeanNamePopulator = getBeanNamePopulator();
    if (BeanNamePopulator != null) {
      reader.setBeanNamePopulator(BeanNamePopulator);
      scanner.setBeanNamePopulator(BeanNamePopulator);
      beanFactory.registerSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, BeanNamePopulator);
    }

    ScopeMetadataResolver scopeMetadataResolver = getScopeMetadataResolver();
    if (scopeMetadataResolver != null) {
      reader.setScopeMetadataResolver(scopeMetadataResolver);
      scanner.setScopeMetadataResolver(scopeMetadataResolver);
    }

    if (!this.componentClasses.isEmpty()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Registering component classes: [" +
                StringUtils.collectionToCommaDelimitedString(this.componentClasses) + "]");
      }
      reader.register(ClassUtils.toClassArray(this.componentClasses));
    }

    if (!this.basePackages.isEmpty()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Scanning base packages: [" +
                StringUtils.collectionToCommaDelimitedString(this.basePackages) + "]");
      }
      scanner.scan(StringUtils.toStringArray(this.basePackages));
    }

    String[] configLocations = getConfigLocations();
    if (configLocations != null) {
      for (String configLocation : configLocations) {
        try {
          Class<?> clazz = ClassUtils.forName(configLocation, getClassLoader());
          if (logger.isTraceEnabled()) {
            logger.trace("Registering [" + configLocation + "]");
          }
          reader.register(clazz);
        }
        catch (ClassNotFoundException ex) {
          if (logger.isTraceEnabled()) {
            logger.trace("Could not load class for config location [" + configLocation +
                    "] - trying package scan. " + ex);
          }
          int count = scanner.scan(configLocation);
          if (count == 0 && logger.isDebugEnabled()) {
            logger.debug("No component classes found for specified class/package [" + configLocation + "]");
          }
        }
      }
    }
  }

  /**
   * Build an {@link AnnotatedBeanDefinitionReader} for the given bean factory.
   * <p>This should be pre-configured with the {@code Environment} (if desired)
   * but not with a {@code BeanNamePopulator} or {@code ScopeMetadataResolver} yet.
   *
   * @param beanFactory the bean factory to load bean definitions into
   * @see #getEnvironment()
   * @see #getBeanNamePopulator()
   * @see #getScopeMetadataResolver()
   * @since 4.1.9
   */
  protected AnnotatedBeanDefinitionReader getAnnotatedBeanDefinitionReader(DefaultListableBeanFactory beanFactory) {
    return new AnnotatedBeanDefinitionReader(beanFactory, getEnvironment());
  }

  /**
   * Build a {@link ClassPathBeanDefinitionScanner} for the given bean factory.
   * <p>This should be pre-configured with the {@code Environment} (if desired)
   * but not with a {@code BeanNamePopulator} or {@code ScopeMetadataResolver} yet.
   *
   * @param beanFactory the bean factory to load bean definitions into
   * @see #getEnvironment()
   * @see #getBeanNamePopulator()
   * @see #getScopeMetadataResolver()
   * @since 4.1.9
   */
  protected ClassPathBeanDefinitionScanner getClassPathBeanDefinitionScanner(DefaultListableBeanFactory beanFactory) {
    return new ClassPathBeanDefinitionScanner(beanFactory, true, getEnvironment());
  }

}
