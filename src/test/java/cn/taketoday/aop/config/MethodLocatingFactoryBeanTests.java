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

package cn.taketoday.aop.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.beans.factory.BeanFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Rick Evans
 * @author Chris Beams
 */
public class MethodLocatingFactoryBeanTests {

	private static final String BEAN_NAME = "string";
	private MethodLocatingFactoryBean factory;
	private BeanFactory beanFactory;

	@BeforeEach
	public void setUp() {
		factory = new MethodLocatingFactoryBean();
		beanFactory = mock(BeanFactory.class);
	}

	@Test
	public void testIsSingleton() {
		assertThat(factory.isSingleton()).isTrue();
	}

	@Test
	public void testGetObjectType() {
		assertThat(factory.getObjectType()).isEqualTo(Method.class);
	}

	@Test
	public void testWithNullTargetBeanName() {
		factory.setMethodName("toString()");
		assertThatIllegalArgumentException().isThrownBy(() ->
				factory.setBeanFactory(beanFactory));
	}

	@Test
	public void testWithEmptyTargetBeanName() {
		factory.setTargetBeanName("");
		factory.setMethodName("toString()");
		assertThatIllegalArgumentException().isThrownBy(() ->
				factory.setBeanFactory(beanFactory));
	}

	@Test
	public void testWithNullTargetMethodName() {
		factory.setTargetBeanName(BEAN_NAME);
		assertThatIllegalArgumentException().isThrownBy(() ->
				factory.setBeanFactory(beanFactory));
	}

	@Test
	public void testWithEmptyTargetMethodName() {
		factory.setTargetBeanName(BEAN_NAME);
		factory.setMethodName("");
		assertThatIllegalArgumentException().isThrownBy(() ->
				factory.setBeanFactory(beanFactory));
	}

	@Test
	public void testWhenTargetBeanClassCannotBeResolved() {
		factory.setTargetBeanName(BEAN_NAME);
		factory.setMethodName("toString()");
		assertThatIllegalArgumentException().isThrownBy(() ->
				factory.setBeanFactory(beanFactory));
		verify(beanFactory).getType(BEAN_NAME);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testSunnyDayPath() throws Exception {
		given(beanFactory.getType(BEAN_NAME)).willReturn((Class)String.class);
		factory.setTargetBeanName(BEAN_NAME);
		factory.setMethodName("toString()");
		factory.setBeanFactory(beanFactory);
		Object result = factory.getObject();
		assertThat(result).isNotNull();
		boolean condition = result instanceof Method;
		assertThat(condition).isTrue();
		Method method = (Method) result;
		assertThat(method.invoke("Bingo")).isEqualTo("Bingo");
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testWhereMethodCannotBeResolved() {
		given(beanFactory.getType(BEAN_NAME)).willReturn((Class)String.class);
		factory.setTargetBeanName(BEAN_NAME);
		factory.setMethodName("loadOfOld()");
		assertThatIllegalArgumentException().isThrownBy(() ->
				factory.setBeanFactory(beanFactory));
	}

}
