/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.web.mock.samples.standalone;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.http.MediaType;
import infra.stereotype.Controller;
import infra.web.annotation.ControllerAdvice;
import infra.web.annotation.ExceptionHandler;
import infra.web.annotation.GetMapping;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RestController;
import infra.web.annotation.RestControllerAdvice;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.forwardedUrl;
import static infra.test.web.mock.result.MockMvcResultMatchers.jsonPath;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.setup.MockMvcBuilders.standaloneSetup;

/**
 * Exception handling via {@code @ExceptionHandler} methods.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class ExceptionHandlerTests {

  @Nested
  class MvcTests {

    @Test
    void localExceptionHandlerMethod() throws Exception {
      standaloneSetup(new PersonController()).build()
              .perform(get("/person/Clyde"))
              .andExpect(status().isOk())
              .andExpect(forwardedUrl("errorView"));
    }

    @Test
    void globalExceptionHandlerMethod() throws Exception {
      standaloneSetup(new PersonController())
              .setControllerAdvice(new GlobalExceptionHandler())
              .build()
              .perform(get("/person/Bonnie"))
              .andExpect(status().isOk())
              .andExpect(forwardedUrl("globalErrorView"));
    }
  }

  @Controller
  private static class PersonController {

    @GetMapping("/person/{name}")
    String show(@PathVariable String name) {
      if (name.equals("Clyde")) {
        throw new IllegalArgumentException("simulated exception");
      }
      else if (name.equals("Bonnie")) {
        throw new IllegalStateException("simulated exception");
      }
      return "person/show";
    }

    @ExceptionHandler
    String handleException(IllegalArgumentException exception) {
      return "errorView";
    }
  }

  @ControllerAdvice
  private static class GlobalExceptionHandler {

    @ExceptionHandler
    String handleException(IllegalStateException exception) {
      return "globalErrorView";
    }
  }

  @Nested
  class RestTests {

    @Test
    void noException() throws Exception {
      standaloneSetup(RestPersonController.class)
              .setControllerAdvice(RestGlobalExceptionHandler.class, RestPersonControllerExceptionHandler.class).build()
              .perform(get("/person/Yoda").accept(MediaType.APPLICATION_JSON))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.name").value("Yoda"));
    }

    @Test
    void localExceptionHandlerMethod() throws Exception {
      standaloneSetup(RestPersonController.class)
              .setControllerAdvice(RestGlobalExceptionHandler.class, RestPersonControllerExceptionHandler.class).build()
              .perform(get("/person/Luke").accept(MediaType.APPLICATION_JSON))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.error").value("local - IllegalArgumentException"));
    }

    @Test
    void globalExceptionHandlerMethod() throws Exception {
      standaloneSetup(RestPersonController.class)
              .setControllerAdvice(RestGlobalExceptionHandler.class).build()
              .perform(get("/person/Leia").accept(MediaType.APPLICATION_JSON))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.error").value("global - IllegalStateException"));
    }

    @Test
    void globalRestPersonControllerExceptionHandlerTakesPrecedenceOverGlobalExceptionHandler() throws Exception {
      standaloneSetup(RestPersonController.class)
              .setControllerAdvice(RestGlobalExceptionHandler.class, RestPersonControllerExceptionHandler.class).build()
              .perform(get("/person/Leia").accept(MediaType.APPLICATION_JSON))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.error").value("globalPersonController - IllegalStateException"));
    }

    @Test
    void noHandlerFound() throws Exception {
      standaloneSetup(RestPersonController.class)
              .setControllerAdvice(RestGlobalExceptionHandler.class, RestPersonControllerExceptionHandler.class)
              .addDispatcherCustomizer(servlet -> servlet.setThrowExceptionIfNoHandlerFound(true))
              .build()
              .perform(get("/bogus").accept(MediaType.APPLICATION_JSON))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.error").value("global - HandlerNotFoundException"));
    }
  }

  @RestController
  private static class RestPersonController {

    @GetMapping("/person/{name}")
    Person get(@PathVariable String name) {
      return switch (name) {
        case "Luke" -> throw new IllegalArgumentException();
        case "Leia" -> throw new IllegalStateException();
        default -> new Person("Yoda");
      };
    }

    @ExceptionHandler
    Error handleException(IllegalArgumentException exception) {
      return new Error("local - " + exception.getClass().getSimpleName());
    }
  }

  @RestControllerAdvice(assignableTypes = RestPersonController.class)
  @Order(Ordered.HIGHEST_PRECEDENCE)
  private static class RestPersonControllerExceptionHandler {

    @ExceptionHandler
    Error handleException(Throwable exception) {
      return new Error("globalPersonController - " + exception.getClass().getSimpleName());
    }
  }

  @RestControllerAdvice
  @Order(Ordered.LOWEST_PRECEDENCE)
  private static class RestGlobalExceptionHandler {

    @ExceptionHandler
    Error handleException(Throwable exception) {
      return new Error("global - " + exception.getClass().getSimpleName());
    }
  }

  static class Person {

    private final String name;

    Person(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

  static class Error {

    private final String error;

    Error(String error) {
      this.error = error;
    }

    public String getError() {
      return error;
    }
  }

}
