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

package cn.taketoday.framework.test.mock.mockito;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.framework.test.context.ApplicationTest;
import cn.taketoday.framework.test.context.InfraApplicationTestContextBootstrapper;
import cn.taketoday.test.context.BootstrapContext;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.cache.DefaultCacheAwareContextLoaderDelegate;
import cn.taketoday.test.context.cache.DefaultContextCache;
import cn.taketoday.test.util.ReflectionTestUtils;

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
    InfraApplicationTestContextBootstrapper bootstrapper = new InfraApplicationTestContextBootstrapper();
    BootstrapContext bootstrapContext = mock(BootstrapContext.class);
    given((Class) bootstrapContext.getTestClass()).willReturn(testClass);
    bootstrapper.setBootstrapContext(bootstrapContext);
    given(bootstrapContext.getCacheAwareContextLoaderDelegate()).willReturn(this.delegate);
    TestContext testContext = bootstrapper.buildTestContext();
    testContext.getApplicationContext();
  }

  @ApplicationTest(classes = TestConfiguration.class)
  static class TestClass {

  }

  @ApplicationTest(classes = TestConfiguration.class)
  static class MockedBeanTestClass {

    @MockBean
    private TestBean testBean;

  }

  @ApplicationTest(classes = TestConfiguration.class)
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
