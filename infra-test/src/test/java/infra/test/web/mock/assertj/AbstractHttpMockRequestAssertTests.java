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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import infra.mock.api.http.HttpMockRequest;
import infra.mock.api.http.HttpSession;
import infra.mock.web.HttpMockRequestImpl;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link AbstractHttpMockRequestAssert}.
 *
 * @author Stephane Nicoll
 */
public class AbstractHttpMockRequestAssertTests {

  @Nested
  class AttributesTests {

    @Test
    void attributesAreCopied() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("one", 1);
      map.put("two", 2);
      assertThat(createRequest(map)).attributes()
              .containsExactly(entry("one", 1), entry("two", 2));
    }

    @Test
    void attributesWithWrongKey() {
      HttpMockRequest request = createRequest(Map.of("one", 1));
      assertThatExceptionOfType(AssertionError.class)
              .isThrownBy(() -> assertThat(request).attributes().containsKey("two"))
              .withMessageContainingAll("Request Attributes", "two", "one");
    }

    private HttpMockRequest createRequest(Map<String, Object> attributes) {
      HttpMockRequestImpl request = new HttpMockRequestImpl();
      attributes.forEach(request::setAttribute);
      return request;
    }

  }

  @Nested
  class SessionAttributesTests {

    @Test
    void sessionAttributesAreCopied() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("one", 1);
      map.put("two", 2);
      assertThat(createRequest(map)).sessionAttributes()
              .containsExactly(entry("one", 1), entry("two", 2));
    }

    @Test
    void sessionAttributesWithWrongKey() {
      HttpMockRequest request = createRequest(Map.of("one", 1));
      assertThatExceptionOfType(AssertionError.class)
              .isThrownBy(() -> assertThat(request).sessionAttributes().containsKey("two"))
              .withMessageContainingAll("Session Attributes", "two", "one");
    }

    private HttpMockRequest createRequest(Map<String, Object> attributes) {
      HttpMockRequestImpl request = new HttpMockRequestImpl();
      HttpSession session = request.getSession();
      attributes.forEach(session::setAttribute);
      return request;
    }

  }

  @Test
  void hasAsyncStartedTrue() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setAsyncStarted(true);
    assertThat(request).hasAsyncStarted(true);
  }

  @Test
  void hasAsyncStartedTrueWithFalse() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setAsyncStarted(false);
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(request).hasAsyncStarted(true))
            .withMessage("Async expected to have started");
  }

  @Test
  void hasAsyncStartedFalse() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setAsyncStarted(false);
    assertThat(request).hasAsyncStarted(false);
  }

  @Test
  void hasAsyncStartedFalseWithTrue() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setAsyncStarted(true);
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(request).hasAsyncStarted(false))
            .withMessage("Async expected not to have started");
  }

  private static RequestAssert assertThat(HttpMockRequest request) {
    return new RequestAssert(request);
  }

  private static final class RequestAssert extends AbstractHttpMockRequestAssert<RequestAssert, HttpMockRequest> {

    RequestAssert(HttpMockRequest actual) {
      super(actual, RequestAssert.class);
    }
  }

}
