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

package cn.taketoday.test.web.servlet.samples.spr;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import cn.taketoday.context.annotation.AnnotatedBeanDefinitionReader;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.mock.web.MockServletContext;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.web.bind.annotation.RequestMapping;
import cn.taketoday.web.bind.annotation.RestController;
import cn.taketoday.web.context.request.RequestAttributes;
import cn.taketoday.web.context.request.RequestContextHolder;
import cn.taketoday.web.context.request.ServletRequestAttributes;
import cn.taketoday.web.context.request.ServletWebRequest;
import cn.taketoday.web.context.support.GenericWebApplicationContext;
import cn.taketoday.web.servlet.config.annotation.EnableWebMvc;
import cn.taketoday.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Integration tests for SPR-13211 which verify that a custom mock request
 * is not reused by MockMvc.
 *
 * @author Sam Brannen
 * @since 4.2
 * @see RequestContextHolderTests
 */
public class CustomRequestAttributesRequestContextHolderTests {

	private static final String FROM_CUSTOM_MOCK = "fromCustomMock";
	private static final String FROM_MVC_TEST_DEFAULT = "fromSpringMvcTestDefault";
	private static final String FROM_MVC_TEST_MOCK = "fromSpringMvcTestMock";

	private final GenericWebApplicationContext wac = new GenericWebApplicationContext();

	private MockMvc mockMvc;


	@BeforeEach
	public void setUp() {
		ServletContext servletContext = new MockServletContext();
		MockHttpServletRequest mockRequest = new MockHttpServletRequest(servletContext);
		mockRequest.setAttribute(FROM_CUSTOM_MOCK, FROM_CUSTOM_MOCK);
		RequestContextHolder.setRequestAttributes(new ServletWebRequest(mockRequest, new MockHttpServletResponse()));

		this.wac.setServletContext(servletContext);
		new AnnotatedBeanDefinitionReader(this.wac).register(WebConfig.class);
		this.wac.refresh();

		this.mockMvc = webAppContextSetup(this.wac)
				.defaultRequest(get("/").requestAttr(FROM_MVC_TEST_DEFAULT, FROM_MVC_TEST_DEFAULT))
				.alwaysExpect(status().isOk())
				.build();
	}

	@Test
	public void singletonController() throws Exception {
		this.mockMvc.perform(get("/singletonController").requestAttr(FROM_MVC_TEST_MOCK, FROM_MVC_TEST_MOCK));
	}

	@AfterEach
	public void verifyCustomRequestAttributesAreRestored() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		assertThat(requestAttributes).isInstanceOf(ServletRequestAttributes.class);
		HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

		assertThat(request.getAttribute(FROM_CUSTOM_MOCK)).isEqualTo(FROM_CUSTOM_MOCK);
		assertThat(request.getAttribute(FROM_MVC_TEST_DEFAULT)).isNull();
		assertThat(request.getAttribute(FROM_MVC_TEST_MOCK)).isNull();

		RequestContextHolder.resetRequestAttributes();
		this.wac.close();
	}


	// -------------------------------------------------------------------

	@Configuration
	@EnableWebMvc
	static class WebConfig implements WebMvcConfigurer {

		@Bean
		public SingletonController singletonController() {
			return new SingletonController();
		}
	}

	@RestController
	private static class SingletonController {

		@RequestMapping("/singletonController")
		public void handle() {
			assertRequestAttributes();
		}
	}

	private static void assertRequestAttributes() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		assertThat(requestAttributes).isInstanceOf(ServletRequestAttributes.class);
		assertRequestAttributes(((ServletRequestAttributes) requestAttributes).getRequest());
	}

	private static void assertRequestAttributes(ServletRequest request) {
		assertThat(request.getAttribute(FROM_CUSTOM_MOCK)).isNull();
		assertThat(request.getAttribute(FROM_MVC_TEST_DEFAULT)).isEqualTo(FROM_MVC_TEST_DEFAULT);
		assertThat(request.getAttribute(FROM_MVC_TEST_MOCK)).isEqualTo(FROM_MVC_TEST_MOCK);
	}

}
