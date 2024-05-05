/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.annotation.config.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.util.Map;

import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.framework.test.system.CapturedOutput;
import cn.taketoday.framework.test.system.OutputCaptureExtension;
import cn.taketoday.web.server.context.AnnotationConfigWebServerApplicationContext;
import cn.taketoday.web.server.error.ErrorAttributeOptions;
import cn.taketoday.web.server.error.ErrorAttributeOptions.Include;
import cn.taketoday.web.server.error.ErrorAttributes;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.web.mock.ServletRequestContext;
import cn.taketoday.web.util.WebUtils;
import cn.taketoday.web.view.View;

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
      ServletRequestContext webRequest = createWebRequest(
              new IllegalStateException("Exception message"), false);
      errorView.render(errorAttributes.getErrorAttributes(webRequest, withAllOptions()), webRequest);
      assertThat(webRequest.getResponse().getContentType()).isEqualTo("text/html");
      String responseString = ((MockHttpServletResponse) webRequest.getResponse()).getContentAsString();
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
      ServletRequestContext webRequest = createWebRequest(new IllegalStateException("Exception message"),
              false);
      Map<String, Object> attributes = errorAttributes.getErrorAttributes(webRequest, withAllOptions());
      attributes.put("timestamp", Clock.systemUTC().instant());
      errorView.render(attributes, webRequest);
      assertThat(webRequest.getResponse().getContentType()).isEqualTo("text/html");
      String responseString = ((MockHttpServletResponse) webRequest.getResponse()).getContentAsString();
      assertThat(responseString).contains("This application has no explicit mapping for /error");
    });
  }

  @Test
  void renderWhenAlreadyCommittedLogsMessage(CapturedOutput output) {
    this.contextRunner.run((context) -> {
      View errorView = context.getBean("error", View.class);
      ErrorAttributes errorAttributes = context.getBean(ErrorAttributes.class);
      ServletRequestContext webRequest = createWebRequest(
              new IllegalStateException("Exception message"), true);
      errorView.render(errorAttributes.getErrorAttributes(webRequest, withAllOptions()), webRequest);
      assertThat(output).contains("Cannot render error page for request [/path] "
              + "and exception [Exception message] as the response has "
              + "already been committed. As a result, the response may have the wrong status code.");
    });
  }

  private ServletRequestContext createWebRequest(Exception ex, boolean committed) {
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/path");
    MockHttpServletResponse response = new MockHttpServletResponse();

    ServletRequestContext context = new ServletRequestContext(null, request, response);

    context.setAttribute("cn.taketoday.mock.api.error.exception", ex);
    context.setAttribute("cn.taketoday.mock.api.error.request_uri", "/path");
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
