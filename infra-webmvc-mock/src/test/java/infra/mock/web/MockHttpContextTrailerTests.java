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
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import infra.http.HttpHeaders;
import infra.web.Trailer;
import infra.web.HttpContext;
import infra.web.mock.MockHttpContext;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    context.setTrailerFields(
            Trailer.field("Content-Digest", () -> "sha-256=:abc:"));

    context.requestCompleted();

    HttpHeaders trailers = context.getResponseTrailers();
    assertThat(trailers).isNotNull();
    assertThat(trailers.get("Content-Digest")).containsExactly("sha-256=:abc:");
  }

  @Test
  void trailersAreCopiedDefensively() {
    MockHttpContext context = new MockHttpContext();
    Trailer[] trailers = { Trailer.field("X-Checksum", () -> "v1") };
    context.setTrailerFields(trailers);

    trailers[0] = Trailer.field("X-Other", () -> "v2");

    context.requestCompleted();

    HttpHeaders result = context.getResponseTrailers();
    assertThat(result).isNotNull();
    assertThat(result.contains("X-Checksum")).isTrue();
    assertThat(result.contains("X-Other")).isFalse();
  }

  @Test
  void laterRegistrationReplacesPreviousOne() {
    MockHttpContext context = new MockHttpContext();
    context.setTrailerFields(Trailer.field("X-First", () -> "1"));
    context.setTrailerFields(Trailer.field("X-Second", () -> "2"));

    context.requestCompleted();

    HttpHeaders trailers = context.getResponseTrailers();
    assertThat(trailers).isNotNull();
    assertThat(trailers.contains("X-Second")).isTrue();
    assertThat(trailers.contains("X-First")).isFalse();
  }

  @Test
  void sameNameProducesMultipleValues() {
    MockHttpContext context = new MockHttpContext();
    context.setTrailerFields(
            Trailer.field("X-Checksum", () -> "v1"),
            Trailer.field("X-Checksum", () -> "v2"));

    context.requestCompleted();

    HttpHeaders trailers = context.getResponseTrailers();
    assertThat(trailers).isNotNull();
    assertThat(trailers.get("X-Checksum")).containsExactly("v1", "v2");
  }

  @Test
  void resetClearsRegistration() {
    MockHttpContext context = new MockHttpContext();
    context.setTrailerFields(Trailer.field("X-Checksum", () -> "v1"));

    context.reset();
    context.requestCompleted();

    assertThat(context.getResponseTrailers()).isNull();
  }

  @Test
  void sendErrorClearsRegistration() throws Exception {
    MockHttpContext context = new MockHttpContext();
    AtomicInteger calls = new AtomicInteger();
    context.setTrailerFields(Trailer.field("X-Checksum", () -> {
      calls.incrementAndGet();
      return "v1";
    }));

    context.sendError(500);
    context.requestCompleted();

    assertThat(calls).hasValue(0);
    assertThat(context.getResponseTrailers()).isNull();
  }

  @Test
  void emptyRegistrationClearsTrailersWithoutResettingResponse() {
    MockHttpContext context = new MockHttpContext();
    AtomicInteger calls = new AtomicInteger();
    context.setStatus(202);
    context.responseHeaders().set("X-Preserved", "value");
    context.setTrailerFields(Trailer.field("X-Checksum", () -> {
      calls.incrementAndGet();
      return "v1";
    }));

    context.setTrailerFields();
    context.requestCompleted();

    assertThat(context.getStatus()).isEqualTo(202);
    assertThat(context.responseHeaders().getFirst("X-Preserved")).isEqualTo("value");
    assertThat(context.getResponseTrailers()).isNull();
    assertThat(calls).hasValue(0);
  }

  @Test
  void rejectsInvalidFieldName() {
    MockHttpContext context = new MockHttpContext();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> context.setTrailerFields(
                    Trailer.field("Bad Name", () -> "1")));
  }

  @Test
  void rejectsForbiddenFieldNameIgnoringCase() {
    MockHttpContext context = new MockHttpContext();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> context.setTrailerFields(
                    Trailer.field("content-length", () -> "1")));
  }

  @Test
  void rejectsRegistrationAfterCommit() {
    MockHttpContext context = new MockHttpContext();
    context.response.setCommitted(true);

    assertThatIllegalStateException()
            .isThrownBy(() -> context.setTrailerFields(
                    Trailer.field("X-A", () -> "1")));
  }

  @Test
  void nullValueSkipsField() {
    MockHttpContext context = new MockHttpContext();
    context.setTrailerFields(
            Trailer.field("X-Skipped", () -> null),
            Trailer.field("X-Present", () -> "v1"));

    context.requestCompleted();

    HttpHeaders trailers = context.getResponseTrailers();
    assertThat(trailers).isNotNull();
    assertThat(trailers.contains("X-Skipped")).isFalse();
    assertThat(trailers.get("X-Present")).containsExactly("v1");
  }

  @Test
  void rejectsLineBreaksInValues() {
    MockHttpContext context = new MockHttpContext();
    context.setTrailerFields(
            Trailer.field("X-Checksum", () -> "v1\r\nInjected: 1"));

    assertThatIllegalStateException()
            .isThrownBy(context::requestCompleted)
            .withMessageContaining("Invalid trailer value");
  }

  @Test
  void allValuesSkippedProducesNoTrailers() {
    MockHttpContext context = new MockHttpContext();
    context.setTrailerFields(
            Trailer.field("X-First", () -> null),
            Trailer.field("X-Second", () -> null));

    context.requestCompleted();

    assertThat(context.getResponseTrailers()).isNull();
  }

  @Test
  void supplierRunsAtMostOnce() {
    MockHttpContext context = new MockHttpContext();
    AtomicInteger calls = new AtomicInteger();
    context.setTrailerFields(Trailer.field("X-Checksum", () -> {
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
    context.setTrailerFields(Trailer.field("X-Checksum", () -> {
      calls.incrementAndGet();
      return "v1";
    }));

    context.completeWithError();

    assertThat(calls).hasValue(0);
    assertThat(context.getResponseTrailers()).isNull();
  }

  @Test
  void finalNoContentOrUpgradeStatusDiscardsTrailers() {
    for (int status : new int[] { 100, 101, 199, 204, 304 }) {
      InspectableMockHttpContext context = new InspectableMockHttpContext();
      AtomicInteger calls = new AtomicInteger();
      context.setTrailerFields(Trailer.field("X-Checksum", () -> {
        calls.incrementAndGet();
        return "v1";
      }));
      context.setStatus(status);

      context.validateTrailers();

      assertThat(context.trailerNames()).isNull();
      context.requestCompleted();
      assertThat(context.getResponseTrailers()).isNull();
      assertThat(calls).hasValue(0);
    }
  }

  @Test
  void committingCallbackCanChangeStatusAndDiscardTrailers() {
    InspectableMockHttpContext context = new InspectableMockHttpContext();
    AtomicInteger calls = new AtomicInteger();
    context.setTrailerFields(Trailer.field("X-Checksum", () -> {
      calls.incrementAndGet();
      return "v1";
    }));
    context.registerCallback(HttpContext.Lifecycle.COMMITTING, "no-content", () -> context.setStatus(204));

    context.flush();

    assertThat(context.trailerNames()).isNull();
    context.requestCompleted();
    assertThat(context.getStatus()).isEqualTo(204);
    assertThat(context.getResponseTrailers()).isNull();
    assertThat(calls).hasValue(0);
  }

  @Test
  void finalSuccessfulConnectDiscardsTrailers() {
    InspectableMockHttpContext context = new InspectableMockHttpContext();
    context.request.setMethod("CONNECT");
    context.setStatus(400);
    AtomicInteger calls = new AtomicInteger();
    context.setTrailerFields(Trailer.field("X-Checksum", () -> {
      calls.incrementAndGet();
      return "v1";
    }));
    context.setStatus(200);

    context.requestCompleted();

    assertThat(context.getResponseTrailers()).isNull();
    assertThat(calls).hasValue(0);
  }

  @Test
  void declaredNamesAreImmutableAndCaseInsensitive() {
    InspectableMockHttpContext context = new InspectableMockHttpContext();
    context.setTrailerFields(
            Trailer.field("X-Checksum", () -> "v1"),
            Trailer.field("x-checksum", () -> "v2"));

    Set<String> names = context.trailerNames();
    assertThat(names).isNotNull().containsExactly("X-Checksum");
    assertThat(names.contains("x-CHECKSUM")).isTrue();
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(names::clear);
    context.requestCompleted();
    HttpHeaders trailers = context.getResponseTrailers();
    assertThat(trailers).isNotNull();
    assertThat(trailers.get("X-Checksum")).containsExactly("v1", "v2");
  }

  @Test
  void rejectsEveryControlCharacterExceptHorizontalTab() {
    for (char c = 0; c <= 0x7f; c++) {
      if ((c >= 0x20 && c != 0x7f) || c == '\t') {
        continue;
      }
      MockHttpContext context = new MockHttpContext();
      String value = "before" + c + "after";
      context.setTrailerFields(Trailer.field("X-Checksum", () -> value));
      assertThatIllegalStateException().isThrownBy(context::requestCompleted)
              .withMessageContaining("Invalid trailer value");
      assertThat(context.getResponseTrailers()).isNull();
    }
    MockHttpContext context = new MockHttpContext();
    context.setTrailerFields(Trailer.field("X-Checksum", () -> "before\tafter"));
    context.requestCompleted();
    HttpHeaders trailers = context.getResponseTrailers();
    assertThat(trailers).isNotNull();
    assertThat(trailers.getFirst("X-Checksum")).isEqualTo("before\tafter");
  }

  @Test
  void defaultCompletionFiresCallbacksSynchronouslyAfterTrailerEvaluation() {
    MockHttpContext context = new MockHttpContext();
    List<String> events = new ArrayList<>();
    context.setTrailerFields(Trailer.field("X-Checksum", () -> {
      events.add("trailer");
      return "v1";
    }));
    context.registerCallback(HttpContext.Lifecycle.COMPLETED, "cleanup", () -> events.add("completed"));

    context.requestCompleted();

    assertThat(events).containsExactly("trailer", "completed");
  }

  @Test
  void completionHookCanDeferLifecycleCallbacks() {
    DeferredCompletingMockHttpContext context = new DeferredCompletingMockHttpContext();
    AtomicInteger callbacks = new AtomicInteger();
    context.registerCallback(HttpContext.Lifecycle.COMPLETED, "cleanup", callbacks::incrementAndGet);

    context.requestCompleted();

    assertThat(callbacks).hasValue(0);
    context.finishCompletion();
    assertThat(callbacks).hasValue(1);
    context.finishCompletion();
    assertThat(callbacks).hasValue(1);
  }

  @Test
  void supplierFailureStillFiresCompletedCallbackAndPreservesOriginalFailure() {
    MockHttpContext context = new MockHttpContext();
    IllegalStateException supplierFailure = new IllegalStateException("supplier");
    IllegalArgumentException callbackFailure = new IllegalArgumentException("callback");
    AtomicInteger callbacks = new AtomicInteger();
    context.setTrailerFields(Trailer.field("X-Checksum", () -> {
      throw supplierFailure;
    }));
    context.registerCallback(HttpContext.Lifecycle.COMPLETED, "cleanup", () -> {
      callbacks.incrementAndGet();
      throw callbackFailure;
    });

    assertThatThrownBy(context::requestCompleted).isSameAs(supplierFailure)
            .hasSuppressedException(callbackFailure);
    assertThat(callbacks).hasValue(1);
    assertThat(context.getResponseTrailers()).isNull();
  }

  @Test
  void concurrentEvaluationDoesNotInvokeSupplierAgain() throws Exception {
    InspectableMockHttpContext context = new InspectableMockHttpContext();
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    AtomicInteger calls = new AtomicInteger();
    context.setTrailerFields(Trailer.field("X-Checksum", () -> {
      calls.incrementAndGet();
      entered.countDown();
      try {
        if (!release.await(5, TimeUnit.SECONDS)) {
          throw new IllegalStateException("Timed out waiting for concurrent evaluation");
        }
      }
      catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(ex);
      }
      return "v1";
    }));
    var executor = Executors.newSingleThreadExecutor();
    try {
      var first = executor.submit(context::evaluateTrailers);
      assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
      assertThat(context.evaluateTrailers()).isNull();
      release.countDown();
      HttpHeaders trailers = first.get(5, TimeUnit.SECONDS);
      assertThat(trailers).isNotNull();
      assertThat(trailers.getFirst("X-Checksum")).isEqualTo("v1");
      assertThat(calls).hasValue(1);
    }
    finally {
      release.countDown();
      executor.shutdownNow();
    }
  }

  static final class DeferredCompletingMockHttpContext extends MockHttpContext {

    @Override
    protected void onRequestCompleted() {
    }

    void finishCompletion() {
      super.onRequestCompleted();
    }
  }

  static final class InspectableMockHttpContext extends MockHttpContext {

    @Nullable Set<String> trailerNames() {
      return getTrailerFieldNames();
    }

    void validateTrailers() {
      validateTrailerFieldsBeforeCommit();
    }

    @Nullable HttpHeaders evaluateTrailers() {
      return evaluateTrailerFields();
    }
  }

  /**
   * Exposes the error completion path, which is protected on the base class.
   */
  static final class ErrorCompletingMockHttpContext extends MockHttpContext {

    ErrorCompletingMockHttpContext() {
      super(new MockRequest(), new MockResponse());
    }

    void completeWithError() {
      completeRequest(new IllegalStateException("boom"));
    }
  }

}
