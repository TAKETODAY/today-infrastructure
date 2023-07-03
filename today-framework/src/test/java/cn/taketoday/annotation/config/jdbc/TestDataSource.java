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

package cn.taketoday.annotation.config.jdbc;

import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import cn.taketoday.jdbc.datasource.SimpleDriverDataSource;

/**
 * {@link BasicDataSource} used for testing.
 *
 * @author Phillip Webb
 * @author Kazuki Shimizu
 * @author Stephane Nicoll
 */
public class TestDataSource extends SimpleDriverDataSource {

	/**
	 * Create an in-memory database with a random name.
	 */
	public TestDataSource() {
		this(false);
	}

	/**
	 * Create an in-memory database with a random name.
	 * @param addTestUser if a test user should be added
	 */
	public TestDataSource(boolean addTestUser) {
		this(UUID.randomUUID().toString(), addTestUser);
	}

	/**
	 * Create an in-memory database with the specified name.
	 * @param name the name of the database
	 * @param addTestUser if a test user should be added
	 */
	public TestDataSource(String name, boolean addTestUser) {
		setDriverClass(org.hsqldb.jdbc.JDBCDriver.class);
		setUrl("jdbc:hsqldb:mem:" + name);
		setUsername("sa");
		setupDatabase(addTestUser);
		setUrl(getUrl() + ";create=false");
	}

	private void setupDatabase(boolean addTestUser) {
		try (Connection connection = getConnection()) {
			if (addTestUser) {
				connection.prepareStatement("CREATE USER \"test\" password \"secret\" ADMIN").execute();
			}
		}
		catch (SQLException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
