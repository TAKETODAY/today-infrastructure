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

package cn.taketoday.test.context.junit4.rules;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.TimeUnit;

import cn.taketoday.test.annotation.Repeat;
import cn.taketoday.test.context.junit4.TimedTransactionalRunnerTests;
import cn.taketoday.test.transaction.TransactionAssert;

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
