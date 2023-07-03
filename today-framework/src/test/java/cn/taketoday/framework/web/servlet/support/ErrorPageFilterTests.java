/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.web.servlet.support;

import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.core.testfixture.DisabledIfInContinuousIntegration;
import cn.taketoday.framework.test.system.CapturedOutput;
import cn.taketoday.framework.test.system.OutputCaptureExtension;
import cn.taketoday.framework.web.server.ErrorPage;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.mock.web.MockFilterChain;
import cn.taketoday.mock.web.MockFilterConfig;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.mock.web.MockRequestDispatcher;
import cn.taketoday.web.bind.MissingRequestParameterException;
import cn.taketoday.web.context.async.DeferredResult;
import cn.taketoday.web.context.async.StandardServletAsyncWebRequest;
import cn.taketoday.web.context.async.WebAsyncManager;
import cn.taketoday.web.servlet.ServletRequestContext;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link ErrorPageFilter}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
@ExtendWith(OutputCaptureExtension.class)
class ErrorPageFilterTests {

  private ErrorPageFilter filter = new ErrorPageFilter();

  private DispatchRecordingMockHttpServletRequest request = new DispatchRecordingMockHttpServletRequest();

  private MockHttpServletResponse response = new MockHttpServletResponse();

  private MockFilterChain chain = new TestFilterChain((request, response, chain) -> {
  });

  @Test
  void notAnError() throws Exception {
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(this.chain.getRequest()).isEqualTo(this.request);
    assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getResponse()).isEqualTo(this.response);
    assertThat(this.response.isCommitted()).isTrue();
    assertThat(this.response.getForwardedUrl()).isNull();
  }

  @Test
  void notAnErrorButNotOK() throws Exception {
    this.chain = new TestFilterChain((request, response, chain) -> {
      response.setStatus(201);
      chain.call();
      response.flushBuffer();
    });
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(((HttpServletResponse) this.chain.getResponse()).getStatus()).isEqualTo(201);
    assertThat(((HttpServletResponse) ((HttpServletResponseWrapper) this.chain.getResponse()).getResponse())
            .getStatus()).isEqualTo(201);
    assertThat(this.response.isCommitted()).isTrue();
  }

  @Test
  void unauthorizedWithErrorPath() throws Exception {
    this.filter.addErrorPages(new ErrorPage("/error"));
    this.chain = new TestFilterChain((request, response, chain) -> response.sendError(401, "UNAUTHORIZED"));
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(this.chain.getRequest()).isEqualTo(this.request);
    HttpServletResponseWrapper wrapper = (HttpServletResponseWrapper) this.chain.getResponse();
    assertThat(wrapper.getResponse()).isEqualTo(this.response);
    assertThat(this.response.isCommitted()).isTrue();
    assertThat(wrapper.getStatus()).isEqualTo(401);
    // The real response has to be 401 as well...
    assertThat(this.response.getStatus()).isEqualTo(401);
    assertThat(this.response.getForwardedUrl()).isEqualTo("/error");
  }

  @Test
  void responseCommitted() throws Exception {
    this.filter.addErrorPages(new ErrorPage("/error"));
    this.response.setCommitted(true);
    this.chain = new TestFilterChain((request, response, chain) -> response.sendError(400, "BAD"));
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(this.chain.getRequest()).isEqualTo(this.request);
    assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getResponse()).isEqualTo(this.response);
    assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getStatus()).isEqualTo(400);
    assertThat(this.response.getForwardedUrl()).isNull();
    assertThat(this.response.isCommitted()).isTrue();
  }

  @Test
  void responseCommittedWhenFromClientAbortException(CapturedOutput output) throws Exception {
    this.filter.addErrorPages(new ErrorPage("/error"));
    this.response.setCommitted(true);
    this.chain = new TestFilterChain((request, response, chain) -> {
      chain.call();
      throw new ClientAbortException();
    });
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(this.response.isCommitted()).isTrue();
    assertThat(output).doesNotContain("Cannot forward");
  }

  @Test
  void responseUncommittedWithoutErrorPage() throws Exception {
    this.chain = new TestFilterChain((request, response, chain) -> response.sendError(400, "BAD"));
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(this.chain.getRequest()).isEqualTo(this.request);
    assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getResponse()).isEqualTo(this.response);
    assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getStatus()).isEqualTo(400);
    assertThat(this.response.getForwardedUrl()).isNull();
    assertThat(this.response.isCommitted()).isTrue();
  }

  @Test
  void oncePerRequest() throws Exception {
    this.chain = new TestFilterChain((request, response, chain) -> {
      response.sendError(400, "BAD");
      assertThat(request.getAttribute("FILTER.FILTERED")).isNotNull();
    });
    this.filter.init(new MockFilterConfig("FILTER"));
    this.filter.doFilter(this.request, this.response, this.chain);
  }

  @Test
  void globalError() throws Exception {
    this.filter.addErrorPages(new ErrorPage("/error"));
    this.chain = new TestFilterChain((request, response, chain) -> response.sendError(400, "BAD"));
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getStatus()).isEqualTo(400);
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).isEqualTo(400);
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).isEqualTo("BAD");
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI)).isEqualTo("/test/path");
    assertThat(this.response.isCommitted()).isTrue();
    assertThat(this.response.getForwardedUrl()).isEqualTo("/error");
  }

  @Test
  void statusError() throws Exception {
    this.filter.addErrorPages(new ErrorPage(HttpStatus.BAD_REQUEST, "/400"));
    this.chain = new TestFilterChain((request, response, chain) -> response.sendError(400, "BAD"));
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getStatus()).isEqualTo(400);
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).isEqualTo(400);
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).isEqualTo("BAD");
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI)).isEqualTo("/test/path");
    assertThat(this.response.isCommitted()).isTrue();
    assertThat(this.response.getForwardedUrl()).isEqualTo("/400");
  }

  @Test
  void statusErrorWithCommittedResponse() throws Exception {
    this.filter.addErrorPages(new ErrorPage(HttpStatus.BAD_REQUEST, "/400"));
    this.chain = new TestFilterChain((request, response, chain) -> {
      response.sendError(400, "BAD");
      response.flushBuffer();
    });
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getStatus()).isEqualTo(400);
    assertThat(this.response.isCommitted()).isTrue();
    assertThat(this.response.getForwardedUrl()).isNull();
  }

  @Test
  void exceptionError() throws Exception {
    this.filter.addErrorPages(new ErrorPage(RuntimeException.class, "/500"));
    this.chain = new TestFilterChain((request, response, chain) -> {
      chain.call();
      throw new RuntimeException("BAD");
    });
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getStatus()).isEqualTo(500);
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).isEqualTo(500);
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).isEqualTo("BAD");
    Map<String, Object> requestAttributes = getAttributesForDispatch("/500");
    assertThat(requestAttributes.get(RequestDispatcher.ERROR_EXCEPTION_TYPE)).isEqualTo(RuntimeException.class);
    assertThat(requestAttributes.get(RequestDispatcher.ERROR_EXCEPTION)).isInstanceOf(RuntimeException.class);
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE)).isNull();
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).isNull();
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI)).isEqualTo("/test/path");
    assertThat(this.response.isCommitted()).isTrue();
    assertThat(this.response.getForwardedUrl()).isEqualTo("/500");
  }

  @Test
  void exceptionErrorWithCommittedResponse() throws Exception {
    this.filter.addErrorPages(new ErrorPage(RuntimeException.class, "/500"));
    this.chain = new TestFilterChain((request, response, chain) -> {
      chain.call();
      response.flushBuffer();
      throw new RuntimeException("BAD");
    });
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(this.response.getForwardedUrl()).isNull();
  }

  @Test
  void statusCode() throws Exception {
    this.chain = new TestFilterChain((request, response, chain) -> assertThat(response.getStatus()).isEqualTo(200));
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getStatus()).isEqualTo(200);
  }

  @Test
  void subClassExceptionError() throws Exception {
    this.filter.addErrorPages(new ErrorPage(RuntimeException.class, "/500"));
    this.chain = new TestFilterChain((request, response, chain) -> {
      chain.call();
      throw new IllegalStateException("BAD");
    });
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getStatus()).isEqualTo(500);
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).isEqualTo(500);
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).isEqualTo("BAD");
    Map<String, Object> requestAttributes = getAttributesForDispatch("/500");
    assertThat(requestAttributes.get(RequestDispatcher.ERROR_EXCEPTION_TYPE))
            .isEqualTo(IllegalStateException.class);
    assertThat(requestAttributes.get(RequestDispatcher.ERROR_EXCEPTION)).isInstanceOf(IllegalStateException.class);
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE)).isNull();
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).isNull();
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI)).isEqualTo("/test/path");
    assertThat(this.response.isCommitted()).isTrue();
  }

  @Test
  void responseIsNotCommittedWhenRequestIsAsync() throws Exception {
    this.request.setAsyncStarted(true);
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(this.chain.getRequest()).isEqualTo(this.request);
    assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getResponse()).isEqualTo(this.response);
    assertThat(this.response.isCommitted()).isFalse();
  }

  @Test
  void responseIsCommittedWhenRequestIsAsyncAndExceptionIsThrown() throws Exception {
    this.filter.addErrorPages(new ErrorPage("/error"));
    this.request.setAsyncStarted(true);
    this.chain = new TestFilterChain((request, response, chain) -> {
      chain.call();
      throw new RuntimeException("BAD");
    });
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(this.chain.getRequest()).isEqualTo(this.request);
    assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getResponse()).isEqualTo(this.response);
    assertThat(this.response.isCommitted()).isTrue();
  }

  @Test
  void responseIsCommittedWhenRequestIsAsyncAndStatusIs400Plus() throws Exception {
    this.filter.addErrorPages(new ErrorPage("/error"));
    this.request.setAsyncStarted(true);
    this.chain = new TestFilterChain((request, response, chain) -> {
      chain.call();
      response.sendError(400, "BAD");
    });
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(this.chain.getRequest()).isEqualTo(this.request);
    assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getResponse()).isEqualTo(this.response);
    assertThat(this.response.isCommitted()).isTrue();
  }

  @Test
  void responseIsNotCommittedDuringAsyncDispatch() throws Exception {
    setUpAsyncDispatch();
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(this.chain.getRequest()).isEqualTo(this.request);
    assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getResponse()).isEqualTo(this.response);
    assertThat(this.response.isCommitted()).isFalse();
  }

  @Test
  void responseIsCommittedWhenExceptionIsThrownDuringAsyncDispatch() throws Exception {
    this.filter.addErrorPages(new ErrorPage("/error"));
    setUpAsyncDispatch();
    this.chain = new TestFilterChain((request, response, chain) -> {
      chain.call();
      throw new RuntimeException("BAD");
    });
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(this.chain.getRequest()).isEqualTo(this.request);
    assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getResponse()).isEqualTo(this.response);
    assertThat(this.response.isCommitted()).isTrue();
  }

  @Test
  void responseIsCommittedWhenStatusIs400PlusDuringAsyncDispatch() throws Exception {
    this.filter.addErrorPages(new ErrorPage("/error"));
    setUpAsyncDispatch();
    this.chain = new TestFilterChain((request, response, chain) -> {
      chain.call();
      response.sendError(400, "BAD");
    });
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(this.chain.getRequest()).isEqualTo(this.request);
    assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getResponse()).isEqualTo(this.response);
    assertThat(this.response.isCommitted()).isTrue();
  }

  @Test
  void responseIsNotFlushedIfStatusIsLessThan400AndItHasAlreadyBeenCommitted() throws Exception {
    HttpServletResponse committedResponse = mock(HttpServletResponse.class);
    given(committedResponse.isCommitted()).willReturn(true);
    given(committedResponse.getStatus()).willReturn(200);
    this.filter.doFilter(this.request, committedResponse, this.chain);
    then(committedResponse).should(never()).flushBuffer();
  }

  @Test
  @DisabledIfInContinuousIntegration
  void errorMessageForRequestWithoutPathInfo(CapturedOutput output) throws IOException, ServletException {
    this.request.setServletPath("/test");
    this.filter.addErrorPages(new ErrorPage("/error"));
    this.chain = new TestFilterChain((request, response, chain) -> {
      chain.call();
      throw new RuntimeException();
    });
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(output).contains("request [/test]");
  }

  @Test
  @DisabledIfInContinuousIntegration
  void errorMessageForRequestWithPathInfo(CapturedOutput output) throws IOException, ServletException {
    this.request.setServletPath("/test");
    this.request.setPathInfo("/alpha");
    this.filter.addErrorPages(new ErrorPage("/error"));
    this.chain = new TestFilterChain((request, response, chain) -> {
      chain.call();
      throw new RuntimeException();
    });
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(output).contains("request [/test/alpha]");
  }

  @Test
  void nestedServletExceptionIsUnwrapped() throws Exception {
    this.filter.addErrorPages(new ErrorPage(RuntimeException.class, "/500"));
    this.chain = new TestFilterChain((request, response, chain) -> {
      chain.call();
      throw new ServletException("Wrapper", new RuntimeException("BAD"));
    });
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getStatus()).isEqualTo(500);
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).isEqualTo(500);
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).isEqualTo("BAD");
    Map<String, Object> requestAttributes = getAttributesForDispatch("/500");
    assertThat(requestAttributes.get(RequestDispatcher.ERROR_EXCEPTION_TYPE)).isEqualTo(RuntimeException.class);
    assertThat(requestAttributes.get(RequestDispatcher.ERROR_EXCEPTION)).isInstanceOf(RuntimeException.class);
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE)).isNull();
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).isNull();
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI)).isEqualTo("/test/path");
    assertThat(this.response.isCommitted()).isTrue();
    assertThat(this.response.getForwardedUrl()).isEqualTo("/500");
  }

  @Test
  void nestedServletExceptionWithNoCause() throws Exception {
    this.filter.addErrorPages(new ErrorPage(MissingRequestParameterException.class, "/500"));
    this.chain = new TestFilterChain((request, response, chain) -> {
      chain.call();
      throw new MissingRequestParameterException("test", "string");
    });
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(((HttpServletResponseWrapper) this.chain.getResponse()).getStatus()).isEqualTo(500);
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).isEqualTo(500);
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_MESSAGE))
            .isEqualTo("Required request parameter 'test' for method parameter type string is not present");
    Map<String, Object> requestAttributes = getAttributesForDispatch("/500");
    assertThat(requestAttributes.get(RequestDispatcher.ERROR_EXCEPTION_TYPE))
            .isEqualTo(MissingRequestParameterException.class);
    assertThat(requestAttributes.get(RequestDispatcher.ERROR_EXCEPTION))
            .isInstanceOf(MissingRequestParameterException.class);
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE)).isNull();
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).isNull();
    assertThat(this.request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI)).isEqualTo("/test/path");
    assertThat(this.response.isCommitted()).isTrue();
    assertThat(this.response.getForwardedUrl()).isEqualTo("/500");
  }

  @Test
  void whenErrorIsSentAndWriterIsFlushedErrorIsSentToTheClient() throws Exception {
    this.chain = new TestFilterChain((request, response, chain) -> {
      response.sendError(400);
      response.getWriter().flush();
    });
    this.filter.doFilter(this.request, this.response, this.chain);
    assertThat(this.response.getStatus()).isEqualTo(400);
  }

  private void setUpAsyncDispatch() throws Exception {
    this.request.setAsyncSupported(true);
    this.request.setAsyncStarted(true);
    DeferredResult<String> result = new DeferredResult<>();
    ServletRequestContext context = new ServletRequestContext(null, request, response);
    WebAsyncManager asyncManager = context.getAsyncManager();
    asyncManager.setAsyncRequest(new StandardServletAsyncWebRequest(request, response));
    asyncManager.startDeferredResultProcessing(result);
  }

  private Map<String, Object> getAttributesForDispatch(String path) {
    return this.request.getDispatcher(path).getRequestAttributes();
  }

  static class TestFilterChain extends MockFilterChain {

    private final FilterHandler handler;

    TestFilterChain(FilterHandler handler) {
      this.handler = handler;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
      AtomicBoolean called = new AtomicBoolean();
      Chain chain = () -> {
        if (called.compareAndSet(false, true)) {
          super.doFilter(request, response);
        }
      };
      this.handler.handle((HttpServletRequest) request, (HttpServletResponse) response, chain);
      chain.call();
    }

  }

  @FunctionalInterface
  interface FilterHandler {

    void handle(HttpServletRequest request, HttpServletResponse response, Chain chain)
            throws IOException, ServletException;

  }

  @FunctionalInterface
  interface Chain {

    void call() throws IOException, ServletException;

  }

  private static final class DispatchRecordingMockHttpServletRequest extends MockHttpServletRequest {

    private final Map<String, AttributeCapturingRequestDispatcher> dispatchers = new HashMap<>();

    private DispatchRecordingMockHttpServletRequest() {
      super("GET", "/test/path");
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
      AttributeCapturingRequestDispatcher dispatcher = new AttributeCapturingRequestDispatcher(path);
      this.dispatchers.put(path, dispatcher);
      return dispatcher;
    }

    private AttributeCapturingRequestDispatcher getDispatcher(String path) {
      return this.dispatchers.get(path);
    }

    private static final class AttributeCapturingRequestDispatcher extends MockRequestDispatcher {

      private final Map<String, Object> requestAttributes = new HashMap<>();

      private AttributeCapturingRequestDispatcher(String resource) {
        super(resource);
      }

      @Override
      public void forward(ServletRequest request, ServletResponse response) {
        captureAttributes(request);
        super.forward(request, response);
      }

      private void captureAttributes(ServletRequest request) {
        Enumeration<String> names = request.getAttributeNames();
        while (names.hasMoreElements()) {
          String name = names.nextElement();
          this.requestAttributes.put(name, request.getAttribute(name));
        }
      }

      private Map<String, Object> getRequestAttributes() {
        return this.requestAttributes;
      }

    }

  }

}
