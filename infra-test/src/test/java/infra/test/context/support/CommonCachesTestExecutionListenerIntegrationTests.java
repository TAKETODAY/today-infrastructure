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

package infra.test.context.support;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.ClassPathScanningCandidateComponentProvider;
import infra.context.annotation.Configuration;
import infra.context.annotation.Lazy;
import infra.context.support.AbstractApplicationContext;
import infra.core.io.ResourceLoader;
import infra.core.type.classreading.MetadataReader;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.support.samples.SampleComponent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that verify that common caches are cleared at the end of a test
 * class. Regular callbacks cannot be used to validate this as they run
 * before the listener, so we need two test classes that are ordered to
 * validate the result.
 *
 * @author Stephane Nicoll
 */
@JUnitConfig
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class CommonCachesTestExecutionListenerIntegrationTests {

  @Autowired
  AbstractApplicationContext applicationContext;

  @Nested
  @Order(1)
  class FirstTests {

    @Test
    void lazyInitBeans() {
      assertThat(applicationContext.getBean(String.class)).isEqualTo("Dummy");
      assertThat(applicationContext.getResourceCache(MetadataReader.class)).isNotEmpty();
    }

  }

  @Nested
  @Order(2)
  class SecondTests {

    @Test
    void validateCommonCacheIsCleared() {
      assertThat(applicationContext.getResourceCache(MetadataReader.class)).isEmpty();
    }

  }

  @Configuration
  static class TestConfiguration {

    @Bean
    @Lazy
    String dummyBean(ResourceLoader resourceLoader) {
      ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
      scanner.setResourceLoader(resourceLoader);
      scanner.findCandidateComponents(SampleComponent.class.getPackageName());
      return "Dummy";
    }
  }

}
