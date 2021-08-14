/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import javax.servlet.Filter;
import javax.servlet.Registration;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;

import cn.taketoday.core.Assert;
import cn.taketoday.core.OrderedSupport;
import cn.taketoday.core.utils.StringUtils;

/**
 * Initialize {@link Filter}, {@link Servlet},Listener
 *
 * @author TODAY <br>
 *         2019-02-03 12:22
 */
public abstract class WebComponentInitializer<D extends Registration.Dynamic>
        extends OrderedSupport implements ServletContextInitializer {

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

  public void setInitParameters(Map<String, String> initParameters) {
    this.initParameters = new LinkedHashMap<>(initParameters);
  }

  public void addInitParameter(String name, String value) {
    this.initParameters.put(name, value);
  }

  public Map<String, String> getInitParameters() {
    return this.initParameters;
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

  public String getName() {
    if (StringUtils.isEmpty(name)) {
      return getDefaultName();
    }
    return name;
  }

  protected String getDefaultName() {
    return null;
  }

  // -----------

  public void setName(String name) {
    this.name = name;
  }

  public boolean isAsyncSupported() {
    return asyncSupported;
  }

  public void setAsyncSupported(boolean asyncSupported) {
    this.asyncSupported = asyncSupported;
  }

  public ServletContext getServletContext() {
    return servletContext;
  }

  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

}
