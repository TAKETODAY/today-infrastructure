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

package cn.taketoday.beans.factory.xml;

import org.junit.jupiter.api.Test;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.DummyBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.core.io.ClassPathResource;

import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Costin Leau
 */
public class SimpleConstructorNamespaceHandlerTests {

	@Test
	public void simpleValue() throws Exception {
		StandardBeanFactory beanFactory = createFactory("simpleConstructorNamespaceHandlerTests.xml");
		String name = "simple";
		//		beanFactory.getBean("simple1", DummyBean.class);
		DummyBean nameValue = beanFactory.getBean(name, DummyBean.class);
		assertThat(nameValue.getValue()).isEqualTo("simple");
	}

	@Test
	public void simpleRef() throws Exception {
		StandardBeanFactory beanFactory = createFactory("simpleConstructorNamespaceHandlerTests.xml");
		String name = "simple-ref";
		//		beanFactory.getBean("name-value1", TestBean.class);
		DummyBean nameValue = beanFactory.getBean(name, DummyBean.class);
		assertThat(nameValue.getValue()).isEqualTo(beanFactory.getBean("name"));
	}

	@Test
	public void nameValue() throws Exception {
		StandardBeanFactory beanFactory = createFactory("simpleConstructorNamespaceHandlerTests.xml");
		String name = "name-value";
		//		beanFactory.getBean("name-value1", TestBean.class);
		TestBean nameValue = beanFactory.getBean(name, TestBean.class);
		assertThat(nameValue.getName()).isEqualTo(name);
		assertThat(nameValue.getAge()).isEqualTo(10);
	}

	@Test
	public void nameRef() throws Exception {
		StandardBeanFactory beanFactory = createFactory("simpleConstructorNamespaceHandlerTests.xml");
		TestBean nameValue = beanFactory.getBean("name-value", TestBean.class);
		DummyBean nameRef = beanFactory.getBean("name-ref", DummyBean.class);

		assertThat(nameRef.getName()).isEqualTo("some-name");
		assertThat(nameRef.getSpouse()).isEqualTo(nameValue);
	}

	@Test
	public void typeIndexedValue() throws Exception {
		StandardBeanFactory beanFactory = createFactory("simpleConstructorNamespaceHandlerTests.xml");
		DummyBean typeRef = beanFactory.getBean("indexed-value", DummyBean.class);

		assertThat(typeRef.getName()).isEqualTo("at");
		assertThat(typeRef.getValue()).isEqualTo("austria");
		assertThat(typeRef.getAge()).isEqualTo(10);
	}

	@Test
	public void typeIndexedRef() throws Exception {
		StandardBeanFactory beanFactory = createFactory("simpleConstructorNamespaceHandlerTests.xml");
		DummyBean typeRef = beanFactory.getBean("indexed-ref", DummyBean.class);

		assertThat(typeRef.getName()).isEqualTo("some-name");
		assertThat(typeRef.getSpouse()).isEqualTo(beanFactory.getBean("name-value"));
	}

	@Test
	public void ambiguousConstructor() throws Exception {
		StandardBeanFactory bf = new StandardBeanFactory();
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
				new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
							new ClassPathResource("simpleConstructorNamespaceHandlerTestsWithErrors.xml", getClass())));
	}

	@Test
	public void constructorWithNameEndingInRef() throws Exception {
		StandardBeanFactory beanFactory = createFactory("simpleConstructorNamespaceHandlerTests.xml");
		DummyBean derivedBean = beanFactory.getBean("beanWithRefConstructorArg", DummyBean.class);
		assertThat(derivedBean.getAge()).isEqualTo(10);
		assertThat(derivedBean.getName()).isEqualTo("silly name");
	}

	private StandardBeanFactory createFactory(String resourceName) {
		StandardBeanFactory bf = new StandardBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource(resourceName, getClass()));
		return bf;
	}
}
