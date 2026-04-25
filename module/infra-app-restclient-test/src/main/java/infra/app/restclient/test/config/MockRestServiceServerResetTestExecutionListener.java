/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.restclient.test.config;

import infra.context.ApplicationContext;
import infra.core.Ordered;
import infra.test.context.TestContext;
import infra.test.context.TestExecutionListener;
import infra.test.context.support.AbstractTestExecutionListener;
import infra.test.web.client.MockRestServiceServer;

/**
 * {@link TestExecutionListener} to reset {@link MockRestServiceServer} beans.
 *
 * @author Phillip Webb
 */
class MockRestServiceServerResetTestExecutionListener extends AbstractTestExecutionListener {

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE - 100;
  }

  @Override
  public void afterTestMethod(TestContext testContext) throws Exception {
    ApplicationContext applicationContext = testContext.getApplicationContext();
    String[] names = applicationContext.getBeanNamesForType(MockRestServiceServer.class, false, false);
    for (String name : names) {
      applicationContext.getBean(name, MockRestServiceServer.class).reset();
    }
  }

}
