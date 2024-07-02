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

package cn.taketoday.web.handler;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.BindException;
import java.net.SocketException;

import cn.taketoday.http.MediaType;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.handler.method.ExceptionHandlerMethodResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 22:44
 */
class ExceptionHandlerMethodResolverTests {

  @Test
  void shouldResolveMethodFromAnnotationAttribute() {
    ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(ExceptionController.class);
    IOException exception = new IOException();
    assertThat(resolver.resolveMethod(exception).getName()).isEqualTo("handleIOException");
  }

  @Test
  void shouldResolveMethodFromMethodArgument() {
    ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(ExceptionController.class);
    IllegalArgumentException exception = new IllegalArgumentException();
    assertThat(resolver.resolveMethod(exception).getName()).isEqualTo("handleIllegalArgumentException");
  }

  @Test
  void shouldResolveMethodWithExceptionSubType() {
    ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(ExceptionController.class);
    IOException ioException = new FileNotFoundException();
    assertThat(resolver.resolveMethod(ioException).getName()).isEqualTo("handleIOException");
    SocketException bindException = new BindException();
    assertThat(resolver.resolveMethod(bindException).getName()).isEqualTo("handleSocketException");
  }

  @Test
  void shouldResolveMethodWithExceptionBestMatch() {
    ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(ExceptionController.class);
    SocketException exception = new SocketException();
    assertThat(resolver.resolveMethod(exception).getName()).isEqualTo("handleSocketException");
  }

  @Test
  void shouldNotResolveMethodWhenExceptionNoMatch() {
    ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(ExceptionController.class);
    Exception exception = new Exception();
    assertThat(resolver.resolveMethod(exception)).as("1st lookup").isNull();
    assertThat(resolver.resolveMethod(exception)).as("2nd lookup from cache").isNull();
  }

  @Test
  void ShouldResolveMethodWithExceptionCause() {
    ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(ExceptionController.class);

    SocketException bindException = new BindException();
    bindException.initCause(new FileNotFoundException());

    Exception exception = new Exception(new Exception(new Exception(bindException)));

    assertThat(resolver.resolveMethod(exception).getName()).isEqualTo("handleSocketException");
  }

  @Test
  void shouldResolveMethodFromSuperClass() {
    ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(InheritedController.class);
    IOException exception = new IOException();
    assertThat(resolver.resolveMethod(exception).getName()).isEqualTo("handleIOException");
  }

  @Test
  void shouldThrowExceptionWhenAmbiguousExceptionMapping() {
    assertThatIllegalStateException().isThrownBy(() ->
            new ExceptionHandlerMethodResolver(AmbiguousController.class));
  }

  @Test
  void shouldThrowExceptionWhenNoExceptionMapping() {
    assertThatIllegalStateException().isThrownBy(() ->
            new ExceptionHandlerMethodResolver(NoExceptionController.class));
  }

  @Test
  void shouldResolveMethodWithMediaType() {
    ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(MediaTypeController.class);
    assertThat(resolver.resolveExceptionMapping(new IllegalArgumentException(), MediaType.APPLICATION_JSON).getHandlerMethod().getName()).isEqualTo("handleJson");
    assertThat(resolver.resolveExceptionMapping(new IllegalArgumentException(), MediaType.TEXT_HTML).getHandlerMethod().getName()).isEqualTo("handleHtml");
  }

  @Test
  void shouldResolveMethodWithCompatibleMediaType() {
    ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(MediaTypeController.class);
    assertThat(resolver.resolveExceptionMapping(new IllegalArgumentException(), MediaType.parseMediaType("application/*")).getHandlerMethod().getName()).isEqualTo("handleJson");
  }

  @Test
  void shouldFavorMethodWithExplicitAcceptAll() {
    ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(MediaTypeController.class);
    assertThat(resolver.resolveExceptionMapping(new IllegalArgumentException(), MediaType.ALL).getHandlerMethod().getName()).isEqualTo("handleHtml");
  }

  @Test
  void shouldThrowExceptionWhenInvalidMediaTypeMapping() {
    assertThatIllegalStateException().isThrownBy(() ->
                    new ExceptionHandlerMethodResolver(InvalidMediaTypeController.class))
            .withMessageContaining("Invalid media type [invalid-mediatype] declared on @ExceptionHandler");
  }

  @Test
  void shouldThrowExceptionWhenAmbiguousMediaTypeMapping() {
    assertThatIllegalStateException().isThrownBy(() ->
                    new ExceptionHandlerMethodResolver(AmbiguousMediaTypeController.class))
            .withMessageContaining("Ambiguous @ExceptionHandler method mapped for [ExceptionHandler{exceptionType=java.lang.IllegalArgumentException, mediaType=application/json}]")
            .withMessageContaining("AmbiguousMediaTypeController.handleJson()")
            .withMessageContaining("AmbiguousMediaTypeController.handleJsonToo()");
  }

  @Test
  void shouldResolveMethodWithMediaTypeFallback() {
    ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(MixedController.class);
    assertThat(resolver.resolveExceptionMapping(new IllegalArgumentException(), MediaType.TEXT_HTML).getHandlerMethod().getName()).isEqualTo("handleOther");
  }

  @Controller
  static class ExceptionController {

    public void handle() { }

    @ExceptionHandler(IOException.class)
    public void handleIOException() {
    }

    @ExceptionHandler(SocketException.class)
    public void handleSocketException() {
    }

    @ExceptionHandler
    public void handleIllegalArgumentException(IllegalArgumentException exception) {
    }
  }

  @Controller
  static class InheritedController extends ExceptionController {

    @Override
    public void handleIOException() {
    }
  }

  @Controller
  static class AmbiguousController {

    public void handle() { }

    @ExceptionHandler({ BindException.class, IllegalArgumentException.class })
    public String handle1(Exception ex) {
      return ClassUtils.getShortName(ex.getClass());
    }

    @ExceptionHandler
    public String handle2(IllegalArgumentException ex) {
      return ClassUtils.getShortName(ex.getClass());
    }
  }

  @Controller
  static class NoExceptionController {

    @ExceptionHandler
    public void handle() {
    }
  }

  @Controller
  static class MediaTypeController {

    @ExceptionHandler(exception = { IllegalArgumentException.class }, produces = "application/json")
    public void handleJson() {

    }

    @ExceptionHandler(exception = { IllegalArgumentException.class }, produces = { "text/html", "*/*" })
    public void handleHtml() {

    }

  }

  @Controller
  static class AmbiguousMediaTypeController {

    @ExceptionHandler(exception = { IllegalArgumentException.class }, produces = "application/json")
    public void handleJson() {

    }

    @ExceptionHandler(exception = { IllegalArgumentException.class }, produces = "application/json")
    public void handleJsonToo() {

    }

  }

  @Controller
  static class MixedController {

    @ExceptionHandler(exception = { IllegalArgumentException.class }, produces = "application/json")
    public void handleJson() {

    }

    @ExceptionHandler(IllegalArgumentException.class)
    public void handleOther() {

    }

  }

  @Controller
  static class InvalidMediaTypeController {

    @ExceptionHandler(exception = { IllegalArgumentException.class }, produces = "invalid-mediatype")
    public void handle() {

    }
  }

}
