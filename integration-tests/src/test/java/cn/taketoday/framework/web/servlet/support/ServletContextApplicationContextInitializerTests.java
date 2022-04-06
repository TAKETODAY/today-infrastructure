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

package cn.taketoday.framework.web.servlet.support;

import org.junit.jupiter.api.Test;

import cn.taketoday.web.context.ConfigurableWebApplicationContext;
import cn.taketoday.web.context.WebApplicationContext;
import jakarta.servlet.ServletContext;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link ServletContextApplicationContextInitializer}.
 *
 * @author Andy Wilkinson
 */
class ServletContextApplicationContextInitializerTests {

  private final ServletContext servletContext = mock(ServletContext.class);

  private final ConfigurableWebApplicationContext applicationContext = mock(ConfigurableWebApplicationContext.class);

  @Test
  void servletContextIsSetOnTheApplicationContext() {
    new ServletContextApplicationContextInitializer(this.servletContext).initialize(this.applicationContext);
    then(this.applicationContext).should().setServletContext(this.servletContext);
  }

  @Test
  void applicationContextIsNotStoredInServletContextByDefault() {
    new ServletContextApplicationContextInitializer(this.servletContext).initialize(this.applicationContext);
    then(this.servletContext).should(never())
            .setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.applicationContext);
  }

  @Test
  void applicationContextCanBeStoredInServletContext() {
    new ServletContextApplicationContextInitializer(this.servletContext, true).initialize(this.applicationContext);
    then(this.servletContext).should().setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
            this.applicationContext);
  }

}
