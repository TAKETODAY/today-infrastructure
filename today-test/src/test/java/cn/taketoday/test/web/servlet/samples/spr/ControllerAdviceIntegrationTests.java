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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Scope;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.context.junit.jupiter.web.JUnitWebConfig;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.ui.Model;
import cn.taketoday.web.bind.annotation.ControllerAdvice;
import cn.taketoday.web.bind.annotation.GetMapping;
import cn.taketoday.web.bind.annotation.ModelAttribute;
import cn.taketoday.web.bind.annotation.RequestParam;
import cn.taketoday.web.context.WebApplicationContext;
import cn.taketoday.web.context.annotation.RequestScope;
import cn.taketoday.web.servlet.config.annotation.EnableWebMvc;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Integration tests for {@link ControllerAdvice @ControllerAdvice}.
 *
 * <p>Introduced in conjunction with
 * <a href="https://github.com/spring-projects/spring-framework/issues/24017">gh-24017</a>.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitWebConfig
class ControllerAdviceIntegrationTests {

	MockMvc mockMvc;

	@BeforeEach
	void setUpMockMvc(WebApplicationContext wac) {
		this.mockMvc = webAppContextSetup(wac).build();
		resetCounters();
	}

	@Test
	void controllerAdviceIsAppliedOnlyOnce() throws Exception {
		this.mockMvc.perform(get("/test").param("requestParam", "foo"))//
				.andExpect(status().isOk())//
				.andExpect(forwardedUrl("singleton:1;prototype:1;request-scoped:1;requestParam:foo"));

		assertThat(SingletonControllerAdvice.invocationCount).hasValue(1);
		assertThat(PrototypeControllerAdvice.invocationCount).hasValue(1);
		assertThat(RequestScopedControllerAdvice.invocationCount).hasValue(1);
	}

	@Test
	void prototypeAndRequestScopedControllerAdviceBeansAreNotCached() throws Exception {
		this.mockMvc.perform(get("/test").param("requestParam", "foo"))//
				.andExpect(status().isOk())//
				.andExpect(forwardedUrl("singleton:1;prototype:1;request-scoped:1;requestParam:foo"));

		// singleton @ControllerAdvice beans should not be instantiated again.
		assertThat(SingletonControllerAdvice.instanceCount).hasValue(0);
		// prototype and request-scoped @ControllerAdvice beans should be instantiated once per request.
		assertThat(PrototypeControllerAdvice.instanceCount).hasValue(1);
		assertThat(RequestScopedControllerAdvice.instanceCount).hasValue(1);

		this.mockMvc.perform(get("/test").param("requestParam", "bar"))//
				.andExpect(status().isOk())//
				.andExpect(forwardedUrl("singleton:2;prototype:2;request-scoped:2;requestParam:bar"));

		// singleton @ControllerAdvice beans should not be instantiated again.
		assertThat(SingletonControllerAdvice.instanceCount).hasValue(0);
		// prototype and request-scoped @ControllerAdvice beans should be instantiated once per request.
		assertThat(PrototypeControllerAdvice.instanceCount).hasValue(2);
		assertThat(RequestScopedControllerAdvice.instanceCount).hasValue(2);
	}

	private void resetCounters() {
		SingletonControllerAdvice.invocationCount.set(0);
		SingletonControllerAdvice.instanceCount.set(0);
		PrototypeControllerAdvice.invocationCount.set(0);
		PrototypeControllerAdvice.instanceCount.set(0);
		RequestScopedControllerAdvice.invocationCount.set(0);
		RequestScopedControllerAdvice.instanceCount.set(0);
	}

	@Configuration
	@EnableWebMvc
	static class Config {

		@Bean
		TestController testController() {
			return new TestController();
		}

		@Bean
		SingletonControllerAdvice singletonControllerAdvice() {
			return new SingletonControllerAdvice();
		}

		@Bean
		@Scope("prototype")
		PrototypeControllerAdvice prototypeControllerAdvice() {
			return new PrototypeControllerAdvice();
		}

		@Bean
		@RequestScope
		RequestScopedControllerAdvice requestScopedControllerAdvice() {
			return new RequestScopedControllerAdvice();
		}
	}

	@ControllerAdvice
	static class SingletonControllerAdvice {

		static final AtomicInteger instanceCount = new AtomicInteger();
		static final AtomicInteger invocationCount = new AtomicInteger();

		{
			instanceCount.incrementAndGet();
		}

		@ModelAttribute
		void initModel(Model model) {
			model.addAttribute("singleton", invocationCount.incrementAndGet());
		}
	}

	@ControllerAdvice
	static class PrototypeControllerAdvice {

		static final AtomicInteger instanceCount = new AtomicInteger();
		static final AtomicInteger invocationCount = new AtomicInteger();

		{
			instanceCount.incrementAndGet();
		}

		@ModelAttribute
		void initModel(Model model) {
			model.addAttribute("prototype", invocationCount.incrementAndGet());
		}
	}

	@ControllerAdvice
	static class RequestScopedControllerAdvice {

		static final AtomicInteger instanceCount = new AtomicInteger();
		static final AtomicInteger invocationCount = new AtomicInteger();

		{
			instanceCount.incrementAndGet();
		}

		@ModelAttribute
		void initModel(@RequestParam String requestParam, Model model) {
			model.addAttribute("requestParam", requestParam);
			model.addAttribute("request-scoped", invocationCount.incrementAndGet());
		}
	}

	@Controller
	static class TestController {

		@GetMapping("/test")
		String get(Model model) {
			return "singleton:" + model.getAttribute("singleton") +
					";prototype:" + model.getAttribute("prototype") +
					";request-scoped:" + model.getAttribute("request-scoped") +
					";requestParam:" + model.getAttribute("requestParam");
		}
	}

}
