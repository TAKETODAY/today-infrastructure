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

package cn.taketoday.test.context.transaction.ejb.dao;

import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * EJB implementation of {@link TestEntityDao} which declares transaction
 * semantics for {@link #incrementCount(String)} with
 * {@link TransactionAttributeType#REQUIRES_NEW}.
 *
 * @author Sam Brannen
 * @author Xavier Detant
 * @since 4.0
 * @see RequiredEjbTxTestEntityDao
 */
@Stateless
@Local(TestEntityDao.class)
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class RequiresNewEjbTxTestEntityDao extends AbstractEjbTxTestEntityDao {

	@Override
	public int getCount(String name) {
		return super.getCountInternal(name);
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override
	public int incrementCount(String name) {
		return super.incrementCountInternal(name);
	}

}
