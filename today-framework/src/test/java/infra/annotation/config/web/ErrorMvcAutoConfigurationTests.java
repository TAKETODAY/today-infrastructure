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

package infra.annotation.config.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.util.Map;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.app.test.system.CapturedOutput;
import infra.app.test.system.OutputCaptureExtension;
import infra.context.annotation.config.AutoConfigurations;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.mock.MockRequestContext;
import infra.web.server.context.AnnotationConfigWebServerApplicationContext;
import infra.web.server.error.ErrorAttributeOptions;
import infra.web.server.error.ErrorAttributeOptions.Include;
import infra.web.server.error.ErrorAttributes;
import infra.web.util.WebUtils;
import infra.web.view.View;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ErrorMvcAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Scott Frederick
 */
@ExtendWith(OutputCaptureExtension.class)
class ErrorMvcAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = ApplicationContextRunner.forProvider(AnnotationConfigWebServerApplicationContext::new)
          .withConfiguration(AutoConfigurations.of(RandomPortWebServerConfig.class,
                  WebMvcAutoConfiguration.class, ErrorMvcAutoConfiguration.class));

  @Test
  void renderContainsViewWithExceptionDetails() {
    this.contextRunner.run((context) -> {
      View errorView = context.getBean("error", View.class);
      ErrorAttributes errorAttributes = context.getBean(ErrorAttributes.class);
      MockRequestContext webRequest = createWebRequest(
              new IllegalStateException("Exception message"), false);
      errorView.render(errorAttributes.getErrorAttributes(webRequest, withAllOptions()), webRequest);
      assertThat(webRequest.getResponse().getContentType()).isEqualTo("text/html");
      String responseString = ((MockHttpResponseImpl) webRequest.getResponse()).getContentAsString();
      assertThat(responseString).contains(
                      "<p>This application has no explicit mapping for /error, so you are seeing this as a fallback.</p>")
              .contains("<div>Exception message</div>")
              .contains("<div style='white-space:pre-wrap;'>java.lang.IllegalStateException");
    });
  }

  @Test
  void renderCanUseJavaTimeTypeAsTimestamp() { // gh-23256
    this.contextRunner.run((context) -> {
      View errorView = context.getBean("error", View.class);
      ErrorAttributes errorAttributes = context.getBean(ErrorAttributes.class);
      MockRequestContext webRequest = createWebRequest(new IllegalStateException("Exception message"),
              false);
      Map<String, Object> attributes = errorAttributes.getErrorAttributes(webRequest, withAllOptions());
      attributes.put("timestamp", Clock.systemUTC().instant());
      errorView.render(attributes, webRequest);
      assertThat(webRequest.getResponse().getContentType()).isEqualTo("text/html");
      String responseString = ((MockHttpResponseImpl) webRequest.getResponse()).getContentAsString();
      assertThat(responseString).contains("This application has no explicit mapping for /error");
    });
  }

  @Test
  void renderWhenAlreadyCommittedLogsMessage(CapturedOutput output) {
    this.contextRunner.run((context) -> {
      View errorView = context.getBean("error", View.class);
      ErrorAttributes errorAttributes = context.getBean(ErrorAttributes.class);
      MockRequestContext webRequest = createWebRequest(
              new IllegalStateException("Exception message"), true);
      errorView.render(errorAttributes.getErrorAttributes(webRequest, withAllOptions()), webRequest);
      assertThat(output).contains("Cannot render error page for request [/path] "
              + "and exception [Exception message] as the response has "
              + "already been committed. As a result, the response may have the wrong status code.");
    });
  }

  private MockRequestContext createWebRequest(Exception ex, boolean committed) {
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/path");
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    MockRequestContext context = new MockRequestContext(null, request, response);

    context.setAttribute("infra.mock.api.error.exception", ex);
    context.setAttribute("infra.mock.api.error.request_uri", "/path");
    context.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex);
    response.setCommitted(committed);
    response.setOutputStreamAccessAllowed(!committed);
    response.setWriterAccessAllowed(!committed);
    return context;
  }

  private ErrorAttributeOptions withAllOptions() {
    return ErrorAttributeOptions.of(Include.values());
  }

}
