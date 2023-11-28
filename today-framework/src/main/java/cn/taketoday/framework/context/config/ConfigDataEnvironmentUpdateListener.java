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

package cn.taketoday.framework.context.config;

import java.util.EventListener;

import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.lang.Nullable;

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
