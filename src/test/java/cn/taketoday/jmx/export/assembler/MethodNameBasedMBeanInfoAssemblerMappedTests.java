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

package cn.taketoday.jmx.export.assembler;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import javax.management.MBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Chris Beams
 */
public class MethodNameBasedMBeanInfoAssemblerMappedTests extends AbstractJmxAssemblerTests {

	protected static final String OBJECT_NAME = "bean:name=testBean4";


	@Test
	public void testGetAgeIsReadOnly() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();
		ModelMBeanAttributeInfo attr = info.getAttribute(AGE_ATTRIBUTE);

		assertThat(attr.isReadable()).as("Age is not readable").isTrue();
		assertThat(attr.isWritable()).as("Age is not writable").isFalse();
	}

	@Test
	public void testWithFallThrough() throws Exception {
		MethodNameBasedMBeanInfoAssembler assembler =
				getWithMapping("foobar", "add,myOperation,getName,setName,getAge");
		assembler.setManagedMethods("getNickName", "setNickName");

		ModelMBeanInfo inf = assembler.getMBeanInfo(getBean(), getObjectName());
		MBeanAttributeInfo attr = inf.getAttribute("NickName");

		assertNickName(attr);
	}

	@Test
	public void testNickNameIsExposed() throws Exception {
		ModelMBeanInfo inf = (ModelMBeanInfo) getMBeanInfo();
		MBeanAttributeInfo attr = inf.getAttribute("NickName");

		assertNickName(attr);
	}

	@Override
	protected String getObjectName() {
		return OBJECT_NAME;
	}

	@Override
	protected int getExpectedOperationCount() {
		return 7;
	}

	@Override
	protected int getExpectedAttributeCount() {
		return 3;
	}

	@Override
	protected MBeanInfoAssembler getAssembler() throws Exception {
		return getWithMapping("getNickName,setNickName,add,myOperation,getName,setName,getAge");
	}

	@Override
	protected String getApplicationContextPath() {
		return "cn/taketoday/jmx/export/assembler/methodNameAssemblerMapped.xml";
	}

	private MethodNameBasedMBeanInfoAssembler getWithMapping(String mapping) {
		return getWithMapping(OBJECT_NAME, mapping);
	}

	private MethodNameBasedMBeanInfoAssembler getWithMapping(String name, String mapping) {
		MethodNameBasedMBeanInfoAssembler assembler = new MethodNameBasedMBeanInfoAssembler();
		Properties props = new Properties();
		props.setProperty(name, mapping);
		assembler.setMethodMappings(props);
		return assembler;
	}

	private void assertNickName(MBeanAttributeInfo attr) {
		assertThat(attr).as("Nick Name should not be null").isNotNull();
		assertThat(attr.isWritable()).as("Nick Name should be writable").isTrue();
		assertThat(attr.isReadable()).as("Nick Name should be readable").isTrue();
	}

}
