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
package cn.taketoday.web;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.aware.ApplicationContextSupport;
import cn.taketoday.lang.Nullable;

/**
 * Convenient superclass for application objects running in a {@link WebApplicationContext}.
 * Provides {@code getWebApplicationContext()}
 *
 * @author TODAY <br>
 * @since 2019-12-27 09:36
 */
public class WebApplicationContextSupport extends ApplicationContextSupport {

  /**
   * Overrides the base class behavior to enforce running in an ApplicationContext.
   * All accessors will throw IllegalStateException if not running in a context.
   *
   * @see #getApplicationContext()
   * @see #getMessageSourceAccessor()
   * @see #getWebApplicationContext()
   */
  @Override
  protected boolean isContextRequired() {
    return true;
  }

  public String getContextPath() {
    return obtainApplicationContext().getContextPath();
  }

  /**
   * Return the current application context as {@link WebApplicationContext}.
   *
   * @throws IllegalStateException if not running in a WebApplicationContext
   * @see #getApplicationContext()
   */
  @Nullable
  public final WebApplicationContext getWebApplicationContext() {
    ApplicationContext ctx = getApplicationContext();
    if (ctx instanceof WebApplicationContext) {
      return (WebApplicationContext) ctx;
    }
    else if (isContextRequired()) {
      throw new IllegalStateException("WebApplicationContextSupport instance [" + this +
              "] does not run in a WebApplicationContext but in: " + ctx);
    }
    else {
      return null;
    }
  }

  @Override
  public WebApplicationContext obtainApplicationContext() {
    return (WebApplicationContext) super.obtainApplicationContext();
  }

}
