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

package infra.test.web.mock.samples.client.standalone.resultmatches;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import infra.core.annotation.AliasFor;
import infra.http.HttpStatus;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.test.web.reactive.server.WebTestClient;
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
