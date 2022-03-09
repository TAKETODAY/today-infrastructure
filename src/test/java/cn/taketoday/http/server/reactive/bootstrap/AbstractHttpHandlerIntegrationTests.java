/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.http.server.reactive.bootstrap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.stream.Stream;

import cn.taketoday.http.server.reactive.HttpHandler;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.client.HttpServerErrorException;
import reactor.core.publisher.Flux;

@Execution(ExecutionMode.SAME_THREAD)
public abstract class AbstractHttpHandlerIntegrationTests {

  /**
   * Custom JUnit Jupiter extension that handles exceptions thrown by test methods.
   *
   * <p>If the test method throws an {@link HttpServerErrorException}, this
   * extension will throw an {@link AssertionError} that wraps the
   * {@code HttpServerErrorException} using the
   * {@link HttpServerErrorException#getResponseBodyAsString() response body}
   * as the failure message.
   *
   * <p>This mechanism provides an actually meaningful failure message if the
   * test fails due to an {@code AssertionError} on the server.
   */
  @RegisterExtension
  TestExecutionExceptionHandler serverErrorToAssertionErrorConverter = (context, throwable) -> {
    if (throwable instanceof HttpServerErrorException ex) {
      String responseBody = ex.getResponseBodyAsString();
      if (StringUtils.hasText(responseBody)) {
        String prefix = AssertionError.class.getName() + ": ";
        if (responseBody.startsWith(prefix)) {
          responseBody = responseBody.substring(prefix.length());
        }
        throw new AssertionError(responseBody, ex);
      }
    }
    // Else throw as-is in order to comply with the contract of TestExecutionExceptionHandler.
    throw throwable;
  };

  protected HttpServer server;

  protected int port;

  protected void startServer(HttpServer httpServer) throws Exception {
    this.server = httpServer;
    this.server.setHandler(createHttpHandler());
    this.server.afterPropertiesSet();
    this.server.start();

    // Set dynamically chosen port
    this.port = this.server.getPort();
  }

  @AfterEach
  void stopServer() {
    if (this.server != null) {
      this.server.stop();
      this.port = 0;
    }
  }

  protected abstract HttpHandler createHttpHandler();

  /**
   * Return an interval stream of N number of ticks and buffer the emissions
   * to avoid back pressure failures (e.g. on slow CI server).
   *
   * <p>Use this method as follows:
   * <ul>
   * <li>Tests that verify N number of items followed by verifyOnComplete()
   * should set the number of emissions to N.
   * <li>Tests that verify N number of items followed by thenCancel() should
   * set the number of buffered to an arbitrary number greater than N.
   * </ul>
   */
  public static Flux<Long> testInterval(Duration period, int count) {
    return Flux.interval(period).take(count).onBackpressureBuffer(count);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("cn.taketoday.http.server.reactive.bootstrap.AbstractHttpHandlerIntegrationTests#httpServers()")
  // public for Kotlin
  public @interface ParameterizedHttpServerTest {
  }

  static Stream<HttpServer> httpServers() {
    return Stream.of(
            new ReactorHttpServer(),
            new JettyHttpServer(),
            new TomcatHttpServer(),
            new UndertowHttpServer()
    );
  }

}
