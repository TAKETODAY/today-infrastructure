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

import cn.taketoday.test.annotation.Rollback;
import cn.taketoday.test.context.transaction.TransactionalTestExecutionListener;

/**
 * Extension of {@link CommitForRequiresNewEjbTxDaoTests} which sets the default
 * rollback semantics for the {@link TransactionalTestExecutionListener} to
 * {@code true}. The transaction managed by the TestContext framework will be
 * rolled back after each test method. Consequently, any work performed in
 * transactional methods that participate in the test-managed transaction will
 * be rolled back automatically. On the other hand, any work performed in
 * transactional methods that do <strong>not</strong> participate in the
 * test-managed transaction will not be affected by the rollback of the
 * test-managed transaction. For example, such work may in fact be committed
 * outside the scope of the test-managed transaction.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@Rollback
class RollbackForRequiresNewEjbTxDaoTests extends CommitForRequiresNewEjbTxDaoTests {

  /* test methods in superclass */

}
