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
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 14:46
 */
class RequestContextDecoratorTests {

  @Test
  void constructorWithNullDelegateShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new RequestContextDecorator(null))
            .withMessageContaining("RequestContext delegate is required");
  }

  @Test
  void getDelegateShouldReturnWrappedInstance() {
    RequestContext mockRequest = mock(RequestContext.class);
    RequestContextDecorator decorator = new RequestContextDecorator(mockRequest);

    assertThat(decorator.delegate()).isSameAs(mockRequest);
  }

  @Test
  void equalsWithSameDelegateShouldReturnTrue() {
    RequestContext mockRequest = mock(RequestContext.class);
    RequestContextDecorator decorator1 = new RequestContextDecorator(mockRequest);
    RequestContextDecorator decorator2 = new RequestContextDecorator(mockRequest);

    assertThat(decorator1).isEqualTo(decorator2);
  }

  @Test
  void equalsWithDifferentDelegateShouldReturnFalse() {
    RequestContext mockRequest1 = mock(RequestContext.class);
    RequestContext mockRequest2 = mock(RequestContext.class);
    RequestContextDecorator decorator1 = new RequestContextDecorator(mockRequest1);
    RequestContextDecorator decorator2 = new RequestContextDecorator(mockRequest2);

    assertThat(decorator1).isNotEqualTo(decorator2);
  }

  @Test
  void equalsWithNullShouldReturnFalse() {
    RequestContext mockRequest = mock(RequestContext.class);
    RequestContextDecorator decorator = new RequestContextDecorator(mockRequest);

    assertThat(decorator).isNotEqualTo(null);
  }

  @Test
  void equalsWithDifferentClassShouldReturnFalse() {
    RequestContext mockRequest = mock(RequestContext.class);
    RequestContextDecorator decorator = new RequestContextDecorator(mockRequest);

    assertThat(decorator).isNotEqualTo(new Object());
  }

  @Test
  void hashCodeShouldBeBasedOnDelegate() {
    RequestContext mockRequest = mock(RequestContext.class);
    RequestContextDecorator decorator = new RequestContextDecorator(mockRequest);

    assertThat(decorator.hashCode()).isNotEqualTo(Objects.hash(mockRequest));
  }

  @Test
  void toStringShouldContainDelegateInfo() {
    RequestContext mockRequest = mock(RequestContext.class);
    when(mockRequest.toString()).thenReturn("MockRequest");
    RequestContextDecorator decorator = new RequestContextDecorator(mockRequest);

    assertThat(decorator.toString()).contains("MockRequest");
  }

  @Test
  void allMethodsShouldDelegateToWrappedInstance() throws IOException {
    RequestContext mockRequest = mock(RequestContext.class);
    RequestContextDecorator decorator = new RequestContextDecorator(mockRequest);

    // Test a few key methods to ensure delegation works
    decorator.getRequestTimeMillis();
    verify(mockRequest).getRequestTimeMillis();

    decorator.getMethod();
    verify(mockRequest).getMethod();

    decorator.getRequestURI();
    verify(mockRequest).getRequestURI();

    decorator.getCookies();
    verify(mockRequest).getCookies();

    decorator.getParameters();
    verify(mockRequest).getParameters();

    decorator.getInputStream();
    verify(mockRequest).getInputStream();

    decorator.isMultipart();
    verify(mockRequest).isMultipart();

    decorator.asyncWebRequest();
    verify(mockRequest).asyncWebRequest();

    decorator.setContentType("text/html");
    verify(mockRequest).setContentType("text/html");

    decorator.setStatus(200);
    verify(mockRequest).setStatus(200);

    decorator.flush();
    verify(mockRequest).flush();
  }

  @Test
  void getDelegateReturnsCorrectInstance() {
    RequestContext mockRequest = mock(RequestContext.class);
    RequestContextDecorator decorator = new RequestContextDecorator(mockRequest);

    assertThat(decorator.delegate()).isSameAs(mockRequest);
  }

  @Test
  void equalsWithSameDelegateReturnsTrue() {
    RequestContext mockRequest = mock(RequestContext.class);
    RequestContextDecorator decorator1 = new RequestContextDecorator(mockRequest);
    RequestContextDecorator decorator2 = new RequestContextDecorator(mockRequest);

    assertThat(decorator1).isEqualTo(decorator2);
    assertThat(decorator1.equals(decorator1)).isTrue();
  }

  @Test
  void equalsWithDifferentDelegatesReturnsFalse() {
    RequestContext mockRequest1 = mock(RequestContext.class);
    RequestContext mockRequest2 = mock(RequestContext.class);
    RequestContextDecorator decorator1 = new RequestContextDecorator(mockRequest1);
    RequestContextDecorator decorator2 = new RequestContextDecorator(mockRequest2);

    assertThat(decorator1).isNotEqualTo(decorator2);
  }

  @Test
  void equalsWithNullReturnsFalse() {
    RequestContext mockRequest = mock(RequestContext.class);
    RequestContextDecorator decorator = new RequestContextDecorator(mockRequest);

    assertThat(decorator).isNotEqualTo(null);
  }

  @Test
  void equalsWithDifferentTypeReturnsFalse() {
    RequestContext mockRequest = mock(RequestContext.class);
    RequestContextDecorator decorator = new RequestContextDecorator(mockRequest);

    assertThat(decorator).isNotEqualTo(new Object());
  }

  @Test
  void hashCodeReturnsConsistentValue() {
    RequestContext mockRequest = mock(RequestContext.class);
    RequestContextDecorator decorator = new RequestContextDecorator(mockRequest);

    int hashCode1 = decorator.hashCode();
    int hashCode2 = decorator.hashCode();

    assertThat(hashCode1).isEqualTo(hashCode2);
  }

  @Test
  void toStringContainsDelegateInfo() {
    RequestContext mockRequest = mock(RequestContext.class);
    when(mockRequest.toString()).thenReturn("MockRequest");
    RequestContextDecorator decorator = new RequestContextDecorator(mockRequest);

    assertThat(decorator.toString()).contains("MockRequest");
  }

  @Test
  void allMethodsDelegateToWrappedInstance() throws IOException {
    RequestContext mockRequest = mock(RequestContext.class);
    RequestContextDecorator decorator = new RequestContextDecorator(mockRequest);

    // Test various methods delegate correctly
    decorator.getRequestTimeMillis();
    verify(mockRequest).getRequestTimeMillis();

    decorator.getMethod();
    verify(mockRequest).getMethod();

    decorator.getRequestURI();
    verify(mockRequest).getRequestURI();

    decorator.getCookies();
    verify(mockRequest).getCookies();

    decorator.getParameters();
    verify(mockRequest).getParameters();

    decorator.getInputStream();
    verify(mockRequest).getInputStream();

    decorator.isMultipart();
    verify(mockRequest).isMultipart();

    decorator.asyncWebRequest();
    verify(mockRequest).asyncWebRequest();

    decorator.setContentType("text/html");
    verify(mockRequest).setContentType("text/html");

    decorator.setStatus(200);
    verify(mockRequest).setStatus(200);

    decorator.flush();
    verify(mockRequest).flush();

    decorator.getRequestURL();
    verify(mockRequest).getRequestURL();

    decorator.getQueryString();
    verify(mockRequest).getQueryString();

    decorator.getCookie("test");
    verify(mockRequest).getCookie("test");

    decorator.addCookie("name", "value");
    verify(mockRequest).addCookie("name", "value");

    decorator.removeCookie("name");
    verify(mockRequest).removeCookie("name");

    decorator.hasResponseCookie();
    verify(mockRequest).hasResponseCookie();

    decorator.responseCookies();
    verify(mockRequest).responseCookies();

    decorator.getParameterNames();
    verify(mockRequest).getParameterNames();

    decorator.getParameter("name");
    verify(mockRequest).getParameter("name");

    decorator.getParameters("name");
    verify(mockRequest).getParameters("name");

    decorator.getScheme();
    verify(mockRequest).getScheme();

    decorator.getServerName();
    verify(mockRequest).getServerName();

    decorator.getServerPort();
    verify(mockRequest).getServerPort();

    decorator.getRemoteAddress();
    verify(mockRequest).getRemoteAddress();

    decorator.getRemotePort();
    verify(mockRequest).getRemotePort();

    decorator.localAddress();
    verify(mockRequest).localAddress();

    decorator.remoteAddress();
    verify(mockRequest).remoteAddress();

    decorator.getContentLength();
    verify(mockRequest).getContentLength();

    decorator.getHeaders();
    verify(mockRequest).getHeaders();

    decorator.getReader();
    verify(mockRequest).getReader();

    decorator.getWriter();
    verify(mockRequest).getWriter();

    decorator.getOutputStream();
    verify(mockRequest).getOutputStream();

    decorator.isCommitted();
    verify(mockRequest).isCommitted();

    decorator.getStatus();
    verify(mockRequest).getStatus();

    decorator.getResponseContentType();
    verify(mockRequest).getResponseContentType();

    decorator.containsResponseHeader("name");
    verify(mockRequest).containsResponseHeader("name");

    decorator.responseHeaders();
    verify(mockRequest).responseHeaders();

    decorator.hasBinding();
    verify(mockRequest).hasBinding();

    decorator.getBinding();
    verify(mockRequest).getBinding();

    decorator.hasMatchingMetadata();
    verify(mockRequest).hasMatchingMetadata();

    decorator.getMatchingMetadata();
    verify(mockRequest).getMatchingMetadata();

    decorator.isPreFlightRequest();
    verify(mockRequest).isPreFlightRequest();

    decorator.isCorsRequest();
    verify(mockRequest).isCorsRequest();

    decorator.getRequestPath();
    verify(mockRequest).getRequestPath();

    decorator.getLocale();
    verify(mockRequest).getLocale();

    decorator.isNotModified();
    verify(mockRequest).isNotModified();

    decorator.isConcurrentHandlingStarted();
    verify(mockRequest).isConcurrentHandlingStarted();

    decorator.asMultipartRequest();
    verify(mockRequest).asMultipartRequest();

    decorator.asyncManager();
    verify(mockRequest).asyncManager();

    decorator.getAttribute("name");
    verify(mockRequest).getAttribute("name");

    decorator.getAttributeNames();
    verify(mockRequest).getAttributeNames();

    decorator.hasAttributes();
    verify(mockRequest).hasAttributes();

    decorator.attributeNames();
    verify(mockRequest).attributeNames();

    decorator.getApplicationContext();
    verify(mockRequest).getApplicationContext();
  }

}