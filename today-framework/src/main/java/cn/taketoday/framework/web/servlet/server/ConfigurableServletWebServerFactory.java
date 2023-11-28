/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.framework.web.servlet.server;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.taketoday.framework.web.server.ConfigurableWebServerFactory;
import cn.taketoday.framework.web.server.MimeMappings;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizer;
import cn.taketoday.framework.web.servlet.ServletContextInitializer;
import cn.taketoday.framework.web.servlet.WebListenerRegistry;
import cn.taketoday.session.config.SameSite;
import cn.taketoday.session.config.SessionProperties;
import jakarta.servlet.ServletContext;

/**
 * A configurable {@link ServletWebServerFactory}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ServletWebServerFactory
 * @see WebServerFactoryCustomizer
 * @since 4.0
 */
public interface ConfigurableServletWebServerFactory
        extends ConfigurableWebServerFactory, ServletWebServerFactory, WebListenerRegistry {

  /**
   * Sets the context path for the web server. The context should start with a "/"
   * character but not end with a "/" character. The default context path can be
   * specified using an empty string.
   *
   * @param contextPath the contextPath to set
   */
  void setContextPath(String contextPath);

  /**
   * Sets the display name of the application deployed in the web server.
   *
   * @param displayName the displayName to set
   */
  void setDisplayName(String displayName);

  /**
   * Sets the configuration that will be applied to the container's HTTP session
   * support.
   *
   * @param session the session configuration
   */
  void setSession(SessionProperties session);

  /**
   * Set if the DefaultServlet should be registered. Defaults to {@code false}
   *
   * @param registerDefaultServlet if the default servlet should be registered
   */
  void setRegisterDefaultServlet(boolean registerDefaultServlet);

  /**
   * Sets the mime-type mappings.
   *
   * @param mimeMappings the mime type mappings (defaults to
   * {@link MimeMappings#DEFAULT})
   */
  void setMimeMappings(MimeMappings mimeMappings);

  /**
   * Sets the document root directory which will be used by the web context to serve
   * static files.
   *
   * @param documentRoot the document root or {@code null} if not required
   */
  void setDocumentRoot(File documentRoot);

  /**
   * Sets {@link ServletContextInitializer} that should be applied in addition to
   * {@link ServletWebServerFactory#getWebServer(ServletContextInitializer...)}
   * parameters. This method will replace any previously set or added initializers.
   *
   * @param initializers the initializers to set
   * @see #addInitializers
   */
  void setInitializers(List<? extends ServletContextInitializer> initializers);

  /**
   * Add {@link ServletContextInitializer}s to those that should be applied in addition
   * to {@link ServletWebServerFactory#getWebServer(ServletContextInitializer...)}
   * parameters.
   *
   * @param initializers the initializers to add
   * @see #setInitializers
   */
  void addInitializers(ServletContextInitializer... initializers);

  /**
   * Sets the Locale to Charset mappings.
   *
   * @param localeCharsetMappings the Locale to Charset mappings
   */
  void setLocaleCharsetMappings(Map<Locale, Charset> localeCharsetMappings);

  /**
   * Sets the init parameters that are applied to the container's
   * {@link ServletContext}.
   *
   * @param initParameters the init parameters
   */
  void setInitParameters(Map<String, String> initParameters);

  /**
   * Sets {@link CookieSameSiteSupplier CookieSameSiteSuppliers} that should be used to
   * obtain the {@link SameSite} attribute of any added cookie. This method will replace
   * any previously set or added suppliers.
   *
   * @param cookieSameSiteSuppliers the suppliers to add
   * @see #addCookieSameSiteSuppliers
   */
  void setCookieSameSiteSuppliers(List<? extends CookieSameSiteSupplier> cookieSameSiteSuppliers);

  /**
   * Add {@link CookieSameSiteSupplier CookieSameSiteSuppliers} to those that should be
   * used to obtain the {@link SameSite} attribute of any added cookie.
   *
   * @param cookieSameSiteSuppliers the suppliers to add
   * @see #setCookieSameSiteSuppliers
   */
  void addCookieSameSiteSuppliers(CookieSameSiteSupplier... cookieSameSiteSuppliers);

}
