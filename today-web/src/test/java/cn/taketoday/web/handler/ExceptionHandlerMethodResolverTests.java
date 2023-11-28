/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.handler;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.BindException;
import java.net.SocketException;

import cn.taketoday.util.ClassUtils;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.handler.method.ExceptionHandlerMethodResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 22:44
 */
class ExceptionHandlerMethodResolverTests {

  @Test
  public void resolveMethodFromAnnotation() {
    ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(ExceptionController.class);
    IOException exception = new IOException();
    assertThat(resolver.resolveMethod(exception).getName()).isEqualTo("handleIOException");
  }

  @Test
  public void resolveMethodFromArgument() {
    ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(ExceptionController.class);
    IllegalArgumentException exception = new IllegalArgumentException();
    assertThat(resolver.resolveMethod(exception).getName()).isEqualTo("handleIllegalArgumentException");
  }

  @Test
  public void resolveMethodExceptionSubType() {
    ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(ExceptionController.class);
    IOException ioException = new FileNotFoundException();
    assertThat(resolver.resolveMethod(ioException).getName()).isEqualTo("handleIOException");
    SocketException bindException = new BindException();
    assertThat(resolver.resolveMethod(bindException).getName()).isEqualTo("handleSocketException");
  }

  @Test
  public void resolveMethodBestMatch() {
    ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(ExceptionController.class);
    SocketException exception = new SocketException();
    assertThat(resolver.resolveMethod(exception).getName()).isEqualTo("handleSocketException");
  }

  @Test
  public void resolveMethodNoMatch() {
    ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(ExceptionController.class);
    Exception exception = new Exception();
    assertThat(resolver.resolveMethod(exception)).as("1st lookup").isNull();
    assertThat(resolver.resolveMethod(exception)).as("2nd lookup from cache").isNull();
  }

  @Test
  public void resolveMethodExceptionCause() {
    ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(ExceptionController.class);

    SocketException bindException = new BindException();
    bindException.initCause(new FileNotFoundException());

    Exception exception = new Exception(new Exception(new Exception(bindException)));

    assertThat(resolver.resolveMethod(exception).getName()).isEqualTo("handleSocketException");
  }

  @Test
  public void resolveMethodInherited() {
    ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(InheritedController.class);
    IOException exception = new IOException();
    assertThat(resolver.resolveMethod(exception).getName()).isEqualTo("handleIOException");
  }

  @Test
  public void ambiguousExceptionMapping() {
    assertThatIllegalStateException().isThrownBy(() ->
            new ExceptionHandlerMethodResolver(AmbiguousController.class));
  }

  @Test
  public void noExceptionMapping() {
    assertThatIllegalStateException().isThrownBy(() ->
            new ExceptionHandlerMethodResolver(NoExceptionController.class));
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
    public String handle1(Exception ex, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
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

}
