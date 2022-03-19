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

package cn.taketoday.context.support;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
class SimpleThreadScopeTests {

	private final ApplicationContext applicationContext =
			new ClassPathXmlApplicationContext("simpleThreadScopeTests.xml", getClass());


	@Test
	void getFromScope() throws Exception {
		String name = "removeNodeStatusScreen";
		TestBean bean = this.applicationContext.getBean(name, TestBean.class);
		assertThat(bean).isNotNull();
		assertThat(this.applicationContext.getBean(name)).isSameAs(bean);
		TestBean bean2 = this.applicationContext.getBean(name, TestBean.class);
		assertThat(bean2).isSameAs(bean);
	}

	@Test
	void getMultipleInstances() throws Exception {
		// Arrange
		TestBean[] beans = new TestBean[2];
		Thread thread1 = new Thread(() -> beans[0] = applicationContext.getBean("removeNodeStatusScreen", TestBean.class));
		Thread thread2 = new Thread(() -> beans[1] = applicationContext.getBean("removeNodeStatusScreen", TestBean.class));
		// Act
		thread1.start();
		thread2.start();
		// Assert
		Awaitility.await()
					.atMost(500, TimeUnit.MILLISECONDS)
					.pollInterval(10, TimeUnit.MILLISECONDS)
					.until(() -> (beans[0] != null) && (beans[1] != null));
		assertThat(beans[1]).isNotSameAs(beans[0]);
	}

}
