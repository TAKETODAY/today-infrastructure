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

package cn.taketoday.framework.web.servlet.context;

import cn.taketoday.lang.Assert;
import cn.taketoday.web.servlet.ConfigurableWebApplicationContext;
import cn.taketoday.web.servlet.support.ServletContextAwareProcessor;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

/**
 * Variant of {@link ServletContextAwareProcessor} for use with a
 * {@link ConfigurableWebApplicationContext}. Can be used when registering the processor
 * can occur before the {@link ServletContext} or {@link ServletConfig} have been
 * initialized.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class WebApplicationContextServletContextAwareProcessor extends ServletContextAwareProcessor {

  private final ConfigurableWebApplicationContext webApplicationContext;

  public WebApplicationContextServletContextAwareProcessor(ConfigurableWebApplicationContext webApplicationContext) {
    Assert.notNull(webApplicationContext, "WebApplicationContext is required");
    this.webApplicationContext = webApplicationContext;
  }

  @Override
  protected ServletContext getServletContext() {
    ServletContext servletContext = this.webApplicationContext.getServletContext();
    return (servletContext != null) ? servletContext : super.getServletContext();
  }

  @Override
  protected ServletConfig getServletConfig() {
    ServletConfig servletConfig = this.webApplicationContext.getServletConfig();
    return (servletConfig != null) ? servletConfig : super.getServletConfig();
  }

}
