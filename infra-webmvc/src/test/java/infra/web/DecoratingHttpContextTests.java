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
import java.util.Set;

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
import infra.util.MultiValueMap;
import infra.web.async.AsyncWebRequest;
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
class DecoratingHttpContextTests {

  @Test
  void getDelegate_ShouldReturnDelegateInstance() {
    HttpContext delegate = mock(HttpContext.class);
    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    assertThat(wrapper.delegate()).isSameAs(delegate);
  }

  @Test
  void equals_ShouldReturnTrue_ForSameDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    DecoratingHttpContext wrapper1 = new DecoratingHttpContext(delegate);
    DecoratingHttpContext wrapper2 = new DecoratingHttpContext(delegate);

    assertThat(wrapper1).isEqualTo(wrapper2);
  }

  @Test
  void equals_ShouldReturnFalse_ForDifferentDelegate() {
    HttpContext delegate1 = mock(HttpContext.class);
    HttpContext delegate2 = mock(HttpContext.class);
    DecoratingHttpContext wrapper1 = new DecoratingHttpContext(delegate1);
    DecoratingHttpContext wrapper2 = new DecoratingHttpContext(delegate2);

    assertThat(wrapper1).isNotEqualTo(wrapper2);
  }

  @Test
  void equals_ShouldReturnFalse_ForNonDecoratingHttpContext() {
    HttpContext delegate = mock(HttpContext.class);
    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    assertThat(wrapper).isNotEqualTo(delegate);
  }

  @Test
  void equals_ShouldReturnTrue_ForSameInstance() {
    DecoratingHttpContext wrapper = new DecoratingHttpContext(mock());

    assertThat(wrapper).isEqualTo(wrapper);
  }

  @Test
  void hashCode_ShouldReturnIdentityHashCode() {
    DecoratingHttpContext wrapper = new DecoratingHttpContext(mock(HttpContext.class));

    assertThat(wrapper.hashCode()).isEqualTo(System.identityHashCode(wrapper));
  }

  @Test
  void toString_ShouldIncludeDelegateInfo() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.toString()).thenReturn("MockDelegate");

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    assertThat(wrapper.toString()).isEqualTo("Wrapper for MockDelegate");
  }

  @Test
  void allMethodsShouldDelegateToWrappedInstance() throws IOException {
    HttpContext delegate = mock(HttpContext.class);
    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

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
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.getRequestTimeMillis()).thenReturn(12345L);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    long result = wrapper.getRequestTimeMillis();

    assertThat(result).isEqualTo(12345L);
    verify(delegate).getRequestTimeMillis();
  }

  @Test
  void getApplicationContext_ShouldDelegateToDelegate() {
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.getApplicationContext()).thenReturn(applicationContext);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    ApplicationContext result = wrapper.getApplicationContext();

    assertThat(result).isSameAs(applicationContext);
    verify(delegate).getApplicationContext();
  }

  @Test
  void getReaderWithCharset_ShouldDelegateToDelegate() throws IOException {
    HttpContext delegate = mock(HttpContext.class);
    Reader reader = mock(Reader.class);
    Charset charset = Charset.defaultCharset();
    when(delegate.getReader(charset)).thenReturn(reader);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    Reader result = wrapper.getReader(charset);

    assertThat(result).isSameAs(reader);
    verify(delegate).getReader(charset);
  }

  @Test
  void readableChannel_ShouldDelegateToDelegate() throws IOException {
    HttpContext delegate = mock(HttpContext.class);
    ReadableByteChannel channel = mock(ReadableByteChannel.class);
    when(delegate.readableChannel()).thenReturn(channel);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    ReadableByteChannel result = wrapper.readableChannel();

    assertThat(result).isSameAs(channel);
    verify(delegate).readableChannel();
  }

  @Test
  void writableChannel_ShouldDelegateToDelegate() throws IOException {
    HttpContext delegate = mock(HttpContext.class);
    WritableByteChannel channel = mock(WritableByteChannel.class);
    when(delegate.writableChannel()).thenReturn(channel);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    WritableByteChannel result = wrapper.writableChannel();

    assertThat(result).isSameAs(channel);
    verify(delegate).writableChannel();
  }

  @Test
  void getScheme_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.getScheme()).thenReturn("https");

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    String result = wrapper.getScheme();

    assertThat(result).isEqualTo("https");
    verify(delegate).getScheme();
  }

  @Test
  void getServerPort_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.getServerPort()).thenReturn(8080);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    int result = wrapper.getServerPort();

    assertThat(result).isEqualTo(8080);
    verify(delegate).getServerPort();
  }

  @Test
  void getServerName_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.getServerName()).thenReturn("localhost");

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    String result = wrapper.getServerName();

    assertThat(result).isEqualTo("localhost");
    verify(delegate).getServerName();
  }

  @Test
  void getURI_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    URI uri = URI.create("http://localhost:8080/test");
    when(delegate.getURI()).thenReturn(uri);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    URI result = wrapper.getURI();

    assertThat(result).isSameAs(uri);
    verify(delegate).getURI();
  }

  @Test
  void isPreFlightRequest_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.isPreFlightRequest()).thenReturn(true);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    boolean result = wrapper.isPreFlightRequest();

    assertThat(result).isTrue();
    verify(delegate).isPreFlightRequest();
  }

  @Test
  void isCorsRequest_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.isCorsRequest()).thenReturn(true);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    boolean result = wrapper.isCorsRequest();

    assertThat(result).isTrue();
    verify(delegate).isCorsRequest();
  }

  @Test
  void getRequestPath_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    RequestPath requestPath = mock(RequestPath.class);
    when(delegate.getRequestPath()).thenReturn(requestPath);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    RequestPath result = wrapper.getRequestPath();

    assertThat(result).isSameAs(requestPath);
    verify(delegate).getRequestPath();
  }

  @Test
  void getRequestURL_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.getRequestURL()).thenReturn("http://localhost:8080/test");

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    String result = wrapper.getRequestURL();

    assertThat(result).isEqualTo("http://localhost:8080/test");
    verify(delegate).getRequestURL();
  }

  @Test
  void getQueryString_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.getQueryString()).thenReturn("param=value");

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    String result = wrapper.getQueryString();

    assertThat(result).isEqualTo("param=value");
    verify(delegate).getQueryString();
  }

  @Test
  void getCookies_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    HttpCookie[] cookies = new HttpCookie[0];
    when(delegate.getCookies()).thenReturn(cookies);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    HttpCookie[] result = wrapper.getCookies();

    assertThat(result).isSameAs(cookies);
    verify(delegate).getCookies();
  }

  @Test
  void getCookie_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    HttpCookie cookie = mock(HttpCookie.class);
    when(delegate.getCookie("test")).thenReturn(cookie);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    HttpCookie result = wrapper.getCookie("test");

    assertThat(result).isSameAs(cookie);
    verify(delegate).getCookie("test");
  }

  @Test
  void addCookieWithResponseCookie_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    ResponseCookie cookie = mock(ResponseCookie.class);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    wrapper.addCookie(cookie);

    verify(delegate).addCookie(cookie);
  }

  @Test
  void addCookieWithNameAndValue_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    wrapper.addCookie("name", "value");

    verify(delegate).addCookie("name", "value");
  }

  @Test
  void removeCookie_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    List<ResponseCookie> cookies = new ArrayList<>();
    when(delegate.removeCookie("name")).thenReturn(cookies);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    List<ResponseCookie> result = wrapper.removeCookie("name");

    assertThat(result).isSameAs(cookies);
    verify(delegate).removeCookie("name");
  }

  @Test
  void hasResponseCookie_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.hasResponseCookie()).thenReturn(true);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    boolean result = wrapper.hasResponseCookie();

    assertThat(result).isTrue();
    verify(delegate).hasResponseCookie();
  }

  @Test
  void responseCookies_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    ArrayList<ResponseCookie> cookies = new ArrayList<>();
    when(delegate.responseCookies()).thenReturn(cookies);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    List<ResponseCookie> result = wrapper.responseCookies();

    assertThat(result).isSameAs(cookies);
    verify(delegate).responseCookies();
  }

  @Test
  void getParameters_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    MultiValueMap<String, String> parameters = mock(MultiValueMap.class);
    when(delegate.getParameters()).thenReturn(parameters);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    MultiValueMap<String, String> result = wrapper.getParameters();

    assertThat(result).isSameAs(parameters);
    verify(delegate).getParameters();
  }

  @Test
  void getParameterNames_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    Set<String> parameterNames = Set.of("param1", "param2");
    when(delegate.getParameterNames()).thenReturn(parameterNames);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    Set<String> result = wrapper.getParameterNames();

    assertThat(result).isSameAs(parameterNames);
    verify(delegate).getParameterNames();
  }

  @Test
  void getParametersByName_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    String[] values = { "value1", "value2" };
    when(delegate.getParameters("param")).thenReturn(values);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    String[] result = wrapper.getParameters("param");

    assertThat(result).isSameAs(values);
    verify(delegate).getParameters("param");
  }

  @Test
  void getParameter_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.getParameter("param")).thenReturn("value");

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    String result = wrapper.getParameter("param");

    assertThat(result).isEqualTo("value");
    verify(delegate).getParameter("param");
  }

  @Test
  void getRemoteAddress_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.getRemoteAddress()).thenReturn("192.168.1.1");

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    String result = wrapper.getRemoteAddress();

    assertThat(result).isEqualTo("192.168.1.1");
    verify(delegate).getRemoteAddress();
  }

  @Test
  void getRemotePort_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.getRemotePort()).thenReturn(12345);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    int result = wrapper.getRemotePort();

    assertThat(result).isEqualTo(12345);
    verify(delegate).getRemotePort();
  }

  @Test
  void localAddress_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    SocketAddress address = mock(SocketAddress.class);
    when(delegate.localAddress()).thenReturn(address);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    SocketAddress result = wrapper.localAddress();

    assertThat(result).isSameAs(address);
    verify(delegate).localAddress();
  }

  @Test
  void remoteAddress_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    InetSocketAddress address = mock(InetSocketAddress.class);
    when(delegate.remoteAddress()).thenReturn(address);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    InetSocketAddress result = wrapper.remoteAddress();

    assertThat(result).isSameAs(address);
    verify(delegate).remoteAddress();
  }

  @Test
  void getContentLength_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.getContentLength()).thenReturn(1024L);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    long result = wrapper.getContentLength();

    assertThat(result).isEqualTo(1024L);
    verify(delegate).getContentLength();
  }

  @Test
  void getBody_ShouldDelegateToDelegate() throws IOException {
    HttpContext delegate = mock(HttpContext.class);
    InputStream body = mock(InputStream.class);
    when(delegate.getBody()).thenReturn(body);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    InputStream result = wrapper.getBody();

    assertThat(result).isSameAs(body);
    verify(delegate).getBody();
  }

  @Test
  void getHeaders_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(delegate.getHeaders()).thenReturn(headers);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    HttpHeaders result = wrapper.getHeaders();

    assertThat(result).isSameAs(headers);
    verify(delegate).getHeaders();
  }

  @Test
  void getInputStream_ShouldDelegateToDelegate() throws IOException {
    HttpContext delegate = mock(HttpContext.class);
    InputStream inputStream = mock(InputStream.class);
    when(delegate.getInputStream()).thenReturn(inputStream);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    InputStream result = wrapper.getInputStream();

    assertThat(result).isSameAs(inputStream);
    verify(delegate).getInputStream();
  }

  @Test
  void getReader_ShouldDelegateToDelegate() throws IOException {
    HttpContext delegate = mock(HttpContext.class);
    java.io.BufferedReader reader = mock(java.io.BufferedReader.class);
    when(delegate.getReader()).thenReturn(reader);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    java.io.BufferedReader result = wrapper.getReader();

    assertThat(result).isSameAs(reader);
    verify(delegate).getReader();
  }

  @Test
  void isMultipart_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.isMultipart()).thenReturn(true);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    boolean result = wrapper.isMultipart();

    assertThat(result).isTrue();
    verify(delegate).isMultipart();
  }

  @Test
  void getContentType_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.getContentTypeAsString()).thenReturn("application/json");

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    String result = wrapper.getContentTypeAsString();

    assertThat(result).isEqualTo("application/json");
    verify(delegate).getContentTypeAsString();
  }

  @Test
  void requestHeaders_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(delegate.requestHeaders()).thenReturn(headers);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    HttpHeaders result = wrapper.requestHeaders();

    assertThat(result).isSameAs(headers);
    verify(delegate).requestHeaders();
  }

  @Test
  void getLocale_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    Locale locale = Locale.ENGLISH;
    when(delegate.getLocale()).thenReturn(locale);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    Locale result = wrapper.getLocale();

    assertThat(result).isSameAs(locale);
    verify(delegate).getLocale();
  }

  @Test
  void checkNotModifiedWithTimestamp_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.checkNotModified(12345L)).thenReturn(true);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    boolean result = wrapper.checkNotModified(12345L);

    assertThat(result).isTrue();
    verify(delegate).checkNotModified(12345L);
  }

  @Test
  void checkNotModifiedWithETag_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.checkNotModified("etag-value")).thenReturn(true);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    boolean result = wrapper.checkNotModified("etag-value");

    assertThat(result).isTrue();
    verify(delegate).checkNotModified("etag-value");
  }

  @Test
  void checkNotModifiedWithETagAndTimestamp_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.checkNotModified("etag-value", 12345L)).thenReturn(true);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    boolean result = wrapper.checkNotModified("etag-value", 12345L);

    assertThat(result).isTrue();
    verify(delegate).checkNotModified("etag-value", 12345L);
  }

  @Test
  void isNotModified_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.isNotModified()).thenReturn(true);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    boolean result = wrapper.isNotModified();

    assertThat(result).isTrue();
    verify(delegate).isNotModified();
  }

  @Test
  void setContentLength_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    wrapper.setContentLength(1024L);

    verify(delegate).setContentLength(1024L);
  }

  @Test
  void isCommitted_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.isCommitted()).thenReturn(true);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    boolean result = wrapper.isCommitted();

    assertThat(result).isTrue();
    verify(delegate).isCommitted();
  }

  @Test
  void reset_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    wrapper.reset();

    verify(delegate).reset();
  }

  @Test
  void sendRedirect_ShouldDelegateToDelegate() throws IOException {
    HttpContext delegate = mock(HttpContext.class);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    wrapper.sendRedirect("http://example.com");

    verify(delegate).sendRedirect("http://example.com");
  }

  @Test
  void setStatusWithInt_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    wrapper.setStatus(200);

    verify(delegate).setStatus(200);
  }

  @Test
  void setStatusWithHttpStatusCode_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    wrapper.setStatus(HttpStatusCode.valueOf(200));

    verify(delegate).setStatus(HttpStatusCode.valueOf(200));
  }

  @Test
  void getStatus_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.getStatus()).thenReturn(200);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    int result = wrapper.getStatus();

    assertThat(result).isEqualTo(200);
    verify(delegate).getStatus();
  }

  @Test
  void sendErrorWithHttpStatusCode_ShouldDelegateToDelegate() throws IOException {
    HttpContext delegate = mock(HttpContext.class);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    wrapper.sendError(HttpStatus.NOT_FOUND);

    verify(delegate).sendError(HttpStatus.NOT_FOUND);
  }

  @Test
  void sendErrorWithHttpStatusCodeAndMessage_ShouldDelegateToDelegate() throws IOException {
    HttpContext delegate = mock(HttpContext.class);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    wrapper.sendError(HttpStatus.NOT_FOUND, "Not found");

    verify(delegate).sendError(HttpStatus.NOT_FOUND, "Not found");
  }

  @Test
  void sendErrorWithInt_ShouldDelegateToDelegate() throws IOException {
    HttpContext delegate = mock(HttpContext.class);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    wrapper.sendError(404);

    verify(delegate).sendError(404);
  }

  @Test
  void sendErrorWithIntAndMessage_ShouldDelegateToDelegate() throws IOException {
    HttpContext delegate = mock(HttpContext.class);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    wrapper.sendError(404, "Not found");

    verify(delegate).sendError(404, "Not found");
  }

  @Test
  void getOutputStream_ShouldDelegateToDelegate() throws IOException {
    HttpContext delegate = mock(HttpContext.class);
    java.io.OutputStream outputStream = mock(java.io.OutputStream.class);
    when(delegate.getOutputStream()).thenReturn(outputStream);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    java.io.OutputStream result = wrapper.getOutputStream();

    assertThat(result).isSameAs(outputStream);
    verify(delegate).getOutputStream();
  }

  @Test
  void getWriter_ShouldDelegateToDelegate() throws IOException {
    HttpContext delegate = mock(HttpContext.class);
    java.io.PrintWriter writer = mock(java.io.PrintWriter.class);
    when(delegate.getWriter()).thenReturn(writer);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    java.io.PrintWriter result = wrapper.getWriter();

    assertThat(result).isSameAs(writer);
    verify(delegate).getWriter();
  }

  @Test
  void setContentTypeWithString_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    wrapper.setContentType("text/html");

    verify(delegate).setContentType("text/html");
  }

  @Test
  void setContentTypeWithMediaType_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    wrapper.setContentType(MediaType.TEXT_HTML);

    verify(delegate).setContentType(MediaType.TEXT_HTML);
  }

  @Test
  void getResponseContentType_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.getResponseContentType()).thenReturn("text/html");

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    String result = wrapper.getResponseContentType();

    assertThat(result).isEqualTo("text/html");
    verify(delegate).getResponseContentType();
  }

  @Test
  void setHeader_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    wrapper.setHeader("Content-Type", "application/json");

    verify(delegate).setHeader("Content-Type", "application/json");
  }

  @Test
  void addHeader_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    wrapper.addHeader("Cache-Control", "no-cache");

    verify(delegate).addHeader("Cache-Control", "no-cache");
  }

  @Test
  void removeHeader_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    wrapper.removeHeader("Cache-Control");

    verify(delegate).removeHeader("Cache-Control");
  }

  @Test
  void containsResponseHeader_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.containsResponseHeader("Content-Type")).thenReturn(true);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    boolean result = wrapper.containsResponseHeader("Content-Type");

    assertThat(result).isTrue();
    verify(delegate).containsResponseHeader("Content-Type");
  }

  @Test
  void responseHeaders_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(delegate.responseHeaders()).thenReturn(headers);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    HttpHeaders result = wrapper.responseHeaders();

    assertThat(result).isSameAs(headers);
    verify(delegate).responseHeaders();
  }

  @Test
  void addHeaders_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    HttpHeaders headers = mock(HttpHeaders.class);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    wrapper.addHeaders(headers);

    verify(delegate).addHeaders(headers);
  }

  @Test
  void asHttpOutputMessage_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    ServerHttpResponse response = mock(ServerHttpResponse.class);
    when(delegate.asHttpOutputMessage()).thenReturn(response);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    ServerHttpResponse result = wrapper.asHttpOutputMessage();

    assertThat(result).isSameAs(response);
    verify(delegate).asHttpOutputMessage();
  }

  @Test
  void getMatchingMetadata_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    HandlerMatchingMetadata metadata = mock(HandlerMatchingMetadata.class);
    when(delegate.getMatchingMetadata()).thenReturn(metadata);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    HandlerMatchingMetadata result = wrapper.getMatchingMetadata();

    assertThat(result).isSameAs(metadata);
    verify(delegate).getMatchingMetadata();
  }

  @Test
  void setMatchingMetadata_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    HandlerMatchingMetadata metadata = mock(HandlerMatchingMetadata.class);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    wrapper.setMatchingMetadata(metadata);

    verify(delegate).setMatchingMetadata(metadata);
  }

  @Test
  void hasMatchingMetadata_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.hasMatchingMetadata()).thenReturn(true);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    boolean result = wrapper.hasMatchingMetadata();

    assertThat(result).isTrue();
    verify(delegate).hasMatchingMetadata();
  }

  @Test
  void getAttribute_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.getAttribute("key")).thenReturn("value");

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    Object result = wrapper.getAttribute("key");

    assertThat(result).isEqualTo("value");
    verify(delegate).getAttribute("key");
  }

  @Test
  void setAttribute_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    wrapper.setAttribute("key", "value");

    verify(delegate).setAttribute("key", "value");
  }

  @Test
  void removeAttribute_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.removeAttribute("key")).thenReturn("value");

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    Object result = wrapper.removeAttribute("key");

    assertThat(result).isEqualTo("value");
    verify(delegate).removeAttribute("key");
  }

  @Test
  void clearAttributes_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    wrapper.clearAttributes();

    verify(delegate).clearAttributes();
  }

  @Test
  void getAttributeNames_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    String[] attributeNames = { "attr1", "attr2" };
    when(delegate.getAttributeNames()).thenReturn(attributeNames);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    String[] result = wrapper.getAttributeNames();

    assertThat(result).isSameAs(attributeNames);
    verify(delegate).getAttributeNames();
  }

  @Test
  void flush_ShouldDelegateToDelegate() throws IOException {
    HttpContext delegate = mock(HttpContext.class);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    wrapper.flush();

    verify(delegate).flush();
  }

  @Test
  void asyncWebRequest_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    AsyncWebRequest asyncWebRequest = mock(AsyncWebRequest.class);
    when(delegate.asyncWebRequest()).thenReturn(asyncWebRequest);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    AsyncWebRequest result = wrapper.asyncWebRequest();

    assertThat(result).isSameAs(asyncWebRequest);
    verify(delegate).asyncWebRequest();
  }

  @Test
  void isConcurrentHandlingStarted_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.isConcurrentHandlingStarted()).thenReturn(true);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    boolean result = wrapper.isConcurrentHandlingStarted();

    assertThat(result).isTrue();
    verify(delegate).isConcurrentHandlingStarted();
  }

  @Test
  void multipartRequest_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    MultipartRequest multipartRequest = mock(MultipartRequest.class);
    when(delegate.asMultipartRequest()).thenReturn(multipartRequest);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    MultipartRequest result = wrapper.asMultipartRequest();

    assertThat(result).isSameAs(multipartRequest);
    verify(delegate).asMultipartRequest();
  }

  @Test
  void setBinding_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    BindingContext bindingContext = mock(BindingContext.class);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    wrapper.setBinding(bindingContext);

    verify(delegate).setBinding(bindingContext);
  }

  @Test
  void getBinding_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    BindingContext bindingContext = mock(BindingContext.class);
    when(delegate.getBinding()).thenReturn(bindingContext);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    BindingContext result = wrapper.getBinding();

    assertThat(result).isSameAs(bindingContext);
    verify(delegate).getBinding();
  }

  @Test
  void binding_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    BindingContext bindingContext = mock(BindingContext.class);
    when(delegate.binding()).thenReturn(bindingContext);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    BindingContext result = wrapper.binding();

    assertThat(result).isSameAs(bindingContext);
    verify(delegate).binding();
  }

  @Test
  void hasBinding_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.hasBinding()).thenReturn(true);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    boolean result = wrapper.hasBinding();

    assertThat(result).isTrue();
    verify(delegate).hasBinding();
  }

  @Test
  void getInputRedirectModel_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    RedirectModel redirectModel = mock(RedirectModel.class);
    when(delegate.getInputRedirectModel()).thenReturn(redirectModel);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    RedirectModel result = wrapper.getInputRedirectModel();

    assertThat((Object) result).isSameAs(redirectModel);
    verify(delegate).getInputRedirectModel();
  }

  @Test
  void getInputRedirectModelWithManager_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    RedirectModel redirectModel = mock(RedirectModel.class);
    RedirectModelManager manager = mock(RedirectModelManager.class);
    when(delegate.getInputRedirectModel(manager)).thenReturn(redirectModel);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    RedirectModel result = wrapper.getInputRedirectModel(manager);

    assertThat((Object) result).isSameAs(redirectModel);
    verify(delegate).getInputRedirectModel(manager);
  }

  @Test
  void hasAttribute_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.hasAttribute("key")).thenReturn(true);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    boolean result = wrapper.hasAttribute("key");

    assertThat(result).isTrue();
    verify(delegate).hasAttribute("key");
  }

  @Test
  void attributeNames_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    Iterable<String> names = List.of("attr1", "attr2");
    when(delegate.attributeNames()).thenReturn(names);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    Iterable<String> result = wrapper.attributeNames();

    assertThat(result).isSameAs(names);
    verify(delegate).attributeNames();
  }

  @Test
  void copyFrom_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    AttributeAccessor source = mock(AttributeAccessor.class);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    wrapper.copyAttributeFrom(source);

    verify(delegate).copyAttributeFrom(source);
  }

  @Test
  void hasAttributes_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.hasAttributes()).thenReturn(true);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    boolean result = wrapper.hasAttributes();

    assertThat(result).isTrue();
    verify(delegate).hasAttributes();
  }

  @Test
  void getAttributes_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    java.util.Map<String, Object> attributes = new java.util.HashMap<>();
    attributes.put("key", "value");
    when(delegate.getAttributes()).thenReturn(attributes);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    java.util.Map<String, Object> result = wrapper.getAttributes();

    assertThat(result).isSameAs(attributes);
    verify(delegate).getAttributes();
  }

  @Test
  void getMethod_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.getMethod()).thenReturn(HttpMethod.GET);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    HttpMethod result = wrapper.getMethod();

    assertThat(result).isSameAs(HttpMethod.GET);
    verify(delegate).getMethod();
  }

  @Test
  void getMethodAsString_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    when(delegate.getMethodAsString()).thenReturn("GET");

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    String result = wrapper.getMethodAsString();

    assertThat(result).isEqualTo("GET");
    verify(delegate).getMethodAsString();
  }

  @Test
  void matchingMetadata_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    HandlerMatchingMetadata metadata = mock(HandlerMatchingMetadata.class);
    when(delegate.matchingMetadata()).thenReturn(metadata);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    HandlerMatchingMetadata result = wrapper.matchingMetadata();

    assertThat(result).isSameAs(metadata);
    verify(delegate).matchingMetadata();
  }

  @Test
  void asyncManager_ShouldDelegateToDelegate() {
    HttpContext delegate = mock(HttpContext.class);
    WebAsyncManager asyncManager = mock(WebAsyncManager.class);
    when(delegate.asyncManager()).thenReturn(asyncManager);

    DecoratingHttpContext wrapper = new DecoratingHttpContext(delegate);

    WebAsyncManager result = wrapper.asyncManager();

    assertThat(result).isSameAs(asyncManager);
    verify(delegate).asyncManager();
  }

}