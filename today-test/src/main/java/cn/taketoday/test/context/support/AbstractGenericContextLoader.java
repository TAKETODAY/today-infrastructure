/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.test.context.support;

import java.util.Arrays;

import cn.taketoday.beans.factory.support.BeanDefinitionReader;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigUtils;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.test.context.ContextLoadException;
import cn.taketoday.test.context.ContextLoader;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.context.SmartContextLoader;
import cn.taketoday.test.context.aot.AotContextLoader;

/**
 * Abstract, generic extension of {@link AbstractContextLoader} that loads a
 * {@link GenericApplicationContext}.
 *
 * <ul>
 * <li>If instances of concrete subclasses are invoked via the
 * {@link ContextLoader ContextLoader} SPI, the
 * context will be loaded from the <em>locations</em> provided to
 * {@link #loadContext(String...)}.</li>
 * <li>If instances of concrete subclasses are invoked via the
 * {@link SmartContextLoader SmartContextLoader}
 * SPI, the context will be loaded from the {@link MergedContextConfiguration}
 * provided to {@link #loadContext(MergedContextConfiguration)}. In such
 * cases, a {@code SmartContextLoader} will decide whether to load the context
 * from <em>locations</em> or <em>annotated classes</em>.</li>
 * </ul>
 *
 * <p>Concrete subclasses must provide an appropriate implementation of
 * {@link #createBeanDefinitionReader createBeanDefinitionReader()},
 * potentially overriding {@link #loadBeanDefinitions loadBeanDefinitions()}
 * as well.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #loadContext(MergedContextConfiguration)
 * @see #loadContext(String...)
 * @since 4.0
 */
public abstract class AbstractGenericContextLoader extends AbstractContextLoader implements AotContextLoader {

  protected static final Logger log = LoggerFactory.getLogger(AbstractGenericContextLoader.class);

  /**
   * Load a {@link GenericApplicationContext} for the supplied
   * {@link MergedContextConfiguration}.
   * <p>Implementation details:
   * <ul>
   * <li>Calls {@link #validateMergedContextConfiguration(MergedContextConfiguration)}
   * to allow subclasses to validate the supplied configuration before proceeding.</li>
   * <li>Calls {@link #createContext()} to create a {@link GenericApplicationContext}
   * instance.</li>
   * <li>If the supplied {@code MergedContextConfiguration} references a
   * {@linkplain MergedContextConfiguration#getParent() parent configuration},
   * the corresponding {@link MergedContextConfiguration#getParentApplicationContext()
   * ApplicationContext} will be retrieved and
   * {@linkplain GenericApplicationContext#setParent(ApplicationContext) set as the parent}
   * for the context created by this method.</li>
   * <li>Calls {@link #prepareContext(GenericApplicationContext)} for backwards
   * compatibility with the {@link cn.taketoday.test.context.ContextLoader
   * ContextLoader} SPI.</li>
   * <li>Calls {@link #prepareContext(ConfigurableApplicationContext, MergedContextConfiguration)}
   * to allow for customizing the context before bean definitions are loaded.</li>
   * <li>Calls {@link #customizeBeanFactory(StandardBeanFactory)} to allow for customizing the
   * context's {@code StandardBeanFactory}.</li>
   * <li>Delegates to {@link #loadBeanDefinitions(GenericApplicationContext, MergedContextConfiguration)}
   * to populate the context from the locations or classes in the supplied
   * {@code MergedContextConfiguration}.</li>
   * <li>Delegates to {@link AnnotationConfigUtils} for
   * {@link AnnotationConfigUtils#registerAnnotationConfigProcessors registering}
   * annotation configuration processors.</li>
   * <li>Calls {@link #customizeContext(GenericApplicationContext)} to allow for customizing the context
   * before it is refreshed.</li>
   * <li>Calls {@link #customizeContext(ConfigurableApplicationContext, MergedContextConfiguration)} to
   * allow for customizing the context before it is refreshed.</li>
   * <li>{@link ConfigurableApplicationContext#refresh Refreshes} the
   * context and registers a JVM shutdown hook for it.</li>
   * </ul>
   *
   * @param mergedConfig the merged context configuration to use to load the
   * application context
   * @return a new application context
   * @see cn.taketoday.test.context.SmartContextLoader#loadContext(MergedContextConfiguration)
   */
  @Override
  public final ApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception {
    return loadContext(mergedConfig, false);
  }

  /**
   * Load a {@link GenericApplicationContext} for AOT build-time processing based
   * on the supplied {@link MergedContextConfiguration}.
   * <p>In contrast to {@link #loadContext(MergedContextConfiguration)}, this
   * method does not
   * {@linkplain cn.taketoday.context.ConfigurableApplicationContext#refresh()
   * refresh} the {@code ApplicationContext} or
   * {@linkplain cn.taketoday.context.ConfigurableApplicationContext#registerShutdownHook()
   * register a JVM shutdown hook} for it. Otherwise, this method implements
   * behavior identical to {@link #loadContext(MergedContextConfiguration)}.
   *
   * @param mergedConfig the merged context configuration to use to load the
   * application context
   * @return a new application context
   * @throws Exception if context loading failed
   * @see AotContextLoader#loadContextForAotProcessing(MergedContextConfiguration)
   */
  @Override
  public final GenericApplicationContext loadContextForAotProcessing(MergedContextConfiguration mergedConfig)
          throws Exception {
    return loadContext(mergedConfig, true);
  }

  /**
   * Load a {@link GenericApplicationContext} for AOT run-time execution based on
   * the supplied {@link MergedContextConfiguration} and
   * {@link ApplicationContextInitializer}.
   *
   * @param mergedConfig the merged context configuration to use to load the
   * application context
   * @param initializer the {@code ApplicationContextInitializer} that should
   * be applied to the context in order to recreate bean definitions
   * @return a new application context
   * @throws Exception if context loading failed
   * @see AotContextLoader#loadContextForAotRuntime(MergedContextConfiguration, ApplicationContextInitializer)
   */
  @Override
  public final GenericApplicationContext loadContextForAotRuntime(MergedContextConfiguration mergedConfig,
          ApplicationContextInitializer initializer) throws Exception {

    Assert.notNull(mergedConfig, "MergedContextConfiguration is required");
    Assert.notNull(initializer, "ApplicationContextInitializer is required");

    if (log.isTraceEnabled()) {
      log.trace("Loading ApplicationContext for AOT runtime for " + mergedConfig);
    }
    else if (log.isDebugEnabled()) {
      log.debug("Loading ApplicationContext for AOT runtime for test class " +
              mergedConfig.getTestClass().getName());
    }

    validateMergedContextConfiguration(mergedConfig);

    GenericApplicationContext context = createContext();
    try {
      prepareContext(context);
      prepareContext(context, mergedConfig);
      initializer.initialize(context);
      customizeContext(context);
      customizeContext(context, mergedConfig);
      context.refresh();
      return context;
    }
    catch (Exception ex) {
      throw new ContextLoadException(context, ex);
    }
  }

  /**
   * Load a {@link GenericApplicationContext} for the supplied
   * {@link MergedContextConfiguration}.
   *
   * @param mergedConfig the merged context configuration to use to load the
   * application context
   * @param forAotProcessing {@code true} if the context is being loaded for
   * AOT processing, meaning not to refresh the {@code ApplicationContext} or
   * register a JVM shutdown hook for it
   * @return a new application context
   */
  private GenericApplicationContext loadContext(
          MergedContextConfiguration mergedConfig, boolean forAotProcessing) throws Exception {

    if (log.isTraceEnabled()) {
      log.trace("Loading ApplicationContext %sfor %s".formatted(
              (forAotProcessing ? "for AOT processing " : ""), mergedConfig));
    }
    else if (log.isDebugEnabled()) {
      log.debug("Loading ApplicationContext %sfor test class %s".formatted(
              (forAotProcessing ? "for AOT processing " : ""), mergedConfig.getTestClass().getName()));
    }

    validateMergedContextConfiguration(mergedConfig);

    GenericApplicationContext context = createContext();
    try {
      ApplicationContext parent = mergedConfig.getParentApplicationContext();
      if (parent != null) {
        context.setParent(parent);
      }

      prepareContext(context);
      prepareContext(context, mergedConfig);
      customizeBeanFactory(context.getBeanFactory());
      loadBeanDefinitions(context, mergedConfig);
      AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
      customizeContext(context);
      customizeContext(context, mergedConfig);

      if (!forAotProcessing) {
        context.refresh();
        context.registerShutdownHook();
      }

      return context;
    }
    catch (Exception ex) {
      throw new ContextLoadException(context, ex);
    }
  }

  /**
   * Validate the supplied {@link MergedContextConfiguration} with respect to
   * what this context loader supports.
   * <p>The default implementation is a <em>no-op</em> but can be overridden by
   * subclasses as appropriate.
   *
   * @param mergedConfig the merged configuration to validate
   * @throws IllegalStateException if the supplied configuration is not valid
   * for this context loader
   */
  protected void validateMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
    // no-op
  }

  /**
   * Load a Infra ApplicationContext from the supplied {@code locations}.
   * <p>Implementation details:
   * <ul>
   * <li>Calls {@link #createContext()} to create a {@link GenericApplicationContext}
   * instance.</li>
   * <li>Calls {@link #prepareContext(GenericApplicationContext)} to allow for customizing the context
   * before bean definitions are loaded.</li>
   * <li>Calls {@link #customizeBeanFactory(StandardBeanFactory)} to allow for customizing the
   * context's {@code StandardBeanFactory}.</li>
   * <li>Delegates to {@link #createBeanDefinitionReader(GenericApplicationContext)} to create a
   * {@link BeanDefinitionReader} which is then used to populate the context
   * from the specified locations.</li>
   * <li>Delegates to {@link AnnotationConfigUtils} for
   * {@link AnnotationConfigUtils#registerAnnotationConfigProcessors registering}
   * annotation configuration processors.</li>
   * <li>Calls {@link #customizeContext(GenericApplicationContext)} to allow for customizing the context
   * before it is refreshed.</li>
   * <li>{@link ConfigurableApplicationContext#refresh Refreshes} the
   * context and registers a JVM shutdown hook for it.</li>
   * </ul>
   * <p><b>Note</b>: this method does not provide a means to set active bean definition
   * profiles for the loaded context. See {@link #loadContext(MergedContextConfiguration)}
   * and {@link AbstractContextLoader#prepareContext(ConfigurableApplicationContext, MergedContextConfiguration)}
   * for an alternative.
   *
   * @return a new application context
   * @see cn.taketoday.test.context.ContextLoader#loadContext
   * @see GenericApplicationContext
   * @see #loadContext(MergedContextConfiguration)
   */
  @Override
  public final ConfigurableApplicationContext loadContext(String... locations) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("Loading ApplicationContext for locations {}", Arrays.toString(locations));
    }

    GenericApplicationContext context = createContext();

    prepareContext(context);
    customizeBeanFactory(context.getBeanFactory());
    createBeanDefinitionReader(context).loadBeanDefinitions(locations);
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    customizeContext(context);

    context.refresh();
    context.registerShutdownHook();

    return context;
  }

  /**
   * Factory method for creating the {@link GenericApplicationContext} used by
   * this {@code ContextLoader}.
   * <p>The default implementation creates a {@code GenericApplicationContext}
   * using the default constructor. This method may be overridden &mdash; for
   * example, to use a custom context subclass or to create a
   * {@code GenericApplicationContext} with a custom
   * {@link StandardBeanFactory} implementation.
   *
   * @return a newly instantiated {@code GenericApplicationContext}
   */
  protected GenericApplicationContext createContext() {
    return new GenericApplicationContext();
  }

  /**
   * Prepare the {@link GenericApplicationContext} created by this {@code ContextLoader}.
   * Called <i>before</i> bean definitions are read.
   * <p>The default implementation is empty. Can be overridden in subclasses to
   * customize {@code GenericApplicationContext}'s standard settings.
   *
   * @param context the context that should be prepared
   * @see #loadContext(MergedContextConfiguration)
   * @see GenericApplicationContext#setAllowBeanDefinitionOverriding
   * @see GenericApplicationContext#setResourceLoader
   * @see GenericApplicationContext#setId
   * @see #prepareContext(ConfigurableApplicationContext, MergedContextConfiguration)
   */
  protected void prepareContext(GenericApplicationContext context) {

  }

  /**
   * Customize the internal bean factory of the ApplicationContext created by
   * this {@code ContextLoader}.
   * <p>The default implementation is empty but can be overridden in subclasses
   * to customize {@code StandardBeanFactory}'s standard settings.
   *
   * @param beanFactory the bean factory created by this {@code ContextLoader}
   * @see #loadContext(MergedContextConfiguration)
   * @see StandardBeanFactory#setAllowBeanDefinitionOverriding
   * @see StandardBeanFactory#setAllowEagerClassLoading
   * @see StandardBeanFactory#setAllowCircularReferences
   * @see StandardBeanFactory#setAllowRawInjectionDespiteWrapping
   */
  protected void customizeBeanFactory(StandardBeanFactory beanFactory) {

  }

  /**
   * Load bean definitions into the supplied {@link GenericApplicationContext context}
   * from the locations or classes in the supplied {@code MergedContextConfiguration}.
   * <p>The default implementation delegates to the {@link BeanDefinitionReader}
   * returned by {@link #createBeanDefinitionReader(GenericApplicationContext)} to
   * {@link BeanDefinitionReader#loadBeanDefinitions(String) load} the
   * bean definitions.
   * <p>Subclasses must provide an appropriate implementation of
   * {@link #createBeanDefinitionReader(GenericApplicationContext)}. Alternatively subclasses
   * may provide a <em>no-op</em> implementation of {@code createBeanDefinitionReader()}
   * and override this method to provide a custom strategy for loading or
   * registering bean definitions.
   *
   * @param context the context into which the bean definitions should be loaded
   * @param mergedConfig the merged context configuration
   * @see #loadContext(MergedContextConfiguration)
   */
  protected void loadBeanDefinitions(GenericApplicationContext context, MergedContextConfiguration mergedConfig) {
    createBeanDefinitionReader(context).loadBeanDefinitions(mergedConfig.getLocations());
  }

  /**
   * Factory method for creating a new {@link BeanDefinitionReader} for loading
   * bean definitions into the supplied {@link GenericApplicationContext context}.
   *
   * @param context the context for which the {@code BeanDefinitionReader}
   * should be created
   * @return a {@code BeanDefinitionReader} for the supplied context
   * @see #loadContext(MergedContextConfiguration)
   * @see #loadBeanDefinitions
   * @see BeanDefinitionReader
   */
  protected abstract BeanDefinitionReader createBeanDefinitionReader(GenericApplicationContext context);

  /**
   * Customize the {@link GenericApplicationContext} created by this
   * {@code ContextLoader} <i>after</i> bean definitions have been
   * loaded into the context but <i>before</i> the context is refreshed.
   * <p>The default implementation is empty but can be overridden in subclasses
   * to customize the application context.
   *
   * @param context the newly created application context
   * @see #loadContext(MergedContextConfiguration)
   * @see #customizeContext(ConfigurableApplicationContext, MergedContextConfiguration)
   */
  protected void customizeContext(GenericApplicationContext context) {

  }

}
