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

package cn.taketoday.framework;

import java.time.Duration;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.lang.TodayStrategies;

/**
 * Listener for the {@link Application} {@code run} method.
 * {@link ApplicationStartupListener}s are loaded via the {@link TodayStrategies}
 * and should declare a public constructor that accepts a {@link Application}
 * instance and a {@code String[]} of arguments. A new
 * {@link ApplicationStartupListener} instance will be created for each run.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Chris Bono
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 20:02
 */
public interface ApplicationStartupListener {

  /**
   * Called immediately when the run method has first started. Can be used for very
   * early initialization.
   *
   * @param bootstrapContext the bootstrap context
   */
  default void starting(ConfigurableBootstrapContext bootstrapContext, Class<?> mainApplicationClass, ApplicationArguments arguments) { }

  /**
   * Called once the environment has been prepared, but before the
   * {@link ApplicationContext} has been created.
   *
   * @param bootstrapContext the bootstrap context
   * @param environment the environment
   */
  default void environmentPrepared(ConfigurableBootstrapContext bootstrapContext, ConfigurableEnvironment environment) { }

  /**
   * Called once the {@link ApplicationContext} has been created and prepared, but
   * before sources have been loaded.
   *
   * @param context the application context
   */
  default void contextPrepared(ConfigurableApplicationContext context) { }

  /**
   * Called once the application context has been loaded but before it has been
   * refreshed.
   *
   * @param context the application context
   */
  default void contextLoaded(ConfigurableApplicationContext context) { }

  /**
   * The context has been refreshed and the application has started but
   * {@link CommandLineRunner CommandLineRunners} and {@link ApplicationRunner
   * ApplicationRunners} have not been called.
   *
   * @param context the application context.
   * @param timeTaken the time taken to start the application or {@code null} if unknown
   */
  default void started(ConfigurableApplicationContext context, Duration timeTaken) { }

  /**
   * Called immediately before the run method finishes, when the application context has
   * been refreshed and all {@link CommandLineRunner CommandLineRunners} and
   * {@link ApplicationRunner ApplicationRunners} have been called.
   *
   * @param context the application context.
   * @param timeTaken the time taken for the application to be ready or {@code null} if
   * unknown
   */
  default void ready(ConfigurableApplicationContext context, Duration timeTaken) { }

  /**
   * Called when a failure occurs when running the application.
   *
   * @param context the application context or {@code null} if a failure occurred before
   * the context was created
   * @param exception the failure
   */
  default void failed(ConfigurableApplicationContext context, Throwable exception) { }

}
