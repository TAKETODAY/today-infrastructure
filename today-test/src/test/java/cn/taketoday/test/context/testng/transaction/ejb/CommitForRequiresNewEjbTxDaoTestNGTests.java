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

import cn.taketoday.test.annotation.Commit;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.transaction.TransactionalTestExecutionListener;
import cn.taketoday.test.context.transaction.ejb.dao.RequiresNewEjbTxTestEntityDao;

import org.testng.annotations.Test;

/**
 * Concrete subclass of {@link AbstractEjbTxDaoTestNGTests} which uses the
 * {@link RequiresNewEjbTxTestEntityDao} and sets the default rollback semantics
 * for the {@link TransactionalTestExecutionListener} to {@code false} (i.e.,
 * <em>commit</em>).
 *
 * @author Sam Brannen
 * @since 4.0
 */
@Test(suiteName = "Commit for REQUIRES_NEW")
@ContextConfiguration("/org/springframework/test/context/transaction/ejb/requires-new-tx-config.xml")
@Commit
public class CommitForRequiresNewEjbTxDaoTestNGTests extends AbstractEjbTxDaoTestNGTests {

	/* test methods in superclass */

}
