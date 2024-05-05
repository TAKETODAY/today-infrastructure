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
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MockHttpMapping implements HttpMockMapping {

  private final String matchValue;

  private final String pattern;

  private final String mockName;

  @Nullable
  private final MappingMatch mappingMatch;

  public MockHttpMapping(String matchValue, String pattern, String mockName, @Nullable MappingMatch match) {
    this.matchValue = matchValue;
    this.pattern = pattern;
    this.mockName = mockName;
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
  public String getMockName() {
    return this.mockName;
  }

  @Override
  @Nullable
  public MappingMatch getMappingMatch() {
    return this.mappingMatch;
  }

  @Override
  public String toString() {
    return "MockHttpServletMapping [matchValue=\"" + this.matchValue + "\", " +
            "pattern=\"" + this.pattern + "\", servletName=\"" + this.mockName + "\", " +
            "mappingMatch=" + this.mappingMatch + "]";
  }

}
