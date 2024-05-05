/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.mock.web;

import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.http.HttpMockMapping;
import cn.taketoday.mock.api.http.MappingMatch;

/**
 * Mock implementation of {@link HttpMockMapping}.
 *
 * <p>Currently not exposed in {@link HttpMockRequestImpl} as a setter to
 * avoid issues for Maven builds in applications with a Servlet 3.1 runtime
 * requirement.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MockHttpMapping implements HttpMockMapping {

  private final String matchValue;

  private final String pattern;

  private final String servletName;

  @Nullable
  private final MappingMatch mappingMatch;

  public MockHttpMapping(
          String matchValue, String pattern, String servletName, @Nullable MappingMatch match) {

    this.matchValue = matchValue;
    this.pattern = pattern;
    this.servletName = servletName;
    this.mappingMatch = match;
  }

  @Override
  public String getMatchValue() {
    return this.matchValue;
  }

  @Override
  public String getPattern() {
    return this.pattern;
  }

  @Override
  public String getServletName() {
    return this.servletName;
  }

  @Override
  @Nullable
  public MappingMatch getMappingMatch() {
    return this.mappingMatch;
  }

  @Override
  public String toString() {
    return "MockHttpServletMapping [matchValue=\"" + this.matchValue + "\", " +
            "pattern=\"" + this.pattern + "\", servletName=\"" + this.servletName + "\", " +
            "mappingMatch=" + this.mappingMatch + "]";
  }

}
