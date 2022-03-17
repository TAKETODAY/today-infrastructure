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

package cn.taketoday.test.context;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.test.context.support.AnnotationConfigContextLoader;
import cn.taketoday.test.context.support.DelegatingSmartContextLoader;
import cn.taketoday.test.context.support.GenericXmlContextLoader;
import cn.taketoday.test.context.web.AnnotationConfigWebContextLoader;
import cn.taketoday.test.context.web.GenericXmlWebContextLoader;
import cn.taketoday.test.context.web.WebDelegatingSmartContextLoader;

/**
 * Strategy interface for loading an {@link ApplicationContext application context}
 * for an integration test managed by the Spring TestContext Framework.
 *
 * <p>The {@code SmartContextLoader} SPI supersedes the {@link ContextLoader} SPI
 * introduced in Spring 2.5: a {@code SmartContextLoader} can choose to process
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
 * <p>Spring provides the following out-of-the-box implementations:
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
 * @see ContextConfiguration
 * @see ActiveProfiles
 * @see ContextConfigurationAttributes
 * @see MergedContextConfiguration
 *@since 4.0
 */
public interface SmartContextLoader extends ContextLoader {

  /**
   * Processes the {@link ContextConfigurationAttributes} for a given test class.
   * <p>Concrete implementations may choose to <em>modify</em> the {@code locations}
   * or {@code classes} in the supplied {@link ContextConfigurationAttributes},
   * <em>generate</em> default configuration locations, or <em>detect</em>
   * default configuration classes if the supplied values are {@code null}
   * or empty.
   * <p><b>Note</b>: in contrast to a standard {@code ContextLoader}, a
   * {@code SmartContextLoader} <b>must</b> <em>preemptively</em> verify that
   * a generated or detected default actually exists before setting the corresponding
   * {@code locations} or {@code classes} property in the supplied
   * {@link ContextConfigurationAttributes}. Consequently, leaving the
   * {@code locations} or {@code classes} property empty signals that
   * this {@code SmartContextLoader} was not able to generate or detect defaults.
   *
   * @param configAttributes the context configuration attributes to process
   */
  void processContextConfiguration(ContextConfigurationAttributes configAttributes);

  /**
   * Loads a new {@link ApplicationContext context} based on the supplied
   * {@link MergedContextConfiguration merged context configuration},
   * configures the context, and finally returns the context in a fully
   * <em>refreshed</em> state.
   * <p>Concrete implementations should register annotation configuration
   * processors with bean factories of
   * {@link ApplicationContext application contexts} loaded by this
   * {@code SmartContextLoader}. Beans will therefore automatically be
   * candidates for annotation-based dependency injection using
   * {@link cn.taketoday.beans.factory.annotation.Autowired @Autowired},
   * {@link jakarta.annotation.Resource @Resource}, and
   * {@link jakarta.inject.Inject @Inject}. In addition, concrete implementations
   * should set the active bean definition profiles in the context's
   * {@link cn.taketoday.core.env.Environment Environment}.
   * <p>Any {@code ApplicationContext} loaded by a
   * {@code SmartContextLoader} <strong>must</strong> register a JVM
   * shutdown hook for itself. Unless the context gets closed early, all context
   * instances will be automatically closed on JVM shutdown. This allows for
   * freeing of external resources held by beans within the context (e.g.,
   * temporary files).
   *
   * @param mergedConfig the merged context configuration to use to load the
   * application context
   * @return a new application context
   * @throws Exception if context loading failed
   * @see #processContextConfiguration(ContextConfigurationAttributes)
   * @see cn.taketoday.context.annotation.AnnotationConfigUtils
   * #registerAnnotationConfigProcessors(cn.taketoday.beans.factory.support.BeanDefinitionRegistry)
   * @see MergedContextConfiguration#getActiveProfiles()
   * @see cn.taketoday.context.ConfigurableApplicationContext#getEnvironment()
   */
  ApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception;

}
