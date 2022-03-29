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

package cn.taketoday.framework;

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.ConfigurableApplicationContext;

/**
 * {@link ApplicationEvent} published by a {@link BootstrapContext} when it's closed.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BootstrapRegistry#addCloseListener(ApplicationListener)
 * @since 4.0 2022/3/29 10:54
 */
public class BootstrapContextClosedEvent extends ApplicationEvent {

  private final ConfigurableApplicationContext applicationContext;

  BootstrapContextClosedEvent(BootstrapContext source, ConfigurableApplicationContext applicationContext) {
    super(source);
    this.applicationContext = applicationContext;
  }

  /**
   * Return the {@link BootstrapContext} that was closed.
   *
   * @return the bootstrap context
   */
  public BootstrapContext getBootstrapContext() {
    return (BootstrapContext) this.source;
  }

  /**
   * Return the prepared application context.
   *
   * @return the application context
   */
  public ConfigurableApplicationContext getApplicationContext() {
    return this.applicationContext;
  }

}
