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

package infra.test.web.mock.result;

import org.junit.jupiter.api.Test;

import infra.mock.web.MockHttpResponseImpl;
import infra.test.web.mock.StubMvcResult;

import static infra.test.web.mock.result.MockMvcResultMatchers.forwardedUrl;
import static infra.test.web.mock.result.MockMvcResultMatchers.forwardedUrlPattern;
import static infra.test.web.mock.result.MockMvcResultMatchers.forwardedUrlTemplate;
import static infra.test.web.mock.result.MockMvcResultMatchers.redirectedUrl;
import static infra.test.web.mock.result.MockMvcResultMatchers.redirectedUrlPattern;
import static infra.test.web.mock.result.MockMvcResultMatchers.redirectedUrlTemplate;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link MockMvcResultMatchers}.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 */
public class MockMvcResultMatchersTests {

  @Test
  public void redirect() throws Exception {
    assertThatCode(() -> redirectedUrl("/resource/1").match(redirectedUrlStub("/resource/1")))
            .doesNotThrowAnyException();
  }

  @Test
  public void redirectNonMatching() throws Exception {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> redirectedUrl("/resource/2").match(redirectedUrlStub("/resource/1")))
            .withMessageEndingWith("expected:</resource/2> but was:</resource/1>");
  }

  @Test
  public void redirectNonMatchingBecauseNotRedirect() throws Exception {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> redirectedUrl("/resource/1").match(forwardedUrlStub("/resource/1")))
            .withMessageEndingWith("expected:</resource/1> but was:<null>");
  }

  @Test
  public void redirectWithUrlTemplate() throws Exception {
    assertThatCode(() -> redirectedUrlTemplate("/orders/{orderId}/items/{itemId}", 1, 2).match(redirectedUrlStub("/orders/1/items/2")))
            .doesNotThrowAnyException();
  }

  @Test
  public void redirectWithMatchingPattern() throws Exception {
    assertThatCode(() -> redirectedUrlPattern("/resource/*").match(redirectedUrlStub("/resource/1")))
            .doesNotThrowAnyException();
  }

  @Test
  public void redirectWithNonMatchingPattern() throws Exception {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> redirectedUrlPattern("/resource/").match(redirectedUrlStub("/resource/1")))
            .withMessage("'/resource/' is not an Ant-style path pattern");
  }

  @Test
  public void redirectWithNonMatchingPatternBecauseNotRedirect() {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> redirectedUrlPattern("/resource/*").match(forwardedUrlStub("/resource/1")))
            .withMessage("Redirected URL 'null' does not match the expected URL pattern '/resource/*'");
  }

  @Test
  public void forward() throws Exception {
    assertThatCode(() -> forwardedUrl("/api/resource/1").match(forwardedUrlStub("/api/resource/1")))
            .doesNotThrowAnyException();
  }

  @Test
  public void forwardNonMatching() {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> forwardedUrlPattern("api/resource/2").match(forwardedUrlStub("api/resource/1")))
            .withMessage("'api/resource/2' is not an Ant-style path pattern");
  }

  @Test
  public void forwardNonMatchingBecauseNotForward() {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> forwardedUrlPattern("/resource/*").match(redirectedUrlStub("/resource/1")))
            .withMessage("Forwarded URL 'null' does not match the expected URL pattern '/resource/*'");
  }

  @Test
  public void forwardWithQueryString() throws Exception {
    assertThatCode(() -> forwardedUrl("/api/resource/1?arg=value").match(forwardedUrlStub("/api/resource/1?arg=value")))
            .doesNotThrowAnyException();
  }

  @Test
  public void forwardWithUrlTemplate() throws Exception {
    assertThatCode(() -> forwardedUrlTemplate("/orders/{orderId}/items/{itemId}", 1, 2).match(forwardedUrlStub("/orders/1/items/2")))
            .doesNotThrowAnyException();
  }

  @Test
  public void forwardWithMatchingPattern() throws Exception {
    assertThatCode(() -> forwardedUrlPattern("/api/**/?").match(forwardedUrlStub("/api/resource/1")))
            .doesNotThrowAnyException();
  }

  @Test
  public void forwardWithNonMatchingPattern() throws Exception {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> forwardedUrlPattern("/resource/").match(forwardedUrlStub("/resource/1")))
            .withMessage("'/resource/' is not an Ant-style path pattern");
  }

  @Test
  public void forwardWithNonMatchingPatternBecauseNotForward() {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> forwardedUrlPattern("/resource/*").match(redirectedUrlStub("/resource/1")))
            .withMessage("Forwarded URL 'null' does not match the expected URL pattern '/resource/*'");
  }

  private StubMvcResult redirectedUrlStub(String redirectUrl) throws Exception {
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    response.sendRedirect(redirectUrl);
    return new StubMvcResult(null, null, null, null, null, null, response);
  }

  private StubMvcResult forwardedUrlStub(String forwardedUrl) {
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    response.setForwardedUrl(forwardedUrl);
    return new StubMvcResult(null, null, null, null, null, null, response);
  }

}
