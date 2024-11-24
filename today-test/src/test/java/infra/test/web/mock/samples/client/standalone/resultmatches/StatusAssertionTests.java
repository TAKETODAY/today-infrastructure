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

package infra.test.web.mock.samples.client.standalone.resultmatches;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import infra.core.annotation.AliasFor;
import infra.http.HttpStatus;
import infra.test.web.reactive.server.WebTestClient;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.web.annotation.ExceptionHandler;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.ResponseStatus;
import infra.web.annotation.RestController;

import static infra.http.HttpStatus.BAD_REQUEST;
import static infra.http.HttpStatus.CREATED;
import static infra.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static infra.http.HttpStatus.I_AM_A_TEAPOT;
import static infra.http.HttpStatus.NOT_IMPLEMENTED;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link infra.test.web.mock.samples.standalone.resultmatchers.StatusAssertionTests}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
@TestInstance(PER_CLASS)
class StatusAssertionTests {

  private final WebTestClient testClient =
          MockMvcWebTestClient.bindToController(new StatusController()).build();

  @Test
  void statusInt() {
    testClient.get().uri("/teaPot").exchange().expectStatus().isEqualTo(I_AM_A_TEAPOT.value());
    testClient.get().uri("/created").exchange().expectStatus().isEqualTo(CREATED.value());
    testClient.get().uri("/createdWithComposedAnnotation").exchange().expectStatus().isEqualTo(CREATED.value());
    testClient.get().uri("/badRequest").exchange().expectStatus().isEqualTo(BAD_REQUEST.value());
    testClient.get().uri("/throwsException").exchange().expectStatus().isEqualTo(I_AM_A_TEAPOT.value());
  }

  @Test
  void httpStatus() {
    testClient.get().uri("/created").exchange().expectStatus().isCreated();
    testClient.get().uri("/createdWithComposedAnnotation").exchange().expectStatus().isCreated();
    testClient.get().uri("/badRequest").exchange().expectStatus().isBadRequest();
  }

  @Test
  void matcher() {
    testClient.get().uri("/badRequest").exchange().expectStatus().value(equalTo(BAD_REQUEST.value()));
  }

  @RequestMapping
  @ResponseStatus
  @Retention(RetentionPolicy.RUNTIME)
  @interface Get {

    @AliasFor(annotation = RequestMapping.class, attribute = "path")
    String[] path() default {};

    @AliasFor(annotation = ResponseStatus.class, attribute = "code")
    HttpStatus status() default INTERNAL_SERVER_ERROR;
  }

  @RestController
  @ResponseStatus(I_AM_A_TEAPOT)
  private static class StatusController {

    @RequestMapping("/teaPot")
    void teaPot() {
    }

    @RequestMapping("/created")
    @ResponseStatus(CREATED)
    void created() {
    }

    @Get(path = "/createdWithComposedAnnotation", status = CREATED)
    void createdWithComposedAnnotation() {
    }

    @RequestMapping("/badRequest")
    @ResponseStatus(code = BAD_REQUEST, reason = "Expired token")
    void badRequest() {
    }

    @RequestMapping("/notImplemented")
    @ResponseStatus(NOT_IMPLEMENTED)
    void notImplemented() {
    }

    @RequestMapping("/throwsException")
    @ResponseStatus(NOT_IMPLEMENTED)
    void throwsException() {
      throw new IllegalStateException();
    }

    @ExceptionHandler
    void exceptionHandler(IllegalStateException ex) {
    }
  }

}
