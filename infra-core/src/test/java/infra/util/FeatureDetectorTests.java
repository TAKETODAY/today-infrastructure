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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FeatureDetector}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
class FeatureDetectorTests {

  @Test
  void coreFrameworkModulesArePresentOnTestClasspath() {
    assertThat(FeatureDetector.isPresent(Feature.APP)).isTrue();
    assertThat(FeatureDetector.isPresent(Feature.CONTEXT)).isTrue();
    assertThat(FeatureDetector.isPresent(Feature.BEANS)).isTrue();
  }

  @Test
  void detectionMatchesClassUtils() {
    assertDetected(Feature.APP, "infra.app.Application");
    assertDetected(Feature.CONTEXT, "infra.context.ApplicationContext");
    assertDetected(Feature.BEANS, "infra.beans.BeansException");
    assertDetected(Feature.AOP, "infra.aop.Advisor");
    assertDetected(Feature.EXPRESSION, "infra.expression.ExpressionParser");
    assertDetected(Feature.JDBC, "infra.jdbc.core.JdbcTemplate");
    assertDetected(Feature.TX, "infra.transaction.TransactionManager");
    assertDetected(Feature.HTTP, "infra.http.HttpMethod");
    assertDetected(Feature.WEB, "infra.web.ErrorResponse");
    assertDetected(Feature.WEB_MVC, "infra.web.DispatcherHandler");
    assertDetected(Feature.WEB_REACTIVE, "infra.web.reactive.client.WebClient");
    assertDetected(Feature.WEB_SOCKET, "infra.web.socket.WebSocketSession");
    assertDetected(Feature.HTTP_SERVICE, "infra.http.service.annotation.HttpExchange");
    assertDetected(Feature.REACTIVE_STREAMS, "org.reactivestreams.Publisher");
    assertDetected(Feature.REACTOR, "reactor.core.publisher.Flux");
    assertDetected(Feature.RX_JAVA_3, "io.reactivex.rxjava3.core.Flowable");
    assertDetected(Feature.MUTINY, "io.smallrye.mutiny.Multi");
    assertDetected(Feature.JACKSON, "tools.jackson.databind.ObjectMapper");
    assertDetected(Feature.GSON, "com.google.gson.Gson");
    assertDetected(Feature.MOCKITO, "org.mockito.Mockito");
    assertDetected(Feature.ASPECTJ, "org.aspectj.lang.annotation.Aspect");
  }

  @Test
  void featureExposesItsIndicatorClassName() {
    assertThat(Feature.APP.indicatorClassName()).isEqualTo("infra.app.Application");
    assertThat(Feature.REACTOR.indicatorClassName()).isEqualTo("reactor.core.publisher.Flux");
  }

  @Test
  void ofCreatesFeatureDescriptor() {
    Feature feature = Feature.of("com.example.DoesNotExist");
    assertThat(feature.indicatorClassName()).isEqualTo("com.example.DoesNotExist");
  }

  @Test
  void isPresentByFeatureMatchesClassName() {
    assertThat(FeatureDetector.isPresent(Feature.APP))
            .isEqualTo(ClassUtils.isPresent("infra.app.Application"));
    assertThat(FeatureDetector.isPresent(Feature.of("com.example.DoesNotExist"))).isFalse();
  }

  @Test
  void isMissingIsTheInverseOfIsPresent() {
    assertThat(FeatureDetector.isMissing(Feature.APP)).isFalse();
    assertThat(FeatureDetector.isMissing(Feature.of("com.example.DoesNotExist"))).isTrue();
    assertThat(FeatureDetector.isMissing(Feature.APP)).isEqualTo(!FeatureDetector.isPresent(Feature.APP));
  }

  private static void assertDetected(Feature feature, String className) {
    assertThat(FeatureDetector.isPresent(feature)).isEqualTo(ClassUtils.isPresent(className));
  }

}
