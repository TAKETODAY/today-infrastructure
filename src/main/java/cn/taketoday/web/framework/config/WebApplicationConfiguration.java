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
package cn.taketoday.web.framework.config;

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import cn.taketoday.web.framework.server.AbstractWebServer;
import cn.taketoday.web.session.SessionConfiguration;

/**
 * @author TODAY <br>
 * 2019-06-18 17:35
 */
public interface WebApplicationConfiguration {

  /**
   * Configure compression
   */
  default void configureCompression(CompressionConfiguration compressionConfiguration) { }

  /**
   * Configure session
   */
  default void configureSession(SessionConfiguration sessionConfiguration) { }

  /**
   * Configure jsp servlet
   */
  default void configureJspServlet(JspServletConfiguration jspServletConfiguration) { }

  /**
   * Configure default servlet
   */
  default void configureDefaultServlet(DefaultServletConfiguration defaultServletConfiguration) { }

  /**
   * Configure {@link WebServer}
   *
   * @param webServer {@link WebServer} instance
   */
  default void configureWebServer(AbstractWebServer webServer) { }

  /**
   * Configure {@link ErrorPage}s
   *
   * @param errorPages a set of {@link ErrorPage}s
   */
  default void configureErrorPages(Set<ErrorPage> errorPages) { }

  /**
   * Configure {@link MimeMappings}
   *
   * @param mimeMappings {@link MimeMappings}
   */
  default void configureMimeMappings(MimeMappings mimeMappings) { }

  /**
   * Configure welcome pages
   */
  default void configureWelcomePages(Set<String> welcomePages) { }

  default void configureLocaleCharsetMapping(Map<Locale, Charset> localeMappings) { }

}
