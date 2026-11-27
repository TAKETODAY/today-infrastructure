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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.ResponseCookie;
import infra.context.support.GenericApplicationContext;
import infra.session.Session;
import infra.session.SessionManager;
import infra.util.MultiValueMap;
import infra.web.async.WebAsyncRequest;
import infra.web.i18n.AcceptHeaderLocaleResolver;
import infra.web.i18n.CookieLocaleResolver;
import infra.web.i18n.SessionLocaleResolver;
import infra.web.mock.MockServerHttpExchange;
import infra.web.multipart.MultipartRequest;
import infra.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * White-box tests for the bit-packed {@link AbstractServerHttpExchange} state.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class AbstractServerHttpExchangeStateTests {

  private final StateServerHttpExchange context = new StateServerHttpExchange();

  @Test
  void resolvesHostAndPortWithoutAnInetLocalAddress() {
    context.localAddress = new SocketAddress() { };
    assertThat(context.getServerName()).isEqualTo("localhost");
    assertThat(context.getServerPort()).isEqualTo(80);

    context.requestHeaders.set(HttpHeaders.HOST, "[::1]:8443");
    assertThat(context.getServerName()).isEqualTo("[::1]");
    assertThat(context.getServerPort()).isEqualTo(8443);
    assertThat(context.getURI()).isEqualTo(URI.create("https://[::1]:8443/"));

    context.requestHeaders.set(HttpHeaders.HOST, "example.test:invalid");
    context.secure = true;
    assertThat(context.getServerName()).isEqualTo("example.test");
    assertThat(context.getServerPort()).isEqualTo(443);
  }

  @Test
  void usesLocalSocketWhenHostIsAbsentAndResolvesRemoteEndpoint() {
    assertThat(context.getServerName()).isEqualTo("127.0.0.1");
    assertThat(context.getServerPort()).isEqualTo(443);
    assertThat(context.getRemoteAddress()).isEqualTo("127.0.0.1");
    assertThat(context.getRemotePort()).isEqualTo(1234);
  }

  @Test
  void cachesRequestReaderAndDecodesUtf8() throws IOException {
    context.body = "你好\n世界";
    var reader = context.getReader();
    assertThat(reader.readLine()).isEqualTo("你好");
    assertThat(context.getReader()).isSameAs(reader);
    assertThat(reader.readLine()).isEqualTo("世界");
    assertThat(reader.readLine()).isNull();
  }

  @Test
  void rejectsAnInvalidRequestUrlEvenAfterRemovingTheQuery() {
    context.requestHeaders.set(HttpHeaders.HOST, "[broken");
    assertThatIllegalStateException().isThrownBy(context::getURI)
            .withCauseInstanceOf(URISyntaxException.class);

    var withQuery = new StateServerHttpExchange();
    withQuery.requestHeaders.set(HttpHeaders.HOST, "[broken");
    withQuery.query = "q=value";
    assertThatIllegalStateException().isThrownBy(withQuery::getURI)
            .withMessage("Could not resolve ServerHttpExchange as URI: https://[broken/")
            .withCauseInstanceOf(URISyntaxException.class);
  }

  @Test
  void honorsLegacyConditionalDateAndPublishesValidators() {
    long lastModified = 1397037462000L;
    context.requestHeaders.set(HttpHeaders.IF_UNMODIFIED_SINCE, "Wed, 09 Apr 2014 09:57:42 GMT; length=13774");

    assertThat(context.checkNotModified("revision", lastModified)).isFalse();
    assertThat(context.getStatus()).isEqualTo(200);
    assertThat(context.responseHeaders().getLastModified()).isEqualTo(lastModified);
    assertThat(context.responseHeaders().getETag()).isEqualTo("\"revision\"");
  }

  @Test
  void ignoresMalformedConditionalDate() {
    context.requestHeaders.set(HttpHeaders.IF_UNMODIFIED_SINCE, "not a date");
    context.requestHeaders.set(HttpHeaders.IF_MODIFIED_SINCE, "not a date; length=4");

    assertThat(context.checkNotModified(1000L)).isFalse();
    assertThat(context.getStatus()).isEqualTo(200);
    assertThat(context.responseHeaders().getLastModified()).isEqualTo(1000L);
  }

  @Test
  void retriesSessionLookupAfterAMissAndCachesTheResolvedSession() {
    try (var application = new GenericApplicationContext()) {
      var manager = mock(SessionManager.class);
      application.getBeanFactory().registerSingleton(SessionManager.BEAN_NAME, manager);
      application.refresh();
      var exchange = new StateServerHttpExchange(application, new DispatcherHandler(application));
      var session = mock(Session.class);
      given(manager.getSession(exchange, true)).willReturn(session);

      assertThat(exchange.getSession(false)).isNull();
      assertThat(exchange.getSession()).isSameAs(session);
      assertThat(exchange.getSession(false)).isSameAs(session);
      verify(manager, times(1)).getSession(exchange, false);
      verify(manager, times(1)).getSession(exchange, true);
    }
  }

  @Test
  void outputMessageSharesResponseStatusHeadersAndBody() throws IOException {
    var response = context.asHttpOutputMessage();
    response.setStatusCode(HttpStatus.ACCEPTED);
    response.setContentType(MediaType.TEXT_PLAIN);
    assertThat(context.getStatus()).isEqualTo(202);
    assertThat(response.getHeaders()).isSameAs(context.responseHeaders());
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
    assertThat(response.getBody()).isSameAs(context.getOutputStream());
    response.flush();
    response.close();
  }

  @Test
  void localeIsNullWithoutAcceptLanguage() {
    assertThat(context.getPreferredLocale()).isNull();
    assertThat(WebUtils.resolveLocale(context)).isEqualTo(Locale.getDefault());
    assertThat(context.getPreferredLocale()).isNull();
  }

  @Test
  void localeIsNullWithoutAcceptableConcreteLanguage() {
    for (String value : List.of("", " ", "*", "*;q=0.8", "en;q=0", "en;q=0, *")) {
      context.requestHeaders.set(HttpHeaders.ACCEPT_LANGUAGE, value);
      assertThat(context.getPreferredLocale()).as("Accept-Language: %s", value).isNull();
    }
  }

  @Test
  void localeUsesQualityOrderAndIgnoresWildcards() {
    context.requestHeaders.set(HttpHeaders.ACCEPT_LANGUAGE, "*;q=1, en-US;q=0.5, zh-CN;q=0.9");

    assertThat(context.getPreferredLocale()).isEqualTo(Locale.SIMPLIFIED_CHINESE);
    assertThat(WebUtils.resolveLocale(context)).isEqualTo(Locale.SIMPLIFIED_CHINESE);
  }

  @Test
  void localeRejectsInvalidAcceptLanguage() {
    context.requestHeaders.set(HttpHeaders.ACCEPT_LANGUAGE, "en;q=invalid");

    assertThatIllegalArgumentException().isThrownBy(context::getPreferredLocale);
  }

  @Test
  void localeResolversFallBackWithoutRequestLocale() {
    AcceptHeaderLocaleResolver acceptHeaderResolver = new AcceptHeaderLocaleResolver();
    CookieLocaleResolver cookieResolver = new CookieLocaleResolver();
    SessionLocaleResolver sessionResolver = new SessionLocaleResolver();

    assertThat(acceptHeaderResolver.resolveLocale(context)).isEqualTo(Locale.getDefault());
    assertThat(cookieResolver.resolveLocale(context)).isEqualTo(Locale.getDefault());
    assertThat(sessionResolver.resolveLocale(new MockServerHttpExchange())).isEqualTo(Locale.getDefault());
    assertThat(context.getPreferredLocale()).isNull();
  }

  @Test
  void acceptHeaderResolverUsesConfiguredDefaultForZeroQualityLanguages() {
    context.requestHeaders.set(HttpHeaders.ACCEPT_LANGUAGE, "en;q=0, *");
    AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
    resolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);

    assertThat(resolver.resolveLocale(context)).isEqualTo(Locale.SIMPLIFIED_CHINESE);
    assertThat(context.getPreferredLocale()).isNull();
  }

  @Test
  void acceptHeaderResolverDoesNotSelectZeroQualitySupportedLanguage() {
    context.requestHeaders.set(HttpHeaders.ACCEPT_LANGUAGE, "fr, en;q=0");
    AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
    resolver.setSupportedLocales(List.of(Locale.ENGLISH));
    resolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);

    assertThat(resolver.resolveLocale(context)).isEqualTo(Locale.SIMPLIFIED_CHINESE);
  }

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
    StateServerHttpExchange failed = new StateServerHttpExchange();
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
    int bits = AbstractServerHttpExchange.MULTIPART_KNOWN | AbstractServerHttpExchange.MULTIPART;
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
    int bits = AbstractServerHttpExchange.PRE_FLIGHT_KNOWN | AbstractServerHttpExchange.PRE_FLIGHT;
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
    int bits = AbstractServerHttpExchange.CORS_KNOWN | AbstractServerHttpExchange.CORS;
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
    assertThat(context.state() & AbstractServerHttpExchange.NOT_MODIFIED).isNotZero();

    context.reset();
    assertThat(context.isNotModified()).isTrue();

    context.markNotModified(false);
    assertThat(context.isNotModified()).isFalse();
    assertThat(context.state() & AbstractServerHttpExchange.NOT_MODIFIED).isZero();
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
  void rejectsTrailersForResponsesThatCannotCarryThem() {
    var trailer = Trailer.field("X-Checksum", () -> "value");
    var head = new StateServerHttpExchange();
    head.method = HttpMethod.HEAD;
    assertThatIllegalStateException().isThrownBy(() -> head.setTrailerFields(trailer)).withMessageContaining("HEAD");

    context.setStatus(204);
    assertThatIllegalStateException().isThrownBy(() -> context.setTrailerFields(trailer)).withMessageContaining("204");

    var connect = new StateServerHttpExchange();
    connect.method = HttpMethod.CONNECT;
    assertThatIllegalStateException().isThrownBy(() -> connect.setTrailerFields(trailer)).withMessageContaining("CONNECT");
  }

  @Test
  void validatesTrailerNamesAndValuesSeparately() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> context.setTrailerFields(Trailer.field("invalid name", () -> "value")))
            .withMessageContaining("Invalid trailer field name");
    assertThatIllegalArgumentException()
            .isThrownBy(() -> context.setTrailerFields(Trailer.field(HttpHeaders.CONTENT_TYPE, () -> "text/plain")))
            .withMessageContaining("Trailer field is not allowed");

    context.setTrailerFields(Trailer.field("X-Checksum", () -> "value\n"));
    assertThatIllegalStateException().isThrownBy(context::evaluateTrailerFields)
            .withMessageContaining("Invalid trailer value", "X-Checksum");
    assertThat(context.evaluateTrailerFields()).isNull();
  }

  @Test
  void dropsTrailersOnExplicitClearOrWhenFinalStatusDisallowsThem() {
    var evaluations = new AtomicInteger();
    var trailer = Trailer.field("X-Checksum", () -> Integer.toString(evaluations.incrementAndGet()));
    context.setTrailerFields(trailer);
    context.setTrailerFields();
    assertThat(context.getTrailerFieldNames()).isNull();
    assertThat(context.evaluateTrailerFields()).isNull();

    context.setTrailerFields(trailer);
    context.setStatus(205);
    assertThat(context.evaluateTrailerFields()).isNull();
    assertThat(context.getTrailerFieldNames()).isNull();
    assertThat(evaluations).hasValue(0);
  }

  @Test
  void unnamedCallbackRegistrationsAreIndependent() {
    for (ServerHttpExchange.Lifecycle phase : ServerHttpExchange.Lifecycle.values()) {
      AtomicInteger invocations = new AtomicInteger();
      Runnable callback = invocations::incrementAndGet;

      String firstName = context.registerCallback(phase, callback);
      String secondName = context.registerCallback(phase, callback);

      assertThat(firstName).isNotBlank().isNotEqualTo(secondName);
      context.fireCallbacks(phase);
      assertThat(invocations).hasValue(2);
      context.fireCallbacks(phase);
      assertThat(invocations).hasValue(2);
    }
  }

  @Test
  void generatedCallbackNameAllowsIndependentRemoval() {
    for (ServerHttpExchange.Lifecycle phase : ServerHttpExchange.Lifecycle.values()) {
      AtomicInteger removedInvocations = new AtomicInteger();
      AtomicInteger retainedInvocations = new AtomicInteger();
      String name = context.registerCallback(phase, removedInvocations::incrementAndGet);
      context.registerCallback(phase, retainedInvocations::incrementAndGet);

      context.removeCallback(phase, name);
      context.fireCallbacks(phase);

      assertThat(removedInvocations).hasValue(0);
      assertThat(retainedInvocations).hasValue(1);
    }
  }

  @Test
  void explicitlyNamedCallbackRegistrationReplacesPreviousCallback() {
    for (ServerHttpExchange.Lifecycle phase : ServerHttpExchange.Lifecycle.values()) {
      AtomicInteger replacedInvocations = new AtomicInteger();
      AtomicInteger replacementInvocations = new AtomicInteger();
      context.registerCallback(phase, "count", replacedInvocations::incrementAndGet);
      context.registerCallback(phase, "count", replacementInvocations::incrementAndGet);

      context.fireCallbacks(phase);

      assertThat(replacedInvocations).hasValue(0);
      assertThat(replacementInvocations).hasValue(1);
    }
  }

  @Test
  @SuppressWarnings("NullAway")
  void unnamedCallbackRegistrationRejectsNullArguments() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> context.registerCallback(null, () -> { }))
            .withMessage("Phase is required");
    assertThatIllegalArgumentException()
            .isThrownBy(() -> context.registerCallback(ServerHttpExchange.Lifecycle.COMPLETED, null))
            .withMessage("Callback is required");
  }

  @Test
  void requestCompletedIsIdempotent() {
    AtomicInteger completions = new AtomicInteger();
    context.registerCallback(ServerHttpExchange.Lifecycle.COMPLETED, "count", completions::incrementAndGet);

    assertThat(context.isRequestCompleted()).isFalse();
    assertThat(context.state() & AbstractServerHttpExchange.COMPLETED).isZero();

    context.completeRequest(null);

    assertThat(context.isRequestCompleted()).isTrue();
    assertThat(context.state() & AbstractServerHttpExchange.COMPLETED).isNotZero();
    assertThat(completions).hasValue(1);

    // repeated completion is ignored
    context.completeRequest(null);
    assertThat(completions).hasValue(1);

    // response reset does not re-open a completed request
    context.reset();
    assertThat(context.isRequestCompleted()).isTrue();
  }

  @Test
  void resetClearsResponseCookies() {
    context.addCookie(ResponseCookie.forSimple("session", "old"));
    assertThat(context.hasResponseCookies()).isTrue();

    context.reset();

    assertThat(context.hasResponseCookies()).isFalse();
  }

  @Test
  void resetRebuildsWriterButKeepsCachedOutputStream() throws IOException {
    var writer = context.getWriter();
    var outputStream = context.getOutputStream();

    context.reset();

    assertThat(context.getWriter()).isNotSameAs(writer);
    assertThat(context.getOutputStream()).isSameAs(outputStream);
  }

  @Test
  void removeCookieReturnsRemovedCookiesAndClearsState() {
    ResponseCookie cookie = ResponseCookie.forSimple("session", "old");
    context.addCookie(cookie);

    List<ResponseCookie> removed = context.removeCookies("session");

    assertThat(removed).containsExactly(cookie);
    assertThat(context.hasResponseCookies()).isFalse();
  }

  @Test
  void removeCookieReturnsNullWhenNoCookieMatches() {
    ResponseCookie cookie = ResponseCookie.forSimple("session", "old");
    context.addCookie(cookie);

    List<ResponseCookie> removed = context.removeCookies("missing");

    assertThat(removed).isNull();
    // a non-matching removal must not drop the existing cookie
    assertThat(context.hasResponseCookies()).isTrue();
  }

  @Test
  void removeCookieReturnsNullWhenNoCookieWasAdded() {
    assertThat(context.removeCookies("missing")).isNull();
  }

  @Test
  void removeCookieReturnsNullAfterReset() {
    context.addCookie(ResponseCookie.forSimple("session", "old"));
    context.reset();

    // reset keeps the (empty) list for reuse, but no cookie remains to remove
    assertThat(context.removeCookies("session")).isNull();
    assertThat(context.hasResponseCookies()).isFalse();
  }

  @Test
  void resolveResponseCharsetDefaultsToUtf8() {
    assertThat(context.resolveResponseCharset()).isEqualTo(StandardCharsets.UTF_8);
  }

  @Test
  void resolveResponseCharsetUsesCharsetFromResponseContentType() {
    context.setContentType("text/plain;charset=ISO-8859-1");

    assertThat(context.resolveResponseCharset()).isEqualTo(StandardCharsets.ISO_8859_1);
  }

  @Test
  void resolveResponseCharsetDefaultsToUtf8WhenContentTypeHasNoCharset() {
    context.setContentType("text/plain");

    assertThat(context.resolveResponseCharset()).isEqualTo(StandardCharsets.UTF_8);
  }

  @Test
  void resolveResponseCharsetDefaultsToUtf8WhenContentTypeIsMalformed() {
    context.setContentType("not a media type");

    assertThat(context.resolveResponseCharset()).isEqualTo(StandardCharsets.UTF_8);
  }

  @SuppressWarnings("NullAway")
  private static class StateServerHttpExchange extends AbstractServerHttpExchange {

    HttpMethod method = HttpMethod.GET;

    SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 443);

    boolean secure;

    String body = "";

    @Nullable String query;

    final HttpHeaders requestHeaders = HttpHeaders.forWritable();

    final HttpHeaders responseHeaders = HttpHeaders.forWritable();

    private int status = 200;

    StateServerHttpExchange() {
      super(null, null);
    }

    StateServerHttpExchange(GenericApplicationContext context, DispatcherHandler dispatcher) {
      super(context, dispatcher);
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
      return query;
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
      return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
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
    protected WebAsyncRequest createAsyncRequest() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSecure() {
      return secure;
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
      return localAddress;
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
