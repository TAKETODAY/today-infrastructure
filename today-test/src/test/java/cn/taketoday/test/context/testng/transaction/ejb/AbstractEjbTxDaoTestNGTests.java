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

import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.annotation.DirtiesContext.ClassMode;
import cn.taketoday.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import cn.taketoday.test.context.transaction.ejb.dao.TestEntityDao;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import jakarta.ejb.EJB;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for all TestNG-based tests involving EJB transaction
 * support in the TestContext framework.
 *
 * @author Sam Brannen
 * @author Xavier Detant
 * @since 4.0
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractEjbTxDaoTestNGTests extends AbstractTransactionalTestNGSpringContextTests {

	protected static final String TEST_NAME = "test-name";

	@EJB
	protected TestEntityDao dao;

	@PersistenceContext
	protected EntityManager em;


	@Test
	public void test1InitialState() {
		int count = dao.getCount(TEST_NAME);
		assertThat(count).as("New TestEntity should have count=0.").isEqualTo(0);
	}

	@Test(dependsOnMethods = "test1InitialState")
	public void test2IncrementCount1() {
		int count = dao.incrementCount(TEST_NAME);
		assertThat(count).as("Expected count=1 after first increment.").isEqualTo(1);
	}

	/**
	 * The default implementation of this method assumes that the transaction
	 * for {@link #test2IncrementCount1()} was committed. Therefore, it is
	 * expected that the previous increment has been persisted in the database.
	 */
	@Test(dependsOnMethods = "test2IncrementCount1")
	public void test3IncrementCount2() {
		int count = dao.getCount(TEST_NAME);
		assertThat(count).as("Expected count=1 after test2IncrementCount1().").isEqualTo(1);

		count = dao.incrementCount(TEST_NAME);
		assertThat(count).as("Expected count=2 now.").isEqualTo(2);
	}

	@AfterMethod(alwaysRun = true)
	public void synchronizePersistenceContext() {
		em.flush();
	}

}
