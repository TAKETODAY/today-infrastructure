/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.test.context.bootstrap;

import org.junit.jupiter.api.Test;

import cn.taketoday.framework.test.context.InfraTest;
import cn.taketoday.framework.test.context.InfraTest.WebEnvironment;
import cn.taketoday.framework.test.context.InfraTestContextBootstrapper;
import cn.taketoday.test.context.BootstrapContext;
import cn.taketoday.test.context.CacheAwareContextLoaderDelegate;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link InfraTestContextBootstrapper}.
 *
 * @author Andy Wilkinson
 */
class InfraTestContextBootstrapperTests {

  @Test
  void infraTestWithANonMockWebEnvironmentAndWebAppConfigurationFailsFast() {
    assertThatIllegalStateException()
            .isThrownBy(() -> buildTestContext(InfraTestNonMockWebEnvironmentAndWebAppConfiguration.class))
            .withMessageContaining("@WebAppConfiguration should only be used with "
                    + "@InfraTest when @InfraTest is configured with a mock web "
                    + "environment. Please remove @WebAppConfiguration or reconfigure @InfraTest.");
  }

  @Test
  void InfraTestWithAMockWebEnvironmentCanBeUsedWithWebAppConfiguration() {
    buildTestContext(InfraTestMockWebEnvironmentAndWebAppConfiguration.class);
  }

  @Test
  void mergedContextConfigurationWhenArgsDifferentShouldNotBeConsideredEqual() {
    TestContext context = buildTestContext(InfraTestArgsConfiguration.class);
    MergedContextConfiguration contextConfiguration = getMergedContextConfiguration(context);
    TestContext otherContext2 = buildTestContext(InfraTestOtherArgsConfiguration.class);
    MergedContextConfiguration otherContextConfiguration = getMergedContextConfiguration(otherContext2);
    assertThat(contextConfiguration).isNotEqualTo(otherContextConfiguration);
  }

  @Test
  void mergedContextConfigurationWhenArgsSameShouldBeConsideredEqual() {
    TestContext context = buildTestContext(InfraTestArgsConfiguration.class);
    MergedContextConfiguration contextConfiguration = getMergedContextConfiguration(context);
    TestContext otherContext2 = buildTestContext(InfraTestSameArgsConfiguration.class);
    MergedContextConfiguration otherContextConfiguration = getMergedContextConfiguration(otherContext2);
    assertThat(contextConfiguration).isEqualTo(otherContextConfiguration);
  }

  @Test
  void mergedContextConfigurationWhenWebEnvironmentsDifferentShouldNotBeConsideredEqual() {
    TestContext context = buildTestContext(InfraTestMockWebEnvironmentConfiguration.class);
    MergedContextConfiguration contextConfiguration = getMergedContextConfiguration(context);
    TestContext otherContext = buildTestContext(InfraTestDefinedPortWebEnvironmentConfiguration.class);
    MergedContextConfiguration otherContextConfiguration = getMergedContextConfiguration(otherContext);
    assertThat(contextConfiguration).isNotEqualTo(otherContextConfiguration);
  }

  @Test
  void mergedContextConfigurationWhenWebEnvironmentsSameShouldBeConsideredEqual() {
    TestContext context = buildTestContext(InfraTestMockWebEnvironmentConfiguration.class);
    MergedContextConfiguration contextConfiguration = getMergedContextConfiguration(context);
    TestContext otherContext = buildTestContext(InfraTestAnotherMockWebEnvironmentConfiguration.class);
    MergedContextConfiguration otherContextConfiguration = getMergedContextConfiguration(otherContext);
    assertThat(contextConfiguration).isEqualTo(otherContextConfiguration);
  }

  @Test
  void mergedContextConfigurationClassesShouldNotContainDuplicates() {
    TestContext context = buildTestContext(InfraTestClassesConfiguration.class);
    MergedContextConfiguration contextConfiguration = getMergedContextConfiguration(context);
    Class<?>[] classes = contextConfiguration.getClasses();
    assertThat(classes).containsExactly(InfraTestContextBootstrapperExampleConfig.class);
  }

  @SuppressWarnings("rawtypes")
  private TestContext buildTestContext(Class<?> testClass) {
    InfraTestContextBootstrapper bootstrapper = new InfraTestContextBootstrapper();
    BootstrapContext bootstrapContext = mock(BootstrapContext.class);
    bootstrapper.setBootstrapContext(bootstrapContext);
    given((Class) bootstrapContext.getTestClass()).willReturn(testClass);
    CacheAwareContextLoaderDelegate contextLoaderDelegate = mock(CacheAwareContextLoaderDelegate.class);
    given(bootstrapContext.getCacheAwareContextLoaderDelegate()).willReturn(contextLoaderDelegate);
    return bootstrapper.buildTestContext();
  }

  private MergedContextConfiguration getMergedContextConfiguration(TestContext context) {
    return (MergedContextConfiguration) ReflectionTestUtils.getField(context, "mergedConfig");
  }

  @InfraTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @WebAppConfiguration
  static class InfraTestNonMockWebEnvironmentAndWebAppConfiguration {

  }

  @InfraTest
  @WebAppConfiguration
  static class InfraTestMockWebEnvironmentAndWebAppConfiguration {

  }

  @InfraTest(args = "--app.test=same")
  static class InfraTestArgsConfiguration {

  }

  @InfraTest(webEnvironment = WebEnvironment.MOCK)
  static class InfraTestMockWebEnvironmentConfiguration {

  }

  @InfraTest(webEnvironment = WebEnvironment.MOCK)
  static class InfraTestAnotherMockWebEnvironmentConfiguration {

  }

  @InfraTest(webEnvironment = WebEnvironment.DEFINED_PORT)
  static class InfraTestDefinedPortWebEnvironmentConfiguration {

  }

  @InfraTest(args = "--app.test=same")
  static class InfraTestSameArgsConfiguration {

  }

  @InfraTest(args = "--app.test=different")
  static class InfraTestOtherArgsConfiguration {

  }

  @InfraTest(classes = InfraTestContextBootstrapperExampleConfig.class)
  static class InfraTestClassesConfiguration {

  }

}
