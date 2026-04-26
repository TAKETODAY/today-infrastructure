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

package infra.app.webmvc.test.config;

import infra.core.Ordered;
import infra.test.context.TestContext;
import infra.test.context.TestExecutionListener;
import infra.test.context.support.AbstractTestExecutionListener;
import infra.app.webmvc.test.config.InfraMockMvcBuilderCustomizer.DeferredLinesWriter;

/**
 * {@link TestExecutionListener} used to print MVC lines only on failure.
 *
 * @author Phillip Webb
 */
class MockMvcPrintOnlyOnFailureTestExecutionListener extends AbstractTestExecutionListener {

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE - 100;
  }

  @Override
  public void afterTestMethod(TestContext testContext) throws Exception {
    DeferredLinesWriter writer = DeferredLinesWriter.get(testContext.getApplicationContext());
    if (writer != null) {
      if (testContext.getTestException() != null) {
        writer.writeDeferredResult();
      }
      writer.clear();
    }

  }

}
