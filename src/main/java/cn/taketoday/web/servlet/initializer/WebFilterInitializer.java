/**
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

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletContext;

import cn.taketoday.lang.Assert;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY <br>
 * 2019-02-03 13:22
 */
public class WebFilterInitializer<T extends Filter>
        extends WebComponentInitializer<FilterRegistration.Dynamic> {

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
    return servletContext.addFilter(getName(), filter);
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
    return filter;
  }

  public boolean isMatchAfter() {
    return matchAfter;
  }

  public DispatcherType[] getDispatcherTypes() {
    return dispatcherTypes;
  }

  public Set<String> getServletNames() {
    return servletNames;
  }

  public WebFilterInitializer<T> setFilter(T filter) {
    this.filter = filter;
    return this;
  }

  public WebFilterInitializer<T> setMatchAfter(boolean matchAfter) {
    this.matchAfter = matchAfter;
    return this;
  }

  public WebFilterInitializer<T> setDispatcherTypes(DispatcherType[] dispatcherTypes) {
    this.dispatcherTypes = dispatcherTypes;
    return this;
  }

  public WebFilterInitializer<T> setServletNames(Set<String> servletNames) {
    this.servletNames = servletNames;
    return this;
  }

  public WebFilterInitializer<T> addServletNames(String... servletNames) {
    Collections.addAll(this.servletNames, servletNames);
    return this;
  }

  @Override
  protected String getDefaultName() {

    final T t = getFilter();
    if (t != null) {
      return t.getClass().getName();
    }
    return null;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("{\n\t\"filter\":\"");
    builder.append(filter);
    builder.append("\",\n\t\"matchAfter\":\"");
    builder.append(matchAfter);
    builder.append("\",\n\t\"dispatcherTypes\":\"");
    builder.append(Arrays.toString(dispatcherTypes));
    builder.append("\",\n\t\"servletNames\":\"");
    builder.append(servletNames);
    builder.append("\",\n\t\"initParameters\":\"");
    builder.append(getInitParameters());
    builder.append("\",\n\t\"urlMappings\":\"");
    builder.append(getUrlMappings());
    builder.append("\",\n\t\"order\":\"");
    builder.append(getOrder());
    builder.append("\",\n\t\"name\":\"");
    builder.append(getName());
    builder.append("\",\n\t\"asyncSupported\":\"");
    builder.append(isAsyncSupported());
    builder.append("\"\n}");
    return builder.toString();
  }

}
