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

package cn.taketoday.context.annotation.config;

import java.util.EventListener;

import cn.taketoday.beans.factory.Aware;
import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.context.EnvironmentAware;
import cn.taketoday.context.ResourceLoaderAware;

/**
 * Listener that can be registered with {@code today-strategies.properties} to receive details of
 * imported auto-configurations.
 * <p>
 * An {@link AutoConfigurationImportListener} may implement any of the following
 * {@link Aware Aware} interfaces, and their respective
 * methods will be called prior to
 * {@link #onAutoConfigurationImportEvent(AutoConfigurationImportEvent)}:
 * <ul>
 * <li>{@link EnvironmentAware}</li>
 * <li>{@link BeanFactoryAware}</li>
 * <li>{@link BeanClassLoaderAware}</li>
 * <li>{@link ResourceLoaderAware}</li>
 * </ul>
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/5 23:23
 */
@FunctionalInterface
public interface AutoConfigurationImportListener extends EventListener {

  /**
   * Handle an auto-configuration import event.
   *
   * @param event the event to respond to
   */
  void onAutoConfigurationImportEvent(AutoConfigurationImportEvent event);

}
