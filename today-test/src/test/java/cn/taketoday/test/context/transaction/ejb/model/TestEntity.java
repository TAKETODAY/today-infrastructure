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

package cn.taketoday.test.context.transaction.ejb.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Test entity for EJB transaction support in the TestContext framework.
 *
 * @author Xavier Detant
 * @author Sam Brannen
 * @since 4.0
 */
@Entity
@Table(name = TestEntity.TABLE_NAME)
public class TestEntity {

	public static final String TABLE_NAME = "TEST_ENTITY";

	@Id
	@Column(name = "TE_NAME", nullable = false)
	private String name;

	@Column(name = "TE_COUNT", nullable = false)
	private int count;


	public TestEntity() {
	}

	public TestEntity(String name, int count) {
		this.name = name;
		this.count = count;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getCount() {
		return this.count;
	}

	public void setCount(int count) {
		this.count = count;
	}
}
