/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.mock.web;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import infra.http.HttpHeaders;
import infra.http.ResponseTrailer;
import infra.web.mock.MockHttpContext;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Contract tests for response trailer registration, run against
 * {@link MockHttpContext}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class MockHttpContextTrailerTests {

  @Test
  void evaluatesTrailersOnCompletion() {
    MockHttpContext context = new MockHttpContext();
    context.setResponseTrailers(
            ResponseTrailer.of("Content-Digest", () -> "sha-256=:abc:"));

    context.requestCompleted();

    HttpHeaders trailers = context.getResponseTrailers();
    assertThat(trailers).isNotNull();
    assertThat(trailers.get("Content-Digest")).containsExactly("sha-256=:abc:");
  }

  @Test
  void trailersAreCopiedDefensively() {
    MockHttpContext context = new MockHttpContext();
    ResponseTrailer[] trailers = { ResponseTrailer.of("X-Checksum", () -> "v1") };
    context.setResponseTrailers(trailers);

    trailers[0] = ResponseTrailer.of("X-Other", () -> "v2");

    context.requestCompleted();

    HttpHeaders result = context.getResponseTrailers();
    assertThat(result).isNotNull();
    assertThat(result.contains("X-Checksum")).isTrue();
    assertThat(result.contains("X-Other")).isFalse();
  }

  @Test
  void laterRegistrationReplacesPreviousOne() {
    MockHttpContext context = new MockHttpContext();
    context.setResponseTrailers(ResponseTrailer.of("X-First", () -> "1"));
    context.setResponseTrailers(ResponseTrailer.of("X-Second", () -> "2"));

    context.requestCompleted();

    HttpHeaders trailers = context.getResponseTrailers();
    assertThat(trailers).isNotNull();
    assertThat(trailers.contains("X-Second")).isTrue();
    assertThat(trailers.contains("X-First")).isFalse();
  }

  @Test
  void sameNameProducesMultipleValues() {
    MockHttpContext context = new MockHttpContext();
    context.setResponseTrailers(
            ResponseTrailer.of("X-Checksum", () -> "v1"),
            ResponseTrailer.of("X-Checksum", () -> "v2"));

    context.requestCompleted();

    HttpHeaders trailers = context.getResponseTrailers();
    assertThat(trailers).isNotNull();
    assertThat(trailers.get("X-Checksum")).containsExactly("v1", "v2");
  }

  @Test
  void resetClearsRegistration() {
    MockHttpContext context = new MockHttpContext();
    context.setResponseTrailers(ResponseTrailer.of("X-Checksum", () -> "v1"));

    context.reset();
    context.requestCompleted();

    assertThat(context.getResponseTrailers()).isNull();
  }

  @Test
  void sendErrorClearsRegistration() throws Exception {
    MockHttpContext context = new MockHttpContext();
    AtomicInteger calls = new AtomicInteger();
    context.setResponseTrailers(ResponseTrailer.of("X-Checksum", () -> {
      calls.incrementAndGet();
      return "v1";
    }));

    context.sendError(500);
    context.requestCompleted();

    assertThat(calls).hasValue(0);
    assertThat(context.getResponseTrailers()).isNull();
  }

  @Test
  void rejectsEmptyRegistration() {
    MockHttpContext context = new MockHttpContext();
    assertThatIllegalArgumentException()
            .isThrownBy(context::setResponseTrailers);
  }

  @Test
  void rejectsInvalidFieldName() {
    MockHttpContext context = new MockHttpContext();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> context.setResponseTrailers(
                    ResponseTrailer.of("Bad Name", () -> "1")));
  }

  @Test
  void rejectsForbiddenFieldNameIgnoringCase() {
    MockHttpContext context = new MockHttpContext();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> context.setResponseTrailers(
                    ResponseTrailer.of("content-length", () -> "1")));
  }

  @Test
  void rejectsRegistrationAfterCommit() {
    MockHttpContext context = new MockHttpContext();
    context.response.setCommitted(true);

    assertThatIllegalStateException()
            .isThrownBy(() -> context.setResponseTrailers(
                    ResponseTrailer.of("X-A", () -> "1")));
  }

  @Test
  void nullValueSkipsField() {
    MockHttpContext context = new MockHttpContext();
    context.setResponseTrailers(
            ResponseTrailer.of("X-Skipped", () -> null),
            ResponseTrailer.of("X-Present", () -> "v1"));

    context.requestCompleted();

    HttpHeaders trailers = context.getResponseTrailers();
    assertThat(trailers).isNotNull();
    assertThat(trailers.contains("X-Skipped")).isFalse();
    assertThat(trailers.get("X-Present")).containsExactly("v1");
  }

  @Test
  void rejectsLineBreaksInValues() {
    MockHttpContext context = new MockHttpContext();
    context.setResponseTrailers(
            ResponseTrailer.of("X-Checksum", () -> "v1\r\nInjected: 1"));

    assertThatIllegalStateException()
            .isThrownBy(context::requestCompleted)
            .withMessageContaining("Invalid trailer value");
  }

  @Test
  void allValuesSkippedProducesNoTrailers() {
    MockHttpContext context = new MockHttpContext();
    context.setResponseTrailers(
            ResponseTrailer.of("X-First", () -> null),
            ResponseTrailer.of("X-Second", () -> null));

    context.requestCompleted();

    assertThat(context.getResponseTrailers()).isNull();
  }

  @Test
  void supplierRunsAtMostOnce() {
    MockHttpContext context = new MockHttpContext();
    AtomicInteger calls = new AtomicInteger();
    context.setResponseTrailers(ResponseTrailer.of("X-Checksum", () -> {
      calls.incrementAndGet();
      return "v1";
    }));

    context.requestCompleted();
    context.requestCompleted();

    assertThat(calls).hasValue(1);
    HttpHeaders trailers = context.getResponseTrailers();
    assertThat(trailers).isNotNull();
    assertThat(trailers.contains("X-Checksum")).isTrue();
  }

  @Test
  void failedRequestDoesNotEvaluateSupplier() {
    ErrorCompletingMockHttpContext context = new ErrorCompletingMockHttpContext();
    AtomicInteger calls = new AtomicInteger();
    context.setResponseTrailers(ResponseTrailer.of("X-Checksum", () -> {
      calls.incrementAndGet();
      return "v1";
    }));

    context.completeWithError();

    assertThat(calls).hasValue(0);
    assertThat(context.getResponseTrailers()).isNull();
  }

  /**
   * Exposes the error completion path, which is protected on the base class.
   */
  static final class ErrorCompletingMockHttpContext extends MockHttpContext {

    ErrorCompletingMockHttpContext() {
      super(new MockRequest(), new MockResponse());
    }

    void completeWithError() {
      requestCompleted(new IllegalStateException("boom"));
    }
  }

}
