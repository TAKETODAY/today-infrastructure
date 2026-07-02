/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.web.mock.assertj;

import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import infra.http.HttpHeaders;
import infra.web.mock.MockResponse;
import infra.test.json.JsonContent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link AbstractMockResponseAssert}.
 *
 * @author Stephane Nicoll
 */
public class AbstractMockResponseAssertTests {

  @Test
  void bodyText() {
    MockResponse response = createResponse("OK");
    assertThat(fromResponse(response)).bodyText().isEqualTo("OK");
  }

  @Test
  void bodyJsonWithJsonPath() {
    MockResponse response = createResponse("{\"albumById\": {\"name\": \"Greatest hits\"}}");
    assertThat(fromResponse(response)).bodyJson()
            .extractingPath("$.albumById.name").isEqualTo("Greatest hits");
  }

  @Test
  void bodyJsonCanLoadResourceRelativeToClass() {
    MockResponse response = createResponse("{ \"name\" : \"INFRA\", \"age\" : 123 }");
    // See infra/test/json/example.json
    assertThat(fromResponse(response)).bodyJson().withResourceLoadClass(JsonContent.class)
            .isLenientlyEqualTo("example.json");
  }

  @Test
  void bodyWithByteArray() throws UnsupportedEncodingException {
    byte[] bytes = "OK".getBytes(StandardCharsets.UTF_8);
    MockResponse response = new MockResponse();
    response.getWriter().write("OK");
    response.setContentType(StandardCharsets.UTF_8.name());
    assertThat(fromResponse(response)).body().isEqualTo(bytes);
  }

  @Test
  void hasBodyTextEqualTo() throws UnsupportedEncodingException {
    MockResponse response = new MockResponse();
    response.getWriter().write("OK");
    response.setContentType(StandardCharsets.UTF_8.name());
    assertThat(fromResponse(response)).hasBodyTextEqualTo("OK");
  }

  @Test
  void hasForwardedUrl() {
    String forwardedUrl = "https://example.com/42";
    MockResponse response = new MockResponse();
    response.setForwardedUrl(forwardedUrl);
    assertThat(fromResponse(response)).hasForwardedUrl(forwardedUrl);
  }

  @Test
  void hasForwardedUrlWithWrongValue() {
    String forwardedUrl = "https://example.com/42";
    MockResponse response = new MockResponse();
    response.setForwardedUrl(forwardedUrl);
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(fromResponse(response)).hasForwardedUrl("another"))
            .withMessageContainingAll("Forwarded URL", forwardedUrl, "another");
  }

  @Test
  void hasRedirectedUrl() {
    String redirectedUrl = "https://example.com/42";
    MockResponse response = new MockResponse();
    response.addHeader(HttpHeaders.LOCATION, redirectedUrl);
    assertThat(fromResponse(response)).hasRedirectedUrl(redirectedUrl);
  }

  @Test
  void hasRedirectedUrlWithWrongValue() {
    String redirectedUrl = "https://example.com/42";
    MockResponse response = new MockResponse();
    response.addHeader(HttpHeaders.LOCATION, redirectedUrl);
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(fromResponse(response)).hasRedirectedUrl("another"))
            .withMessageContainingAll("Redirected URL", redirectedUrl, "another");
  }

  @Test
  void hasServletErrorMessage() throws Exception {
    MockResponse response = new MockResponse();
    response.sendError(403, "expected error message");
    assertThat(fromResponse(response)).hasErrorMessage("expected error message");
  }

  private MockResponse createResponse(String body) {
    try {
      MockResponse response = new MockResponse();
      response.setContentType(StandardCharsets.UTF_8.name());
      response.getWriter().write(body);
      return response;
    }
    catch (UnsupportedEncodingException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static AssertProvider<ResponseAssert> fromResponse(MockResponse response) {
    return () -> new ResponseAssert(response);
  }

  private static final class ResponseAssert extends AbstractMockResponseAssert<ResponseAssert, MockResponse> {

    ResponseAssert(MockResponse actual) {
      super(null, actual, ResponseAssert.class);
    }

    @Override
    protected MockResponse getResponse() {
      return this.actual;
    }

  }

}
