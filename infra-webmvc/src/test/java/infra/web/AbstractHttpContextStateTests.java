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

package infra.web;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.MediaType;
import infra.util.MultiValueMap;
import infra.web.async.AsyncWebRequest;
import infra.web.multipart.MultipartRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * White-box tests for the bit-packed {@link AbstractHttpContext} state.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class AbstractHttpContextStateTests {

  private final StateHttpContext context = new StateHttpContext();

  @Test
  void commitStateTransitions() {
    assertThat(context.isCommitted()).isFalse();
    assertThat(context.isCommitting()).isFalse();

    assertThat(context.tryBeginCommit()).isTrue();
    assertThat(context.isCommitting()).isTrue();
    // the head has not been written yet
    assertThat(context.isCommitted()).isFalse();

    // another attempt must not claim the commit again
    assertThat(context.tryBeginCommit()).isFalse();

    // success path: only COMMITTED remains
    context.finishCommit(true);
    assertThat(context.isCommitting()).isFalse();
    assertThat(context.isCommitted()).isTrue();
    // an already committed response must not begin another commit
    assertThat(context.tryBeginCommit()).isFalse();

    // failure path: both bits are rolled back
    StateHttpContext failed = new StateHttpContext();
    assertThat(failed.tryBeginCommit()).isTrue();
    failed.finishCommit(false);
    assertThat(failed.isCommitting()).isFalse();
    assertThat(failed.isCommitted()).isFalse();
  }

  @Test
  void multipartFlagIsCachedAndSurvivesReset() {
    context.method = HttpMethod.POST;
    context.requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

    assertThat(context.isMultipart()).isTrue();
    int bits = AbstractHttpContext.MULTIPART_KNOWN | AbstractHttpContext.MULTIPART;
    assertThat(context.state() & bits).isEqualTo(bits);

    // even if the underlying headers change, the cached value is used
    context.requestHeaders.setContentType(MediaType.TEXT_PLAIN);
    assertThat(context.isMultipart()).isTrue();

    context.reset();
    assertThat(context.state() & bits).isEqualTo(bits);
    assertThat(context.isMultipart()).isTrue();
  }

  @Test
  void preFlightFlagIsCachedAndSurvivesReset() {
    context.method = HttpMethod.OPTIONS;
    context.requestHeaders.set(HttpHeaders.ORIGIN, "https://example.com");
    context.requestHeaders.set(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");

    assertThat(context.isPreFlightRequest()).isTrue();
    int bits = AbstractHttpContext.PRE_FLIGHT_KNOWN | AbstractHttpContext.PRE_FLIGHT;
    assertThat(context.state() & bits).isEqualTo(bits);

    context.requestHeaders.remove(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
    assertThat(context.isPreFlightRequest()).isTrue();

    context.reset();
    assertThat(context.state() & bits).isEqualTo(bits);
    assertThat(context.isPreFlightRequest()).isTrue();
  }

  @Test
  void corsFlagIsCachedAndSurvivesReset() {
    context.requestHeaders.set(HttpHeaders.HOST, "example.com");
    context.requestHeaders.set(HttpHeaders.ORIGIN, "https://other.com");

    assertThat(context.isCorsRequest()).isTrue();
    int bits = AbstractHttpContext.CORS_KNOWN | AbstractHttpContext.CORS;
    assertThat(context.state() & bits).isEqualTo(bits);

    context.requestHeaders.set(HttpHeaders.ORIGIN, "https://example.com");
    assertThat(context.isCorsRequest()).isTrue();

    context.reset();
    assertThat(context.state() & bits).isEqualTo(bits);
    assertThat(context.isCorsRequest()).isTrue();
  }

  @Test
  void notModifiedBitIsNotClearedByReset() {
    context.markNotModified(true);
    assertThat(context.isNotModified()).isTrue();
    assertThat(context.state() & AbstractHttpContext.NOT_MODIFIED).isNotZero();

    context.reset();
    assertThat(context.isNotModified()).isTrue();

    context.markNotModified(false);
    assertThat(context.isNotModified()).isFalse();
    assertThat(context.state() & AbstractHttpContext.NOT_MODIFIED).isZero();
  }

  @Test
  void trailerEvaluationGuardIsPerRegistration() {
    AtomicInteger first = new AtomicInteger();
    AtomicInteger second = new AtomicInteger();
    AtomicInteger third = new AtomicInteger();

    context.setTrailerFields(Trailer.field("X-First", () -> {
      first.incrementAndGet();
      return "first";
    }));
    context.setTrailerFields(Trailer.field("X-Second", () -> {
      second.incrementAndGet();
      return "second";
    }));

    HttpHeaders evaluated = context.evaluateTrailerFields();
    assertThat(evaluated).isNotNull();
    assertThat(evaluated.getFirst("X-Second")).isEqualTo("second");
    assertThat(first).hasValue(0);
    assertThat(second).hasValue(1);

    // suppliers run at most once per registration
    assertThat(context.evaluateTrailerFields()).isNull();
    assertThat(second).hasValue(1);

    // a new registration gets a fresh evaluation guard
    context.clearTrailerFields();
    context.setTrailerFields(Trailer.field("X-Third", () -> {
      third.incrementAndGet();
      return "third";
    }));

    HttpHeaders reEvaluated = context.evaluateTrailerFields();
    assertThat(reEvaluated).isNotNull();
    assertThat(reEvaluated.getFirst("X-Third")).isEqualTo("third");
    assertThat(third).hasValue(1);
  }

  @Test
  void requestCompletedIsIdempotent() {
    AtomicInteger completions = new AtomicInteger();
    context.registerCallback(HttpContext.Lifecycle.COMPLETED, "count", completions::incrementAndGet);

    assertThat(context.isRequestCompleted()).isFalse();
    assertThat(context.state() & AbstractHttpContext.COMPLETED).isZero();

    context.completeRequest(null);

    assertThat(context.isRequestCompleted()).isTrue();
    assertThat(context.state() & AbstractHttpContext.COMPLETED).isNotZero();
    assertThat(completions).hasValue(1);

    // repeated completion is ignored
    context.completeRequest(null);
    assertThat(completions).hasValue(1);

    // response reset does not re-open a completed request
    context.reset();
    assertThat(context.isRequestCompleted()).isTrue();
  }

  @SuppressWarnings("NullAway")
  private static class StateHttpContext extends AbstractHttpContext {

    HttpMethod method = HttpMethod.GET;

    final HttpHeaders requestHeaders = HttpHeaders.forWritable();

    final HttpHeaders responseHeaders = HttpHeaders.forWritable();

    private int status = 200;

    StateHttpContext() {
      super(null, null);
    }

    @Override
    public long getRequestTimeMillis() {
      return 0;
    }

    @Override
    protected String readRequestURI() {
      return "/";
    }

    @Override
    protected @Nullable String readQueryString() {
      return null;
    }

    @Override
    protected HttpCookie[] readCookies() {
      return EMPTY_COOKIES;
    }

    @Override
    protected MultiValueMap<String, String> readParameters() {
      return MultiValueMap.forLinkedHashMap();
    }

    @Override
    protected String readMethod() {
      return method.name();
    }

    @Override
    public long getContentLength() {
      return 0;
    }

    @Override
    protected InputStream createInputStream() throws IOException {
      return InputStream.nullInputStream();
    }

    @Override
    protected OutputStream createOutputStream() throws IOException {
      return OutputStream.nullOutputStream();
    }

    @Override
    protected HttpHeaders createRequestHeaders() {
      return requestHeaders;
    }

    @Override
    protected HttpHeaders createResponseHeaders() {
      return responseHeaders;
    }

    @Override
    protected MultipartRequest createMultipartRequest() {
      throw new UnsupportedOperationException();
    }

    @Override
    protected AsyncWebRequest createAsyncWebRequest() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSecure() {
      return false;
    }

    @Override
    public String getScheme() {
      return "https";
    }

    @Override
    public InetSocketAddress remoteAddress() {
      return new InetSocketAddress("127.0.0.1", 1234);
    }

    @Override
    public SocketAddress localAddress() {
      return new InetSocketAddress("127.0.0.1", 443);
    }

    @Override
    public void sendError(int sc) throws IOException {
    }

    @Override
    public void sendError(int sc, @Nullable String msg) throws IOException {
    }

    @Override
    public void sendRedirect(String location) throws IOException {
    }

    @Override
    public void setStatus(int sc) {
      this.status = sc;
    }

    @Override
    public int getStatus() {
      return status;
    }
  }

}
