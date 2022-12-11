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

package cn.taketoday.framework.test.mock.mockito;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.lang.reflect.Field;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.support.DependencyInjectionTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MockitoTestExecutionListener}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class MockitoTestExecutionListenerTests {

	private MockitoTestExecutionListener listener = new MockitoTestExecutionListener();

	@Mock
	private ApplicationContext applicationContext;

	@Mock
	private MockitoPostProcessor postProcessor;

	@Captor
	private ArgumentCaptor<Field> fieldCaptor;

	@Test
	void prepareTestInstanceShouldInitMockitoAnnotations() throws Exception {
		WithMockitoAnnotations instance = new WithMockitoAnnotations();
		this.listener.prepareTestInstance(mockTestContext(instance));
		assertThat(instance.mock).isNotNull();
		assertThat(instance.captor).isNotNull();
	}

	@Test
	void prepareTestInstanceShouldInjectMockBean() throws Exception {
		given(this.applicationContext.getBean(MockitoPostProcessor.class)).willReturn(this.postProcessor);
		WithMockBean instance = new WithMockBean();
		TestContext testContext = mockTestContext(instance);
		given(testContext.getApplicationContext()).willReturn(this.applicationContext);
		this.listener.prepareTestInstance(testContext);
		then(this.postProcessor).should().inject(this.fieldCaptor.capture(), eq(instance), any(MockDefinition.class));
		assertThat(this.fieldCaptor.getValue().getName()).isEqualTo("mockBean");
	}

	@Test
	void beforeTestMethodShouldDoNothingWhenDirtiesContextAttributeIsNotSet() throws Exception {
		this.listener.beforeTestMethod(mock(TestContext.class));
		then(this.postProcessor).shouldHaveNoMoreInteractions();
	}

	@Test
	void beforeTestMethodShouldInjectMockBeanWhenDirtiesContextAttributeIsSet() throws Exception {
		given(this.applicationContext.getBean(MockitoPostProcessor.class)).willReturn(this.postProcessor);
		WithMockBean instance = new WithMockBean();
		TestContext mockTestContext = mockTestContext(instance);
		given(mockTestContext.getApplicationContext()).willReturn(this.applicationContext);
		given(mockTestContext.getAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE))
				.willReturn(Boolean.TRUE);
		this.listener.beforeTestMethod(mockTestContext);
		then(this.postProcessor).should().inject(this.fieldCaptor.capture(), eq(instance), any(MockDefinition.class));
		assertThat(this.fieldCaptor.getValue().getName()).isEqualTo("mockBean");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private TestContext mockTestContext(Object instance) {
		TestContext testContext = mock(TestContext.class);
		given(testContext.getTestInstance()).willReturn(instance);
		given(testContext.getTestClass()).willReturn((Class) instance.getClass());
		return testContext;
	}

	static class WithMockitoAnnotations {

		@Mock
		InputStream mock;

		@Captor
		ArgumentCaptor<InputStream> captor;

	}

	static class WithMockBean {

		@MockBean
		InputStream mockBean;

	}

}
