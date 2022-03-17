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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.lang.Assert;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.StringUtils;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.FilterRegistration.Dynamic;
import jakarta.servlet.ServletContext;

/**
 * A {@link ServletContextInitializer} to register {@link Filter}s in a Servlet 3.0+
 * container. Similar to the {@link ServletContext#addFilter(String, Filter) registration}
 * features provided by {@link ServletContext} but with a Bean friendly design.
 * <p>
 *
 * @param <T> the type of {@link Filter} to register
 * @author TODAY
 * @since 2019-02-03 13:22
 */
public class WebFilterInitializer<T extends Filter>
        extends WebComponentInitializer<FilterRegistration.Dynamic> {

  private static final String[] DEFAULT_URL_MAPPINGS = { "/*" };

  private T filter;
  private boolean matchAfter;
  private DispatcherType[] dispatcherTypes;

  private Set<String> servletNames = new LinkedHashSet<>();

  public WebFilterInitializer() { }

  public WebFilterInitializer(T filter) {
    this.filter = filter;
  }

  @Override
  protected Dynamic addRegistration(ServletContext servletContext) {
    final T filter = getFilter();
    Assert.state(filter != null, "filter can't be null");
    String name = getOrDeduceName(filter);
    return servletContext.addFilter(name, filter);
  }

  @Override
  protected void configureRegistration(Dynamic registration) {
    LoggerFactory.getLogger(WebFilterInitializer.class).debug("Configure filter registration: [{}]", this);

    super.configureRegistration(registration);

    EnumSet<DispatcherType> dispatcherTypes;
    if (this.dispatcherTypes == null) {
      dispatcherTypes = EnumSet.of(DispatcherType.REQUEST);
    }
    else {
      dispatcherTypes = EnumSet.noneOf(DispatcherType.class);
      Collections.addAll(dispatcherTypes, this.dispatcherTypes);
    }

    final Collection<String> urlMappings = getUrlMappings();

    if (servletNames.isEmpty() && urlMappings.isEmpty()) {
      registration.addMappingForUrlPatterns(dispatcherTypes, this.matchAfter, DEFAULT_MAPPINGS);
    }
    else {
      if (!servletNames.isEmpty()) {
        registration.addMappingForServletNames(dispatcherTypes, this.matchAfter, StringUtils.toStringArray(servletNames));
      }
      if (!urlMappings.isEmpty()) {
        registration.addMappingForUrlPatterns(dispatcherTypes, this.matchAfter, StringUtils.toStringArray(urlMappings));
      }
    }
  }

  public T getFilter() {
    return this.filter;
  }

  /**
   * Set the filter to be registered.
   *
   * @param filter the filter
   */
  public void setFilter(T filter) {
    Assert.notNull(filter, "Filter must not be null");
    this.filter = filter;
  }

  public DispatcherType[] getDispatcherTypes() {
    return dispatcherTypes;
  }

  public Set<String> getServletNames() {
    return servletNames;
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

  public void setDispatcherTypes(DispatcherType... dispatcherTypes) {
    this.dispatcherTypes = dispatcherTypes;
  }

  public void setServletNames(Set<String> servletNames) {
    this.servletNames = servletNames;
  }

  public void addServletNames(String... servletNames) {
    Collections.addAll(this.servletNames, servletNames);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(getOrDeduceName(this));
    if (this.servletNames.isEmpty() && getUrlMappings().isEmpty()) {
      builder.append(" urls=").append(Arrays.toString(DEFAULT_URL_MAPPINGS));
    }
    else {
      if (!this.servletNames.isEmpty()) {
        builder.append(" servlets=").append(this.servletNames);
      }
      if (!getUrlMappings().isEmpty()) {
        builder.append(" urls=").append(getUrlMappings());
      }
    }
    builder.append(" order=").append(getOrder());
    return builder.toString();
  }

}
