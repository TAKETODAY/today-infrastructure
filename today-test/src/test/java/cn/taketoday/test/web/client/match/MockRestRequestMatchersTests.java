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

package cn.taketoday.test.web.client.match;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.mock.http.client.MockClientHttpRequest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;

/**
 * Unit tests for {@link MockRestRequestMatchers}.
 *
 * @author Craig Walls
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class MockRestRequestMatchersTests {

  private final MockClientHttpRequest request = new MockClientHttpRequest();

  @Test
  public void requestTo() throws Exception {
    this.request.setURI(new URI("http://www.foo.example/bar"));

    MockRestRequestMatchers.requestTo("http://www.foo.example/bar").match(this.request);
  }

  @Test  // SPR-15819
  public void requestToUriTemplate() throws Exception {
    this.request.setURI(new URI("http://www.foo.example/bar"));

    MockRestRequestMatchers.requestToUriTemplate("http://www.foo.example/{bar}", "bar").match(this.request);
  }

  @Test
  public void requestToNoMatch() throws Exception {
    this.request.setURI(new URI("http://www.foo.example/bar"));

    assertThatThrownBy(
            () -> MockRestRequestMatchers.requestTo("http://www.foo.example/wrong").match(this.request))
            .isInstanceOf(AssertionError.class);
  }

  @Test
  public void requestToContains() throws Exception {
    this.request.setURI(new URI("http://www.foo.example/bar"));

    MockRestRequestMatchers.requestTo(containsString("bar")).match(this.request);
  }

  @Test
  public void method() throws Exception {
    this.request.setMethod(HttpMethod.GET);

    MockRestRequestMatchers.method(HttpMethod.GET).match(this.request);
  }

  @Test
  public void methodNoMatch() throws Exception {
    this.request.setMethod(HttpMethod.POST);

    assertThatThrownBy(() -> MockRestRequestMatchers.method(HttpMethod.GET).match(this.request))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("expected:<GET> but was:<POST>");
  }

  @Test
  public void header() throws Exception {
    this.request.getHeaders().put("foo", Arrays.asList("bar", "baz"));

    MockRestRequestMatchers.header("foo", "bar", "baz").match(this.request);
  }

  @Test
  public void headerDoesNotExist() throws Exception {
    MockRestRequestMatchers.headerDoesNotExist(null).match(this.request);
    MockRestRequestMatchers.headerDoesNotExist("").match(this.request);
    MockRestRequestMatchers.headerDoesNotExist("foo").match(this.request);

    List<String> values = Arrays.asList("bar", "baz");
    this.request.getHeaders().put("foo", values);
    assertThatThrownBy(() -> MockRestRequestMatchers.headerDoesNotExist("foo").match(this.request))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected header <foo> not to exist, but it exists with values: " + values);
  }

  @Test
  public void headerMissing() throws Exception {
    assertThatThrownBy(() -> MockRestRequestMatchers.header("foo", "bar").match(this.request))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("was null");
  }

  @Test
  public void headerMissingValue() throws Exception {
    this.request.getHeaders().put("foo", Arrays.asList("bar", "baz"));

    assertThatThrownBy(() -> MockRestRequestMatchers.header("foo", "bad").match(this.request))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("expected:<bad> but was:<bar>");
  }

  @Test
  public void headerContains() throws Exception {
    this.request.getHeaders().put("foo", Arrays.asList("bar", "baz"));

    MockRestRequestMatchers.header("foo", containsString("ba")).match(this.request);
  }

  @Test
  public void headerContainsWithMissingHeader() throws Exception {
    assertThatThrownBy(() -> MockRestRequestMatchers.header("foo", containsString("baz")).match(this.request))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("but was null");
  }

  @Test
  public void headerContainsWithMissingValue() throws Exception {
    this.request.getHeaders().put("foo", Arrays.asList("bar", "baz"));

    assertThatThrownBy(() -> MockRestRequestMatchers.header("foo", containsString("bx")).match(this.request))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("was \"bar\"");
  }

  @Test
  public void headers() throws Exception {
    this.request.getHeaders().put("foo", Arrays.asList("bar", "baz"));

    MockRestRequestMatchers.header("foo", "bar", "baz").match(this.request);
  }

  @Test
  public void headersWithMissingHeader() throws Exception {
    assertThatThrownBy(() -> MockRestRequestMatchers.header("foo", "bar").match(this.request))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("but was null");
  }

  @Test
  public void headersWithMissingValue() throws Exception {
    this.request.getHeaders().put("foo", Collections.singletonList("bar"));

    assertThatThrownBy(() -> MockRestRequestMatchers.header("foo", "bar", "baz").match(this.request))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("to have at least <2> values");
  }

  @Test
  public void queryParam() throws Exception {
    this.request.setURI(new URI("http://www.foo.example/a?foo=bar&foo=baz"));

    MockRestRequestMatchers.queryParam("foo", "bar", "baz").match(this.request);
  }

  @Test
  public void queryParamMissing() throws Exception {
    this.request.setURI(new URI("http://www.foo.example/a"));

    assertThatThrownBy(() -> MockRestRequestMatchers.queryParam("foo", "bar").match(this.request))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("but was null");
  }

  @Test
  public void queryParamMissingValue() throws Exception {
    this.request.setURI(new URI("http://www.foo.example/a?foo=bar&foo=baz"));

    assertThatThrownBy(() -> MockRestRequestMatchers.queryParam("foo", "bad").match(this.request))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("expected:<bad> but was:<bar>");
  }

  @Test
  public void queryParamContains() throws Exception {
    this.request.setURI(new URI("http://www.foo.example/a?foo=bar&foo=baz"));

    MockRestRequestMatchers.queryParam("foo", containsString("ba")).match(this.request);
  }

  @Test
  public void queryParamContainsWithMissingValue() throws Exception {
    this.request.setURI(new URI("http://www.foo.example/a?foo=bar&foo=baz"));

    assertThatThrownBy(() -> MockRestRequestMatchers.queryParam("foo", containsString("bx")).match(this.request))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("was \"bar\"");
  }

}
