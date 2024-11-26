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

package infra.test.web.client;

import java.io.IOException;

import infra.http.client.ClientHttpRequest;
import infra.test.web.client.match.MockRestRequestMatchers;

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
