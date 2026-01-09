/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.context.config;

import org.jspecify.annotations.Nullable;

import java.util.EventListener;

import infra.core.env.Environment;
import infra.core.env.PropertySource;

/**
 * {@link EventListener} to listen to {@link Environment} updates triggered by the
 * {@link ConfigDataEnvironmentPostProcessor}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface ConfigDataEnvironmentUpdateListener extends EventListener {

  /**
   * A {@link ConfigDataEnvironmentUpdateListener} that does nothing.
   */
  ConfigDataEnvironmentUpdateListener NONE = new ConfigDataEnvironmentUpdateListener() { };

  /**
   * Called when a new {@link PropertySource} is added to the {@link Environment}.
   *
   * @param propertySource the {@link PropertySource} that was added
   * @param location the original {@link ConfigDataLocation} of the source.
   * @param resource the {@link ConfigDataResource} of the source.
   */
  default void onPropertySourceAdded(PropertySource<?> propertySource,
          ConfigDataLocation location, @Nullable ConfigDataResource resource) {
  }

  /**
   * Called when {@link Environment} profiles are set.
   *
   * @param profiles the profiles being set
   */
  default void onSetProfiles(Profiles profiles) {

  }

}
