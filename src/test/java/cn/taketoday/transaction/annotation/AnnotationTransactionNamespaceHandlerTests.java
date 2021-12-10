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

package cn.taketoday.transaction.annotation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;
import cn.taketoday.jmx.export.annotation.ManagedOperation;
import cn.taketoday.jmx.export.annotation.ManagedResource;
import cn.taketoday.stereotype.Service;
import cn.taketoday.transaction.config.TransactionManagementConfigUtils;
import cn.taketoday.transaction.event.TransactionalEventListenerFactory;
import cn.taketoday.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class AnnotationTransactionNamespaceHandlerTests {

	private final ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
			"org/springframework/transaction/annotation/annotationTransactionNamespaceHandlerTests.xml");

	@AfterEach
	public void tearDown() {
		this.context.close();
	}

	@Test
	public void isProxy() throws Exception {
		TransactionalTestBean bean = getTestBean();
		assertThat(AopUtils.isAopProxy(bean)).as("testBean is not a proxy").isTrue();
		Map<String, Object> services = this.context.getBeansWithAnnotation(Service.class);
		assertThat(services.containsKey("testBean")).as("Stereotype annotation not visible").isTrue();
	}

	@Test
	public void invokeTransactional() throws Exception {
		TransactionalTestBean testBean = getTestBean();
		CallCountingTransactionManager ptm = (CallCountingTransactionManager) context.getBean("transactionManager");

		// try with transactional
		assertThat(ptm.begun).as("Should not have any started transactions").isEqualTo(0);
		testBean.findAllFoos();
		assertThat(ptm.begun).as("Should have 1 started transaction").isEqualTo(1);
		assertThat(ptm.commits).as("Should have 1 committed transaction").isEqualTo(1);

		// try with non-transaction
		testBean.doSomething();
		assertThat(ptm.begun).as("Should not have started another transaction").isEqualTo(1);

		// try with exceptional
		assertThatExceptionOfType(Throwable.class).isThrownBy(() ->
				testBean.exceptional(new IllegalArgumentException("foo")))
			.satisfies(ex -> {
				assertThat(ptm.begun).as("Should have another started transaction").isEqualTo(2);
				assertThat(ptm.rollbacks).as("Should have 1 rolled back transaction").isEqualTo(1);
			});
	}

	@Test
	public void nonPublicMethodsNotAdvised() {
		TransactionalTestBean testBean = getTestBean();
		CallCountingTransactionManager ptm = (CallCountingTransactionManager) context.getBean("transactionManager");

		assertThat(ptm.begun).as("Should not have any started transactions").isEqualTo(0);
		testBean.annotationsOnProtectedAreIgnored();
		assertThat(ptm.begun).as("Should not have any started transactions").isEqualTo(0);
	}

	@Test
	public void mBeanExportAlsoWorks() throws Exception {
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		Object actual = server.invoke(ObjectName.getInstance("test:type=TestBean"), "doSomething", new Object[0], new String[0]);
		assertThat(actual).isEqualTo("done");
	}

	@Test
	public void transactionalEventListenerRegisteredProperly() {
		assertThat(this.context.containsBean(TransactionManagementConfigUtils
				.TRANSACTIONAL_EVENT_LISTENER_FACTORY_BEAN_NAME)).isTrue();
		assertThat(this.context.getBeansOfType(TransactionalEventListenerFactory.class).size()).isEqualTo(1);
	}

	private TransactionalTestBean getTestBean() {
		return (TransactionalTestBean) context.getBean("testBean");
	}


	@Service
	@ManagedResource("test:type=TestBean")
	public static class TransactionalTestBean {

		@Transactional(readOnly = true)
		public Collection<?> findAllFoos() {
			return null;
		}

		@Transactional
		public void saveFoo() {
		}

		@Transactional("qualifiedTransactionManager")
		public void saveQualifiedFoo() {
		}

		@Transactional(transactionManager = "qualifiedTransactionManager")
		public void saveQualifiedFooWithAttributeAlias() {
		}

		@Transactional
		public void exceptional(Throwable t) throws Throwable {
			throw t;
		}

		@ManagedOperation
		public String doSomething() {
			return "done";
		}

		@Transactional
		protected void annotationsOnProtectedAreIgnored() {
		}
	}

}
