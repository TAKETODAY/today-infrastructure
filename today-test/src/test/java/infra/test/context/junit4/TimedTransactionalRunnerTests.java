/*
 * Copyright 2017 - 2026 the original author or authors.
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
