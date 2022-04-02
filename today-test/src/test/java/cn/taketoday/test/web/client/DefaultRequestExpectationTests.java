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

import static cn.taketoday.http.HttpMethod.POST;
import static cn.taketoday.test.web.client.ExpectedCount.once;
import static cn.taketoday.test.web.client.ExpectedCount.twice;
import static cn.taketoday.test.web.client.match.MockRestRequestMatchers.method;
import static cn.taketoday.test.web.client.match.MockRestRequestMatchers.requestTo;
import static cn.taketoday.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link DefaultRequestExpectation}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultRequestExpectationTests {

  @Test
  public void match() throws Exception {
    RequestExpectation expectation = new DefaultRequestExpectation(once(), requestTo("/foo"));
    expectation.match(createRequest());
  }

  @Test
  public void matchWithFailedExpectation() {
    RequestExpectation expectation = new DefaultRequestExpectation(once(), requestTo("/foo"));
    expectation.andExpect(method(POST));
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    expectation.match(createRequest()))
            .withMessageContaining("Unexpected HttpMethod expected:<POST> but was:<GET>");
  }

  @Test
  public void hasRemainingCount() {
    RequestExpectation expectation = new DefaultRequestExpectation(twice(), requestTo("/foo"));
    expectation.andRespond(withSuccess());

    expectation.incrementAndValidate();
    assertThat(expectation.hasRemainingCount()).isTrue();

    expectation.incrementAndValidate();
    assertThat(expectation.hasRemainingCount()).isFalse();
  }

  @Test
  public void isSatisfied() {
    RequestExpectation expectation = new DefaultRequestExpectation(twice(), requestTo("/foo"));
    expectation.andRespond(withSuccess());

    expectation.incrementAndValidate();
    assertThat(expectation.isSatisfied()).isFalse();

    expectation.incrementAndValidate();
    assertThat(expectation.isSatisfied()).isTrue();
  }

  private ClientHttpRequest createRequest() {
    try {
      return new MockClientHttpRequest(HttpMethod.GET, new URI("/foo"));
    }
    catch (URISyntaxException ex) {
      throw new IllegalStateException(ex);
    }
  }

}
