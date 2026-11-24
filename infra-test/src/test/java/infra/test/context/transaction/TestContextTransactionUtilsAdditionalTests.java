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

package infra.test.context.transaction;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import javax.sql.DataSource;

import infra.context.support.GenericApplicationContext;
import infra.test.context.TestContext;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.interceptor.TransactionAttribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

class TestContextTransactionUtilsAdditionalTests {

  @Test
  void dataSourcesAreResolvedByNameByTypeAndDefaultName() {
    DataSource named = mock(DataSource.class);
    try (var context = context("namedDataSource", named)) {
      TestContext testContext = testContext(context);
      assertThat(TestContextTransactionUtils.retrieveDataSource(testContext, "namedDataSource")).isSameAs(named);
    }

    DataSource single = mock(DataSource.class);
    try (var context = context("single", single)) {
      assertThat(TestContextTransactionUtils.retrieveDataSource(testContext(context), null)).isSameAs(single);
    }

    DataSource defaultDataSource = mock(DataSource.class);
    try (var context = context("dataSource", defaultDataSource)) {
      context.getBeanFactory().registerSingleton("otherDataSource", mock(DataSource.class));
      assertThat(TestContextTransactionUtils.retrieveDataSource(testContext(context), null))
              .isSameAs(defaultDataSource);
    }

    try (var context = new GenericApplicationContext()) {
      context.refresh();
      assertThat(TestContextTransactionUtils.retrieveDataSource(testContext(context), null)).isNull();
    }
  }

  @Test
  void transactionManagersAreResolvedByNameByTypeAndDefaultName() {
    PlatformTransactionManager named = mock(PlatformTransactionManager.class);
    try (var context = context("namedManager", named)) {
      TestContext testContext = testContext(context);
      assertThat(TestContextTransactionUtils.retrieveTransactionManager(testContext, "namedManager"))
              .isSameAs(named);
    }

    PlatformTransactionManager single = mock(PlatformTransactionManager.class);
    try (var context = context("singleManager", single)) {
      assertThat(TestContextTransactionUtils.retrieveTransactionManager(testContext(context), null))
              .isSameAs(single);
    }

    PlatformTransactionManager defaultManager = mock(PlatformTransactionManager.class);
    try (var context = context("transactionManager", defaultManager)) {
      context.getBeanFactory().registerSingleton("otherManager", mock(PlatformTransactionManager.class));
      assertThat(TestContextTransactionUtils.retrieveTransactionManager(testContext(context), null))
              .isSameAs(defaultManager);
    }

    try (var context = new GenericApplicationContext()) {
      context.refresh();
      assertThat(TestContextTransactionUtils.retrieveTransactionManager(testContext(context), null)).isNull();
    }
  }

  @Test
  void transactionAttributeNamesCanIncludeOrExcludeTheMethod() throws Exception {
    TestContext testContext = mock(TestContext.class);
    Method method = getClass().getDeclaredMethod("transactionAttributeNamesCanIncludeOrExcludeTheMethod");
    doReturn(TestContextTransactionUtilsAdditionalTests.class).when(testContext).getTestClass();
    when(testContext.getTestMethod()).thenReturn(method);
    TransactionAttribute target = mock(TransactionAttribute.class);

    assertThat(TestContextTransactionUtils.createDelegatingTransactionAttribute(testContext, target).getName())
            .isEqualTo(getClass().getName() + ".transactionAttributeNamesCanIncludeOrExcludeTheMethod");
    assertThat(TestContextTransactionUtils.createDelegatingTransactionAttribute(testContext, target, false).getName())
            .isEqualTo(getClass().getName());
    assertThatIllegalArgumentException()
            .isThrownBy(() -> TestContextTransactionUtils.createDelegatingTransactionAttribute(null, target));
    assertThatIllegalArgumentException()
            .isThrownBy(() -> TestContextTransactionUtils.createDelegatingTransactionAttribute(testContext, null));
  }

  private GenericApplicationContext context(String name, Object bean) {
    var context = new GenericApplicationContext();
    context.registerSingleton(name, bean);
    context.refresh();
    return context;
  }

  private TestContext testContext(GenericApplicationContext context) {
    TestContext testContext = mock(TestContext.class);
    when(testContext.getApplicationContext()).thenReturn(context);
    return testContext;
  }

}
