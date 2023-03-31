/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.web.servlet.htmlunit;

import com.gargoylesoftware.htmlunit.WebRequest;

/**
 * Strategy for matching on a {@link WebRequest}.
 *
 * @author Rob Winch
 * @see cn.taketoday.test.web.servlet.htmlunit.HostRequestMatcher
 * @see cn.taketoday.test.web.servlet.htmlunit.UrlRegexRequestMatcher
 * @since 4.0
 */
@FunctionalInterface
public interface WebRequestMatcher {

  /**
   * Whether this matcher matches on the supplied web request.
   *
   * @param request the {@link WebRequest} to attempt to match on
   * @return {@code true} if this matcher matches on the {@code WebRequest}
   */
  boolean matches(WebRequest request);

}
