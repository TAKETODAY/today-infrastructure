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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import infra.context.ApplicationContext;
import infra.core.AttributeAccessor;
import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.ResponseCookie;
import infra.http.server.RequestPath;
import infra.http.server.ServerHttpResponse;
import infra.session.Session;
import infra.util.MultiValueMap;
import infra.web.async.WebAsyncRequest;
import infra.web.async.WebAsyncManager;
import infra.web.multipart.MultipartRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/9 16:21
 */
class DelegatingServerHttpExchangeTests {

  @Test
  void requestHeadersAndContentTypeAreDelegated() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);
    when(delegate.getContentType()).thenReturn(MediaType.TEXT_PLAIN);
    when(delegate.getHeader("X-Test")).thenReturn("first");
    when(delegate.getHeaders("X-Test")).thenReturn(List.of("first", "second"));
    when(delegate.getHeaderNames()).thenReturn(List.of("X-Test"));
    when(delegate.containsHeader("X-Test")).thenReturn(true);
    when(delegate.containsHeaderValue("X-Test", "second")).thenReturn(true);

    assertThat(wrapper.getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
    assertThat(wrapper.getHeader("X-Test")).isEqualTo("first");
    assertThat(wrapper.getHeaders("X-Test")).containsExactly("first", "second");
    assertThat(wrapper.getHeaderNames()).containsExactly("X-Test");
    assertThat(wrapper.containsHeader("X-Test")).isTrue();
    assertThat(wrapper.containsHeaderValue("X-Test", "second")).isTrue();
    HttpHeaders headers = HttpHeaders.forWritable();
    wrapper.setHeaders(headers);
    verify(delegate).setHeaders(headers);
  }

  @Test
  void cookieLookupAndBuilderOverloadsAreDelegated() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);
    HttpCookie cookie = mock(HttpCookie.class);
    ResponseCookie.Builder builder = mock(ResponseCookie.Builder.class);
    Consumer<ResponseCookie.Builder> configure = candidate -> candidate.path("/");
    when(delegate.cookie("test")).thenReturn(Optional.of(cookie));

    assertThat(wrapper.cookie("test")).containsSame(cookie);
    wrapper.addCookie("test", configure);
    wrapper.addCookie(builder);
    wrapper.addCookie(cookie);

    verify(delegate).addCookie("test", configure);
    verify(delegate).addCookie(builder);
    verify(delegate).addCookie(cookie);
  }

  @Test
  void callbackRegistrationAndRemovalAreDelegated() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);
    Runnable callback = mock(Runnable.class);
    ServerHttpExchange.Lifecycle phase = ServerHttpExchange.Lifecycle.COMPLETED;
    when(delegate.registerCompletedCallback(callback)).thenReturn("completed");
    when(delegate.registerCommittedCallback(callback)).thenReturn("committed");
    when(delegate.registerCommittingCallback(callback)).thenReturn("committing");
    when(delegate.registerCallback(phase, callback)).thenReturn("generated");

    assertThat(wrapper.registerCompletedCallback(callback)).isEqualTo("completed");
    assertThat(wrapper.registerCommittedCallback(callback)).isEqualTo("committed");
    assertThat(wrapper.registerCommittingCallback(callback)).isEqualTo("committing");
    assertThat(wrapper.registerCallback(phase, callback)).isEqualTo("generated");
    wrapper.registerCallback(phase, "custom", callback);
    wrapper.removeCallback(phase, "custom");

    verify(delegate).registerCallback(phase, "custom", callback);
    verify(delegate).removeCallback(phase, "custom");
  }

  @Test
  void sessionAccessIsDelegatedWithoutChangingCreationPolicy() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);
    Session session = mock(Session.class);
    when(delegate.getSession()).thenReturn(session);
    when(delegate.changeSessionId()).thenReturn("new-id");

    assertThat(wrapper.getSession(false)).isNull();
    verify(delegate).getSession(false);
    assertThat(wrapper.getSession()).isSameAs(session);
    assertThat(wrapper.changeSessionId()).isEqualTo("new-id");
  }

  @Test
  void attributesAndRequestCompletionAreDelegated() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);
    Map<String, Object> attributes = Map.of("name", "value");
    Function<String, String> compute = name -> name + "-value";
    when(delegate.computeAttribute("name", compute)).thenReturn("name-value");
    when(delegate.getRequestProcessingTime()).thenReturn(42L);
    when(delegate.isRequestCompleted()).thenReturn(true);

    wrapper.setAttributes(attributes);
    verify(delegate).setAttributes(attributes);
    assertThat(wrapper.computeAttribute("name", compute)).isEqualTo("name-value");
    assertThat(wrapper.getRequestProcessingTime()).isEqualTo(42L);
    assertThat(wrapper.isRequestCompleted()).isTrue();
  }

  @Test
  void forwardAndBodyTransferAreDelegated() throws Exception {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);
    OutputStream output = mock(OutputStream.class);
    when(delegate.transferTo(output)).thenReturn(12L);

    wrapper.forward("/next?mode=test");
    verify(delegate).forward("/next?mode=test");
    assertThat(wrapper.transferTo(output)).isEqualTo(12L);
  }

  @Test
  void sendIsDelegated() throws Exception {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);
    Object result = new Object();

    wrapper.send(result);
    wrapper.send(null);

    verify(delegate).send(result);
    verify(delegate).send(null);
  }

  @Test
  void requestLocaleIsDelegated() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.getPreferredLocale()).thenReturn(Locale.SIMPLIFIED_CHINESE);
    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    assertThat(wrapper.getPreferredLocale()).isEqualTo(Locale.SIMPLIFIED_CHINESE);
    verify(delegate).getPreferredLocale();
  }

  @Test
  void requestLocaleCanDelegateNull() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    assertThat(wrapper.getPreferredLocale()).isNull();
    verify(delegate).getPreferredLocale();
  }

  @Test
  void getDelegate_ShouldReturnDelegateInstance() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    assertThat(wrapper.delegate()).isSameAs(delegate);
  }

  @Test
  void trailerRegistrationIsDelegated() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);
    Trailer trailer = Trailer.field("Content-Digest", () -> "sha-256=:abc:");

    wrapper.setTrailerFields(trailer);

    verify(delegate).setTrailerFields(trailer);
  }

  @Test
  void equals_ShouldReturnTrue_ForSameDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    DelegatingServerHttpExchange wrapper1 = new DelegatingServerHttpExchange(delegate);
    DelegatingServerHttpExchange wrapper2 = new DelegatingServerHttpExchange(delegate);

    assertThat(wrapper1).isEqualTo(wrapper2);
  }

  @Test
  void equals_ShouldReturnFalse_ForDifferentDelegate() {
    ServerHttpExchange delegate1 = mock(ServerHttpExchange.class);
    ServerHttpExchange delegate2 = mock(ServerHttpExchange.class);
    DelegatingServerHttpExchange wrapper1 = new DelegatingServerHttpExchange(delegate1);
    DelegatingServerHttpExchange wrapper2 = new DelegatingServerHttpExchange(delegate2);

    assertThat(wrapper1).isNotEqualTo(wrapper2);
  }

  @Test
  void equals_ShouldReturnFalse_ForNonDelegatingHttpContext() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    assertThat(wrapper).isNotEqualTo(delegate);
  }

  @Test
  void equals_ShouldReturnTrue_ForSameInstance() {
    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(mock());

    assertThat(wrapper).isEqualTo(wrapper);
  }

  @Test
  void hashCode_ShouldReturnIdentityHashCode() {
    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(mock(ServerHttpExchange.class));

    assertThat(wrapper.hashCode()).isEqualTo(System.identityHashCode(wrapper));
  }

  @Test
  void toString_ShouldIncludeDelegateInfo() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.toString()).thenReturn("MockDelegate");

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    assertThat(wrapper.toString()).isEqualTo("Wrapper for MockDelegate");
  }

  @Test
  void allMethodsShouldDelegateToWrappedInstance() throws IOException {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    // Test a few representative methods
    wrapper.getRequestURI();
    verify(delegate).getRequestURI();

    wrapper.getMethod();
    verify(delegate).getMethod();

    wrapper.getParameters();
    verify(delegate).getParameters();

    wrapper.isCommitted();
    verify(delegate).isCommitted();
  }

  @Test
  void getRequestTimeMillis_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.getRequestTimeMillis()).thenReturn(12345L);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    long result = wrapper.getRequestTimeMillis();

    assertThat(result).isEqualTo(12345L);
    verify(delegate).getRequestTimeMillis();
  }

  @Test
  void getApplicationContext_ShouldDelegateToDelegate() {
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.getApplicationContext()).thenReturn(applicationContext);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    ApplicationContext result = wrapper.getApplicationContext();

    assertThat(result).isSameAs(applicationContext);
    verify(delegate).getApplicationContext();
  }

  @Test
  void getReaderWithCharset_ShouldDelegateToDelegate() throws IOException {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    Reader reader = mock(Reader.class);
    Charset charset = Charset.defaultCharset();
    when(delegate.getReader(charset)).thenReturn(reader);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    Reader result = wrapper.getReader(charset);

    assertThat(result).isSameAs(reader);
    verify(delegate).getReader(charset);
  }

  @Test
  void readableChannel_ShouldDelegateToDelegate() throws IOException {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    ReadableByteChannel channel = mock(ReadableByteChannel.class);
    when(delegate.readableChannel()).thenReturn(channel);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    ReadableByteChannel result = wrapper.readableChannel();

    assertThat(result).isSameAs(channel);
    verify(delegate).readableChannel();
  }

  @Test
  void writableChannel_ShouldDelegateToDelegate() throws IOException {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    WritableByteChannel channel = mock(WritableByteChannel.class);
    when(delegate.writableChannel()).thenReturn(channel);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    WritableByteChannel result = wrapper.writableChannel();

    assertThat(result).isSameAs(channel);
    verify(delegate).writableChannel();
  }

  @Test
  void getScheme_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.getScheme()).thenReturn("https");

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    String result = wrapper.getScheme();

    assertThat(result).isEqualTo("https");
    verify(delegate).getScheme();
  }

  @Test
  void getServerPort_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.getServerPort()).thenReturn(8080);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    int result = wrapper.getServerPort();

    assertThat(result).isEqualTo(8080);
    verify(delegate).getServerPort();
  }

  @Test
  void getServerName_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.getServerName()).thenReturn("localhost");

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    String result = wrapper.getServerName();

    assertThat(result).isEqualTo("localhost");
    verify(delegate).getServerName();
  }

  @Test
  void getURI_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    URI uri = URI.create("http://localhost:8080/test");
    when(delegate.getURI()).thenReturn(uri);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    URI result = wrapper.getURI();

    assertThat(result).isSameAs(uri);
    verify(delegate).getURI();
  }

  @Test
  void isPreFlightRequest_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.isPreFlightRequest()).thenReturn(true);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    boolean result = wrapper.isPreFlightRequest();

    assertThat(result).isTrue();
    verify(delegate).isPreFlightRequest();
  }

  @Test
  void isCorsRequest_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.isCorsRequest()).thenReturn(true);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    boolean result = wrapper.isCorsRequest();

    assertThat(result).isTrue();
    verify(delegate).isCorsRequest();
  }

  @Test
  void getRequestPath_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    RequestPath requestPath = mock(RequestPath.class);
    when(delegate.getRequestPath()).thenReturn(requestPath);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    RequestPath result = wrapper.getRequestPath();

    assertThat(result).isSameAs(requestPath);
    verify(delegate).getRequestPath();
  }

  @Test
  void getRequestURL_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.getRequestURL()).thenReturn("http://localhost:8080/test");

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    String result = wrapper.getRequestURL();

    assertThat(result).isEqualTo("http://localhost:8080/test");
    verify(delegate).getRequestURL();
  }

  @Test
  void getQueryString_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.getQueryString()).thenReturn("param=value");

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    String result = wrapper.getQueryString();

    assertThat(result).isEqualTo("param=value");
    verify(delegate).getQueryString();
  }

  @Test
  void getCookies_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    HttpCookie[] cookies = new HttpCookie[0];
    when(delegate.getCookies()).thenReturn(cookies);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    HttpCookie[] result = wrapper.getCookies();

    assertThat(result).isSameAs(cookies);
    verify(delegate).getCookies();
  }

  @Test
  void getCookie_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    HttpCookie cookie = mock(HttpCookie.class);
    when(delegate.getCookie("test")).thenReturn(cookie);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    HttpCookie result = wrapper.getCookie("test");

    assertThat(result).isSameAs(cookie);
    verify(delegate).getCookie("test");
  }

  @Test
  void addCookieWithResponseCookie_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    ResponseCookie cookie = mock(ResponseCookie.class);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    wrapper.addCookie(cookie);

    verify(delegate).addCookie(cookie);
  }

  @Test
  void addCookieWithNameAndValue_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    wrapper.addCookie("name", "value");

    verify(delegate).addCookie("name", "value");
  }

  @Test
  void removeCookie_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    List<ResponseCookie> cookies = new ArrayList<>();
    when(delegate.removeCookies("name")).thenReturn(cookies);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    List<ResponseCookie> result = wrapper.removeCookies("name");

    assertThat(result).isSameAs(cookies);
    verify(delegate).removeCookies("name");
  }

  @Test
  void hasResponseCookie_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.hasResponseCookies()).thenReturn(true);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    boolean result = wrapper.hasResponseCookies();

    assertThat(result).isTrue();
    verify(delegate).hasResponseCookies();
  }

  @Test
  void responseCookies_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    ArrayList<ResponseCookie> cookies = new ArrayList<>();
    when(delegate.responseCookies()).thenReturn(cookies);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    List<ResponseCookie> result = wrapper.responseCookies();

    assertThat(result).isSameAs(cookies);
    verify(delegate).responseCookies();
  }

  @Test
  void getParameters_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    MultiValueMap<String, String> parameters = mock(MultiValueMap.class);
    when(delegate.getParameters()).thenReturn(parameters);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    MultiValueMap<String, String> result = wrapper.getParameters();

    assertThat(result).isSameAs(parameters);
    verify(delegate).getParameters();
  }

  @Test
  void getParameterNames_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    Set<String> parameterNames = Set.of("param1", "param2");
    when(delegate.getParameterNames()).thenReturn(parameterNames);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    Set<String> result = wrapper.getParameterNames();

    assertThat(result).isSameAs(parameterNames);
    verify(delegate).getParameterNames();
  }

  @Test
  void getParametersByName_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    String[] values = { "value1", "value2" };
    when(delegate.getParameterValues("param")).thenReturn(values);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    String[] result = wrapper.getParameterValues("param");

    assertThat(result).isSameAs(values);
    verify(delegate).getParameterValues("param");
  }

  @Test
  void getParameter_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.getParameter("param")).thenReturn("value");

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    String result = wrapper.getParameter("param");

    assertThat(result).isEqualTo("value");
    verify(delegate).getParameter("param");
  }

  @Test
  void getRemoteAddress_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.getRemoteAddress()).thenReturn("192.168.1.1");

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    String result = wrapper.getRemoteAddress();

    assertThat(result).isEqualTo("192.168.1.1");
    verify(delegate).getRemoteAddress();
  }

  @Test
  void getRemotePort_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.getRemotePort()).thenReturn(12345);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    int result = wrapper.getRemotePort();

    assertThat(result).isEqualTo(12345);
    verify(delegate).getRemotePort();
  }

  @Test
  void localAddress_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    SocketAddress address = mock(SocketAddress.class);
    when(delegate.localAddress()).thenReturn(address);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    SocketAddress result = wrapper.localAddress();

    assertThat(result).isSameAs(address);
    verify(delegate).localAddress();
  }

  @Test
  void remoteAddress_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    InetSocketAddress address = mock(InetSocketAddress.class);
    when(delegate.remoteAddress()).thenReturn(address);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    InetSocketAddress result = wrapper.remoteAddress();

    assertThat(result).isSameAs(address);
    verify(delegate).remoteAddress();
  }

  @Test
  void getContentLength_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.getContentLength()).thenReturn(1024L);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    long result = wrapper.getContentLength();

    assertThat(result).isEqualTo(1024L);
    verify(delegate).getContentLength();
  }

  @Test
  void getBody_ShouldDelegateToDelegate() throws IOException {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    InputStream body = mock(InputStream.class);
    when(delegate.getBody()).thenReturn(body);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    InputStream result = wrapper.getBody();

    assertThat(result).isSameAs(body);
    verify(delegate).getBody();
  }

  @Test
  void getHeaders_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(delegate.getHeaders()).thenReturn(headers);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    HttpHeaders result = wrapper.getHeaders();

    assertThat(result).isSameAs(headers);
    verify(delegate).getHeaders();
  }

  @Test
  void getInputStream_ShouldDelegateToDelegate() throws IOException {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    InputStream inputStream = mock(InputStream.class);
    when(delegate.getInputStream()).thenReturn(inputStream);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    InputStream result = wrapper.getInputStream();

    assertThat(result).isSameAs(inputStream);
    verify(delegate).getInputStream();
  }

  @Test
  void getReader_ShouldDelegateToDelegate() throws IOException {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    java.io.BufferedReader reader = mock(java.io.BufferedReader.class);
    when(delegate.getReader()).thenReturn(reader);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    java.io.BufferedReader result = wrapper.getReader();

    assertThat(result).isSameAs(reader);
    verify(delegate).getReader();
  }

  @Test
  void isMultipart_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.isMultipart()).thenReturn(true);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    boolean result = wrapper.isMultipart();

    assertThat(result).isTrue();
    verify(delegate).isMultipart();
  }

  @Test
  void getContentType_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.getContentTypeAsString()).thenReturn("application/json");

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    String result = wrapper.getContentTypeAsString();

    assertThat(result).isEqualTo("application/json");
    verify(delegate).getContentTypeAsString();
  }

  @Test
  void requestHeaders_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(delegate.requestHeaders()).thenReturn(headers);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    HttpHeaders result = wrapper.requestHeaders();

    assertThat(result).isSameAs(headers);
    verify(delegate).requestHeaders();
  }

  @Test
  void getLocale_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    Locale locale = Locale.ENGLISH;
    when(delegate.getPreferredLocale()).thenReturn(locale);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    Locale result = wrapper.getPreferredLocale();

    assertThat(result).isSameAs(locale);
    verify(delegate).getPreferredLocale();
  }

  @Test
  void checkNotModifiedWithTimestamp_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.checkNotModified(12345L)).thenReturn(true);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    boolean result = wrapper.checkNotModified(12345L);

    assertThat(result).isTrue();
    verify(delegate).checkNotModified(12345L);
  }

  @Test
  void checkNotModifiedWithETag_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.checkNotModified("etag-value")).thenReturn(true);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    boolean result = wrapper.checkNotModified("etag-value");

    assertThat(result).isTrue();
    verify(delegate).checkNotModified("etag-value");
  }

  @Test
  void checkNotModifiedWithETagAndTimestamp_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.checkNotModified("etag-value", 12345L)).thenReturn(true);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    boolean result = wrapper.checkNotModified("etag-value", 12345L);

    assertThat(result).isTrue();
    verify(delegate).checkNotModified("etag-value", 12345L);
  }

  @Test
  void isNotModified_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.isNotModified()).thenReturn(true);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    boolean result = wrapper.isNotModified();

    assertThat(result).isTrue();
    verify(delegate).isNotModified();
  }

  @Test
  void setContentLength_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    wrapper.setContentLength(1024L);

    verify(delegate).setContentLength(1024L);
  }

  @Test
  void isCommitted_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.isCommitted()).thenReturn(true);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    boolean result = wrapper.isCommitted();

    assertThat(result).isTrue();
    verify(delegate).isCommitted();
  }

  @Test
  void reset_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    wrapper.reset();

    verify(delegate).reset();
  }

  @Test
  void sendRedirect_ShouldDelegateToDelegate() throws IOException {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    wrapper.sendRedirect("http://example.com");

    verify(delegate).sendRedirect("http://example.com");
  }

  @Test
  void setStatusWithInt_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    wrapper.setStatus(200);

    verify(delegate).setStatus(200);
  }

  @Test
  void setStatusWithHttpStatusCode_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    wrapper.setStatus(HttpStatusCode.valueOf(200));

    verify(delegate).setStatus(HttpStatusCode.valueOf(200));
  }

  @Test
  void getStatus_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.getStatus()).thenReturn(200);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    int result = wrapper.getStatus();

    assertThat(result).isEqualTo(200);
    verify(delegate).getStatus();
  }

  @Test
  void sendErrorWithHttpStatusCode_ShouldDelegateToDelegate() throws IOException {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    wrapper.sendError(HttpStatus.NOT_FOUND);

    verify(delegate).sendError(HttpStatus.NOT_FOUND);
  }

  @Test
  void sendErrorWithHttpStatusCodeAndMessage_ShouldDelegateToDelegate() throws IOException {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    wrapper.sendError(HttpStatus.NOT_FOUND, "Not found");

    verify(delegate).sendError(HttpStatus.NOT_FOUND, "Not found");
  }

  @Test
  void sendErrorWithInt_ShouldDelegateToDelegate() throws IOException {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    wrapper.sendError(404);

    verify(delegate).sendError(404);
  }

  @Test
  void sendErrorWithIntAndMessage_ShouldDelegateToDelegate() throws IOException {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    wrapper.sendError(404, "Not found");

    verify(delegate).sendError(404, "Not found");
  }

  @Test
  void getOutputStream_ShouldDelegateToDelegate() throws IOException {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    java.io.OutputStream outputStream = mock(java.io.OutputStream.class);
    when(delegate.getOutputStream()).thenReturn(outputStream);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    java.io.OutputStream result = wrapper.getOutputStream();

    assertThat(result).isSameAs(outputStream);
    verify(delegate).getOutputStream();
  }

  @Test
  void getWriter_ShouldDelegateToDelegate() throws IOException {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    java.io.PrintWriter writer = mock(java.io.PrintWriter.class);
    when(delegate.getWriter()).thenReturn(writer);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    java.io.PrintWriter result = wrapper.getWriter();

    assertThat(result).isSameAs(writer);
    verify(delegate).getWriter();
  }

  @Test
  void setContentTypeWithString_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    wrapper.setContentType("text/html");

    verify(delegate).setContentType("text/html");
  }

  @Test
  void setContentTypeWithMediaType_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    wrapper.setContentType(MediaType.TEXT_HTML);

    verify(delegate).setContentType(MediaType.TEXT_HTML);
  }

  @Test
  void getResponseContentType_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.getResponseContentType()).thenReturn("text/html");

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    String result = wrapper.getResponseContentType();

    assertThat(result).isEqualTo("text/html");
    verify(delegate).getResponseContentType();
  }

  @Test
  void setHeader_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    wrapper.setHeader("Content-Type", "application/json");

    verify(delegate).setHeader("Content-Type", "application/json");
  }

  @Test
  void addHeader_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    wrapper.addHeader("Cache-Control", "no-cache");

    verify(delegate).addHeader("Cache-Control", "no-cache");
  }

  @Test
  void removeHeader_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    wrapper.removeHeader("Cache-Control");

    verify(delegate).removeHeader("Cache-Control");
  }

  @Test
  void containsResponseHeader_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.containsResponseHeader("Content-Type")).thenReturn(true);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    boolean result = wrapper.containsResponseHeader("Content-Type");

    assertThat(result).isTrue();
    verify(delegate).containsResponseHeader("Content-Type");
  }

  @Test
  void responseHeaders_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(delegate.responseHeaders()).thenReturn(headers);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    HttpHeaders result = wrapper.responseHeaders();

    assertThat(result).isSameAs(headers);
    verify(delegate).responseHeaders();
  }

  @Test
  void addHeaders_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    HttpHeaders headers = mock(HttpHeaders.class);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    wrapper.addHeaders(headers);

    verify(delegate).addHeaders(headers);
  }

  @Test
  void asHttpOutputMessage_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    ServerHttpResponse response = mock(ServerHttpResponse.class);
    when(delegate.asHttpOutputMessage()).thenReturn(response);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    ServerHttpResponse result = wrapper.asHttpOutputMessage();

    assertThat(result).isSameAs(response);
    verify(delegate).asHttpOutputMessage();
  }

  @Test
  void getAttribute_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.getAttribute("key")).thenReturn("value");

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    Object result = wrapper.getAttribute("key");

    assertThat(result).isEqualTo("value");
    verify(delegate).getAttribute("key");
  }

  @Test
  void setAttribute_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    wrapper.setAttribute("key", "value");

    verify(delegate).setAttribute("key", "value");
  }

  @Test
  void removeAttribute_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.removeAttribute("key")).thenReturn("value");

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    Object result = wrapper.removeAttribute("key");

    assertThat(result).isEqualTo("value");
    verify(delegate).removeAttribute("key");
  }

  @Test
  void clearAttributes_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    wrapper.clearAttributes();

    verify(delegate).clearAttributes();
  }

  @Test
  void getAttributeNames_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    String[] attributeNames = { "attr1", "attr2" };
    when(delegate.getAttributeNames()).thenReturn(attributeNames);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    String[] result = wrapper.getAttributeNames();

    assertThat(result).isSameAs(attributeNames);
    verify(delegate).getAttributeNames();
  }

  @Test
  void flush_ShouldDelegateToDelegate() throws IOException {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    wrapper.flush();

    verify(delegate).flush();
  }

  @Test
  void asyncRequest_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    WebAsyncRequest asyncRequest = mock(WebAsyncRequest.class);
    when(delegate.asyncRequest()).thenReturn(asyncRequest);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    WebAsyncRequest result = wrapper.asyncRequest();

    assertThat(result).isSameAs(asyncRequest);
    verify(delegate).asyncRequest();
  }

  @Test
  void isConcurrentHandlingStarted_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.isConcurrentHandlingStarted()).thenReturn(true);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    boolean result = wrapper.isConcurrentHandlingStarted();

    assertThat(result).isTrue();
    verify(delegate).isConcurrentHandlingStarted();
  }

  @Test
  void multipartRequest_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    MultipartRequest multipartRequest = mock(MultipartRequest.class);
    when(delegate.asMultipartRequest()).thenReturn(multipartRequest);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    MultipartRequest result = wrapper.asMultipartRequest();

    assertThat(result).isSameAs(multipartRequest);
    verify(delegate).asMultipartRequest();
  }

  @Test
  void hasAttribute_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.hasAttribute("key")).thenReturn(true);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    boolean result = wrapper.hasAttribute("key");

    assertThat(result).isTrue();
    verify(delegate).hasAttribute("key");
  }

  @Test
  void attributeNames_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    Iterable<String> names = List.of("attr1", "attr2");
    when(delegate.attributeNames()).thenReturn(names);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    Iterable<String> result = wrapper.attributeNames();

    assertThat(result).isSameAs(names);
    verify(delegate).attributeNames();
  }

  @Test
  void copyFrom_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    AttributeAccessor source = mock(AttributeAccessor.class);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    wrapper.copyAttributeFrom(source);

    verify(delegate).copyAttributeFrom(source);
  }

  @Test
  void hasAttributes_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.hasAttributes()).thenReturn(true);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    boolean result = wrapper.hasAttributes();

    assertThat(result).isTrue();
    verify(delegate).hasAttributes();
  }

  @Test
  void getAttributes_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    java.util.Map<String, Object> attributes = new java.util.HashMap<>();
    attributes.put("key", "value");
    when(delegate.getAttributes()).thenReturn(attributes);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    java.util.Map<String, Object> result = wrapper.getAttributes();

    assertThat(result).isSameAs(attributes);
    verify(delegate).getAttributes();
  }

  @Test
  void getMethod_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.getMethod()).thenReturn(HttpMethod.GET);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    HttpMethod result = wrapper.getMethod();

    assertThat(result).isSameAs(HttpMethod.GET);
    verify(delegate).getMethod();
  }

  @Test
  void getMethodAsString_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    when(delegate.getMethodAsString()).thenReturn("GET");

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    String result = wrapper.getMethodAsString();

    assertThat(result).isEqualTo("GET");
    verify(delegate).getMethodAsString();
  }

  @Test
  void asyncManager_ShouldDelegateToDelegate() {
    ServerHttpExchange delegate = mock(ServerHttpExchange.class);
    WebAsyncManager asyncManager = mock(WebAsyncManager.class);
    when(delegate.asyncManager()).thenReturn(asyncManager);

    DelegatingServerHttpExchange wrapper = new DelegatingServerHttpExchange(delegate);

    WebAsyncManager result = wrapper.asyncManager();

    assertThat(result).isSameAs(asyncManager);
    verify(delegate).asyncManager();
  }

}
