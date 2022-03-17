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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.servlet.initializer;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.Conventions;
import cn.taketoday.core.OrderedSupport;
import cn.taketoday.lang.Assert;
import jakarta.servlet.Filter;
import jakarta.servlet.Registration;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;

/**
 * Initialize {@link Filter}, {@link Servlet},Listener
 * <p>
 * Base class for Servlet 3.0+ {@link jakarta.servlet.Registration.Dynamic dynamic} based
 * registration beans.
 *
 * @author TODAY
 * @since 2019-02-03 12:22
 */
public abstract class WebComponentInitializer<D extends Registration.Dynamic>
        extends OrderedSupport implements ServletContextInitializer {
  public static final String DEFAULT_MAPPING = "/";
  public static final String[] DEFAULT_MAPPINGS = { DEFAULT_MAPPING };

  private String name;

  private boolean asyncSupported = false;

  private final Set<String> urlMappings = new LinkedHashSet<>();

  private Map<String, String> initParameters = new LinkedHashMap<>();

  private ServletContext servletContext;

  @Override
  public void onStartup(ServletContext servletContext) {
    setServletContext(servletContext);

    D registration = addRegistration(servletContext);
    if (registration != null) {
      configureRegistration(registration);
    }
  }

  protected abstract D addRegistration(ServletContext servletContext);

  protected void configureRegistration(D registration) {
    registration.setAsyncSupported(this.asyncSupported);
    if (!this.initParameters.isEmpty()) {
      registration.setInitParameters(this.initParameters);
    }
  }

  /**
   * Set the name of this registration. If not specified the bean name will be used.
   *
   * @param name the name of the registration
   */
  public void setName(String name) {
    Assert.hasLength(name, "Name must not be empty");
    this.name = name;
  }

  /**
   * Sets if asynchronous operations are supported for this registration. If not
   * specified defaults to {@code true}.
   *
   * @param asyncSupported if async is supported
   */
  public void setAsyncSupported(boolean asyncSupported) {
    this.asyncSupported = asyncSupported;
  }

  /**
   * Returns if asynchronous operations are supported for this registration.
   *
   * @return if async is supported
   */
  public boolean isAsyncSupported() {
    return this.asyncSupported;
  }

  /**
   * Set init-parameters for this registration. Calling this method will replace any
   * existing init-parameters.
   *
   * @param initParameters the init parameters
   * @see #getInitParameters
   * @see #addInitParameter
   */
  public void setInitParameters(Map<String, String> initParameters) {
    Assert.notNull(initParameters, "InitParameters must not be null");
    this.initParameters = new LinkedHashMap<>(initParameters);
  }

  /**
   * Returns a mutable Map of the registration init-parameters.
   *
   * @return the init parameters
   */
  public Map<String, String> getInitParameters() {
    return this.initParameters;
  }

  /**
   * Add a single init-parameter, replacing any existing parameter with the same name.
   *
   * @param name the init-parameter name
   * @param value the init-parameter value
   */
  public void addInitParameter(String name, String value) {
    Assert.notNull(name, "Name must not be null");
    this.initParameters.put(name, value);
  }

  public void setUrlMappings(Collection<String> urlMappings) {
    Assert.notNull(urlMappings, "UrlMappings must not be null");
    this.urlMappings.clear();
    this.urlMappings.addAll(urlMappings);
  }

  public Collection<String> getUrlMappings() {
    return this.urlMappings;
  }

  public void addUrlMappings(String... urlMappings) {
    Assert.notNull(urlMappings, "UrlMappings must not be null");
    Collections.addAll(this.urlMappings, urlMappings);
  }

  /**
   * Deduces the name for this registration. Will return user specified name or fallback
   * to convention based naming.
   *
   * @param value the object used for convention based names
   * @return the deduced name
   */
  protected final String getOrDeduceName(Object value) {
    return this.name != null ? this.name : Conventions.getVariableName(value);
  }

  // -----------

  public ServletContext getServletContext() {
    return servletContext;
  }

  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

}
