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

package cn.taketoday.framework.context.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import cn.taketoday.core.Ordered;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.ConfigurableBootstrapContext;
import cn.taketoday.framework.DefaultBootstrapContext;
import cn.taketoday.framework.env.EnvironmentPostProcessor;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * {@link EnvironmentPostProcessor} that loads and applies {@link ConfigData} to Infra
 * {@link Environment}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Nguyen Bao Sach
 * @since 4.0
 */
public class ConfigDataEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
  private static final Logger logger = LoggerFactory.getLogger(ConfigDataEnvironmentPostProcessor.class);

  /**
   * The default order for the processor.
   */
  public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

  /**
   * Property used to determine what action to take when a
   * {@code ConfigDataLocationNotFoundException} is thrown.
   *
   * @see ConfigDataNotFoundAction
   */
  public static final String ON_LOCATION_NOT_FOUND_PROPERTY = ConfigDataEnvironment.ON_NOT_FOUND_PROPERTY;

  private final ConfigurableBootstrapContext bootstrapContext;

  @Nullable
  private final ConfigDataEnvironmentUpdateListener environmentUpdateListener;

  public ConfigDataEnvironmentPostProcessor(ConfigurableBootstrapContext bootstrapContext) {
    this(bootstrapContext, null);
  }

  public ConfigDataEnvironmentPostProcessor(ConfigurableBootstrapContext bootstrapContext,
          @Nullable ConfigDataEnvironmentUpdateListener environmentUpdateListener) {
    this.bootstrapContext = bootstrapContext;
    this.environmentUpdateListener = environmentUpdateListener;
  }

  @Override
  public int getOrder() {
    return ORDER;
  }

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, Application application) {
    postProcessEnvironment(environment, application.getResourceLoader(), application.getAdditionalProfiles());
  }

  void postProcessEnvironment(ConfigurableEnvironment environment,
          @Nullable ResourceLoader resourceLoader, Collection<String> additionalProfiles) {
    if (resourceLoader == null) {
      resourceLoader = new DefaultResourceLoader();
    }
    logger.trace("Post-processing environment to add config data");
    getConfigDataEnvironment(environment, resourceLoader, additionalProfiles).processAndApply();
  }

  ConfigDataEnvironment getConfigDataEnvironment(ConfigurableEnvironment environment,
          ResourceLoader resourceLoader, Collection<String> additionalProfiles) {
    return new ConfigDataEnvironment(bootstrapContext, environment, resourceLoader,
            additionalProfiles, this.environmentUpdateListener);
  }

  /**
   * Apply {@link ConfigData} post-processing to an existing {@link Environment}. This
   * method can be useful when working with an {@link Environment} that has been created
   * directly and not necessarily as part of a {@link Application}.
   *
   * @param environment the environment to apply {@link ConfigData} to
   */
  public static void applyTo(ConfigurableEnvironment environment) {
    applyTo(environment, null, null, Collections.emptyList());
  }

  /**
   * Apply {@link ConfigData} post-processing to an existing {@link Environment}. This
   * method can be useful when working with an {@link Environment} that has been created
   * directly and not necessarily as part of a {@link Application}.
   *
   * @param environment the environment to apply {@link ConfigData} to
   * @param resourceLoader the resource loader to use
   * @param bootstrapContext the bootstrap context to use or {@code null} to use a
   * throw-away context
   * @param additionalProfiles any additional profiles that should be applied
   */
  public static void applyTo(ConfigurableEnvironment environment, ResourceLoader resourceLoader,
          ConfigurableBootstrapContext bootstrapContext, String... additionalProfiles) {
    applyTo(environment, resourceLoader, bootstrapContext, Arrays.asList(additionalProfiles));
  }

  /**
   * Apply {@link ConfigData} post-processing to an existing {@link Environment}. This
   * method can be useful when working with an {@link Environment} that has been created
   * directly and not necessarily as part of a {@link Application}.
   *
   * @param environment the environment to apply {@link ConfigData} to
   * @param resourceLoader the resource loader to use
   * @param bootstrapContext the bootstrap context to use or {@code null} to use a
   * throw-away context
   * @param additionalProfiles any additional profiles that should be applied
   */
  public static void applyTo(ConfigurableEnvironment environment, @Nullable ResourceLoader resourceLoader,
          @Nullable ConfigurableBootstrapContext bootstrapContext, Collection<String> additionalProfiles) {
    applyTo(environment, resourceLoader, bootstrapContext, additionalProfiles, null);
  }

  /**
   * Apply {@link ConfigData} post-processing to an existing {@link Environment}. This
   * method can be useful when working with an {@link Environment} that has been created
   * directly and not necessarily as part of a {@link Application}.
   *
   * @param environment the environment to apply {@link ConfigData} to
   * @param resourceLoader the resource loader to use
   * @param bootstrapContext the bootstrap context to use or {@code null} to use a
   * throw-away context
   * @param additionalProfiles any additional profiles that should be applied
   * @param environmentUpdateListener optional
   * {@link ConfigDataEnvironmentUpdateListener} that can be used to track
   * {@link Environment} updates.
   */
  public static void applyTo(
          ConfigurableEnvironment environment,
          @Nullable ResourceLoader resourceLoader,
          @Nullable ConfigurableBootstrapContext bootstrapContext,
          Collection<String> additionalProfiles,
          @Nullable ConfigDataEnvironmentUpdateListener environmentUpdateListener) {
    if (bootstrapContext == null) {
      bootstrapContext = new DefaultBootstrapContext();
    }
    var postProcessor = new ConfigDataEnvironmentPostProcessor(bootstrapContext, environmentUpdateListener);
    postProcessor.postProcessEnvironment(environment, resourceLoader, additionalProfiles);
  }

}
