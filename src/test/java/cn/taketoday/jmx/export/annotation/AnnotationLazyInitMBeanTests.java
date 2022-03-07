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

package cn.taketoday.jmx.export.annotation;

import org.junit.jupiter.api.Test;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;
import cn.taketoday.jmx.support.ObjectNameManager;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
class AnnotationLazyInitMBeanTests {

	@Test
	void lazyNaming() throws Exception {
		try (ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext("cn/taketoday/jmx/export/annotation/lazyNaming.xml")) {
			MBeanServer server = (MBeanServer) ctx.getBean("server");
			ObjectName oname = ObjectNameManager.getInstance("bean:name=testBean4");
			assertThat(server.getObjectInstance(oname)).isNotNull();
			String name = (String) server.getAttribute(oname, "Name");
			assertThat(name).as("Invalid name returned").isEqualTo("TEST");
		}
	}

	@Test
	void lazyAssembling() throws Exception {
		System.setProperty("domain", "bean");
		try (ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext("cn/taketoday/jmx/export/annotation/lazyAssembling.xml")) {
			MBeanServer server = (MBeanServer) ctx.getBean("server");

			ObjectName oname = ObjectNameManager.getInstance("bean:name=testBean4");
			assertThat(server.getObjectInstance(oname)).isNotNull();
			String name = (String) server.getAttribute(oname, "Name");
			assertThat(name).as("Invalid name returned").isEqualTo("TEST");

			oname = ObjectNameManager.getInstance("bean:name=testBean5");
			assertThat(server.getObjectInstance(oname)).isNotNull();
			name = (String) server.getAttribute(oname, "Name");
			assertThat(name).as("Invalid name returned").isEqualTo("FACTORY");

			oname = ObjectNameManager.getInstance("spring:mbean=true");
			assertThat(server.getObjectInstance(oname)).isNotNull();
			name = (String) server.getAttribute(oname, "Name");
			assertThat(name).as("Invalid name returned").isEqualTo("Rob Harrop");

			oname = ObjectNameManager.getInstance("spring:mbean=another");
			assertThat(server.getObjectInstance(oname)).isNotNull();
			name = (String) server.getAttribute(oname, "Name");
			assertThat(name).as("Invalid name returned").isEqualTo("Juergen Hoeller");
		}
		finally {
			System.clearProperty("domain");
		}
	}

	@Test
	void componentScan() throws Exception {
		try (ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext("cn/taketoday/jmx/export/annotation/componentScan.xml")) {
			MBeanServer server = (MBeanServer) ctx.getBean("server");
			ObjectName oname = ObjectNameManager.getInstance("bean:name=testBean4");
			assertThat(server.getObjectInstance(oname)).isNotNull();
			String name = (String) server.getAttribute(oname, "Name");
			assertThat(name).isNull();
		}
	}

}
