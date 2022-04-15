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

package cn.taketoday.framework.web.servlet;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;

/**
 * A {@link ServletContextInitializer} to register {@link Servlet}s in a Servlet 3.0+
 * container. Similar to the {@link ServletContext#addServlet(String, Servlet)
 * registration} features provided by {@link ServletContext} but with a Framework Bean
 * friendly design.
 * <p>
 * The {@link #setServlet(Servlet) servlet} must be specified before calling
 * {@link #onStartup}. URL mapping can be configured used {@link #setUrlMappings} or
 * omitted when mapping to '/*' (unless
 * {@link #ServletRegistrationBean(Servlet, boolean, String...) alwaysMapUrl} is set to
 * {@code false}). The servlet name will be deduced if not specified.
 *
 * @param <T> the type of the {@link Servlet} to register
 * @author Phillip Webb
 * @see ServletContextInitializer
 * @see ServletContext#addServlet(String, Servlet)
 * @since 4.0
 */
public class ServletRegistrationBean<T extends Servlet> extends DynamicRegistrationBean<ServletRegistration.Dynamic> {

  private static final String[] DEFAULT_MAPPINGS = { "/*" };

  private T servlet;

  private Set<String> urlMappings = new LinkedHashSet<>();

  private boolean alwaysMapUrl = true;

  private int loadOnStartup = -1;

  private MultipartConfigElement multipartConfig;

  /**
   * Create a new {@link ServletRegistrationBean} instance.
   */
  public ServletRegistrationBean() { }

  /**
   * Create a new {@link ServletRegistrationBean} instance with the specified
   * {@link Servlet} and URL mappings.
   *
   * @param servlet the servlet being mapped
   * @param urlMappings the URLs being mapped
   */
  public ServletRegistrationBean(T servlet, String... urlMappings) {
    this(servlet, true, urlMappings);
  }

  /**
   * Create a new {@link ServletRegistrationBean} instance with the specified
   * {@link Servlet} and URL mappings.
   *
   * @param servlet the servlet being mapped
   * @param alwaysMapUrl if omitted URL mappings should be replaced with '/*'
   * @param urlMappings the URLs being mapped
   */
  public ServletRegistrationBean(T servlet, boolean alwaysMapUrl, String... urlMappings) {
    Assert.notNull(servlet, "Servlet must not be null");
    Assert.notNull(urlMappings, "UrlMappings must not be null");
    this.servlet = servlet;
    this.alwaysMapUrl = alwaysMapUrl;
    CollectionUtils.addAll(this.urlMappings, urlMappings);
  }

  /**
   * Sets the servlet to be registered.
   *
   * @param servlet the servlet
   */
  public void setServlet(T servlet) {
    Assert.notNull(servlet, "Servlet must not be null");
    this.servlet = servlet;
  }

  /**
   * Return the servlet being registered.
   *
   * @return the servlet
   */
  public T getServlet() {
    return this.servlet;
  }

  /**
   * Set the URL mappings for the servlet. If not specified the mapping will default to
   * '/'. This will replace any previously specified mappings.
   *
   * @param urlMappings the mappings to set
   * @see #addUrlMappings(String...)
   */
  public void setUrlMappings(Collection<String> urlMappings) {
    Assert.notNull(urlMappings, "UrlMappings must not be null");
    this.urlMappings = new LinkedHashSet<>(urlMappings);
  }

  /**
   * Return a mutable collection of the URL mappings, as defined in the Servlet
   * specification, for the servlet.
   *
   * @return the urlMappings
   */
  public Collection<String> getUrlMappings() {
    return this.urlMappings;
  }

  /**
   * Add URL mappings, as defined in the Servlet specification, for the servlet.
   *
   * @param urlMappings the mappings to add
   * @see #setUrlMappings(Collection)
   */
  public void addUrlMappings(String... urlMappings) {
    Assert.notNull(urlMappings, "UrlMappings must not be null");
    CollectionUtils.addAll(this.urlMappings, urlMappings);
  }

  /**
   * Sets the {@code loadOnStartup} priority. See
   * {@link ServletRegistration.Dynamic#setLoadOnStartup} for details.
   *
   * @param loadOnStartup if load on startup is enabled
   */
  public void setLoadOnStartup(int loadOnStartup) {
    this.loadOnStartup = loadOnStartup;
  }

  /**
   * Set the {@link MultipartConfigElement multi-part configuration}.
   *
   * @param multipartConfig the multi-part configuration to set or {@code null}
   */
  public void setMultipartConfig(MultipartConfigElement multipartConfig) {
    this.multipartConfig = multipartConfig;
  }

  /**
   * Returns the {@link MultipartConfigElement multi-part configuration} to be applied
   * or {@code null}.
   *
   * @return the multipart config
   */
  public MultipartConfigElement getMultipartConfig() {
    return this.multipartConfig;
  }

  @Override
  protected String getDescription() {
    Assert.notNull(this.servlet, "Servlet must not be null");
    return "servlet " + getServletName();
  }

  @Override
  protected ServletRegistration.Dynamic addRegistration(String description, ServletContext servletContext) {
    String name = getServletName();
    return servletContext.addServlet(name, this.servlet);
  }

  /**
   * Configure registration settings. Subclasses can override this method to perform
   * additional configuration if required.
   *
   * @param registration the registration
   */
  @Override
  protected void configure(ServletRegistration.Dynamic registration) {
    super.configure(registration);
    String[] urlMapping = StringUtils.toStringArray(urlMappings);
    if (urlMapping.length == 0 && alwaysMapUrl) {
      urlMapping = DEFAULT_MAPPINGS;
    }
    if (ObjectUtils.isNotEmpty(urlMapping)) {
      registration.addMapping(urlMapping);
    }
    registration.setLoadOnStartup(loadOnStartup);
    if (multipartConfig != null) {
      registration.setMultipartConfig(multipartConfig);
    }
  }

  /**
   * Returns the servlet name that will be registered.
   *
   * @return the servlet name
   */
  public String getServletName() {
    return getOrDeduceName(this.servlet);
  }

  @Override
  public String toString() {
    return getServletName() + " urls=" + getUrlMappings();
  }

}
