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

package cn.taketoday.framework.web.servlet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import cn.taketoday.framework.web.servlet.mock.MockServlet;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link ServletRegistrationBean}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class ServletRegistrationBeanTests {

	private final MockServlet servlet = new MockServlet();

	@Mock
	private ServletContext servletContext;

	@Mock
	private ServletRegistration.Dynamic registration;

	@Mock
	private FilterRegistration.Dynamic filterRegistration;

	@Test
	void startupWithDefaults() throws Exception {
		given(this.servletContext.addServlet(anyString(), any(Servlet.class))).willReturn(this.registration);
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>(this.servlet);
		bean.onStartup(this.servletContext);
		then(this.servletContext).should().addServlet("mockServlet", this.servlet);
		then(this.registration).should().setAsyncSupported(true);
		then(this.registration).should().addMapping("/*");
	}

	@Test
	void startupWithDoubleRegistration() throws Exception {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>(this.servlet);
		given(this.servletContext.addServlet(anyString(), any(Servlet.class))).willReturn(null);
		bean.onStartup(this.servletContext);
		then(this.servletContext).should().addServlet("mockServlet", this.servlet);
		then(this.registration).should(never()).setAsyncSupported(true);
	}

	@Test
	void startupWithSpecifiedValues() throws Exception {
		given(this.servletContext.addServlet(anyString(), any(Servlet.class))).willReturn(this.registration);
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>();
		bean.setName("test");
		bean.setServlet(this.servlet);
		bean.setAsyncSupported(false);
		bean.setInitParameters(Collections.singletonMap("a", "b"));
		bean.addInitParameter("c", "d");
		bean.setUrlMappings(new LinkedHashSet<>(Arrays.asList("/a", "/b")));
		bean.addUrlMappings("/c");
		bean.setLoadOnStartup(10);
		bean.onStartup(this.servletContext);
		then(this.servletContext).should().addServlet("test", this.servlet);
		then(this.registration).should().setAsyncSupported(false);
		Map<String, String> expectedInitParameters = new HashMap<>();
		expectedInitParameters.put("a", "b");
		expectedInitParameters.put("c", "d");
		then(this.registration).should().setInitParameters(expectedInitParameters);
		then(this.registration).should().addMapping("/a", "/b", "/c");
		then(this.registration).should().setLoadOnStartup(10);
	}

	@Test
	void specificName() throws Exception {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>();
		bean.setName("specificName");
		bean.setServlet(this.servlet);
		bean.onStartup(this.servletContext);
		then(this.servletContext).should().addServlet("specificName", this.servlet);
	}

	@Test
	void deducedName() throws Exception {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>();
		bean.setServlet(this.servlet);
		bean.onStartup(this.servletContext);
		then(this.servletContext).should().addServlet("mockServlet", this.servlet);
	}

	@Test
	void disable() throws Exception {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>();
		bean.setServlet(this.servlet);
		bean.setEnabled(false);
		bean.onStartup(this.servletContext);
		then(this.servletContext).should(never()).addServlet("mockServlet", this.servlet);
	}

	@Test
	void setServletMustNotBeNull() {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>();
		assertThatIllegalArgumentException().isThrownBy(() -> bean.onStartup(this.servletContext))
				.withMessageContaining("Servlet must not be null");
	}

	@Test
	void createServletMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ServletRegistrationBean<MockServlet>(null))
				.withMessageContaining("Servlet must not be null");
	}

	@Test
	void setMappingMustNotBeNull() {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>(this.servlet);
		assertThatIllegalArgumentException().isThrownBy(() -> bean.setUrlMappings(null))
				.withMessageContaining("UrlMappings must not be null");
	}

	@Test
	void createMappingMustNotBeNull() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ServletRegistrationBean<>(this.servlet, (String[]) null))
				.withMessageContaining("UrlMappings must not be null");
	}

	@Test
	void addMappingMustNotBeNull() {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>(this.servlet);
		assertThatIllegalArgumentException().isThrownBy(() -> bean.addUrlMappings((String[]) null))
				.withMessageContaining("UrlMappings must not be null");
	}

	@Test
	void setMappingReplacesValue() throws Exception {
		given(this.servletContext.addServlet(anyString(), any(Servlet.class))).willReturn(this.registration);
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>(this.servlet, "/a", "/b");
		bean.setUrlMappings(new LinkedHashSet<>(Arrays.asList("/c", "/d")));
		bean.onStartup(this.servletContext);
		then(this.registration).should().addMapping("/c", "/d");
	}

	@Test
	void modifyInitParameters() throws Exception {
		given(this.servletContext.addServlet(anyString(), any(Servlet.class))).willReturn(this.registration);
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>(this.servlet, "/a", "/b");
		bean.addInitParameter("a", "b");
		bean.getInitParameters().put("a", "c");
		bean.onStartup(this.servletContext);
		then(this.registration).should().setInitParameters(Collections.singletonMap("a", "c"));
	}

	@Test
	void withoutDefaultMappings() throws Exception {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>(this.servlet, false);
		bean.onStartup(this.servletContext);
		then(this.registration).should(never()).addMapping(any(String[].class));
	}

}
