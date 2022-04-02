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

package cn.taketoday.test.context.transaction.ejb;

import cn.taketoday.test.annotation.Commit;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.transaction.TransactionalTestExecutionListener;
import cn.taketoday.test.context.transaction.ejb.dao.RequiresNewEjbTxTestEntityDao;

/**
 * Concrete subclass of {@link AbstractEjbTxDaoTests} which uses the
 * {@link RequiresNewEjbTxTestEntityDao} and sets the default rollback semantics
 * for the {@link TransactionalTestExecutionListener} to {@code false} (i.e.,
 * <em>commit</em>).
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ContextConfiguration("requires-new-tx-config.xml")
@Commit
class CommitForRequiresNewEjbTxDaoTests extends AbstractEjbTxDaoTests {

  /* test methods in superclass */

}
