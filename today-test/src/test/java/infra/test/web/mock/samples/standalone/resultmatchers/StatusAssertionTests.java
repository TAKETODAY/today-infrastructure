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

package infra.test.web.mock.samples.standalone.resultmatchers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import infra.core.annotation.AliasFor;
import infra.http.HttpStatus;
import infra.test.web.mock.MockMvc;
import infra.web.annotation.ExceptionHandler;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.ResponseStatus;
import infra.web.annotation.RestController;

import static infra.http.HttpStatus.BAD_REQUEST;
import static infra.http.HttpStatus.CREATED;
import static infra.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static infra.http.HttpStatus.I_AM_A_TEAPOT;
import static infra.http.HttpStatus.NOT_IMPLEMENTED;
import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.setup.MockMvcBuilders.standaloneSetup;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * Examples of expectations on the status and the status reason found in the response.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
@TestInstance(PER_CLASS)
class StatusAssertionTests {

  private final MockMvc mockMvc = standaloneSetup(new StatusController()).build();

  @Test
  void httpStatus() throws Exception {
    this.mockMvc.perform(get("/created")).andExpect(status().isCreated());
    this.mockMvc.perform(get("/createdWithComposedAnnotation")).andExpect(status().isCreated());
    this.mockMvc.perform(get("/badRequest")).andExpect(status().isBadRequest());
  }

  @Test
  void statusCode() throws Exception {
    this.mockMvc.perform(get("/teaPot")).andExpect(status().is(I_AM_A_TEAPOT.value()));
    this.mockMvc.perform(get("/created")).andExpect(status().is(CREATED.value()));
    this.mockMvc.perform(get("/createdWithComposedAnnotation")).andExpect(status().is(CREATED.value()));
    this.mockMvc.perform(get("/badRequest")).andExpect(status().is(BAD_REQUEST.value()));
    this.mockMvc.perform(get("/throwsException")).andExpect(status().is(I_AM_A_TEAPOT.value()));
  }

  @Test
  void statusCodeWithMatcher() throws Exception {
    this.mockMvc.perform(get("/badRequest")).andExpect(status().is(equalTo(BAD_REQUEST.value())));
  }

  @Test
  void reason() throws Exception {
    this.mockMvc.perform(get("/badRequest")).andExpect(status().reason("Expired token"));
  }

  @Test
  void reasonWithMatcher() throws Exception {
    this.mockMvc.perform(get("/badRequest")).andExpect(status().reason(equalTo("Expired token")));
    this.mockMvc.perform(get("/badRequest")).andExpect(status().reason(endsWith("token")));
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
//      System.out.println(ex);
    }
  }

}
