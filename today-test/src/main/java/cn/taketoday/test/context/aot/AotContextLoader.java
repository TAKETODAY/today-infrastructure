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

package cn.taketoday.test.context.aot;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.test.context.ContextLoadException;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.context.SmartContextLoader;

/**
 * Strategy interface for loading an {@link ApplicationContext} for build-time
 * {@linkplain #loadContextForAotProcessing AOT processing} as well as run-time
 * {@linkplain #loadContextForAotRuntime AOT execution} for an integration test
 * managed by the Infra TestContext Framework.
 *
 * <p>{@code AotContextLoader} is an extension of the {@link SmartContextLoader}
 * SPI that allows a context loader to optionally provide ahead-of-time (AOT)
 * support.
 *
 * <p>AOT infrastructure requires that an {@code AotContextLoader}
 * create a {@link cn.taketoday.context.support.GenericApplicationContext
 * GenericApplicationContext} for both build-time processing and run-time execution.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface AotContextLoader extends SmartContextLoader {

  /**
   * Load a new {@link ApplicationContext} for AOT build-time processing based
   * on the supplied {@link MergedContextConfiguration}, configure the context,
   * and return the context.
   * <p>In contrast to {@link #loadContext(MergedContextConfiguration)}, this
   * method must <strong>not</strong>
   * {@linkplain cn.taketoday.context.ConfigurableApplicationContext#refresh()
   * refresh} the {@code ApplicationContext} or
   * {@linkplain cn.taketoday.context.ConfigurableApplicationContext#registerShutdownHook()
   * register a JVM shutdown hook} for it. Otherwise, this method should implement
   * behavior identical to {@code loadContext(MergedContextConfiguration)}.
   * <p>Any exception thrown while attempting to load an {@code ApplicationContext}
   * should be wrapped in a {@link ContextLoadException}. Concrete implementations
   * should therefore contain a try-catch block similar to the following.
   * <pre style="code">
   * GenericApplicationContext context = // create context
   * try {
   *     // configure context
   * }
   * catch (Exception ex) {
   *     throw new ContextLoadException(context, ex);
   * }
   * </pre>
   *
   * @param mergedConfig the merged context configuration to use to load the
   * application context
   * @return a new {@code GenericApplicationContext}
   * @throws ContextLoadException if context loading failed
   * @see #loadContextForAotRuntime(MergedContextConfiguration, ApplicationContextInitializer)
   */
  ApplicationContext loadContextForAotProcessing(MergedContextConfiguration mergedConfig) throws Exception;

  /**
   * Load a new {@link ApplicationContext} for AOT run-time execution based on
   * the supplied {@link MergedContextConfiguration} and
   * {@link ApplicationContextInitializer}.
   * <p>This method must instantiate, initialize, and
   * {@linkplain cn.taketoday.context.ConfigurableApplicationContext#refresh()
   * refresh} the {@code ApplicationContext}.
   * <p>Any exception thrown while attempting to load an {@code ApplicationContext}
   * should be wrapped in a {@link ContextLoadException}. Concrete implementations
   * should therefore contain a try-catch block similar to the following.
   * <pre style="code">
   * GenericApplicationContext context = // create context
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
   * @param initializer the {@code ApplicationContextInitializer} that should
   * be applied to the context in order to recreate bean definitions
   * @return a new {@code GenericApplicationContext}
   * @throws ContextLoadException if context loading failed
   * @see #loadContextForAotProcessing(MergedContextConfiguration)
   */
  ApplicationContext loadContextForAotRuntime(MergedContextConfiguration mergedConfig,
          ApplicationContextInitializer initializer) throws Exception;

}
