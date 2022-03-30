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

package cn.taketoday.framework.builder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.beans.factory.support.AbstractAutowireCapableBeanFactory;
import cn.taketoday.beans.factory.support.BeanNameGenerator;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.format.support.ApplicationConversionService;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.ApplicationContextFactory;
import cn.taketoday.framework.ApplicationType;
import cn.taketoday.framework.Banner;
import cn.taketoday.framework.BootstrapRegistry;
import cn.taketoday.framework.BootstrapRegistryInitializer;
import cn.taketoday.util.StringUtils;

/**
 * Builder for {@link Application} and {@link ApplicationContext} instances with
 * convenient fluent API and context hierarchy support. Simple example of a context
 * hierarchy:
 *
 * <pre class="code">
 * new ApplicationBuilder(ParentConfig.class).child(ChildConfig.class).run(args);
 * </pre>
 *
 * Another common use case is setting active profiles and default properties to set up the
 * environment for an application:
 *
 * <pre class="code">
 * new ApplicationBuilder(Application.class).profiles(&quot;server&quot;)
 * 		.properties(&quot;transport=local&quot;).run(args);
 * </pre>
 *
 * <p>
 * If your needs are simpler, consider using the static convenience methods in
 * Application instead.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Application
 * @since 4.0 2022/3/29 17:46
 */
public class ApplicationBuilder {

  private final Application application;

  private ConfigurableApplicationContext context;

  private ApplicationBuilder parent;

  private final AtomicBoolean running = new AtomicBoolean();

  private final Set<Class<?>> sources = new LinkedHashSet<>();

  private final Map<String, Object> defaultProperties = new LinkedHashMap<>();

  private ConfigurableEnvironment environment;

  private Set<String> additionalProfiles = new LinkedHashSet<>();

  private boolean registerShutdownHookApplied;

  private boolean configuredAsChild = false;

  public ApplicationBuilder(Class<?>... sources) {
    this(null, sources);
  }

  public ApplicationBuilder(ResourceLoader resourceLoader, Class<?>... sources) {
    this.application = createSpringApplication(resourceLoader, sources);
  }

  /**
   * Creates a new {@link Application} instance from the given sources using the
   * given {@link ResourceLoader}. Subclasses may override in order to provide a custom
   * subclass of {@link Application}.
   *
   * @param resourceLoader the resource loader (can be null)
   * @param sources the sources
   * @return the {@link Application} instance
   */
  protected Application createSpringApplication(ResourceLoader resourceLoader, Class<?>... sources) {
    return new Application(resourceLoader, sources);
  }

  /**
   * Accessor for the current application context.
   *
   * @return the current application context (or null if not yet running)
   */
  public ConfigurableApplicationContext context() {
    return this.context;
  }

  /**
   * Accessor for the current application.
   *
   * @return the current application (never null)
   */
  public Application application() {
    return this.application;
  }

  /**
   * Create an application context (and its parent if specified) with the command line
   * args provided. The parent is run first with the same arguments if has not yet been
   * started.
   *
   * @param args the command line arguments
   * @return an application context created from the current state
   */
  public ConfigurableApplicationContext run(String... args) {
    if (this.running.get()) {
      // If already created we just return the existing context
      return this.context;
    }
    configureAsChildIfNecessary(args);
    if (this.running.compareAndSet(false, true)) {
      synchronized(this.running) {
        // If not already running copy the sources over and then run.
        this.context = build().run(args);
      }
    }
    return this.context;
  }

  private void configureAsChildIfNecessary(String... args) {
    if (this.parent != null && !this.configuredAsChild) {
      this.configuredAsChild = true;
      if (!this.registerShutdownHookApplied) {
        this.application.setRegisterShutdownHook(false);
      }
      initializers(new ParentContextApplicationContextInitializer(this.parent.run(args)));
    }
  }

  /**
   * Returns a fully configured {@link Application} that is ready to run.
   *
   * @return the fully configured {@link Application}.
   */
  public Application build() {
    return build(new String[0]);
  }

  /**
   * Returns a fully configured {@link Application} that is ready to run. Any
   * parent that has been configured will be run with the given {@code args}.
   *
   * @param args the parent's args
   * @return the fully configured {@link Application}.
   */
  public Application build(String... args) {
    configureAsChildIfNecessary(args);
    this.application.addPrimarySources(this.sources);
    return this.application;
  }

  /**
   * Create a child application with the provided sources. Default args and environment
   * are copied down into the child, but everything else is a clean sheet.
   *
   * @param sources the sources for the application (Spring configuration)
   * @return the child application builder
   */
  public ApplicationBuilder child(Class<?>... sources) {
    ApplicationBuilder child = new ApplicationBuilder();
    child.sources(sources);

    // Copy environment stuff from parent to child
    child.properties(this.defaultProperties)
            .environment(this.environment)
            .additionalProfiles(this.additionalProfiles);
    child.parent = this;

    // It's not possible if embedded web server are enabled to support web contexts as
    // parents because the servlets cannot be initialized at the right point in
    // lifecycle.
    web(ApplicationType.NONE_WEB);

    // Probably not interested in multiple banners
    bannerMode(Banner.Mode.OFF);

    // Make sure sources get copied over
    this.application.addPrimarySources(this.sources);

    return child;
  }

  /**
   * Add a parent application with the provided sources. Default args and environment
   * are copied up into the parent, but everything else is a clean sheet.
   *
   * @param sources the sources for the application (Spring configuration)
   * @return the parent builder
   */
  public ApplicationBuilder parent(Class<?>... sources) {
    if (this.parent == null) {
      this.parent = new ApplicationBuilder(sources).web(ApplicationType.NONE_WEB)
              .properties(this.defaultProperties).environment(this.environment);
    }
    else {
      this.parent.sources(sources);
    }
    return this.parent;
  }

  private ApplicationBuilder runAndExtractParent(String... args) {
    if (this.context == null) {
      run(args);
    }
    if (this.parent != null) {
      return this.parent;
    }
    throw new IllegalStateException(
            "No parent defined yet (please use the other overloaded parent methods to set one)");
  }

  /**
   * Add an already running parent context to an existing application.
   *
   * @param parent the parent context
   * @return the current builder (not the parent)
   */
  public ApplicationBuilder parent(ConfigurableApplicationContext parent) {
    this.parent = new ApplicationBuilder();
    this.parent.context = parent;
    this.parent.running.set(true);
    return this;
  }

  /**
   * Create a sibling application (one with the same parent). A side effect of calling
   * this method is that the current application (and its parent) are started without
   * any arguments if they are not already running. To supply arguments when starting
   * the current application and its parent use {@link #sibling(Class[], String...)}
   * instead.
   *
   * @param sources the sources for the application (Spring configuration)
   * @return the new sibling builder
   */
  public ApplicationBuilder sibling(Class<?>... sources) {
    return runAndExtractParent().child(sources);
  }

  /**
   * Create a sibling application (one with the same parent). A side effect of calling
   * this method is that the current application (and its parent) are started if they
   * are not already running.
   *
   * @param sources the sources for the application (Spring configuration)
   * @param args the command line arguments to use when starting the current app and its
   * parent
   * @return the new sibling builder
   */
  public ApplicationBuilder sibling(Class<?>[] sources, String... args) {
    return runAndExtractParent(args).child(sources);
  }

  /**
   * Explicitly set the factory used to create the application context.
   *
   * @param factory the factory to use
   * @return the current builder
   */
  public ApplicationBuilder contextFactory(ApplicationContextFactory factory) {
    this.application.setApplicationContextFactory(factory);
    return this;
  }

  /**
   * Add more sources (configuration classes and components) to this application.
   *
   * @param sources the sources to add
   * @return the current builder
   */
  public ApplicationBuilder sources(Class<?>... sources) {
    this.sources.addAll(new LinkedHashSet<>(Arrays.asList(sources)));
    return this;
  }

  /**
   * Flag to explicitly request a specific type of web application. Auto-detected based
   * on the classpath if not set.
   *
   * @param ApplicationType the type of web application
   * @return the current builder
   */
  public ApplicationBuilder web(ApplicationType ApplicationType) {
    this.application.setApplicationType(ApplicationType);
    return this;
  }

  /**
   * Flag to indicate the startup information should be logged.
   *
   * @param logStartupInfo the flag to set. Default true.
   * @return the current builder
   */
  public ApplicationBuilder logStartupInfo(boolean logStartupInfo) {
    this.application.setLogStartupInfo(logStartupInfo);
    return this;
  }

  /**
   * Sets the {@link Banner} instance which will be used to print the banner when no
   * static banner file is provided.
   *
   * @param banner the banner to use
   * @return the current builder
   */
  public ApplicationBuilder banner(Banner banner) {
    this.application.setBanner(banner);
    return this;
  }

  public ApplicationBuilder bannerMode(Banner.Mode bannerMode) {
    this.application.setBannerMode(bannerMode);
    return this;
  }

  /**
   * Sets if the application is headless and should not instantiate AWT. Defaults to
   * {@code true} to prevent java icons appearing.
   *
   * @param headless if the application is headless
   * @return the current builder
   */
  public ApplicationBuilder headless(boolean headless) {
    this.application.setHeadless(headless);
    return this;
  }

  /**
   * Sets if the created {@link ApplicationContext} should have a shutdown hook
   * registered.
   *
   * @param registerShutdownHook if the shutdown hook should be registered
   * @return the current builder
   */
  public ApplicationBuilder registerShutdownHook(boolean registerShutdownHook) {
    this.registerShutdownHookApplied = true;
    this.application.setRegisterShutdownHook(registerShutdownHook);
    return this;
  }

  /**
   * Fixes the main application class that is used to anchor the startup messages.
   *
   * @param mainApplicationClass the class to use.
   * @return the current builder
   */
  public ApplicationBuilder main(Class<?> mainApplicationClass) {
    this.application.setMainApplicationClass(mainApplicationClass);
    return this;
  }

  /**
   * Flag to indicate that command line arguments should be added to the environment.
   *
   * @param addCommandLineProperties the flag to set. Default true.
   * @return the current builder
   */
  public ApplicationBuilder addCommandLineProperties(boolean addCommandLineProperties) {
    this.application.setAddCommandLineProperties(addCommandLineProperties);
    return this;
  }

  /**
   * Flag to indicate if the {@link ApplicationConversionService} should be added to the
   * application context's {@link Environment}.
   *
   * @param addConversionService if the conversion service should be added.
   * @return the current builder
   */
  public ApplicationBuilder setAddConversionService(boolean addConversionService) {
    this.application.setAddConversionService(addConversionService);
    return this;
  }

  /**
   * Adds {@link BootstrapRegistryInitializer} instances that can be used to initialize
   * the {@link BootstrapRegistry}.
   *
   * @param bootstrapRegistryInitializer the bootstrap registry initializer to add
   * @return the current builder
   */
  public ApplicationBuilder addBootstrapRegistryInitializer(
          BootstrapRegistryInitializer bootstrapRegistryInitializer) {
    this.application.addBootstrapRegistryInitializer(bootstrapRegistryInitializer);
    return this;
  }

  /**
   * Flag to control whether the application should be initialized lazily.
   *
   * @param lazyInitialization the flag to set. Defaults to false.
   * @return the current builder
   */
  public ApplicationBuilder lazyInitialization(boolean lazyInitialization) {
    this.application.setLazyInitialization(lazyInitialization);
    return this;
  }

  /**
   * Default properties for the environment in the form {@code key=value} or
   * {@code key:value}. Multiple calls to this method are cumulative and will not clear
   * any previously set properties.
   *
   * @param defaultProperties the properties to set.
   * @return the current builder
   * @see ApplicationBuilder#properties(Properties)
   * @see ApplicationBuilder#properties(Map)
   */
  public ApplicationBuilder properties(String... defaultProperties) {
    return properties(getMapFromKeyValuePairs(defaultProperties));
  }

  private Map<String, Object> getMapFromKeyValuePairs(String[] properties) {
    Map<String, Object> map = new HashMap<>();
    for (String property : properties) {
      int index = lowestIndexOf(property, ":", "=");
      String key = (index > 0) ? property.substring(0, index) : property;
      String value = (index > 0) ? property.substring(index + 1) : "";
      map.put(key, value);
    }
    return map;
  }

  private int lowestIndexOf(String property, String... candidates) {
    int index = -1;
    for (String candidate : candidates) {
      int candidateIndex = property.indexOf(candidate);
      if (candidateIndex > 0) {
        index = (index != -1) ? Math.min(index, candidateIndex) : candidateIndex;
      }
    }
    return index;
  }

  /**
   * Default properties for the environment.Multiple calls to this method are cumulative
   * and will not clear any previously set properties.
   *
   * @param defaultProperties the properties to set.
   * @return the current builder
   * @see ApplicationBuilder#properties(String...)
   * @see ApplicationBuilder#properties(Map)
   */
  public ApplicationBuilder properties(Properties defaultProperties) {
    return properties(getMapFromProperties(defaultProperties));
  }

  private Map<String, Object> getMapFromProperties(Properties properties) {
    Map<String, Object> map = new HashMap<>();
    for (Object key : Collections.list(properties.propertyNames())) {
      map.put((String) key, properties.get(key));
    }
    return map;
  }

  /**
   * Default properties for the environment. Multiple calls to this method are
   * cumulative and will not clear any previously set properties.
   *
   * @param defaults the default properties
   * @return the current builder
   * @see ApplicationBuilder#properties(String...)
   * @see ApplicationBuilder#properties(Properties)
   */
  public ApplicationBuilder properties(Map<String, Object> defaults) {
    this.defaultProperties.putAll(defaults);
    this.application.setDefaultProperties(this.defaultProperties);
    if (this.parent != null) {
      this.parent.properties(this.defaultProperties);
      this.parent.environment(this.environment);
    }
    return this;
  }

  /**
   * Add to the active Spring profiles for this app (and its parent and children).
   *
   * @param profiles the profiles to add.
   * @return the current builder
   */
  public ApplicationBuilder profiles(String... profiles) {
    this.additionalProfiles.addAll(Arrays.asList(profiles));
    this.application.setAdditionalProfiles(StringUtils.toStringArray(this.additionalProfiles));
    return this;
  }

  private ApplicationBuilder additionalProfiles(Collection<String> additionalProfiles) {
    this.additionalProfiles = new LinkedHashSet<>(additionalProfiles);
    this.application.setAdditionalProfiles(StringUtils.toStringArray(this.additionalProfiles));
    return this;
  }

  /**
   * Bean name generator for automatically generated bean names in the application
   * context.
   *
   * @param beanNameGenerator the generator to set.
   * @return the current builder
   */
  public ApplicationBuilder beanNameGenerator(BeanNameGenerator beanNameGenerator) {
    this.application.setBeanNameGenerator(beanNameGenerator);
    return this;
  }

  /**
   * Environment for the application context.
   *
   * @param environment the environment to set.
   * @return the current builder
   */
  public ApplicationBuilder environment(ConfigurableEnvironment environment) {
    this.application.setEnvironment(environment);
    this.environment = environment;
    return this;
  }

  /**
   * Prefix that should be applied when obtaining configuration properties from the
   * system environment.
   *
   * @param environmentPrefix the environment property prefix to set
   * @return the current builder
   */
  public ApplicationBuilder environmentPrefix(String environmentPrefix) {
    this.application.setEnvironmentPrefix(environmentPrefix);
    return this;
  }

  /**
   * {@link ResourceLoader} for the application context. If a custom class loader is
   * needed, this is where it would be added.
   *
   * @param resourceLoader the resource loader to set.
   * @return the current builder
   */
  public ApplicationBuilder resourceLoader(ResourceLoader resourceLoader) {
    this.application.setResourceLoader(resourceLoader);
    return this;
  }

  /**
   * Add some initializers to the application (applied to the {@link ApplicationContext}
   * before any bean definitions are loaded).
   *
   * @param initializers some initializers to add
   * @return the current builder
   */
  public ApplicationBuilder initializers(ApplicationContextInitializer... initializers) {
    this.application.addInitializers(initializers);
    return this;
  }

  /**
   * Add some listeners to the application (listening for Application events as
   * well as regular Spring events once the context is running). Any listeners that are
   * also {@link ApplicationContextInitializer} will be added to the
   * {@link #initializers(ApplicationContextInitializer...) initializers} automatically.
   *
   * @param listeners some listeners to add
   * @return the current builder
   */
  public ApplicationBuilder listeners(ApplicationListener<?>... listeners) {
    this.application.addListeners(listeners);
    return this;
  }

  /**
   * Whether to allow circular references between beans and automatically try to resolve
   * them.
   *
   * @param allowCircularReferences whether circular references are allowed
   * @return the current builder
   * @see AbstractAutowireCapableBeanFactory#setAllowCircularReferences(boolean)
   */
  public ApplicationBuilder allowCircularReferences(boolean allowCircularReferences) {
    this.application.setAllowCircularReferences(allowCircularReferences);
    return this;
  }

}
