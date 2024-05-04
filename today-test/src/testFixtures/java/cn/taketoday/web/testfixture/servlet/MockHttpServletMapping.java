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

package cn.taketoday.web.testfixture.servlet;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.mock.http.HttpServletMapping;
import cn.taketoday.web.mock.http.MappingMatch;

/**
 * Mock implementation of {@link HttpServletMapping}.
 *
 * @author Rossen Stoyanchev
 */
public class MockHttpServletMapping implements HttpServletMapping {

  private final String matchValue;

  private final String pattern;

  private final String servletName;

  @Nullable
  private final MappingMatch mappingMatch;

  public MockHttpServletMapping(
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
