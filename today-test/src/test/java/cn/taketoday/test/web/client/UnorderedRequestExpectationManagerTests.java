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

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.mock.http.client.MockClientHttpRequest;

import static cn.taketoday.http.HttpMethod.GET;
import static cn.taketoday.test.web.client.ExpectedCount.max;
import static cn.taketoday.test.web.client.ExpectedCount.min;
import static cn.taketoday.test.web.client.ExpectedCount.once;
import static cn.taketoday.test.web.client.ExpectedCount.twice;
import static cn.taketoday.test.web.client.match.MockRestRequestMatchers.method;
import static cn.taketoday.test.web.client.match.MockRestRequestMatchers.requestTo;
import static cn.taketoday.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link UnorderedRequestExpectationManager}.
 *
 * @author Rossen Stoyanchev
 */
public class UnorderedRequestExpectationManagerTests {

  private final UnorderedRequestExpectationManager manager = new UnorderedRequestExpectationManager();

  @Test
  public void unexpectedRequest() throws Exception {
    try {
      this.manager.validateRequest(createRequest(GET, "/foo"));
    }
    catch (AssertionError error) {
      assertThat(error.getMessage()).isEqualTo(("No further requests expected: HTTP GET /foo\n" +
              "0 request(s) executed.\n"));
    }
  }

  @Test
  public void zeroExpectedRequests() {
    this.manager.verify();
  }

  @Test
  public void multipleRequests() throws Exception {
    this.manager.expectRequest(once(), requestTo("/foo")).andExpect(method(GET)).andRespond(withSuccess());
    this.manager.expectRequest(once(), requestTo("/bar")).andExpect(method(GET)).andRespond(withSuccess());

    this.manager.validateRequest(createRequest(GET, "/bar"));
    this.manager.validateRequest(createRequest(GET, "/foo"));
    this.manager.verify();
  }

  @Test
  public void repeatedRequests() throws Exception {
    this.manager.expectRequest(twice(), requestTo("/foo")).andExpect(method(GET)).andRespond(withSuccess());
    this.manager.expectRequest(twice(), requestTo("/bar")).andExpect(method(GET)).andRespond(withSuccess());

    this.manager.validateRequest(createRequest(GET, "/bar"));
    this.manager.validateRequest(createRequest(GET, "/foo"));
    this.manager.validateRequest(createRequest(GET, "/foo"));
    this.manager.validateRequest(createRequest(GET, "/bar"));
    this.manager.verify();
  }

  @Test
  public void repeatedRequestsTooMany() throws Exception {
    this.manager.expectRequest(max(2), requestTo("/foo")).andExpect(method(GET)).andRespond(withSuccess());
    this.manager.expectRequest(max(2), requestTo("/bar")).andExpect(method(GET)).andRespond(withSuccess());
    this.manager.validateRequest(createRequest(GET, "/bar"));
    this.manager.validateRequest(createRequest(GET, "/foo"));
    this.manager.validateRequest(createRequest(GET, "/bar"));
    this.manager.validateRequest(createRequest(GET, "/foo"));
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    this.manager.validateRequest(createRequest(GET, "/foo")))
            .withMessage("No further requests expected: HTTP GET /foo\n" +
                    "4 request(s) executed:\n" +
                    "GET /bar\n" +
                    "GET /foo\n" +
                    "GET /bar\n" +
                    "GET /foo\n");
  }

  @Test
  public void repeatedRequestsTooFew() throws Exception {
    this.manager.expectRequest(min(2), requestTo("/foo")).andExpect(method(GET)).andRespond(withSuccess());
    this.manager.expectRequest(min(2), requestTo("/bar")).andExpect(method(GET)).andRespond(withSuccess());
    this.manager.validateRequest(createRequest(GET, "/bar"));
    this.manager.validateRequest(createRequest(GET, "/foo"));
    this.manager.validateRequest(createRequest(GET, "/foo"));
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(this.manager::verify)
            .withMessageContaining("3 request(s) executed:\n" +
                    "GET /bar\n" +
                    "GET /foo\n" +
                    "GET /foo\n");
  }

  private ClientHttpRequest createRequest(HttpMethod method, String url) {
    try {
      return new MockClientHttpRequest(method, new URI(url));
    }
    catch (URISyntaxException ex) {
      throw new IllegalStateException(ex);
    }
  }

}
