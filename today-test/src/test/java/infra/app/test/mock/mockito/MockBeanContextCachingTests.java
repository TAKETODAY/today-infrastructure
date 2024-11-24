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
