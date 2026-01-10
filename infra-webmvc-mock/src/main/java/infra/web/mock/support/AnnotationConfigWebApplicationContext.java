/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.mock.support;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import infra.beans.factory.BeanRegistrar;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.BeanNameGenerator;
import infra.beans.factory.support.BeanRegistryAdapter;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.AnnotationConfigRegistry;
import infra.context.BootstrapContext;
import infra.context.annotation.AnnotatedBeanDefinitionReader;
import infra.context.annotation.AnnotationBeanNameGenerator;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.AnnotationConfigUtils;
import infra.context.annotation.AnnotationScopeMetadataResolver;
import infra.context.annotation.ClassPathBeanDefinitionScanner;
import infra.context.annotation.Configuration;
import infra.context.annotation.ScopeMetadataResolver;
import infra.context.support.GenericApplicationContext;
import infra.lang.Assert;
import infra.stereotype.Component;
import infra.util.ClassUtils;
import infra.util.StringUtils;
import infra.web.mock.ContextLoader;
import infra.web.mock.WebApplicationContext;

/**
 * {@link WebApplicationContext WebApplicationContext}
 * implementation which accepts <em>component classes</em> as input &mdash; in particular
 * {@link Configuration @Configuration}
 * classes, but also plain {@link Component @Component}
 * classes as well as JSR-330 compliant classes using {@code jakarta.inject} annotations.
 *
 * <p>Allows for registering classes one by one (specifying class names as config
 * locations) as well as via classpath scanning (specifying base packages as config
 * locations).
 *
 * <p>This is essentially the equivalent of
 * {@link AnnotationConfigApplicationContext
 * AnnotationConfigApplicationContext} for a web environment. However, in contrast to
 * {@code AnnotationConfigApplicationContext}, this class does not extend
 * {@link GenericApplicationContext
 * GenericApplicationContext} and therefore does not provide some of the convenient
 * {@code registerBean(...)} methods available in a {@code GenericApplicationContext}.
 * If you wish to register annotated <em>component classes</em> with a
 * {@code GenericApplicationContext} in a web environment, you may use a
 * {@code GenericWebApplicationContext} with an
 * {@link AnnotatedBeanDefinitionReader
 * AnnotatedBeanDefinitionReader}. See the Javadoc for {@link GenericWebApplicationContext}
 * for details and an example.
 *
 * <p>To make use of this application context, the
 * {@linkplain ContextLoader#CONTEXT_CLASS_PARAM "contextClass"} context-param for
 * ContextLoader and/or "contextClass" init-param for FrameworkServlet must be set to
 * the fully-qualified name of this class.
 *
 * <p>As an alternative to setting the "contextConfigLocation" parameter, users may
 * implement an {@link infra.context.ApplicationContextInitializer
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
 * @see AnnotationConfigApplicationContext
 * @see GenericWebApplicationContext
 * @since 4.0 2022/2/20 17:55
 */
public class AnnotationConfigWebApplicationContext extends AbstractRefreshableWebApplicationContext implements AnnotationConfigRegistry {

  @Nullable
  private BeanNameGenerator beanNameGenerator;

  @Nullable
  private ScopeMetadataResolver scopeMetadataResolver;

  private final Set<BeanRegistrar> beanRegistrars = new LinkedHashSet<>();

  private final Set<Class<?>> componentClasses = new LinkedHashSet<>();

  private final Set<String> basePackages = new LinkedHashSet<>();

  /**
   * Set a custom {@link BeanNameGenerator} for use with {@link AnnotatedBeanDefinitionReader}
   * and/or {@link ClassPathBeanDefinitionScanner}.
   * <p>Default is {@link AnnotationBeanNameGenerator}.
   *
   * @see AnnotatedBeanDefinitionReader#setBeanNameGenerator(BeanNameGenerator)
   * @see ClassPathBeanDefinitionScanner#setBeanNameGenerator(BeanNameGenerator)
   * @see BootstrapContext#setBeanNameGenerator(BeanNameGenerator)
   */
  public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
    this.beanNameGenerator = beanNameGenerator;
    getBootstrapContext().setBeanNameGenerator(beanNameGenerator);
  }

  /**
   * Return the custom {@link BeanNameGenerator} for use with {@link AnnotatedBeanDefinitionReader}
   * and/or {@link ClassPathBeanDefinitionScanner}, if any.
   */
  @Nullable
  protected BeanNameGenerator getBeanNameGenerator() {
    return this.beanNameGenerator;
  }

  /**
   * Set a custom {@link ScopeMetadataResolver} for use with {@link AnnotatedBeanDefinitionReader}
   * and/or {@link ClassPathBeanDefinitionScanner}.
   * <p>Default is an {@link AnnotationScopeMetadataResolver}.
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
   * e.g. {@link Configuration @Configuration} classes
   * @see #scan(String...)
   * @see #loadBeanDefinitions(StandardBeanFactory)
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
   * @see #loadBeanDefinitions(StandardBeanFactory)
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
   * Invoke the given registrars for registering their beans with this
   * application context.
   * <p>Note that {@link #refresh()} must be called in order for the context
   * to fully process the new classes.
   *
   * @param registrars one or more {@link BeanRegistrar} instances
   * @since 5.0
   */
  @Override
  public void register(BeanRegistrar... registrars) {
    Assert.notEmpty(registrars, "At least one BeanRegistrar must be specified");
    Collections.addAll(this.beanRegistrars, registrars);
  }

  /**
   * Register a {@link BeanDefinition} for
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
  protected void loadBeanDefinitions(StandardBeanFactory beanFactory) {
    AnnotatedBeanDefinitionReader reader = getAnnotatedBeanDefinitionReader(beanFactory);
    ClassPathBeanDefinitionScanner scanner = getClassPathBeanDefinitionScanner(beanFactory);

    BeanNameGenerator nameGenerator = getBeanNameGenerator();
    if (nameGenerator != null) {
      reader.setBeanNameGenerator(nameGenerator);
      scanner.setBeanNameGenerator(nameGenerator);
      beanFactory.registerSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, nameGenerator);
    }

    ScopeMetadataResolver scopeMetadataResolver = getScopeMetadataResolver();
    if (scopeMetadataResolver != null) {
      reader.setScopeMetadataResolver(scopeMetadataResolver);
      scanner.setScopeMetadataResolver(scopeMetadataResolver);
    }

    if (!this.beanRegistrars.isEmpty()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Applying bean registrars: [" +
                StringUtils.collectionToCommaDelimitedString(this.beanRegistrars) + "]");
      }
      for (BeanRegistrar registrar : this.beanRegistrars) {
        new BeanRegistryAdapter(beanFactory, getEnvironment(), registrar.getClass()).register(registrar);
      }
    }

    if (!componentClasses.isEmpty()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Registering component classes: [{}]", StringUtils.collectionToCommaDelimitedString(componentClasses));
      }
      reader.register(ClassUtils.toClassArray(componentClasses));
    }

    if (!basePackages.isEmpty()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Scanning base packages: [{}]", StringUtils.collectionToCommaDelimitedString(basePackages));
      }
      scanner.scan(StringUtils.toStringArray(this.basePackages));
    }

    String[] configLocations = getConfigLocations();
    if (configLocations != null) {
      for (String configLocation : configLocations) {
        try {
          Class<?> clazz = ClassUtils.forName(configLocation, getClassLoader());
          if (logger.isTraceEnabled()) {
            logger.trace("Registering [{}]", configLocation);
          }
          reader.register(clazz);
        }
        catch (ClassNotFoundException ex) {
          if (logger.isTraceEnabled()) {
            logger.trace("Could not load class for config location [{}] - trying package scan. {}",
                    configLocation, ex.toString());
          }
          int count = scanner.scan(configLocation);
          if (count == 0 && logger.isDebugEnabled()) {
            logger.debug("No component classes found for specified class/package [{}]", configLocation);
          }
        }
      }
    }
  }

  /**
   * Build an {@link AnnotatedBeanDefinitionReader} for the given bean factory.
   * <p>This should be pre-configured with the {@code Environment} (if desired)
   * but not with a {@code BeanNameGenerator} or {@code ScopeMetadataResolver} yet.
   *
   * @param beanFactory the bean factory to load bean definitions into
   * @see #getEnvironment()
   * @see #getBeanNameGenerator()
   * @see #getScopeMetadataResolver()
   */
  protected AnnotatedBeanDefinitionReader getAnnotatedBeanDefinitionReader(StandardBeanFactory beanFactory) {
    return new AnnotatedBeanDefinitionReader(beanFactory, getEnvironment());
  }

  /**
   * Build a {@link ClassPathBeanDefinitionScanner} for the given bean factory.
   * <p>This should be pre-configured with the {@code Environment} (if desired)
   * but not with a {@code BeanNameGenerator} or {@code ScopeMetadataResolver} yet.
   *
   * @param beanFactory the bean factory to load bean definitions into
   * @see #getEnvironment()
   * @see #getBeanNameGenerator()
   * @see #getScopeMetadataResolver()
   */
  protected ClassPathBeanDefinitionScanner getClassPathBeanDefinitionScanner(StandardBeanFactory beanFactory) {
    return new ClassPathBeanDefinitionScanner(beanFactory, true, getEnvironment());
  }

}
