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

package infra.test.context.junit4.rules;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.TimeUnit;

import infra.test.annotation.Repeat;
import infra.test.context.junit4.TimedTransactionalRunnerTests;
import infra.test.transaction.TransactionAssert;

/**
 * This class is an extension of {@link TimedTransactionalRunnerTests}
 * that has been modified to use {@link InfraClassRule} and
 * {@link InfraMethodRule}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(JUnit4.class)
public class TimedTransactionalRuleTests extends TimedTransactionalRunnerTests {

  @ClassRule
  public static final InfraClassRule applicationClassRule = new InfraClassRule();

  @Rule
  public final InfraMethodRule infraMethodRule = new InfraMethodRule();

  @Rule
  public Timeout timeout = Timeout.builder().withTimeout(10, TimeUnit.SECONDS).build();

  /**
   * Overridden since Infra Rule-based JUnit support cannot properly
   * integrate with timed execution that is controlled by a third-party runner.
   */
  @Test(timeout = 10000)
  @Repeat(5)
  @Override
  public void transactionalWithJUnitTimeout() {
    TransactionAssert.assertThatTransaction().isNotActive();
  }

  /**
   * {@code timeout} explicitly not declared due to presence of Timeout rule.
   */
  @Test
  public void transactionalWithJUnitRuleBasedTimeout() {
    TransactionAssert.assertThatTransaction().isActive();
  }

  // All other tests are in superclass.

}
