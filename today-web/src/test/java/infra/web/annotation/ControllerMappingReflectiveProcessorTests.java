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

package infra.web.annotation;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import infra.aot.hint.MemberCategory;
import infra.aot.hint.ReflectionHints;
import infra.aot.hint.TypeReference;
import infra.http.HttpEntity;
import infra.web.bind.annotation.ModelAttribute;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/29 14:08
 */
class ControllerMappingReflectiveProcessorTests {

  private final ControllerMappingReflectiveProcessor processor = new ControllerMappingReflectiveProcessor();

  private final ReflectionHints hints = new ReflectionHints();

  @Test
  void registerReflectiveHintsForMethodWithResponseBody() throws NoSuchMethodException {
    Method method = SampleController.class.getDeclaredMethod("get");
    processor.registerReflectionHints(hints, method);
    assertThat(hints.typeHints()).satisfiesExactlyInAnyOrder(
            typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
            typeHint -> {
              assertThat(typeHint.getType()).isEqualTo(TypeReference.of(Response.class));
              assertThat(typeHint.getMemberCategories()).containsExactlyInAnyOrder(
                      MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                      MemberCategory.INVOKE_DECLARED_FIELDS);
              assertThat(typeHint.methods()).satisfiesExactlyInAnyOrder(
                      hint -> assertThat(hint.getName()).isEqualTo("getMessage"),
                      hint -> assertThat(hint.getName()).isEqualTo("setMessage"));
            },
            typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class)));
  }

  @Test
  void registerReflectiveHintsForMethodWithRequestBody() throws NoSuchMethodException {
    Method method = SampleController.class.getDeclaredMethod("post", Request.class);
    processor.registerReflectionHints(hints, method);
    assertThat(hints.typeHints()).satisfiesExactlyInAnyOrder(
            typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
            typeHint -> {
              assertThat(typeHint.getType()).isEqualTo(TypeReference.of(Request.class));
              assertThat(typeHint.getMemberCategories()).containsExactlyInAnyOrder(
                      MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                      MemberCategory.INVOKE_DECLARED_FIELDS);
              assertThat(typeHint.methods()).satisfiesExactlyInAnyOrder(
                      hint -> assertThat(hint.getName()).isEqualTo("getMessage"),
                      hint -> assertThat(hint.getName()).isEqualTo("setMessage"));
            },
            typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class)));
  }

  @Test
  void registerReflectiveHintsForMethodWithModelAttribute() throws NoSuchMethodException {
    Method method = SampleController.class.getDeclaredMethod("postForm", Request.class);
    processor.registerReflectionHints(hints, method);
    assertThat(hints.typeHints()).satisfiesExactlyInAnyOrder(
            typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
            typeHint -> {
              assertThat(typeHint.getType()).isEqualTo(TypeReference.of(Request.class));
              assertThat(typeHint.getMemberCategories()).containsExactlyInAnyOrder(
                      MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                      MemberCategory.INVOKE_DECLARED_FIELDS);
              assertThat(typeHint.methods()).satisfiesExactlyInAnyOrder(
                      hint -> assertThat(hint.getName()).isEqualTo("getMessage"),
                      hint -> assertThat(hint.getName()).isEqualTo("setMessage"));
            },
            typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class)));
  }

  @Test
  void registerReflectiveHintsForMethodWithRestController() throws NoSuchMethodException {
    Method method = SampleRestController.class.getDeclaredMethod("get");
    processor.registerReflectionHints(hints, method);
    assertThat(hints.typeHints()).satisfiesExactlyInAnyOrder(
            typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleRestController.class)),
            typeHint -> {
              assertThat(typeHint.getType()).isEqualTo(TypeReference.of(Response.class));
              assertThat(typeHint.getMemberCategories()).containsExactlyInAnyOrder(
                      MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                      MemberCategory.INVOKE_DECLARED_FIELDS);
              assertThat(typeHint.methods()).satisfiesExactlyInAnyOrder(
                      hint -> assertThat(hint.getName()).isEqualTo("getMessage"),
                      hint -> assertThat(hint.getName()).isEqualTo("setMessage"));
            },
            typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class)));
  }

  @Test
  void registerReflectiveHintsForMethodWithString() throws NoSuchMethodException {
    Method method = SampleController.class.getDeclaredMethod("message");
    processor.registerReflectionHints(hints, method);
    assertThat(hints.typeHints()).satisfiesExactlyInAnyOrder(
            typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
            typeHint -> {
              assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class));
              assertThat(typeHint.constructors()).isEmpty();
              assertThat(typeHint.fields()).isEmpty();
              assertThat(typeHint.methods()).isEmpty();
            });
  }

  @Test
  void registerReflectiveHintsForClassWithMapping() {
    processor.registerReflectionHints(hints, SampleControllerWithClassMapping.class);
    assertThat(hints.typeHints()).singleElement().satisfies(typeHint ->
            assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleControllerWithClassMapping.class)));
  }

  @Test
  void registerReflectiveHintsForMethodReturningHttpEntity() throws NoSuchMethodException {
    Method method = SampleController.class.getDeclaredMethod("getHttpEntity");
    processor.registerReflectionHints(hints, method);
    assertThat(hints.typeHints()).satisfiesExactlyInAnyOrder(
            typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
            typeHint -> {
              assertThat(typeHint.getType()).isEqualTo(TypeReference.of(Response.class));
              assertThat(typeHint.getMemberCategories()).containsExactlyInAnyOrder(
                      MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                      MemberCategory.INVOKE_DECLARED_FIELDS);
              assertThat(typeHint.methods()).satisfiesExactlyInAnyOrder(
                      hint -> assertThat(hint.getName()).isEqualTo("getMessage"),
                      hint -> assertThat(hint.getName()).isEqualTo("setMessage"));
            },
            typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class)));
  }

  @Test
  void registerReflectiveHintsForMethodReturningRawHttpEntity() throws NoSuchMethodException {
    Method method = SampleController.class.getDeclaredMethod("getRawHttpEntity");
    processor.registerReflectionHints(hints, method);
    assertThat(hints.typeHints()).singleElement().satisfies(
            typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleController.class)));
  }

  @Test
  void registerReflectiveHintsForMethodWithHttpEntityParameter() throws NoSuchMethodException {
    Method method = SampleController.class.getDeclaredMethod("postHttpEntity", HttpEntity.class);
    processor.registerReflectionHints(hints, method);
    assertThat(hints.typeHints()).satisfiesExactlyInAnyOrder(
            typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
            typeHint -> {
              assertThat(typeHint.getType()).isEqualTo(TypeReference.of(Request.class));
              assertThat(typeHint.getMemberCategories()).containsExactlyInAnyOrder(
                      MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                      MemberCategory.INVOKE_DECLARED_FIELDS);
              assertThat(typeHint.methods()).satisfiesExactlyInAnyOrder(
                      hint -> assertThat(hint.getName()).isEqualTo("getMessage"),
                      hint -> assertThat(hint.getName()).isEqualTo("setMessage"));
            },
            typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class)));
  }

  @Test
  void registerReflectiveHintsForMethodWithRawHttpEntityParameter() throws NoSuchMethodException {
    Method method = SampleController.class.getDeclaredMethod("postRawHttpEntity", HttpEntity.class);
    processor.registerReflectionHints(hints, method);
    assertThat(hints.typeHints()).singleElement().satisfies(
            typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleController.class)));
  }

  @Test
  void registerReflectiveHintsForMethodWithPartToConvert() throws NoSuchMethodException {
    Method method = SampleController.class.getDeclaredMethod("postPartToConvert", Request.class);
    processor.registerReflectionHints(hints, method);
    assertThat(hints.typeHints()).satisfiesExactlyInAnyOrder(
            typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
            typeHint -> {
              assertThat(typeHint.getType()).isEqualTo(TypeReference.of(Request.class));
              assertThat(typeHint.getMemberCategories()).containsExactlyInAnyOrder(
                      MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                      MemberCategory.INVOKE_DECLARED_FIELDS);
              assertThat(typeHint.methods()).satisfiesExactlyInAnyOrder(
                      hint -> assertThat(hint.getName()).isEqualTo("getMessage"),
                      hint -> assertThat(hint.getName()).isEqualTo("setMessage"));
            },
            typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class)));
  }

  static class SampleController {

    @GetMapping
    @ResponseBody
    Response get() {
      return new Response("response");
    }

    @PostMapping
    void post(@RequestBody Request request) {
    }

    @PostMapping
    void postForm(@ModelAttribute Request request) {
    }

    @GetMapping
    @ResponseBody
    String message() {
      return "";
    }

    @GetMapping
    HttpEntity<Response> getHttpEntity() {
      return new HttpEntity<>(new Response("response"));
    }

    @GetMapping
    @SuppressWarnings({ "rawtypes", "unchecked" })
    HttpEntity getRawHttpEntity() {
      return new HttpEntity(new Response("response"));
    }

    @PostMapping
    void postHttpEntity(HttpEntity<Request> entity) {
    }

    @PostMapping
    @SuppressWarnings("rawtypes")
    void postRawHttpEntity(HttpEntity entity) {
    }

    @PostMapping
    void postPartToConvert(@RequestPart Request request) {
    }

  }

  @RestController
  static class SampleRestController {

    @GetMapping
    Response get() {
      return new Response("response");
    }
  }

  @RequestMapping("/prefix")
  static class SampleControllerWithClassMapping {
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