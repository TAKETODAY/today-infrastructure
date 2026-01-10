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

package infra.test.context.junit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import infra.test.annotation.Repeat;
import infra.test.annotation.Timed;
import infra.test.context.ContextConfiguration;
import infra.test.transaction.TransactionAssert;
import infra.transaction.annotation.Propagation;
import infra.transaction.annotation.Transactional;

/**
 * JUnit 4 based integration test which verifies support of Infra
 * {@link Transactional &#64;Transactional} annotation in conjunction
 * with {@link Timed &#64;Timed} and JUnit 4's {@link Test#timeout()
 * timeout} attribute.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(InfraRunner.class)
@ContextConfiguration("transactionalTests-context.xml")
@Transactional
public class TimedTransactionalRunnerTests {

  @Test
  @Timed(millis = 10000)
  @Repeat(5)
  public void transactionalWithInfraTimeout() {
    TransactionAssert.assertThatTransaction().isActive();
  }

  @Test(timeout = 10000)
  @Repeat(5)
  public void transactionalWithJUnitTimeout() {
    TransactionAssert.assertThatTransaction().isActive();
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @Timed(millis = 10000)
  @Repeat(5)
  public void notTransactionalWithInfraTimeout() {
    TransactionAssert.assertThatTransaction().isNotActive();
  }

  @Test(timeout = 10000)
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @Repeat(5)
  public void notTransactionalWithJUnitTimeout() {
    TransactionAssert.assertThatTransaction().isNotActive();
  }

}
