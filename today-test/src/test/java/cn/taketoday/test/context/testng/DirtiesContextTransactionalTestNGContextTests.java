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

package cn.taketoday.test.context.testng;

import org.testng.annotations.Test;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.TestContextManager;
import cn.taketoday.test.transaction.TransactionAssert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>
 * TestNG based integration test to assess the claim in <a
 * href="https://opensource.atlassian.com/projects/spring/browse/SPR-3880"
 * target="_blank">SPR-3880</a> that a &quot;context marked dirty using
 * {@link DirtiesContext &#064;DirtiesContext} in [a] TestNG based test is not
 * reloaded in subsequent tests&quot;.
 * </p>
 * <p>
 * After careful analysis, it turns out that the {@link ApplicationContext} was
 * in fact reloaded; however, due to how the test instance was instrumented with
 * the {@link TestContextManager} in {@link AbstractTestNGContextTests},
 * dependency injection was not being performed on the test instance between
 * individual tests. DirtiesContextTransactionalTestNGSpringContextTests
 * therefore verifies the expected behavior and correct semantics.
 * </p>
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ContextConfiguration
public class DirtiesContextTransactionalTestNGContextTests extends AbstractTransactionalTestNGContextTests {

  private ApplicationContext dirtiedApplicationContext;

  private void performCommonAssertions() {
    TransactionAssert.assertThatTransaction().isActive();
    assertThat(super.applicationContext)
            .as("The application context should have been set due to ApplicationContextAware semantics.")
            .isNotNull();
    assertThat(super.jdbcTemplate)
            .as("The JdbcTemplate should have been created in setDataSource() via DI for the DataSource.")
            .isNotNull();
  }

  @Test
  @DirtiesContext
  public void dirtyContext() {
    performCommonAssertions();
    this.dirtiedApplicationContext = super.applicationContext;
  }

  @Test(dependsOnMethods = { "dirtyContext" })
  public void verifyContextWasDirtied() {
    performCommonAssertions();
    assertThat(super.applicationContext)
            .as("The application context should have been 'dirtied'.")
            .isNotSameAs(this.dirtiedApplicationContext);
    this.dirtiedApplicationContext = super.applicationContext;
  }

  @Test(dependsOnMethods = { "verifyContextWasDirtied" })
  public void verifyContextWasNotDirtied() {
    assertThat(this.applicationContext)
            .as("The application context should NOT have been 'dirtied'.")
            .isSameAs(this.dirtiedApplicationContext);
  }

}
