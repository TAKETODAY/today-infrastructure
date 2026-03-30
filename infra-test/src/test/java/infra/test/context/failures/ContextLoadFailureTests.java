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

package infra.test.context.failures;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

import infra.beans.BeanInstantiationException;
import infra.beans.factory.BeanCreationException;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.support.GenericApplicationContext;
import infra.test.context.failures.TrackingApplicationContextFailureProcessor.LoadFailure;
import infra.test.context.junit.jupiter.FailingTestCase;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Tests for failures that occur while loading an {@link ApplicationContext}.
 *
 * @author Sam Brannen
 * @since 5.0
 */
class ContextLoadFailureTests {

  @BeforeEach
  @AfterEach
  void clearFailures() {
    TrackingApplicationContextFailureProcessor.loadFailures.clear();
  }

  @Test
  void customBootstrapperAppliesApplicationContextFailureProcessor() {
    assertThat(TrackingApplicationContextFailureProcessor.loadFailures).isEmpty();

    EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(ExplosiveContextTestCase.class))//
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.started(1).succeeded(0).failed(1));

    assertThat(TrackingApplicationContextFailureProcessor.loadFailures).hasSize(1);
    LoadFailure loadFailure = TrackingApplicationContextFailureProcessor.loadFailures.get(0);
    assertThat(loadFailure.context()).isExactlyInstanceOf(GenericApplicationContext.class);
    assertThat(loadFailure.exception())
            .isInstanceOf(BeanCreationException.class)
            .cause().isInstanceOf(BeanInstantiationException.class)
            .rootCause().isInstanceOf(StackOverflowError.class).hasMessage("Boom!");
  }

  @FailingTestCase
  @JUnitConfig
  static class ExplosiveContextTestCase {

    @Test
    void test1() {
      /* no-op */
    }

    @Configuration(proxyBeanMethods = false)
    static class Config {

      @Bean
      String explosion() {
        throw new StackOverflowError("Boom!");
      }
    }
  }

}
