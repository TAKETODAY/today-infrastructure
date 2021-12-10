/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;
import cn.taketoday.transaction.interceptor.TransactionAttributeSource;
import cn.taketoday.transaction.interceptor.TransactionInterceptor;
import cn.taketoday.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rob Harrop
 * @author Adrian Colyer
 */
public class TxNamespaceHandlerTests {

	private ApplicationContext context;

	private Method getAgeMethod;

	private Method setAgeMethod;


	@BeforeEach
	public void setup() throws Exception {
		this.context = new ClassPathXmlApplicationContext("txNamespaceHandlerTests.xml", getClass());
		this.getAgeMethod = ITestBean.class.getMethod("getAge");
		this.setAgeMethod = ITestBean.class.getMethod("setAge", int.class);
	}


	@Test
	public void isProxy() {
		ITestBean bean = getTestBean();
		assertThat(AopUtils.isAopProxy(bean)).as("testBean is not a proxy").isTrue();
	}

	@Test
	public void invokeTransactional() {
		ITestBean testBean = getTestBean();
		CallCountingTransactionManager ptm = (CallCountingTransactionManager) context.getBean("transactionManager");

		// try with transactional
		assertThat(ptm.begun).as("Should not have any started transactions").isEqualTo(0);
		testBean.getName();
		assertThat(ptm.lastDefinition.isReadOnly()).isTrue();
		assertThat(ptm.lastDefinition.getTimeout()).isEqualTo(5);
		assertThat(ptm.begun).as("Should have 1 started transaction").isEqualTo(1);
		assertThat(ptm.commits).as("Should have 1 committed transaction").isEqualTo(1);

		// try with non-transaction
		testBean.haveBirthday();
		assertThat(ptm.begun).as("Should not have started another transaction").isEqualTo(1);

		// try with exceptional
		assertThatExceptionOfType(Throwable.class).isThrownBy(() ->
				testBean.exceptional(new IllegalArgumentException("foo")));
		assertThat(ptm.begun).as("Should have another started transaction").isEqualTo(2);
		assertThat(ptm.rollbacks).as("Should have 1 rolled back transaction").isEqualTo(1);
	}

	@Test
	public void rollbackRules() {
		TransactionInterceptor txInterceptor = (TransactionInterceptor) context.getBean("txRollbackAdvice");
		TransactionAttributeSource txAttrSource = txInterceptor.getTransactionAttributeSource();
		TransactionAttribute txAttr = txAttrSource.getTransactionAttribute(getAgeMethod,ITestBean.class);
		assertThat(txAttr.rollbackOn(new Exception())).as("should be configured to rollback on Exception").isTrue();

		txAttr = txAttrSource.getTransactionAttribute(setAgeMethod, ITestBean.class);
		assertThat(txAttr.rollbackOn(new RuntimeException())).as("should not rollback on RuntimeException").isFalse();
	}

	private ITestBean getTestBean() {
		return (ITestBean) context.getBean("testBean");
	}

}
