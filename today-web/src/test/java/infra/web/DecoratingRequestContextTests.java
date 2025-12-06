/*
 * Copyright 2017 - 2025 the original author or authors.
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
class DecoratingRequestContextTests {

  @Test
  void getDelegate_ShouldReturnDelegateInstance() {
    RequestContext delegate = mock(RequestContext.class);
    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    assertThat(wrapper.getDelegate()).isSameAs(delegate);
  }

  @Test
  void equals_ShouldReturnTrue_ForSameDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    DecoratingRequestContext wrapper1 = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    DecoratingRequestContext wrapper2 = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    assertThat(wrapper1).isEqualTo(wrapper2);
  }

  @Test
  void equals_ShouldReturnFalse_ForDifferentDelegate() {
    RequestContext delegate1 = mock(RequestContext.class);
    RequestContext delegate2 = mock(RequestContext.class);
    DecoratingRequestContext wrapper1 = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate1;
      }
    };

    DecoratingRequestContext wrapper2 = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate2;
      }
    };

    assertThat(wrapper1).isNotEqualTo(wrapper2);
  }

  @Test
  void equals_ShouldReturnFalse_ForNonDecoratingRequestContext() {
    RequestContext delegate = mock(RequestContext.class);
    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    assertThat(wrapper).isNotEqualTo(delegate);
  }

  @Test
  void equals_ShouldReturnTrue_ForSameInstance() {
    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return mock(RequestContext.class);
      }
    };

    assertThat(wrapper).isEqualTo(wrapper);
  }

  @Test
  void hashCode_ShouldReturnIdentityHashCode() {
    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return mock(RequestContext.class);
      }
    };

    assertThat(wrapper.hashCode()).isEqualTo(System.identityHashCode(wrapper));
  }

  @Test
  void toString_ShouldIncludeDelegateInfo() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.toString()).thenReturn("MockDelegate");

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    assertThat(wrapper.toString()).isEqualTo("Wrapper for MockDelegate");
  }

  @Test
  void allMethodsShouldDelegateToWrappedInstance() throws IOException {
    RequestContext delegate = mock(RequestContext.class);
    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

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
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.getRequestTimeMillis()).thenReturn(12345L);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    long result = wrapper.getRequestTimeMillis();

    assertThat(result).isEqualTo(12345L);
    verify(delegate).getRequestTimeMillis();
  }

  @Test
  void getApplicationContext_ShouldDelegateToDelegate() {
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.getApplicationContext()).thenReturn(applicationContext);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    ApplicationContext result = wrapper.getApplicationContext();

    assertThat(result).isSameAs(applicationContext);
    verify(delegate).getApplicationContext();
  }

  @Test
  void getReaderWithCharset_ShouldDelegateToDelegate() throws IOException {
    RequestContext delegate = mock(RequestContext.class);
    Reader reader = mock(Reader.class);
    Charset charset = Charset.defaultCharset();
    when(delegate.getReader(charset)).thenReturn(reader);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    Reader result = wrapper.getReader(charset);

    assertThat(result).isSameAs(reader);
    verify(delegate).getReader(charset);
  }

  @Test
  void readableChannel_ShouldDelegateToDelegate() throws IOException {
    RequestContext delegate = mock(RequestContext.class);
    ReadableByteChannel channel = mock(ReadableByteChannel.class);
    when(delegate.readableChannel()).thenReturn(channel);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    ReadableByteChannel result = wrapper.readableChannel();

    assertThat(result).isSameAs(channel);
    verify(delegate).readableChannel();
  }

  @Test
  void writableChannel_ShouldDelegateToDelegate() throws IOException {
    RequestContext delegate = mock(RequestContext.class);
    WritableByteChannel channel = mock(WritableByteChannel.class);
    when(delegate.writableChannel()).thenReturn(channel);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    WritableByteChannel result = wrapper.writableChannel();

    assertThat(result).isSameAs(channel);
    verify(delegate).writableChannel();
  }

  @Test
  void getScheme_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.getScheme()).thenReturn("https");

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    String result = wrapper.getScheme();

    assertThat(result).isEqualTo("https");
    verify(delegate).getScheme();
  }

  @Test
  void getServerPort_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.getServerPort()).thenReturn(8080);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    int result = wrapper.getServerPort();

    assertThat(result).isEqualTo(8080);
    verify(delegate).getServerPort();
  }

  @Test
  void getServerName_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.getServerName()).thenReturn("localhost");

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    String result = wrapper.getServerName();

    assertThat(result).isEqualTo("localhost");
    verify(delegate).getServerName();
  }

  @Test
  void getURI_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    URI uri = URI.create("http://localhost:8080/test");
    when(delegate.getURI()).thenReturn(uri);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    URI result = wrapper.getURI();

    assertThat(result).isSameAs(uri);
    verify(delegate).getURI();
  }

  @Test
  void isPreFlightRequest_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.isPreFlightRequest()).thenReturn(true);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    boolean result = wrapper.isPreFlightRequest();

    assertThat(result).isTrue();
    verify(delegate).isPreFlightRequest();
  }

  @Test
  void isCorsRequest_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.isCorsRequest()).thenReturn(true);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    boolean result = wrapper.isCorsRequest();

    assertThat(result).isTrue();
    verify(delegate).isCorsRequest();
  }

  @Test
  void getRequestPath_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    RequestPath requestPath = mock(RequestPath.class);
    when(delegate.getRequestPath()).thenReturn(requestPath);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    RequestPath result = wrapper.getRequestPath();

    assertThat(result).isSameAs(requestPath);
    verify(delegate).getRequestPath();
  }

  @Test
  void getRequestURL_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.getRequestURL()).thenReturn("http://localhost:8080/test");

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    String result = wrapper.getRequestURL();

    assertThat(result).isEqualTo("http://localhost:8080/test");
    verify(delegate).getRequestURL();
  }

  @Test
  void getQueryString_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.getQueryString()).thenReturn("param=value");

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    String result = wrapper.getQueryString();

    assertThat(result).isEqualTo("param=value");
    verify(delegate).getQueryString();
  }

  @Test
  void getCookies_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    HttpCookie[] cookies = new HttpCookie[0];
    when(delegate.getCookies()).thenReturn(cookies);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    HttpCookie[] result = wrapper.getCookies();

    assertThat(result).isSameAs(cookies);
    verify(delegate).getCookies();
  }

  @Test
  void getCookie_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    HttpCookie cookie = mock(HttpCookie.class);
    when(delegate.getCookie("test")).thenReturn(cookie);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    HttpCookie result = wrapper.getCookie("test");

    assertThat(result).isSameAs(cookie);
    verify(delegate).getCookie("test");
  }

  @Test
  void addCookieWithResponseCookie_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    ResponseCookie cookie = mock(ResponseCookie.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.addCookie(cookie);

    verify(delegate).addCookie(cookie);
  }

  @Test
  void addCookieWithNameAndValue_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.addCookie("name", "value");

    verify(delegate).addCookie("name", "value");
  }

  @Test
  void removeCookie_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    List<ResponseCookie> cookies = new ArrayList<>();
    when(delegate.removeCookie("name")).thenReturn(cookies);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    List<ResponseCookie> result = wrapper.removeCookie("name");

    assertThat(result).isSameAs(cookies);
    verify(delegate).removeCookie("name");
  }

  @Test
  void hasResponseCookie_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.hasResponseCookie()).thenReturn(true);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    boolean result = wrapper.hasResponseCookie();

    assertThat(result).isTrue();
    verify(delegate).hasResponseCookie();
  }

  @Test
  void responseCookies_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    ArrayList<ResponseCookie> cookies = new ArrayList<>();
    when(delegate.responseCookies()).thenReturn(cookies);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    ArrayList<ResponseCookie> result = wrapper.responseCookies();

    assertThat(result).isSameAs(cookies);
    verify(delegate).responseCookies();
  }

  @Test
  void getParameters_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    MultiValueMap<String, String> parameters = mock(MultiValueMap.class);
    when(delegate.getParameters()).thenReturn(parameters);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    MultiValueMap<String, String> result = wrapper.getParameters();

    assertThat(result).isSameAs(parameters);
    verify(delegate).getParameters();
  }

  @Test
  void getParameterNames_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    Set<String> parameterNames = Set.of("param1", "param2");
    when(delegate.getParameterNames()).thenReturn(parameterNames);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    Set<String> result = wrapper.getParameterNames();

    assertThat(result).isSameAs(parameterNames);
    verify(delegate).getParameterNames();
  }

  @Test
  void getParametersByName_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    String[] values = { "value1", "value2" };
    when(delegate.getParameters("param")).thenReturn(values);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    String[] result = wrapper.getParameters("param");

    assertThat(result).isSameAs(values);
    verify(delegate).getParameters("param");
  }

  @Test
  void getParameter_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.getParameter("param")).thenReturn("value");

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    String result = wrapper.getParameter("param");

    assertThat(result).isEqualTo("value");
    verify(delegate).getParameter("param");
  }

  @Test
  void getRemoteAddress_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.getRemoteAddress()).thenReturn("192.168.1.1");

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    String result = wrapper.getRemoteAddress();

    assertThat(result).isEqualTo("192.168.1.1");
    verify(delegate).getRemoteAddress();
  }

  @Test
  void getRemotePort_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.getRemotePort()).thenReturn(12345);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    int result = wrapper.getRemotePort();

    assertThat(result).isEqualTo(12345);
    verify(delegate).getRemotePort();
  }

  @Test
  void localAddress_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    SocketAddress address = mock(SocketAddress.class);
    when(delegate.localAddress()).thenReturn(address);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    SocketAddress result = wrapper.localAddress();

    assertThat(result).isSameAs(address);
    verify(delegate).localAddress();
  }

  @Test
  void remoteAddress_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    InetSocketAddress address = mock(InetSocketAddress.class);
    when(delegate.remoteAddress()).thenReturn(address);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    InetSocketAddress result = wrapper.remoteAddress();

    assertThat(result).isSameAs(address);
    verify(delegate).remoteAddress();
  }

  @Test
  void getContentLength_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.getContentLength()).thenReturn(1024L);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    long result = wrapper.getContentLength();

    assertThat(result).isEqualTo(1024L);
    verify(delegate).getContentLength();
  }

  @Test
  void getBody_ShouldDelegateToDelegate() throws IOException {
    RequestContext delegate = mock(RequestContext.class);
    InputStream body = mock(InputStream.class);
    when(delegate.getBody()).thenReturn(body);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    InputStream result = wrapper.getBody();

    assertThat(result).isSameAs(body);
    verify(delegate).getBody();
  }

  @Test
  void getHeaders_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(delegate.getHeaders()).thenReturn(headers);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    HttpHeaders result = wrapper.getHeaders();

    assertThat(result).isSameAs(headers);
    verify(delegate).getHeaders();
  }

  @Test
  void getInputStream_ShouldDelegateToDelegate() throws IOException {
    RequestContext delegate = mock(RequestContext.class);
    InputStream inputStream = mock(InputStream.class);
    when(delegate.getInputStream()).thenReturn(inputStream);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    InputStream result = wrapper.getInputStream();

    assertThat(result).isSameAs(inputStream);
    verify(delegate).getInputStream();
  }

  @Test
  void getReader_ShouldDelegateToDelegate() throws IOException {
    RequestContext delegate = mock(RequestContext.class);
    java.io.BufferedReader reader = mock(java.io.BufferedReader.class);
    when(delegate.getReader()).thenReturn(reader);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    java.io.BufferedReader result = wrapper.getReader();

    assertThat(result).isSameAs(reader);
    verify(delegate).getReader();
  }

  @Test
  void isMultipart_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.isMultipart()).thenReturn(true);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    boolean result = wrapper.isMultipart();

    assertThat(result).isTrue();
    verify(delegate).isMultipart();
  }

  @Test
  void getContentType_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.getContentType()).thenReturn("application/json");

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    String result = wrapper.getContentType();

    assertThat(result).isEqualTo("application/json");
    verify(delegate).getContentType();
  }

  @Test
  void requestHeaders_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(delegate.requestHeaders()).thenReturn(headers);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    HttpHeaders result = wrapper.requestHeaders();

    assertThat(result).isSameAs(headers);
    verify(delegate).requestHeaders();
  }

  @Test
  void createRequestHeaders_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(delegate.createRequestHeaders()).thenReturn(headers);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    HttpHeaders result = wrapper.createRequestHeaders();

    assertThat(result).isSameAs(headers);
    verify(delegate).createRequestHeaders();
  }

  @Test
  void getLocale_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    Locale locale = Locale.ENGLISH;
    when(delegate.getLocale()).thenReturn(locale);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    Locale result = wrapper.getLocale();

    assertThat(result).isSameAs(locale);
    verify(delegate).getLocale();
  }

  @Test
  void checkNotModifiedWithTimestamp_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.checkNotModified(12345L)).thenReturn(true);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    boolean result = wrapper.checkNotModified(12345L);

    assertThat(result).isTrue();
    verify(delegate).checkNotModified(12345L);
  }

  @Test
  void checkNotModifiedWithETag_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.checkNotModified("etag-value")).thenReturn(true);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    boolean result = wrapper.checkNotModified("etag-value");

    assertThat(result).isTrue();
    verify(delegate).checkNotModified("etag-value");
  }

  @Test
  void checkNotModifiedWithETagAndTimestamp_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.checkNotModified("etag-value", 12345L)).thenReturn(true);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    boolean result = wrapper.checkNotModified("etag-value", 12345L);

    assertThat(result).isTrue();
    verify(delegate).checkNotModified("etag-value", 12345L);
  }

  @Test
  void isNotModified_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.isNotModified()).thenReturn(true);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    boolean result = wrapper.isNotModified();

    assertThat(result).isTrue();
    verify(delegate).isNotModified();
  }

  @Test
  void setContentLength_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.setContentLength(1024L);

    verify(delegate).setContentLength(1024L);
  }

  @Test
  void isCommitted_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.isCommitted()).thenReturn(true);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    boolean result = wrapper.isCommitted();

    assertThat(result).isTrue();
    verify(delegate).isCommitted();
  }

  @Test
  void reset_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.reset();

    verify(delegate).reset();
  }

  @Test
  void sendRedirect_ShouldDelegateToDelegate() throws IOException {
    RequestContext delegate = mock(RequestContext.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.sendRedirect("http://example.com");

    verify(delegate).sendRedirect("http://example.com");
  }

  @Test
  void setStatusWithInt_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.setStatus(200);

    verify(delegate).setStatus(200);
  }

  @Test
  void setStatusWithHttpStatusCode_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.setStatus(HttpStatusCode.valueOf(200));

    verify(delegate).setStatus(HttpStatusCode.valueOf(200));
  }

  @Test
  void getStatus_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.getStatus()).thenReturn(200);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    int result = wrapper.getStatus();

    assertThat(result).isEqualTo(200);
    verify(delegate).getStatus();
  }

  @Test
  void sendErrorWithHttpStatusCode_ShouldDelegateToDelegate() throws IOException {
    RequestContext delegate = mock(RequestContext.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.sendError(HttpStatus.NOT_FOUND);

    verify(delegate).sendError(HttpStatus.NOT_FOUND);
  }

  @Test
  void sendErrorWithHttpStatusCodeAndMessage_ShouldDelegateToDelegate() throws IOException {
    RequestContext delegate = mock(RequestContext.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.sendError(HttpStatus.NOT_FOUND, "Not found");

    verify(delegate).sendError(HttpStatus.NOT_FOUND, "Not found");
  }

  @Test
  void sendErrorWithInt_ShouldDelegateToDelegate() throws IOException {
    RequestContext delegate = mock(RequestContext.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.sendError(404);

    verify(delegate).sendError(404);
  }

  @Test
  void sendErrorWithIntAndMessage_ShouldDelegateToDelegate() throws IOException {
    RequestContext delegate = mock(RequestContext.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.sendError(404, "Not found");

    verify(delegate).sendError(404, "Not found");
  }

  @Test
  void getOutputStream_ShouldDelegateToDelegate() throws IOException {
    RequestContext delegate = mock(RequestContext.class);
    java.io.OutputStream outputStream = mock(java.io.OutputStream.class);
    when(delegate.getOutputStream()).thenReturn(outputStream);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    java.io.OutputStream result = wrapper.getOutputStream();

    assertThat(result).isSameAs(outputStream);
    verify(delegate).getOutputStream();
  }

  @Test
  void getWriter_ShouldDelegateToDelegate() throws IOException {
    RequestContext delegate = mock(RequestContext.class);
    java.io.PrintWriter writer = mock(java.io.PrintWriter.class);
    when(delegate.getWriter()).thenReturn(writer);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    java.io.PrintWriter result = wrapper.getWriter();

    assertThat(result).isSameAs(writer);
    verify(delegate).getWriter();
  }

  @Test
  void setContentTypeWithString_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.setContentType("text/html");

    verify(delegate).setContentType("text/html");
  }

  @Test
  void setContentTypeWithMediaType_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.setContentType(MediaType.TEXT_HTML);

    verify(delegate).setContentType(MediaType.TEXT_HTML);
  }

  @Test
  void getResponseContentType_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.getResponseContentType()).thenReturn("text/html");

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    String result = wrapper.getResponseContentType();

    assertThat(result).isEqualTo("text/html");
    verify(delegate).getResponseContentType();
  }

  @Test
  void setHeader_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.setHeader("Content-Type", "application/json");

    verify(delegate).setHeader("Content-Type", "application/json");
  }

  @Test
  void addHeader_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.addHeader("Cache-Control", "no-cache");

    verify(delegate).addHeader("Cache-Control", "no-cache");
  }

  @Test
  void removeHeader_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.removeHeader("Cache-Control");

    verify(delegate).removeHeader("Cache-Control");
  }

  @Test
  void containsResponseHeader_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.containsResponseHeader("Content-Type")).thenReturn(true);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    boolean result = wrapper.containsResponseHeader("Content-Type");

    assertThat(result).isTrue();
    verify(delegate).containsResponseHeader("Content-Type");
  }

  @Test
  void responseHeaders_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(delegate.responseHeaders()).thenReturn(headers);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    HttpHeaders result = wrapper.responseHeaders();

    assertThat(result).isSameAs(headers);
    verify(delegate).responseHeaders();
  }

  @Test
  void addHeaders_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    HttpHeaders headers = mock(HttpHeaders.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.addHeaders(headers);

    verify(delegate).addHeaders(headers);
  }

  @Test
  void createResponseHeaders_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(delegate.createResponseHeaders()).thenReturn(headers);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    HttpHeaders result = wrapper.createResponseHeaders();

    assertThat(result).isSameAs(headers);
    verify(delegate).createResponseHeaders();
  }

  @Test
  void asHttpOutputMessage_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    ServerHttpResponse response = mock(ServerHttpResponse.class);
    when(delegate.asHttpOutputMessage()).thenReturn(response);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    ServerHttpResponse result = wrapper.asHttpOutputMessage();

    assertThat(result).isSameAs(response);
    verify(delegate).asHttpOutputMessage();
  }

  @Test
  void nativeRequest_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    Object nativeRequest = new Object();
    when(delegate.nativeRequest()).thenReturn(nativeRequest);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    Object result = wrapper.nativeRequest();

    assertThat(result).isSameAs(nativeRequest);
    verify(delegate).nativeRequest();
  }

  @Test
  void getMatchingMetadata_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    HandlerMatchingMetadata metadata = mock(HandlerMatchingMetadata.class);
    when(delegate.getMatchingMetadata()).thenReturn(metadata);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    HandlerMatchingMetadata result = wrapper.getMatchingMetadata();

    assertThat(result).isSameAs(metadata);
    verify(delegate).getMatchingMetadata();
  }

  @Test
  void setMatchingMetadata_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    HandlerMatchingMetadata metadata = mock(HandlerMatchingMetadata.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.setMatchingMetadata(metadata);

    verify(delegate).setMatchingMetadata(metadata);
  }

  @Test
  void hasMatchingMetadata_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.hasMatchingMetadata()).thenReturn(true);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    boolean result = wrapper.hasMatchingMetadata();

    assertThat(result).isTrue();
    verify(delegate).hasMatchingMetadata();
  }

  @Test
  void getAttribute_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.getAttribute("key")).thenReturn("value");

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    Object result = wrapper.getAttribute("key");

    assertThat(result).isEqualTo("value");
    verify(delegate).getAttribute("key");
  }

  @Test
  void setAttribute_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.setAttribute("key", "value");

    verify(delegate).setAttribute("key", "value");
  }

  @Test
  void removeAttribute_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.removeAttribute("key")).thenReturn("value");

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    Object result = wrapper.removeAttribute("key");

    assertThat(result).isEqualTo("value");
    verify(delegate).removeAttribute("key");
  }

  @Test
  void clearAttributes_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.clearAttributes();

    verify(delegate).clearAttributes();
  }

  @Test
  void getAttributeNames_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    String[] attributeNames = { "attr1", "attr2" };
    when(delegate.getAttributeNames()).thenReturn(attributeNames);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    String[] result = wrapper.getAttributeNames();

    assertThat(result).isSameAs(attributeNames);
    verify(delegate).getAttributeNames();
  }

  @Test
  void flush_ShouldDelegateToDelegate() throws IOException {
    RequestContext delegate = mock(RequestContext.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.flush();

    verify(delegate).flush();
  }

  @Test
  void requestCompleted_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.requestCompleted();

    verify(delegate).requestCompleted();
  }

  @Test
  void requestCompletedWithThrowable_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    Throwable throwable = new RuntimeException("test");

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.requestCompleted(throwable);

    verify(delegate).requestCompleted(throwable);
  }

  @Test
  void asyncWebRequest_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    AsyncWebRequest asyncWebRequest = mock(AsyncWebRequest.class);
    when(delegate.asyncWebRequest()).thenReturn(asyncWebRequest);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    AsyncWebRequest result = wrapper.asyncWebRequest();

    assertThat(result).isSameAs(asyncWebRequest);
    verify(delegate).asyncWebRequest();
  }

  @Test
  void isConcurrentHandlingStarted_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.isConcurrentHandlingStarted()).thenReturn(true);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    boolean result = wrapper.isConcurrentHandlingStarted();

    assertThat(result).isTrue();
    verify(delegate).isConcurrentHandlingStarted();
  }

  @Test
  void multipartRequest_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    MultipartRequest multipartRequest = mock(MultipartRequest.class);
    when(delegate.asMultipartRequest()).thenReturn(multipartRequest);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    MultipartRequest result = wrapper.asMultipartRequest();

    assertThat(result).isSameAs(multipartRequest);
    verify(delegate).asMultipartRequest();
  }

  @Test
  void setBinding_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    BindingContext bindingContext = mock(BindingContext.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.setBinding(bindingContext);

    verify(delegate).setBinding(bindingContext);
  }

  @Test
  void getBinding_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    BindingContext bindingContext = mock(BindingContext.class);
    when(delegate.getBinding()).thenReturn(bindingContext);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    BindingContext result = wrapper.getBinding();

    assertThat(result).isSameAs(bindingContext);
    verify(delegate).getBinding();
  }

  @Test
  void binding_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    BindingContext bindingContext = mock(BindingContext.class);
    when(delegate.binding()).thenReturn(bindingContext);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    BindingContext result = wrapper.binding();

    assertThat(result).isSameAs(bindingContext);
    verify(delegate).binding();
  }

  @Test
  void hasBinding_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.hasBinding()).thenReturn(true);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    boolean result = wrapper.hasBinding();

    assertThat(result).isTrue();
    verify(delegate).hasBinding();
  }

  @Test
  void getInputRedirectModel_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    RedirectModel redirectModel = mock(RedirectModel.class);
    when(delegate.getInputRedirectModel()).thenReturn(redirectModel);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    RedirectModel result = wrapper.getInputRedirectModel();

    assertThat((Object) result).isSameAs(redirectModel);
    verify(delegate).getInputRedirectModel();
  }

  @Test
  void getInputRedirectModelWithManager_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    RedirectModel redirectModel = mock(RedirectModel.class);
    RedirectModelManager manager = mock(RedirectModelManager.class);
    when(delegate.getInputRedirectModel(manager)).thenReturn(redirectModel);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    RedirectModel result = wrapper.getInputRedirectModel(manager);

    assertThat((Object) result).isSameAs(redirectModel);
    verify(delegate).getInputRedirectModel(manager);
  }

  @Test
  void hasAttribute_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.hasAttribute("key")).thenReturn(true);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    boolean result = wrapper.hasAttribute("key");

    assertThat(result).isTrue();
    verify(delegate).hasAttribute("key");
  }

  @Test
  void attributeNames_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    Iterable<String> names = List.of("attr1", "attr2");
    when(delegate.attributeNames()).thenReturn(names);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    Iterable<String> result = wrapper.attributeNames();

    assertThat(result).isSameAs(names);
    verify(delegate).attributeNames();
  }

  @Test
  void copyFrom_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    AttributeAccessor source = mock(AttributeAccessor.class);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    wrapper.copyFrom(source);

    verify(delegate).copyFrom(source);
  }

  @Test
  void hasAttributes_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.hasAttributes()).thenReturn(true);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    boolean result = wrapper.hasAttributes();

    assertThat(result).isTrue();
    verify(delegate).hasAttributes();
  }

  @Test
  void getAttributes_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    java.util.Map<String, Object> attributes = new java.util.HashMap<>();
    attributes.put("key", "value");
    when(delegate.getAttributes()).thenReturn(attributes);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    java.util.Map<String, Object> result = wrapper.getAttributes();

    assertThat(result).isSameAs(attributes);
    verify(delegate).getAttributes();
  }

  @Test
  void getMethod_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.getMethod()).thenReturn(HttpMethod.GET);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    HttpMethod result = wrapper.getMethod();

    assertThat(result).isSameAs(HttpMethod.GET);
    verify(delegate).getMethod();
  }

  @Test
  void getMethodAsString_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    when(delegate.getMethodAsString()).thenReturn("GET");

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    String result = wrapper.getMethodAsString();

    assertThat(result).isEqualTo("GET");
    verify(delegate).getMethodAsString();
  }

  @Test
  void matchingMetadata_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    HandlerMatchingMetadata metadata = mock(HandlerMatchingMetadata.class);
    when(delegate.matchingMetadata()).thenReturn(metadata);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    HandlerMatchingMetadata result = wrapper.matchingMetadata();

    assertThat(result).isSameAs(metadata);
    verify(delegate).matchingMetadata();
  }

  @Test
  void asyncManager_ShouldDelegateToDelegate() {
    RequestContext delegate = mock(RequestContext.class);
    WebAsyncManager asyncManager = mock(WebAsyncManager.class);
    when(delegate.asyncManager()).thenReturn(asyncManager);

    DecoratingRequestContext wrapper = new DecoratingRequestContext() {
      @Override
      public RequestContext getDelegate() {
        return delegate;
      }
    };

    WebAsyncManager result = wrapper.asyncManager();

    assertThat(result).isSameAs(asyncManager);
    verify(delegate).asyncManager();
  }

}