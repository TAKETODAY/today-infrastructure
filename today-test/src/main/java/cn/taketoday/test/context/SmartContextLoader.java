/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.context;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.context.support.AnnotationConfigContextLoader;
import cn.taketoday.test.context.support.DelegatingSmartContextLoader;
import cn.taketoday.test.context.support.GenericXmlContextLoader;
import cn.taketoday.test.context.web.AnnotationConfigWebContextLoader;
import cn.taketoday.test.context.web.GenericXmlWebContextLoader;
import cn.taketoday.test.context.web.WebDelegatingSmartContextLoader;

/**
 * Strategy interface for loading an {@link ApplicationContext application context}
 * for an integration test managed by the TestContext Framework.
 *
 * <p>The {@code SmartContextLoader} SPI supersedes the {@link ContextLoader} SPI
 * introduced a {@code SmartContextLoader} can choose to process
 * either resource locations or annotated classes. Furthermore, a
 * {@code SmartContextLoader} can set active bean definition profiles in the
 * context that it loads (see {@link MergedContextConfiguration#getActiveProfiles()}
 * and {@link #loadContext(MergedContextConfiguration)}).
 *
 * <p>See the Javadoc for {@link ContextConfiguration @ContextConfiguration}
 * for a definition of <em>annotated class</em>.
 *
 * <p>Clients of a {@code SmartContextLoader} should call
 * {@link #processContextConfiguration(ContextConfigurationAttributes)
 * processContextConfiguration()} prior to calling
 * {@link #loadContext(MergedContextConfiguration) loadContext()}. This gives a
 * {@code SmartContextLoader} the opportunity to provide custom support for
 * modifying resource locations or detecting default resource locations or
 * default configuration classes. The results of
 * {@link #processContextConfiguration(ContextConfigurationAttributes)
 * processContextConfiguration()} should be merged for all classes in the
 * hierarchy of the root test class and then supplied to
 * {@link #loadContext(MergedContextConfiguration) loadContext()}.
 *
 * <p>Even though {@code SmartContextLoader} extends {@code ContextLoader},
 * clients should favor {@code SmartContextLoader}-specific methods over those
 * defined in {@code ContextLoader}, particularly because a
 * {@code SmartContextLoader} may choose not to support methods defined in the
 * {@code ContextLoader} SPI.
 *
 * <p>Concrete implementations must provide a {@code public} no-args constructor.
 *
 * <p>provides the following out-of-the-box implementations:
 * <ul>
 * <li>{@link DelegatingSmartContextLoader DelegatingSmartContextLoader}</li>
 * <li>{@link AnnotationConfigContextLoader AnnotationConfigContextLoader}</li>
 * <li>{@link GenericXmlContextLoader GenericXmlContextLoader}</li>
 * <li>{@link WebDelegatingSmartContextLoader WebDelegatingSmartContextLoader}</li>
 * <li>{@link AnnotationConfigWebContextLoader AnnotationConfigWebContextLoader}</li>
 * <li>{@link GenericXmlWebContextLoader GenericXmlWebContextLoader}</li>
 * </ul>
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ContextConfiguration
 * @see ActiveProfiles
 * @see ContextConfigurationAttributes
 * @see MergedContextConfiguration
 * @since 4.0
 */
public interface SmartContextLoader extends ContextLoader {

  /**
   * Process the {@link ContextConfigurationAttributes} for a given test class.
   * <p>Concrete implementations may choose to <em>modify</em> the {@code locations}
   * or {@code classes} in the supplied {@code ContextConfigurationAttributes},
   * <em>generate</em> default configuration locations, or <em>detect</em>
   * default configuration classes if the supplied values are {@code null}
   * or empty.
   * <p><b>Note</b>: a {@code SmartContextLoader} must <em>preemptively</em>
   * verify that a generated or detected default actually exists before setting
   * the corresponding {@code locations} or {@code classes} property in the
   * supplied {@code ContextConfigurationAttributes}. Consequently, leaving the
   * {@code locations} or {@code classes} property empty signals that this
   * {@code SmartContextLoader} was not able to generate or detect defaults.
   *
   * @param configAttributes the context configuration attributes to process
   */
  void processContextConfiguration(ContextConfigurationAttributes configAttributes);

  /**
   * Load a new {@link ApplicationContext} based on the supplied
   * {@link MergedContextConfiguration}, configure the context, and return the
   * context in a fully <em>refreshed</em> state.
   * <p>Concrete implementations should register annotation configuration
   * processors with bean factories of
   * {@link ApplicationContext application contexts} loaded by this
   * {@code SmartContextLoader}. Beans will therefore automatically be
   * candidates for annotation-based dependency injection using
   * {@link cn.taketoday.beans.factory.annotation.Autowired @Autowired},
   * {@link jakarta.annotation.Resource @Resource}, and
   * {@link jakarta.inject.Inject @Inject}. In addition, concrete implementations
   * should perform the following actions.
   * <ul>
   * <li>Set the parent {@code ApplicationContext} if appropriate (see
   * {@link MergedContextConfiguration#getParent()}).</li>
   * <li>Set the active bean definition profiles in the context's
   * {@link cn.taketoday.core.env.Environment Environment} (see
   * {@link MergedContextConfiguration#getActiveProfiles()}).</li>
   * <li>Add test {@link cn.taketoday.core.env.PropertySource PropertySources}
   * to the {@code Environment} (see
   * {@link MergedContextConfiguration#getPropertySourceLocations()},
   * {@link MergedContextConfiguration#getPropertySourceProperties()}, and
   * {@link cn.taketoday.test.context.support.TestPropertySourceUtils
   * TestPropertySourceUtils}).</li>
   * <li>Invoke {@link cn.taketoday.context.ApplicationContextInitializer
   * ApplicationContextInitializers} (see
   * {@link MergedContextConfiguration#getContextInitializerClasses()}).</li>
   * <li>Invoke {@link ContextCustomizer ContextCustomizers} (see
   * {@link MergedContextConfiguration#getContextCustomizers()}).</li>
   * <li>Register a JVM shutdown hook for the {@link ApplicationContext}. Unless
   * the context gets closed early, all context instances will be automatically
   * closed on JVM shutdown. This allows for freeing of external resources held
   * by beans within the context &mdash; for example, temporary files.</li>
   * </ul>
   * <p> any exception thrown while attempting to
   * load an {@code ApplicationContext} should be wrapped in a
   * {@link ContextLoadException}. Concrete implementations should therefore
   * contain a try-catch block similar to the following.
   * <pre style="code">
   * ApplicationContext context = // create context
   * try {
   *     // configure and refresh context
   * }
   * catch (Exception ex) {
   *     throw new ContextLoadException(context, ex);
   * }
   * </pre>
   *
   * @param mergedConfig the merged context configuration to use to load the
   * application context
   * @return a new application context
   * @throws ContextLoadException if context loading failed
   * @see #processContextConfiguration(ContextConfigurationAttributes)
   * @see cn.taketoday.context.annotation.AnnotationConfigUtils#registerAnnotationConfigProcessors(cn.taketoday.beans.factory.support.BeanDefinitionRegistry)
   * @see cn.taketoday.context.ConfigurableApplicationContext#getEnvironment()
   */
  ApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception;

  /**
   * {@code SmartContextLoader} does not support deprecated {@link ContextLoader} methods.
   * Call {@link #processContextConfiguration(ContextConfigurationAttributes)} instead.
   *
   * @throws UnsupportedOperationException in this implementation
   */
  @Override
  default String[] processLocations(Class<?> clazz, @Nullable String... locations) {
    throw new UnsupportedOperationException("""
            SmartContextLoader does not support the ContextLoader SPI. \
            Call processContextConfiguration(ContextConfigurationAttributes) instead.""");
  }

  /**
   * {@code SmartContextLoader} does not support deprecated {@link ContextLoader} methods.
   * <p>Call {@link #loadContext(MergedContextConfiguration)} instead.
   *
   * @throws UnsupportedOperationException in this implementation
   */
  @Override
  default ApplicationContext loadContext(String... locations) throws Exception {
    throw new UnsupportedOperationException("""
            SmartContextLoader does not support the ContextLoader SPI. \
            Call loadContext(MergedContextConfiguration) instead.""");
  }

}
