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

package cn.taketoday.test.context.testng.transaction.ejb;

import cn.taketoday.test.annotation.Rollback;
import cn.taketoday.test.context.transaction.TransactionalTestExecutionListener;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Extension of {@link CommitForRequiredEjbTxDaoTestNGTests} which sets the default
 * rollback semantics for the {@link TransactionalTestExecutionListener} to
 * {@code true}. The transaction managed by the TestContext framework will be
 * rolled back after each test method. Consequently, any work performed in
 * transactional methods that participate in the test-managed transaction will
 * be rolled back automatically.
 *
 * @author Sam Brannen
 * @since 4.0.1
 */
@Test(suiteName = "Rollback for REQUIRED")
@Rollback
public class RollbackForRequiredEjbTxDaoTestNGTests extends CommitForRequiredEjbTxDaoTestNGTests {

	/**
	 * Overrides parent implementation in order to change expectations to align with
	 * behavior associated with "required" transactions on repositories/DAOs and
	 * default rollback semantics for transactions managed by the TestContext
	 * framework.
	 */
	@Test(dependsOnMethods = "test2IncrementCount1")
	@Override
	public void test3IncrementCount2() {
		int count = dao.getCount(TEST_NAME);
		// Expecting count=0 after test2IncrementCount1() since REQUIRED transactions
		// participate in the existing transaction (if present), which in this case is the
		// transaction managed by the TestContext framework which will be rolled back
		// after each test method.
		assertThat(count).as("Expected count=0 after test2IncrementCount1().").isEqualTo(0);

		count = dao.incrementCount(TEST_NAME);
		assertThat(count).as("Expected count=1 now.").isEqualTo(1);
	}

}
