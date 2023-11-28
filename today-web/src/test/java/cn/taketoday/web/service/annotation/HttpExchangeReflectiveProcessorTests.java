/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.service.annotation;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.web.annotation.RequestBody;

import static cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates.reflection;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpExchangeReflectiveProcessor}.
 *
 * @author Sebastien Deleuze
 */
class HttpExchangeReflectiveProcessorTests {

  private final HttpExchangeReflectiveProcessor processor = new HttpExchangeReflectiveProcessor();

  private final RuntimeHints hints = new RuntimeHints();

  @Test
  void registerReflectiveHintsForMethodWithReturnValue() throws NoSuchMethodException {
    Method method = SampleService.class.getDeclaredMethod("get");
    processor.registerReflectionHints(hints.reflection(), method);
    assertThat(reflection().onType(SampleService.class)).accepts(hints);
    assertThat(reflection().onMethod(SampleService.class, "get")).accepts(hints);
    assertThat(reflection().onType(Response.class)).accepts(hints);
    assertThat(reflection().onMethod(Response.class, "getMessage")).accepts(hints);
    assertThat(reflection().onMethod(Response.class, "setMessage")).accepts(hints);
  }

  @Test
  void registerReflectiveHintsForMethodWithRequestBodyParameter() throws NoSuchMethodException {
    Method method = SampleService.class.getDeclaredMethod("post", Request.class);
    processor.registerReflectionHints(hints.reflection(), method);
    assertThat(reflection().onType(SampleService.class)).accepts(hints);
    assertThat(reflection().onMethod(SampleService.class, "post")).accepts(hints);
    assertThat(reflection().onType(Request.class)).accepts(hints);
    assertThat(reflection().onMethod(Request.class, "getMessage")).accepts(hints);
    assertThat(reflection().onMethod(Request.class, "setMessage")).accepts(hints);
  }

  interface SampleService {

    @GetExchange
    Response get();

    @PostExchange
    void post(@RequestBody Request request);
  }

  static class Request {

    private String message;

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }
  }

  static class Response {

    private String message;

    public Response(String message) {
      this.message = message;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }
  }

}
