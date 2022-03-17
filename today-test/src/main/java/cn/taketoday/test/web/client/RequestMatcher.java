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

package cn.taketoday.test.web.client;

import java.io.IOException;

import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.test.web.client.match.MockRestRequestMatchers;

/**
 * A contract for matching requests to expectations.
 *
 * <p>See {@link MockRestRequestMatchers
 * MockRestRequestMatchers} for static factory methods.
 *
 * @author Craig Walls
 * @since 4.0
 */
@FunctionalInterface
public interface RequestMatcher {

  /**
   * Match the given request against specific expectations.
   *
   * @param request the request to make assertions on
   * @throws IOException in case of I/O errors
   * @throws AssertionError if expectations are not met
   */
  void match(ClientHttpRequest request) throws IOException, AssertionError;

}
