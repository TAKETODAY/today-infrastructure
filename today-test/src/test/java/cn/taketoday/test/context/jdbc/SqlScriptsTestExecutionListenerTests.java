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

import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import cn.taketoday.beans.factory.config.AutowireCapableBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.annotation.AnnotationConfigurationException;
import cn.taketoday.core.io.Resource;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.jdbc.Sql;
import cn.taketoday.test.context.jdbc.SqlConfig;
import cn.taketoday.test.context.jdbc.SqlConfig.TransactionMode;
import cn.taketoday.test.context.jdbc.SqlScriptsTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link SqlScriptsTestExecutionListener}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class SqlScriptsTestExecutionListenerTests {

	private final SqlScriptsTestExecutionListener listener = new SqlScriptsTestExecutionListener();

	private final TestContext testContext = mock(TestContext.class);


	@Test
	void missingValueAndScriptsAndStatementsAtClassLevel() throws Exception {
		Class<?> clazz = MissingValueAndScriptsAndStatementsAtClassLevel.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("foo"));

		assertExceptionContains(clazz.getSimpleName() + ".sql");
	}

	@Test
	void missingValueAndScriptsAndStatementsAtMethodLevel() throws Exception {
		Class<?> clazz = MissingValueAndScriptsAndStatementsAtMethodLevel.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("foo"));

		assertExceptionContains(clazz.getSimpleName() + ".foo" + ".sql");
	}

	@Test
	void valueAndScriptsDeclared() throws Exception {
		Class<?> clazz = ValueAndScriptsDeclared.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("foo"));

		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
				listener.beforeTestMethod(testContext))
			.withMessageContaining("Different @AliasFor mirror values")
			.withMessageContaining("attribute 'scripts' and its alias 'value'")
			.withMessageContaining("values of [{bar}] and [{foo}]");
	}

	@Test
	void isolatedTxModeDeclaredWithoutTxMgr() throws Exception {
		ApplicationContext ctx = mock(ApplicationContext.class);
		given(ctx.getResource(anyString())).willReturn(mock(Resource.class));
		given(ctx.getAutowireCapableBeanFactory()).willReturn(mock(AutowireCapableBeanFactory.class));

		Class<?> clazz = IsolatedWithoutTxMgr.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("foo"));
		given(testContext.getApplicationContext()).willReturn(ctx);

		assertExceptionContains("cannot execute SQL scripts using Transaction Mode [ISOLATED] without a PlatformTransactionManager");
	}

	@Test
	void missingDataSourceAndTxMgr() throws Exception {
		ApplicationContext ctx = mock(ApplicationContext.class);
		given(ctx.getResource(anyString())).willReturn(mock(Resource.class));
		given(ctx.getAutowireCapableBeanFactory()).willReturn(mock(AutowireCapableBeanFactory.class));

		Class<?> clazz = MissingDataSourceAndTxMgr.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("foo"));
		given(testContext.getApplicationContext()).willReturn(ctx);

		assertExceptionContains("supply at least a DataSource or PlatformTransactionManager");
	}

	private void assertExceptionContains(String msg) throws Exception {
		assertThatIllegalStateException().isThrownBy(() ->
				listener.beforeTestMethod(testContext))
			.withMessageContaining(msg);
	}


	// -------------------------------------------------------------------------

	@Sql
	static class MissingValueAndScriptsAndStatementsAtClassLevel {

		public void foo() {
		}
	}

	static class MissingValueAndScriptsAndStatementsAtMethodLevel {

		@Sql
		public void foo() {
		}
	}

	static class ValueAndScriptsDeclared {

		@Sql(value = "foo", scripts = "bar")
		public void foo() {
		}
	}

	static class IsolatedWithoutTxMgr {

		@Sql(scripts = "foo.sql", config = @SqlConfig(transactionMode = TransactionMode.ISOLATED))
		public void foo() {
		}
	}

	static class MissingDataSourceAndTxMgr {

		@Sql("foo.sql")
		public void foo() {
		}
	}

}
