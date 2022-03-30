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

package cn.taketoday.test.web.servlet.setup;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ser.impl.UnknownSerializer;

import org.junit.jupiter.api.Test;
import cn.taketoday.http.converter.json.SpringHandlerInstantiator;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.servlet.setup.MockMvcBuilders;
import cn.taketoday.test.web.servlet.setup.StandaloneMockMvcBuilder;
import cn.taketoday.web.bind.annotation.RequestMapping;
import cn.taketoday.web.context.WebApplicationContext;
import cn.taketoday.web.context.support.WebApplicationContextUtils;
import cn.taketoday.web.filter.OncePerRequestFilter;
import cn.taketoday.web.method.HandlerMethod;
import cn.taketoday.web.servlet.HandlerExecutionChain;
import cn.taketoday.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link StandaloneMockMvcBuilder}
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author Sebastien Deleuze
 */
class StandaloneMockMvcBuilderTests {

	@Test  // SPR-10825
	void placeHoldersInRequestMapping() throws Exception {
		TestStandaloneMockMvcBuilder builder = new TestStandaloneMockMvcBuilder(new PlaceholderController());
		builder.addPlaceholderValue("sys.login.ajax", "/foo");
		builder.build();

		RequestMappingHandlerMapping hm = builder.wac.getBean(RequestMappingHandlerMapping.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		HandlerExecutionChain chain = hm.getHandler(request);

		assertThat(chain).isNotNull();
		assertThat(((HandlerMethod) chain.getHandler()).getMethod().getName()).isEqualTo("handleWithPlaceholders");
	}

	@Test  // SPR-13637
	@SuppressWarnings("deprecation")
	void suffixPatternMatch() throws Exception {
		TestStandaloneMockMvcBuilder builder = new TestStandaloneMockMvcBuilder(new PersonController());
		builder.setUseSuffixPatternMatch(false);
		builder.build();

		RequestMappingHandlerMapping hm = builder.wac.getBean(RequestMappingHandlerMapping.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/persons");
		HandlerExecutionChain chain = hm.getHandler(request);
		assertThat(chain).isNotNull();
		assertThat(((HandlerMethod) chain.getHandler()).getMethod().getName()).isEqualTo("persons");

		request = new MockHttpServletRequest("GET", "/persons.xml");
		chain = hm.getHandler(request);
		assertThat(chain).isNull();
	}

	@Test  // SPR-12553
	void applicationContextAttribute() {
		TestStandaloneMockMvcBuilder builder = new TestStandaloneMockMvcBuilder(new PlaceholderController());
		builder.addPlaceholderValue("sys.login.ajax", "/foo");
		WebApplicationContext  wac = builder.initWebAppContext();
		assertThat(WebApplicationContextUtils.getRequiredWebApplicationContext(wac.getServletContext())).isEqualTo(wac);
	}

	@Test
	void addFiltersFiltersNull() {
		StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(new PersonController());
		assertThatIllegalArgumentException().isThrownBy(() ->
				builder.addFilters((Filter[]) null));
	}

	@Test
	void addFiltersFiltersContainsNull() {
		StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(new PersonController());
		assertThatIllegalArgumentException().isThrownBy(() ->
				builder.addFilters(new ContinueFilter(), null));
	}

	@Test
	void addFilterPatternsNull() {
		StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(new PersonController());
		assertThatIllegalArgumentException().isThrownBy(() ->
				builder.addFilter(new ContinueFilter(), (String[]) null));
	}

	@Test
	void addFilterPatternContainsNull() {
		StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(new PersonController());
		assertThatIllegalArgumentException().isThrownBy(() ->
				builder.addFilter(new ContinueFilter(), (String) null));
	}

	@Test  // SPR-13375
	@SuppressWarnings("rawtypes")
	void springHandlerInstantiator() {
		TestStandaloneMockMvcBuilder builder = new TestStandaloneMockMvcBuilder(new PersonController());
		builder.build();
		SpringHandlerInstantiator instantiator = new SpringHandlerInstantiator(builder.wac.getAutowireCapableBeanFactory());
		JsonSerializer serializer = instantiator.serializerInstance(null, null, UnknownSerializer.class);
		assertThat(serializer).isNotNull();
	}


	@Controller
	private static class PlaceholderController {

		@RequestMapping(value = "${sys.login.ajax}")
		private void handleWithPlaceholders() { }
	}


	private static class TestStandaloneMockMvcBuilder extends StandaloneMockMvcBuilder {

		private WebApplicationContext wac;

		private TestStandaloneMockMvcBuilder(Object... controllers) {
			super(controllers);
		}

		@Override
		protected WebApplicationContext initWebAppContext() {
			this.wac = super.initWebAppContext();
			return this.wac;
		}
	}


	@Controller
	private static class PersonController {

		@RequestMapping(value="/persons")
		public String persons() {
			return null;
		}

		@RequestMapping(value="/forward")
		public String forward() {
			return "forward:/persons";
		}
	}


	private static class ContinueFilter extends OncePerRequestFilter {

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) throws ServletException, IOException {

			filterChain.doFilter(request, response);
		}
	}

}
