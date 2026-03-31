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

package infra.test.context.bean.override.mockito.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.Primary;
import infra.stereotype.Component;
import infra.test.context.bean.override.mockito.MockitoBean;
import infra.test.context.junit.jupiter.InfraExtension;

import static infra.test.mockito.MockitoAssertions.assertIsMock;
import static infra.test.mockito.MockitoAssertions.assertIsNotMock;
import static org.mockito.BDDMockito.then;

/**
 * Tests that {@link MockitoBean @MockitoBean} can be used to mock a bean when
 * there are multiple candidates; one is primary; and the field name matches
 * the name of a candidate which is not the primary candidate.
 *
 * @author Sam Brannen
 * @see MockitoBeanWithMultipleExistingBeansAndOnePrimaryIntegrationTests
 * @see MockitoBeanWithMultipleExistingBeansAndExplicitBeanNameIntegrationTests
 * @see MockitoBeanWithMultipleExistingBeansAndExplicitQualifierIntegrationTests
 * @since 5.0
 */
@ExtendWith(InfraExtension.class)
class MockitoBeanWithMultipleExistingBeansAndOnePrimaryAndOneConflictingQualifierIntegrationTests {

  // The name of this field must be "baseService" to match the name of the non-primary candidate.
  @MockitoBean
  BaseService baseService;

  @Autowired
  Client client;

  @Test
    // gh-34374
  void test(ApplicationContext context) {
    assertIsMock(baseService, "baseService field");
    assertIsMock(context.getBean("extendedService"), "extendedService bean");
    assertIsNotMock(context.getBean("baseService"), "baseService bean");

    client.callService();

    then(baseService).should().doSomething();
  }

  @Configuration(proxyBeanMethods = false)
  @Import({ BaseService.class, ExtendedService.class, Client.class })
  static class Config {
  }

  @Component("baseService")
  static class BaseService {

    public void doSomething() {
    }
  }

  @Primary
  @Component("extendedService")
  static class ExtendedService extends BaseService {
  }

  @Component("client")
  static class Client {

    private final BaseService baseService;

    public Client(BaseService baseService) {
      this.baseService = baseService;
    }

    public void callService() {
      this.baseService.doSomething();
    }
  }

}
