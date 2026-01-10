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

package infra.app.test.mock.mockito;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import infra.app.test.context.InfraTest;
import infra.app.test.context.InfraTestContextBootstrapper;
import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.BootstrapContext;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.TestContext;
import infra.test.context.cache.DefaultCacheAwareContextLoaderDelegate;
import infra.test.context.cache.DefaultContextCache;
import infra.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for application context caching when using {@link MockBean @MockBean}.
 *
 * @author Andy Wilkinson
 */
class MockBeanContextCachingTests {

  private final DefaultContextCache contextCache = new DefaultContextCache();

  private final DefaultCacheAwareContextLoaderDelegate delegate = new DefaultCacheAwareContextLoaderDelegate(
          this.contextCache);

  @AfterEach
  @SuppressWarnings("unchecked")
  void clearCache() {
    Map<MergedContextConfiguration, ApplicationContext> contexts = (Map<MergedContextConfiguration, ApplicationContext>) ReflectionTestUtils
            .getField(this.contextCache, "contextMap");
    for (ApplicationContext context : contexts.values()) {
      if (context instanceof ConfigurableApplicationContext configurableContext) {
        configurableContext.close();
      }
    }
    this.contextCache.clear();
  }

  @Test
  void whenThereIsANormalBeanAndAMockBeanThenTwoContextsAreCreated() {
    bootstrapContext(TestClass.class);
    assertThat(this.contextCache.size()).isEqualTo(1);
    bootstrapContext(MockedBeanTestClass.class);
    assertThat(this.contextCache.size()).isEqualTo(2);
  }

  @Test
  void whenThereIsTheSameMockedBeanInEachTestClassThenOneContextIsCreated() {
    bootstrapContext(MockedBeanTestClass.class);
    assertThat(this.contextCache.size()).isEqualTo(1);
    bootstrapContext(AnotherMockedBeanTestClass.class);
    assertThat(this.contextCache.size()).isEqualTo(1);
  }

  @SuppressWarnings("rawtypes")
  private void bootstrapContext(Class<?> testClass) {
    InfraTestContextBootstrapper bootstrapper = new InfraTestContextBootstrapper();
    BootstrapContext bootstrapContext = mock(BootstrapContext.class);
    given((Class) bootstrapContext.getTestClass()).willReturn(testClass);
    bootstrapper.setBootstrapContext(bootstrapContext);
    given(bootstrapContext.getCacheAwareContextLoaderDelegate()).willReturn(this.delegate);
    TestContext testContext = bootstrapper.buildTestContext();
    testContext.getApplicationContext();
  }

  @InfraTest(classes = TestConfiguration.class)
  static class TestClass {

  }

  @InfraTest(classes = TestConfiguration.class)
  static class MockedBeanTestClass {

    @MockBean
    private TestBean testBean;

  }

  @InfraTest(classes = TestConfiguration.class)
  static class AnotherMockedBeanTestClass {

    @MockBean
    private TestBean testBean;

  }

  @Configuration
  static class TestConfiguration {

    @Bean
    TestBean testBean() {
      return new TestBean();
    }

  }

  static class TestBean {

  }

}
