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

package cn.taketoday.test.web.mock.samples.standalone;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.http.MediaType;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.web.annotation.ControllerAdvice;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.GetMapping;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.annotation.RestControllerAdvice;

import static cn.taketoday.test.web.mock.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.forwardedUrl;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.jsonPath;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.mock.setup.MockMvcBuilders.standaloneSetup;

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
