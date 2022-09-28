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

package cn.taketoday.test.context.junit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import cn.taketoday.test.annotation.Repeat;
import cn.taketoday.test.annotation.Timed;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.transaction.TransactionAssert;
import cn.taketoday.transaction.annotation.Propagation;
import cn.taketoday.transaction.annotation.Transactional;

/**
 * JUnit 4 based integration test which verifies support of Spring's
 * {@link Transactional &#64;Transactional} annotation in conjunction
 * with {@link Timed &#64;Timed} and JUnit 4's {@link Test#timeout()
 * timeout} attribute.
 *
 * @author Sam Brannen
 * @see cn.taketoday.test.context.junit.jupiter.transaction.TimedTransactionalSpringExtensionTests
 * @since 4.0
 */
@RunWith(InfraRunner.class)
@ContextConfiguration("transactionalTests-context.xml")
@Transactional
public class TimedTransactionalSpringRunnerTests {

  @Test
  @Timed(millis = 10000)
  @Repeat(5)
  public void transactionalWithSpringTimeout() {
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
  public void notTransactionalWithSpringTimeout() {
    TransactionAssert.assertThatTransaction().isNotActive();
  }

  @Test(timeout = 10000)
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @Repeat(5)
  public void notTransactionalWithJUnitTimeout() {
    TransactionAssert.assertThatTransaction().isNotActive();
  }

}
