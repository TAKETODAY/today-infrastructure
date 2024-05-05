/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.web.mock.samples.client.standalone;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.http.MediaType;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.mock.client.MockMvcWebTestClient;
import cn.taketoday.web.annotation.ControllerAdvice;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.GetMapping;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.annotation.RestControllerAdvice;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link cn.taketoday.test.web.mock.samples.standalone.ExceptionHandlerTests}.
 *
 * @author Rossen Stoyanchev
 */
class ExceptionHandlerTests {

  @Nested
  class MvcTests {

    @Test
    void localExceptionHandlerMethod() {
      WebTestClient client = MockMvcWebTestClient.bindToController(new PersonController()).build();

      client.get().uri("/person/Clyde")
              .exchange()
              .expectStatus().isOk()
              .expectHeader().valueEquals("Forwarded-Url", "errorView");
    }

    @Test
    void globalExceptionHandlerMethod() {
      WebTestClient client = MockMvcWebTestClient.bindToController(new PersonController())
              .controllerAdvice(new GlobalExceptionHandler())
              .build();

      client.get().uri("/person/Bonnie")
              .exchange()
              .expectStatus().isOk()
              .expectHeader().valueEquals("Forwarded-Url", "globalErrorView");
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
    void noException() {
      WebTestClient client = MockMvcWebTestClient.bindToController(new RestPersonController())
              .controllerAdvice(new RestPersonControllerExceptionHandler())
              .build();

      client.get().uri("/person/Yoda")
              .accept(MediaType.APPLICATION_JSON)
              .exchange()
              .expectStatus().isOk()
              .expectBody().jsonPath("$.name", "Yoda");
    }

    @Test
    void localExceptionHandlerMethod() {
      WebTestClient client = MockMvcWebTestClient.bindToController(new RestPersonController())
              .controllerAdvice(new RestPersonControllerExceptionHandler())
              .build();

      client.get().uri("/person/Luke")
              .accept(MediaType.APPLICATION_JSON)
              .exchange()
              .expectStatus().isOk()
              .expectBody().jsonPath("$.error", "local - IllegalArgumentException");
    }

    @Test
    void globalExceptionHandlerMethod() {
      WebTestClient client = MockMvcWebTestClient.bindToController(new RestPersonController())
              .controllerAdvice(new RestGlobalExceptionHandler())
              .build();

      client.get().uri("/person/Leia")
              .accept(MediaType.APPLICATION_JSON)
              .exchange()
              .expectStatus().isOk()
              .expectBody().jsonPath("$.error", "global - IllegalArgumentException");
    }

    @Test
    void globalRestPersonControllerExceptionHandlerTakesPrecedenceOverGlobalExceptionHandler() {
      WebTestClient client = MockMvcWebTestClient.bindToController(new RestPersonController())
              .controllerAdvice(RestGlobalExceptionHandler.class, RestPersonControllerExceptionHandler.class)
              .build();

      client.get().uri("/person/Leia")
              .accept(MediaType.APPLICATION_JSON)
              .exchange()
              .expectStatus().isOk()
              .expectBody().jsonPath("$.error", "globalPersonController - IllegalStateException");
    }

    @Test
    void noHandlerFound() {
      WebTestClient client = MockMvcWebTestClient.bindToController(new RestPersonController())
              .controllerAdvice(RestGlobalExceptionHandler.class, RestPersonControllerExceptionHandler.class)
              .dispatcherServletCustomizer(servlet -> servlet.setThrowExceptionIfNoHandlerFound(true))
              .build();

      client.get().uri("/bogus")
              .accept(MediaType.APPLICATION_JSON)
              .exchange()
              .expectStatus().isOk()
              .expectBody().jsonPath("$.error", "global - NoHandlerFoundException");
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
