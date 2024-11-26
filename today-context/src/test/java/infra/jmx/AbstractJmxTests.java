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

package infra.jmx;

import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;

/**
 * Base JMX test class that pre-loads an ApplicationContext from a user-configurable file. Override the
 * {@link #getApplicationContextPath()} method to control the configuration file location.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public abstract class AbstractJmxTests extends AbstractMBeanServerTests {

  private ConfigurableApplicationContext ctx;

  @Override
  protected final void onSetUp() throws Exception {
    ctx = loadContext(getApplicationContextPath());
  }

  @Override
  protected final void onTearDown() throws Exception {
    if (ctx != null) {
      ctx.close();
    }
  }

  protected String getApplicationContextPath() {
    return "infra/jmx/applicationContext.xml";
  }

  protected ApplicationContext getContext() {
    return this.ctx;
  }
}
