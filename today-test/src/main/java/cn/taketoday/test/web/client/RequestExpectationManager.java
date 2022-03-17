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
import java.time.Duration;

import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.http.client.ClientHttpResponse;

/**
 * Encapsulates the behavior required to implement {@link MockRestServiceServer}
 * including its public API (create expectations + verify/reset) along with an
 * extra method for verifying actual requests.
 *
 * <p>This contract is not used directly in applications but a custom
 * implementation can be
 * {@link MockRestServiceServer.MockRestServiceServerBuilder#build(RequestExpectationManager)
 * plugged} in through the {@code MockRestServiceServer} builder.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface RequestExpectationManager {

  /**
   * Set up a new request expectation. The returned {@link ResponseActions} is
   * used to add more expectations and define a response.
   * <p>This is a delegate for
   * {@link MockRestServiceServer#expect(ExpectedCount, RequestMatcher)}.
   *
   * @param requestMatcher a request expectation
   * @return for setting up further expectations and define a response
   * @see MockRestServiceServer#expect(RequestMatcher)
   * @see MockRestServiceServer#expect(ExpectedCount, RequestMatcher)
   */
  ResponseActions expectRequest(ExpectedCount count, RequestMatcher requestMatcher);

  /**
   * Verify that all expectations have been met.
   * <p>This is a delegate for {@link MockRestServiceServer#verify()}.
   *
   * @throws AssertionError if not all expectations are met
   * @see MockRestServiceServer#verify()
   */
  void verify();

  /**
   * Variant of {@link #verify()} that waits for up to the specified time for
   * all expectations to be fulfilled. This can be useful for tests that
   * involve asynchronous requests.
   *
   * @param timeout how long to wait for all expecations to be met
   * @throws AssertionError if not all expectations are met by the specified
   * timeout, or if any expectation fails at any time before that.
   * @since 5.3.4
   */
  void verify(Duration timeout);

  /**
   * Reset the internal state removing all expectations and recorded requests.
   * <p>This is a delegate for {@link MockRestServiceServer#reset()}.
   *
   * @see MockRestServiceServer#reset()
   */
  void reset();

  /**
   * Validate the given actual request against the declared expectations.
   * Is successful return the mock response to use or raise an error.
   * <p>This is used in {@link MockRestServiceServer} against actual requests.
   *
   * @param request the request
   * @return the response to return if the request was validated.
   * @throws AssertionError when some expectations were not met
   * @throws IOException in case of any validation errors
   */
  ClientHttpResponse validateRequest(ClientHttpRequest request) throws IOException;
}
