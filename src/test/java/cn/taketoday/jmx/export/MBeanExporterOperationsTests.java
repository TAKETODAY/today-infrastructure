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

package cn.taketoday.jmx.export;

import org.junit.jupiter.api.Test;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanInfo;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.RequiredModelMBean;

import cn.taketoday.jmx.AbstractMBeanServerTests;
import cn.taketoday.jmx.JmxTestBean;
import cn.taketoday.jmx.support.ObjectNameManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
class MBeanExporterOperationsTests extends AbstractMBeanServerTests {

	@Test
	void testRegisterManagedResourceWithUserSuppliedObjectName() throws Exception {
		ObjectName objectName = ObjectNameManager.getInstance("spring:name=Foo");

		JmxTestBean bean = new JmxTestBean();
		bean.setName("Rob Harrop");

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		exporter.registerManagedResource(bean, objectName);

		String name = (String) getServer().getAttribute(objectName, "Name");
		assertThat(bean.getName()).as("Incorrect name on MBean").isEqualTo(name);
	}

	@Test
	void testRegisterExistingMBeanWithUserSuppliedObjectName() throws Exception {
		ObjectName objectName = ObjectNameManager.getInstance("spring:name=Foo");
		ModelMBeanInfo info = new ModelMBeanInfoSupport("myClass", "myDescription", null, null, null, null);
		RequiredModelMBean bean = new RequiredModelMBean(info);

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		exporter.registerManagedResource(bean, objectName);

		MBeanInfo infoFromServer = getServer().getMBeanInfo(objectName);
		assertThat(infoFromServer).isEqualTo(info);
	}

	@Test
	void testRegisterManagedResourceWithGeneratedObjectName() throws Exception {
		final ObjectName objectNameTemplate = ObjectNameManager.getInstance("spring:type=Test");

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		exporter.setNamingStrategy((managedBean, beanKey) -> objectNameTemplate);

		JmxTestBean bean1 = new JmxTestBean();
		JmxTestBean bean2 = new JmxTestBean();

		ObjectName reg1 = exporter.registerManagedResource(bean1);
		ObjectName reg2 = exporter.registerManagedResource(bean2);

		assertIsRegistered("Bean 1 not registered with MBeanServer", reg1);
		assertIsRegistered("Bean 2 not registered with MBeanServer", reg2);

		assertObjectNameMatchesTemplate(objectNameTemplate, reg1);
		assertObjectNameMatchesTemplate(objectNameTemplate, reg2);
	}

	@Test
	void testRegisterManagedResourceWithGeneratedObjectNameWithoutUniqueness() throws Exception {
		final ObjectName objectNameTemplate = ObjectNameManager.getInstance("spring:type=Test");

		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(getServer());
		exporter.setEnsureUniqueRuntimeObjectNames(false);
		exporter.setNamingStrategy((managedBean, beanKey) -> objectNameTemplate);

		JmxTestBean bean1 = new JmxTestBean();
		JmxTestBean bean2 = new JmxTestBean();

		ObjectName reg1 = exporter.registerManagedResource(bean1);
		assertIsRegistered("Bean 1 not registered with MBeanServer", reg1);

		assertThatExceptionOfType(MBeanExportException.class).isThrownBy(()->
				exporter.registerManagedResource(bean2))
			.withCauseExactlyInstanceOf(InstanceAlreadyExistsException.class);
	}

	private void assertObjectNameMatchesTemplate(ObjectName objectNameTemplate, ObjectName registeredName) {
		assertThat(registeredName.getDomain()).as("Domain is incorrect").isEqualTo(objectNameTemplate.getDomain());
	}

}
