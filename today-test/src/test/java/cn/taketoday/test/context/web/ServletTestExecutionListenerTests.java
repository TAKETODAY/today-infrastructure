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

package cn.taketoday.test.context.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.mock.web.MockServletContext;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.web.ServletTestExecutionListener;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.web.context.WebApplicationContext;
import cn.taketoday.web.context.request.RequestAttributes;
import cn.taketoday.web.context.request.RequestContextHolder;
import cn.taketoday.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static cn.taketoday.test.context.web.ServletTestExecutionListener.POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE;
import static cn.taketoday.test.context.web.ServletTestExecutionListener.RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE;

/**
 * Unit tests for {@link ServletTestExecutionListener}.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 */
class ServletTestExecutionListenerTests {

	private static final String SET_UP_OUTSIDE_OF_STEL = "setUpOutsideOfStel";

	private final WebApplicationContext wac = mock(WebApplicationContext.class);
	private final MockServletContext mockServletContext = new MockServletContext();
	private final TestContext testContext = mock(TestContext.class);
	private final ServletTestExecutionListener listener = new ServletTestExecutionListener();


	@BeforeEach
	void setUp() {
		given(wac.getServletContext()).willReturn(mockServletContext);
		given(testContext.getApplicationContext()).willReturn(wac);

		MockHttpServletRequest request = new MockHttpServletRequest(mockServletContext);
		MockHttpServletResponse response = new MockHttpServletResponse();
		ServletWebRequest servletWebRequest = new ServletWebRequest(request, response);

		request.setAttribute(SET_UP_OUTSIDE_OF_STEL, "true");

		RequestContextHolder.setRequestAttributes(servletWebRequest);
		assertSetUpOutsideOfStelAttributeExists();
	}

	@Test
	void standardApplicationContext() throws Exception {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(getClass());
		given(testContext.getApplicationContext()).willReturn(mock(ApplicationContext.class));

		listener.beforeTestClass(testContext);
		assertSetUpOutsideOfStelAttributeExists();

		listener.prepareTestInstance(testContext);
		assertSetUpOutsideOfStelAttributeExists();

		listener.beforeTestMethod(testContext);
		assertSetUpOutsideOfStelAttributeExists();

		listener.afterTestMethod(testContext);
		assertSetUpOutsideOfStelAttributeExists();
	}

	@Test
	void legacyWebTestCaseWithoutExistingRequestAttributes() throws Exception {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(LegacyWebTestCase.class);

		RequestContextHolder.resetRequestAttributes();
		assertRequestAttributesDoNotExist();

		listener.beforeTestClass(testContext);

		listener.prepareTestInstance(testContext);
		assertRequestAttributesDoNotExist();
		verify(testContext, times(0)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		given(testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).willReturn(null);

		listener.beforeTestMethod(testContext);
		assertRequestAttributesDoNotExist();
		verify(testContext, times(0)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);

		listener.afterTestMethod(testContext);
		verify(testContext, times(1)).removeAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
		assertRequestAttributesDoNotExist();
	}

	@Test
	void legacyWebTestCaseWithPresetRequestAttributes() throws Exception {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(LegacyWebTestCase.class);

		listener.beforeTestClass(testContext);
		assertSetUpOutsideOfStelAttributeExists();

		listener.prepareTestInstance(testContext);
		assertSetUpOutsideOfStelAttributeExists();
		verify(testContext, times(0)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		given(testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).willReturn(null);

		listener.beforeTestMethod(testContext);
		assertSetUpOutsideOfStelAttributeExists();
		verify(testContext, times(0)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		given(testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).willReturn(null);

		listener.afterTestMethod(testContext);
		verify(testContext, times(1)).removeAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
		assertSetUpOutsideOfStelAttributeExists();
	}

	@Test
	void atWebAppConfigTestCaseWithoutExistingRequestAttributes() throws Exception {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(AtWebAppConfigWebTestCase.class);

		RequestContextHolder.resetRequestAttributes();
		listener.beforeTestClass(testContext);
		assertRequestAttributesDoNotExist();

		assertWebAppConfigTestCase();
	}

	@Test
	void atWebAppConfigTestCaseWithPresetRequestAttributes() throws Exception {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(AtWebAppConfigWebTestCase.class);

		listener.beforeTestClass(testContext);
		assertRequestAttributesExist();

		assertWebAppConfigTestCase();
	}

	/**
	 * @since 4.3
	 */
	@Test
	void activateListenerWithoutExistingRequestAttributes() throws Exception {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(NoAtWebAppConfigWebTestCase.class);
		given(testContext.getAttribute(ServletTestExecutionListener.ACTIVATE_LISTENER)).willReturn(true);

		RequestContextHolder.resetRequestAttributes();
		listener.beforeTestClass(testContext);
		assertRequestAttributesDoNotExist();

		assertWebAppConfigTestCase();
	}


	private RequestAttributes assertRequestAttributesExist() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		assertThat(requestAttributes).as("request attributes should exist").isNotNull();
		return requestAttributes;
	}

	private void assertRequestAttributesDoNotExist() {
		assertThat(RequestContextHolder.getRequestAttributes()).as("request attributes should not exist").isNull();
	}

	private void assertSetUpOutsideOfStelAttributeExists() {
		RequestAttributes requestAttributes = assertRequestAttributesExist();
		Object setUpOutsideOfStel = requestAttributes.getAttribute(SET_UP_OUTSIDE_OF_STEL,
			RequestAttributes.SCOPE_REQUEST);
		assertThat(setUpOutsideOfStel).as(SET_UP_OUTSIDE_OF_STEL + " should exist as a request attribute").isNotNull();
	}

	private void assertSetUpOutsideOfStelAttributeDoesNotExist() {
		RequestAttributes requestAttributes = assertRequestAttributesExist();
		Object setUpOutsideOfStel = requestAttributes.getAttribute(SET_UP_OUTSIDE_OF_STEL,
			RequestAttributes.SCOPE_REQUEST);
		assertThat(setUpOutsideOfStel).as(SET_UP_OUTSIDE_OF_STEL + " should NOT exist as a request attribute").isNull();
	}

	private void assertWebAppConfigTestCase() throws Exception {
		listener.prepareTestInstance(testContext);
		assertRequestAttributesExist();
		assertSetUpOutsideOfStelAttributeDoesNotExist();
		verify(testContext, times(1)).setAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		verify(testContext, times(1)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		given(testContext.getAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).willReturn(Boolean.TRUE);
		given(testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).willReturn(Boolean.TRUE);

		listener.beforeTestMethod(testContext);
		assertRequestAttributesExist();
		assertSetUpOutsideOfStelAttributeDoesNotExist();
		verify(testContext, times(1)).setAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		verify(testContext, times(1)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);

		listener.afterTestMethod(testContext);
		verify(testContext).removeAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
		verify(testContext).removeAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
		assertRequestAttributesDoNotExist();
	}


	static class LegacyWebTestCase {
	}

	@WebAppConfiguration
	static class AtWebAppConfigWebTestCase {
	}

	static class NoAtWebAppConfigWebTestCase {
	}

}
