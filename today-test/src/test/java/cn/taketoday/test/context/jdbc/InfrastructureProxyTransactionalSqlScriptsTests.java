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

package cn.taketoday.test.context.jdbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.InfrastructureProxy;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.jdbc.datasource.DataSourceTransactionManager;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;
import cn.taketoday.transaction.PlatformTransactionManager;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Transactional integration tests for {@link Sql @Sql} support when the
 * {@link DataSource} is wrapped in a proxy that implements
 * {@link InfrastructureProxy}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig
@DirtiesContext
class InfrastructureProxyTransactionalSqlScriptsTests extends AbstractTransactionalTests {

	@BeforeEach
	void preconditions(@Autowired DataSource dataSource, @Autowired DataSourceTransactionManager transactionManager) {
		assertThat(dataSource).isNotEqualTo(transactionManager.getDataSource());
		assertThat(transactionManager.getDataSource()).isNotEqualTo(dataSource);
		assertThat(transactionManager.getDataSource()).isInstanceOf(InfrastructureProxy.class);
	}

	@Test
	@Sql({ "schema.sql", "data.sql", "data-add-dogbert.sql" })
	void methodLevelScripts() {
		assertNumUsers(2);
	}


	@Configuration
	static class DatabaseConfig {

		@Bean
		JdbcTemplate jdbcTemplate(DataSource dataSource) {
			return new JdbcTemplate(dataSource);
		}

		@Bean
		PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(wrapDataSource(dataSource));
		}

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()//
					.setName("empty-sql-scripts-test-db")//
					.build();
		}

	}


	private static DataSource wrapDataSource(DataSource dataSource) {
		return (DataSource) Proxy.newProxyInstance(
			InfrastructureProxyTransactionalSqlScriptsTests.class.getClassLoader(),
			new Class<?>[] { DataSource.class, InfrastructureProxy.class },
			new DataSourceInvocationHandler(dataSource));
	}


	private static class DataSourceInvocationHandler implements InvocationHandler {

		private final DataSource dataSource;


		DataSourceInvocationHandler(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch (method.getName()) {
				case "equals":
					return (proxy == args[0]);
				case "hashCode":
					return System.identityHashCode(proxy);
				case "getWrappedObject":
					return this.dataSource;
				default:
					try {
						return method.invoke(this.dataSource, args);
					}
					catch (InvocationTargetException ex) {
						throw ex.getTargetException();
					}
			}
		}
	}

}
