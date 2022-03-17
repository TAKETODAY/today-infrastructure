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

package cn.taketoday.jmx;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;

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
    return "cn/taketoday/jmx/applicationContext.xml";
  }

  protected ApplicationContext getContext() {
    return this.ctx;
  }
}
