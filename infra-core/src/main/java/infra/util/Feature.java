/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.util;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A {@code Feature} is an immutable descriptor for a detectable module or library on
 * the classpath.
 * <p>
 * A {@code Feature} only carries the fully qualified name of its indicator class.
 * Presence detection and caching are performed by {@link FeatureDetector}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public final class Feature {

  /** infra-app: the core application runtime. */
  public static final Feature APP = Feature.of("infra.app.Application");

  /** infra-context: the application context and event infrastructure. */
  public static final Feature CONTEXT = Feature.of("infra.context.ApplicationContext");

  /** infra-beans: the bean factory and bean definition infrastructure. */
  public static final Feature BEANS = Feature.of("infra.beans.BeansException");

  /** infra-aop: aspect-oriented programming support. */
  public static final Feature AOP = Feature.of("infra.aop.Advisor");

  /** infra-expression: the SpEL-like expression language. */
  public static final Feature EXPRESSION = Feature.of("infra.expression.ExpressionParser");

  /** infra-jdbc: JDBC support and {@code JdbcTemplate}. */
  public static final Feature JDBC = Feature.of("infra.jdbc.Query");

  /** infra-tx: transaction management support. */
  public static final Feature TX = Feature.of("infra.transaction.TransactionManager");

  /** infra-http: the HTTP abstraction layer. */
  public static final Feature HTTP = Feature.of("infra.http.HttpMethod");

  /** infra-web: the web foundation shared by MVC and reactive stacks. */
  public static final Feature WEB = Feature.of("infra.web.ErrorResponse");

  /** infra-webmvc: the web MVC stack. */
  public static final Feature WEB_MVC = Feature.of("infra.web.HttpContext");

  /** infra-web-reactive: the reactive web stack. */
  public static final Feature WEB_REACTIVE = Feature.of("infra.web.reactive.client.WebClient");

  /** infra-websocket: WebSocket support. */
  public static final Feature WEB_SOCKET = Feature.of("infra.web.socket.WebSocketSession");

  /** infra-http-service: HTTP interface / declarative HTTP client support. */
  public static final Feature HTTP_SERVICE = Feature.of("infra.http.service.annotation.HttpExchange");

  /** Reactive Streams API. */
  public static final Feature REACTIVE_STREAMS = Feature.of("org.reactivestreams.Publisher");

  /** Project Reactor. */
  public static final Feature REACTOR = Feature.of("reactor.core.publisher.Flux");

  /** Jackson (databind). */
  public static final Feature JACKSON = Feature.of("tools.jackson.databind.ObjectMapper");

  /** Gson. */
  public static final Feature GSON = Feature.of("com.google.gson.Gson");

  /** Mockito. */
  public static final Feature MOCKITO = Feature.of("org.mockito.Mockito");

  /** AspectJ. */
  public static final Feature ASPECTJ = Feature.of("org.aspectj.lang.annotation.Aspect");

  private final String indicatorClassName;

  volatile @Nullable Boolean present;

  private Feature(String indicatorClassName) {
    this.indicatorClassName = indicatorClassName;
  }

  /**
   * Create a feature for the given indicator class.
   *
   * @param indicatorClassName the fully qualified name of the indicator class
   * @return a new feature
   */
  public static Feature of(String indicatorClassName) {
    return new Feature(indicatorClassName);
  }

  /**
   * Return the fully qualified name of the indicator class used for detection.
   *
   * @return the indicator class name
   */
  public String indicatorClassName() {
    return this.indicatorClassName;
  }

  @Override
  public String toString() {
    return this.indicatorClassName;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (!(o instanceof Feature feature))
      return false;
    return Objects.equals(indicatorClassName, feature.indicatorClassName);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(indicatorClassName);
  }

}
