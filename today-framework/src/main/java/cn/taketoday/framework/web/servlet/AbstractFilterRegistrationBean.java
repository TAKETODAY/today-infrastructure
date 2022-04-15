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
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.servlet.filter.OncePerRequestFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration.Dynamic;
import jakarta.servlet.ServletContext;

/**
 * Abstract base {@link ServletContextInitializer} to register {@link Filter}s in a
 * Servlet 3.0+ container.
 *
 * @param <T> the type of {@link Filter} to register
 * @author Phillip Webb
 * @author Brian Clozel
 * @since 4.0
 */
public abstract class AbstractFilterRegistrationBean<T extends Filter> extends DynamicRegistrationBean<Dynamic> {

  private static final String[] DEFAULT_URL_MAPPINGS = { "/*" };

  private Set<ServletRegistrationBean<?>> servletRegistrationBeans = new LinkedHashSet<>();

  private Set<String> servletNames = new LinkedHashSet<>();

  private Set<String> urlPatterns = new LinkedHashSet<>();

  private EnumSet<DispatcherType> dispatcherTypes;

  private boolean matchAfter = false;

  /**
   * Create a new instance to be registered with the specified
   * {@link ServletRegistrationBean}s.
   *
   * @param servletRegistrationBeans associate {@link ServletRegistrationBean}s
   */
  AbstractFilterRegistrationBean(ServletRegistrationBean<?>... servletRegistrationBeans) {
    Assert.notNull(servletRegistrationBeans, "ServletRegistrationBeans must not be null");
    Collections.addAll(this.servletRegistrationBeans, servletRegistrationBeans);
  }

  /**
   * Set {@link ServletRegistrationBean}s that the filter will be registered against.
   *
   * @param servletRegistrationBeans the Servlet registration beans
   */
  public void setServletRegistrationBeans(Collection<? extends ServletRegistrationBean<?>> servletRegistrationBeans) {
    Assert.notNull(servletRegistrationBeans, "ServletRegistrationBeans must not be null");
    this.servletRegistrationBeans = new LinkedHashSet<>(servletRegistrationBeans);
  }

  /**
   * Return a mutable collection of the {@link ServletRegistrationBean} that the filter
   * will be registered against. {@link ServletRegistrationBean}s.
   *
   * @return the Servlet registration beans
   * @see #setServletNames
   * @see #setUrlPatterns
   */
  public Collection<ServletRegistrationBean<?>> getServletRegistrationBeans() {
    return this.servletRegistrationBeans;
  }

  /**
   * Add {@link ServletRegistrationBean}s for the filter.
   *
   * @param servletRegistrationBeans the servlet registration beans to add
   * @see #setServletRegistrationBeans
   */
  public void addServletRegistrationBeans(ServletRegistrationBean<?>... servletRegistrationBeans) {
    Assert.notNull(servletRegistrationBeans, "ServletRegistrationBeans must not be null");
    Collections.addAll(this.servletRegistrationBeans, servletRegistrationBeans);
  }

  /**
   * Set servlet names that the filter will be registered against. This will replace any
   * previously specified servlet names.
   *
   * @param servletNames the servlet names
   * @see #setServletRegistrationBeans
   * @see #setUrlPatterns
   */
  public void setServletNames(Collection<String> servletNames) {
    Assert.notNull(servletNames, "ServletNames must not be null");
    this.servletNames = new LinkedHashSet<>(servletNames);
  }

  /**
   * Return a mutable collection of servlet names that the filter will be registered
   * against.
   *
   * @return the servlet names
   */
  public Collection<String> getServletNames() {
    return this.servletNames;
  }

  /**
   * Add servlet names for the filter.
   *
   * @param servletNames the servlet names to add
   */
  public void addServletNames(String... servletNames) {
    Assert.notNull(servletNames, "ServletNames must not be null");
    this.servletNames.addAll(Arrays.asList(servletNames));
  }

  /**
   * Set the URL patterns that the filter will be registered against. This will replace
   * any previously specified URL patterns.
   *
   * @param urlPatterns the URL patterns
   * @see #setServletRegistrationBeans
   * @see #setServletNames
   */
  public void setUrlPatterns(Collection<String> urlPatterns) {
    Assert.notNull(urlPatterns, "UrlPatterns must not be null");
    this.urlPatterns = new LinkedHashSet<>(urlPatterns);
  }

  /**
   * Return a mutable collection of URL patterns, as defined in the Servlet
   * specification, that the filter will be registered against.
   *
   * @return the URL patterns
   */
  public Collection<String> getUrlPatterns() {
    return this.urlPatterns;
  }

  /**
   * Add URL patterns, as defined in the Servlet specification, that the filter will be
   * registered against.
   *
   * @param urlPatterns the URL patterns
   */
  public void addUrlPatterns(String... urlPatterns) {
    Assert.notNull(urlPatterns, "UrlPatterns must not be null");
    Collections.addAll(this.urlPatterns, urlPatterns);
  }

  /**
   * Convenience method to {@link #setDispatcherTypes(EnumSet) set dispatcher types}
   * using the specified elements.
   *
   * @param first the first dispatcher type
   * @param rest additional dispatcher types
   */
  public void setDispatcherTypes(DispatcherType first, DispatcherType... rest) {
    this.dispatcherTypes = EnumSet.of(first, rest);
  }

  /**
   * Sets the dispatcher types that should be used with the registration. If not
   * specified the types will be deduced based on the value of
   * {@link #isAsyncSupported()}.
   *
   * @param dispatcherTypes the dispatcher types
   */
  public void setDispatcherTypes(EnumSet<DispatcherType> dispatcherTypes) {
    this.dispatcherTypes = dispatcherTypes;
  }

  /**
   * Set if the filter mappings should be matched after any declared filter mappings of
   * the ServletContext. Defaults to {@code false} indicating the filters are supposed
   * to be matched before any declared filter mappings of the ServletContext.
   *
   * @param matchAfter if filter mappings are matched after
   */
  public void setMatchAfter(boolean matchAfter) {
    this.matchAfter = matchAfter;
  }

  /**
   * Return if filter mappings should be matched after any declared Filter mappings of
   * the ServletContext.
   *
   * @return if filter mappings are matched after
   */
  public boolean isMatchAfter() {
    return this.matchAfter;
  }

  @Override
  protected String getDescription() {
    Filter filter = getFilter();
    Assert.notNull(filter, "Filter must not be null");
    return "filter " + getOrDeduceName(filter);
  }

  @Override
  protected Dynamic addRegistration(String description, ServletContext servletContext) {
    Filter filter = getFilter();
    return servletContext.addFilter(getOrDeduceName(filter), filter);
  }

  /**
   * Configure registration settings. Subclasses can override this method to perform
   * additional configuration if required.
   *
   * @param registration the registration
   */
  @Override
  protected void configure(Dynamic registration) {
    super.configure(registration);
    EnumSet<DispatcherType> dispatcherTypes = this.dispatcherTypes;
    if (dispatcherTypes == null) {
      T filter = getFilter();
      if (ClassUtils.isPresent("cn.taketoday.web.servlet.filter.OncePerRequestFilter",
              filter.getClass().getClassLoader()) && filter instanceof OncePerRequestFilter) {
        dispatcherTypes = EnumSet.allOf(DispatcherType.class);
      }
      else {
        dispatcherTypes = EnumSet.of(DispatcherType.REQUEST);
      }
    }
    var servletNames = new LinkedHashSet<String>();
    for (ServletRegistrationBean<?> servletRegistrationBean : servletRegistrationBeans) {
      servletNames.add(servletRegistrationBean.getServletName());
    }
    servletNames.addAll(this.servletNames);
    if (servletNames.isEmpty() && urlPatterns.isEmpty()) {
      registration.addMappingForUrlPatterns(
              dispatcherTypes, matchAfter, DEFAULT_URL_MAPPINGS);
    }
    else {
      if (!servletNames.isEmpty()) {
        registration.addMappingForServletNames(
                dispatcherTypes, matchAfter, StringUtils.toStringArray(servletNames));
      }
      if (!this.urlPatterns.isEmpty()) {
        registration.addMappingForUrlPatterns(
                dispatcherTypes, matchAfter, StringUtils.toStringArray(this.urlPatterns));
      }
    }
  }

  /**
   * Return the {@link Filter} to be registered.
   *
   * @return the filter
   */
  public abstract T getFilter();

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(getOrDeduceName(this));
    if (this.servletNames.isEmpty() && this.urlPatterns.isEmpty()) {
      builder.append(" urls=").append(Arrays.toString(DEFAULT_URL_MAPPINGS));
    }
    else {
      if (!this.servletNames.isEmpty()) {
        builder.append(" servlets=").append(this.servletNames);
      }
      if (!this.urlPatterns.isEmpty()) {
        builder.append(" urls=").append(this.urlPatterns);
      }
    }
    builder.append(" order=").append(getOrder());
    return builder.toString();
  }

}
