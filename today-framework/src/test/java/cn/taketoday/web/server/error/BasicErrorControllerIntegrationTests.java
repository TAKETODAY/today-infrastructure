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

package cn.taketoday.web.server.error;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.util.Map;

import cn.taketoday.annotation.config.context.PropertyPlaceholderAutoConfiguration;
import cn.taketoday.annotation.config.freemarker.FreeMarkerAutoConfiguration;
import cn.taketoday.annotation.config.http.HttpMessageConvertersAutoConfiguration;
import cn.taketoday.annotation.config.web.ErrorMvcAutoConfiguration;
import cn.taketoday.annotation.config.web.RandomPortWebServerConfig;
import cn.taketoday.annotation.config.web.WebMvcAutoConfiguration;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.ImportAutoConfiguration;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.test.web.client.TestRestTemplate;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.RequestEntity;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.validation.BindException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.PostMapping;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.bind.MethodArgumentNotValidException;
import cn.taketoday.web.server.error.BasicErrorController;
import cn.taketoday.web.view.AbstractView;
import cn.taketoday.web.view.View;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BasicErrorController} using a real HTTP server.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
class BasicErrorControllerIntegrationTests {

  private ConfigurableApplicationContext context;

  @AfterEach
  void closeContext() {
    if (context != null) {
      Application.exit(context);
    }
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  void testErrorForMachineClientDefault() {
    load();
    ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(createUrl("?trace=true"), Map.class);
    assertErrorAttributes(entity.getBody(), "500", "Internal Server Error", null, null, "/");
    assertThat(entity.getBody()).doesNotContainKey("exception");
    assertThat(entity.getBody()).doesNotContainKey("trace");
  }

  @Test
  void testErrorForMachineClientWithParamsTrue() {
    load("--server.error.include-exception=true", "--server.error.include-stacktrace=on-param",
            "--server.error.include-message=on-param");
    exceptionWithStackTraceAndMessage("?trace=true&message=true");
  }

  @Test
  void testErrorForMachineClientWithParamsFalse() {
    load("--server.error.include-exception=true", "--server.error.include-stacktrace=on-param",
            "--server.error.include-message=on-param");
    exceptionWithoutStackTraceAndMessage("?trace=false&message=false");
  }

  @Test
  void testErrorForMachineClientWithParamsAbsent() {
    load("--server.error.include-exception=true", "--server.error.include-stacktrace=on-param",
            "--server.error.include-message=on-param");
    exceptionWithoutStackTraceAndMessage("");
  }

  @Test
  void testErrorForMachineClientNeverParams() {
    load("--server.error.include-exception=true", "--server.error.include-stacktrace=never",
            "--server.error.include-message=never");
    exceptionWithoutStackTraceAndMessage("?trace=true&message=true");
  }

  @Test
  void testErrorForMachineClientAlwaysParams() {
    load("--server.error.include-exception=true", "--server.error.include-stacktrace=always",
            "--server.error.include-message=always");
    exceptionWithStackTraceAndMessage("?trace=false&message=false");
  }

  @Test
  @SuppressWarnings("rawtypes")
  void testErrorForMachineClientAlwaysParamsWithoutMessage() {
    load("--server.error.include-exception=true", "--server.error.include-message=always");
    ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(createUrl("/noMessage"), Map.class);
    assertErrorAttributes(entity.getBody(), "500", "Internal Server Error", IllegalStateException.class,
            "No message available", "/noMessage");
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void exceptionWithStackTraceAndMessage(String path) {
    ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(createUrl(path), Map.class);
    assertErrorAttributes(entity.getBody(), "500", "Internal Server Error", IllegalStateException.class,
            "Expected!", "/");
    assertThat(entity.getBody()).containsKey("trace");
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void exceptionWithoutStackTraceAndMessage(String path) {
    ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(createUrl(path), Map.class);
    assertErrorAttributes(entity.getBody(), "500", "Internal Server Error", IllegalStateException.class, null, "/");
    assertThat(entity.getBody()).doesNotContainKey("trace");
  }

  @Test
  @SuppressWarnings("rawtypes")
  void testErrorForAnnotatedExceptionWithoutMessage() {
    load("--server.error.include-exception=true");
    ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(createUrl("/annotated"), Map.class);
    assertErrorAttributes(entity.getBody(), "400", "Bad Request", TestConfiguration.Errors.ExpectedException.class,
            null, "/annotated");
  }

  @Test
  @SuppressWarnings("rawtypes")
  void testErrorForAnnotatedExceptionWithMessage() {
    load("--server.error.include-exception=true", "--server.error.include-message=always");
    ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(createUrl("/annotated"), Map.class);
    assertErrorAttributes(entity.getBody(), "400", "Bad Request", TestConfiguration.Errors.ExpectedException.class,
            "Expected!", "/annotated");
  }

  @Test
  @SuppressWarnings("rawtypes")
  void testErrorForAnnotatedNoReasonExceptionWithoutMessage() {
    load("--server.error.include-exception=true");
    ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(createUrl("/annotatedNoReason"), Map.class);
    assertErrorAttributes(entity.getBody(), "406", "Not Acceptable",
            TestConfiguration.Errors.NoReasonExpectedException.class, null, "/annotatedNoReason");
  }

  @Test
  @SuppressWarnings("rawtypes")
  void testErrorForAnnotatedNoReasonExceptionWithMessage() {
    load("--server.error.include-exception=true", "--server.error.include-message=always");
    ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(createUrl("/annotatedNoReason"), Map.class);
    assertErrorAttributes(entity.getBody(), "406", "Not Acceptable",
            TestConfiguration.Errors.NoReasonExpectedException.class, "Expected message", "/annotatedNoReason");
  }

  @Test
  @SuppressWarnings("rawtypes")
  void testErrorForAnnotatedNoMessageExceptionWithMessage() {
    load("--server.error.include-exception=true", "--server.error.include-message=always");
    ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(createUrl("/annotatedNoMessage"), Map.class);
    assertErrorAttributes(entity.getBody(), "406", "Not Acceptable",
            TestConfiguration.Errors.NoReasonExpectedException.class, "No message available",
            "/annotatedNoMessage");
  }

  @Test
  void testBindingExceptionForMachineClientWithErrorsParamTrue() {
    load("--server.error.include-exception=true", "--server.error.include-binding-errors=on-param");
    bindingExceptionWithErrors("?errors=true");
  }

  @Test
  void testBindingExceptionForMachineClientWithErrorsParamFalse() {
    load("--server.error.include-exception=true", "--server.error.include-binding-errors=on-param");
    bindingExceptionWithoutErrors("?errors=false");
  }

  @Test
  void testBindingExceptionForMachineClientWithErrorsParamAbsent() {
    load("--server.error.include-exception=true", "--server.error.include-binding-errors=on-param");
    bindingExceptionWithoutErrors("");
  }

  @Test
  void testBindingExceptionForMachineClientAlwaysErrors() {
    load("--server.error.include-exception=true", "--server.error.include-binding-errors=always");
    bindingExceptionWithErrors("?errors=false");
  }

  @Test
  void testBindingExceptionForMachineClientNeverErrors() {
    load("--server.error.include-exception=true", "--server.error.include-binding-errors=never");
    bindingExceptionWithoutErrors("?errors=true");
  }

  @Test
  void testBindingExceptionForMachineClientWithMessageParamTrue() {
    load("--server.error.include-exception=true", "--server.error.include-message=on-param");
    bindingExceptionWithMessage("?message=true");
  }

  @Test
  void testBindingExceptionForMachineClientWithMessageParamFalse() {
    load("--server.error.include-exception=true", "--server.error.include-message=on-param");
    bindingExceptionWithoutMessage("?message=false");
  }

  @Test
  void testBindingExceptionForMachineClientWithMessageParamAbsent() {
    load("--server.error.include-exception=true", "--server.error.include-message=on-param");
    bindingExceptionWithoutMessage("");
  }

  @Test
  void testBindingExceptionForMachineClientAlwaysMessage() {
    load("--server.error.include-exception=true", "--server.error.include-message=always");
    bindingExceptionWithMessage("?message=false");
  }

  @Test
  void testBindingExceptionForMachineClientNeverMessage() {
    load("--server.error.include-exception=true", "--server.error.include-message=never");
    bindingExceptionWithoutMessage("?message=true");
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void bindingExceptionWithErrors(String param) {
    ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(createUrl("/bind" + param), Map.class);
    assertErrorAttributes(entity.getBody(), "400", "Bad Request", BindException.class, null, "/bind");
    assertThat(entity.getBody()).containsKey("errors");
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void bindingExceptionWithoutErrors(String param) {
    ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(createUrl("/bind" + param), Map.class);
    assertErrorAttributes(entity.getBody(), "400", "Bad Request", BindException.class, null, "/bind");
    assertThat(entity.getBody()).doesNotContainKey("errors");
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void bindingExceptionWithMessage(String param) {
    ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(createUrl("/bind" + param), Map.class);
    assertErrorAttributes(entity.getBody(), "400", "Bad Request", BindException.class,
            "Validation failed for object='test'. Error count: 1", "/bind");
    assertThat(entity.getBody()).doesNotContainKey("errors");
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void bindingExceptionWithoutMessage(String param) {
    ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(createUrl("/bind" + param), Map.class);
    assertErrorAttributes(entity.getBody(), "400", "Bad Request", BindException.class, null, "/bind");
    assertThat(entity.getBody()).doesNotContainKey("errors");
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  void testRequestBodyValidationForMachineClient() {
    load("--server.error.include-exception=true");
    RequestEntity request = RequestEntity.post(URI.create(createUrl("/bodyValidation")))
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}");
    ResponseEntity<Map> entity = new TestRestTemplate().exchange(request, Map.class);
    assertErrorAttributes(entity.getBody(), "400", "Bad Request", MethodArgumentNotValidException.class, null,
            "/bodyValidation");
    assertThat(entity.getBody()).doesNotContainKey("errors");
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  void testBindingExceptionForMachineClientDefault() {
    load();
    RequestEntity request = RequestEntity.get(URI.create(createUrl("/bind?trace=true,message=true")))
            .accept(MediaType.APPLICATION_JSON)
            .build();
    ResponseEntity<Map> entity = new TestRestTemplate().exchange(request, Map.class);
    assertThat(entity.getBody()).doesNotContainKey("exception");
    assertThat(entity.getBody()).doesNotContainKey("trace");
    assertThat(entity.getBody()).doesNotContainKey("errors");
  }

  @Test
  void testConventionTemplateMapping() {
    load();
    RequestEntity<?> request = RequestEntity.get(URI.create(createUrl("/noStorage")))
            .accept(MediaType.TEXT_HTML)
            .build();
    ResponseEntity<String> entity = new TestRestTemplate().exchange(request, String.class);
    String resp = entity.getBody();
    assertThat(resp).contains("We are out of storage");
  }

  @Test
  void testIncompatibleMediaType() {
    load();
    RequestEntity<?> request = RequestEntity.get(URI.create(createUrl("/incompatibleType")))
            .accept(MediaType.TEXT_PLAIN)
            .build();
    ResponseEntity<String> entity = new TestRestTemplate().exchange(request, String.class);
    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(entity.getHeaders().getContentType()).isNull();
    assertThat(entity.getBody()).isNull();
  }

  private void assertErrorAttributes(Map<?, ?> content, String status, String error, Class<?> exception,
          String message, String path) {
    assertThat(content.get("status")).as("Wrong status").hasToString(status);
    assertThat(content.get("error")).as("Wrong error").isEqualTo(error);
    if (exception != null) {
      assertThat(content.get("exception")).as("Wrong exception").isEqualTo(exception.getName());
    }
    else {
      assertThat(content.containsKey("exception")).as("Exception attribute should not be set").isFalse();
    }
    assertThat(content.get("message")).as("Wrong message").isEqualTo(message);
    assertThat(content.get("path")).as("Wrong path").isEqualTo(path);
  }

  private String createUrl(String path) {
    int port = this.context.getEnvironment().getRequiredProperty("local.server.port", int.class);
    return "http://localhost:" + port + path;
  }

  private void load(String... arguments) {
    this.context = Application.run(TestConfiguration.class, arguments);
  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @ImportAutoConfiguration({
          RandomPortWebServerConfig.class,
          WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
          ErrorMvcAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
  private @interface MinimalWebConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  @MinimalWebConfiguration
  @ImportAutoConfiguration(FreeMarkerAutoConfiguration.class)
  public static class TestConfiguration {

    // For manual testing
    static void main(String[] args) {
      Application.run(TestConfiguration.class, args);
    }

    @Bean
    View error() {
      return new AbstractView() {

        @Override
        protected void renderMergedOutputModel(Map<String, Object> model, RequestContext request) throws Exception {
          request.getWriter().write("ERROR_BEAN");
        }
      };
    }

    @RestController
    public static class Errors {

      public String getFoo() {
        return "foo";
      }

      @RequestMapping("/")
      String home() {
        throw new IllegalStateException("Expected!");
      }

      @RequestMapping("/noMessage")
      String noMessage() {
        throw new IllegalStateException();
      }

      @RequestMapping("/annotated")
      String annotated() {
        throw new ExpectedException();
      }

      @RequestMapping("/annotatedNoReason")
      String annotatedNoReason() {
        throw new NoReasonExpectedException("Expected message");
      }

      @RequestMapping("/annotatedNoMessage")
      String annotatedNoMessage() {
        throw new NoReasonExpectedException("");
      }

      @RequestMapping("/bind")
      String bind() throws Exception {
        BindException error = new BindException(this, "test");
        error.rejectValue("foo", "bar.error");
        throw error;
      }

      @PostMapping(path = "/bodyValidation", produces = "application/json")
      String bodyValidation(@Valid @RequestBody DummyBody body) {
        return body.content;
      }

      @RequestMapping(path = "/noStorage")
      String noStorage() {
        throw new InsufficientStorageException();
      }

      @RequestMapping(path = "/incompatibleType", produces = "text/plain")
      String incompatibleType() {
        throw new ExpectedException();
      }

      @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Expected!")
      static class ExpectedException extends RuntimeException {

      }

      @ResponseStatus(HttpStatus.INSUFFICIENT_STORAGE)
      static class InsufficientStorageException extends RuntimeException {

      }

      @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
      static class NoReasonExpectedException extends RuntimeException {

        NoReasonExpectedException(String message) {
          super(message);
        }

      }

      static class DummyBody {

        @NotNull
        private String content;

        String getContent() {
          return this.content;
        }

        void setContent(String content) {
          this.content = content;
        }

      }

    }

  }

}
