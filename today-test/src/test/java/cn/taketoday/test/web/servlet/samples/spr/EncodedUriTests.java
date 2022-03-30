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
import org.junit.jupiter.api.extension.ExtendWith;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.config.BeanPostProcessor;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.PriorityOrdered;
import cn.taketoday.stereotype.Component;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit.jupiter.ApplicationExtension;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.ResultActions;
import cn.taketoday.ui.Model;
import cn.taketoday.web.bind.annotation.PathVariable;
import cn.taketoday.web.bind.annotation.RequestMapping;
import cn.taketoday.web.bind.annotation.RequestMethod;
import cn.taketoday.web.context.WebApplicationContext;
import cn.taketoday.web.servlet.config.annotation.EnableWebMvc;
import cn.taketoday.web.servlet.config.annotation.ViewResolverRegistry;
import cn.taketoday.web.servlet.config.annotation.WebMvcConfigurer;
import cn.taketoday.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import cn.taketoday.web.util.UriComponentsBuilder;

import java.net.URI;

import static org.hamcrest.core.Is.is;
import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.model;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Tests for SPR-11441 (MockMvc accepts an already encoded URI).
 *
 * @author Sebastien Deleuze
 */
@ExtendWith(ApplicationExtension.class)
@WebAppConfiguration
@ContextConfiguration
public class EncodedUriTests {

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;

	@BeforeEach
	public void setup() {
		this.mockMvc = webAppContextSetup(this.wac).build();
	}

	@Test
	public void test() throws Exception {
		String id = "a/b";
		URI url = UriComponentsBuilder.fromUriString("/circuit").pathSegment(id).build().encode().toUri();
		ResultActions result = mockMvc.perform(get(url));
		result.andExpect(status().isOk()).andExpect(model().attribute("receivedId", is(id)));
	}


	@Configuration
	@EnableWebMvc
	static class WebConfig implements WebMvcConfigurer {

		@Bean
		public MyController myController() {
			return new MyController();
		}

		@Bean
		public HandlerMappingConfigurer myHandlerMappingConfigurer() {
			return new HandlerMappingConfigurer();
		}

		@Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			registry.jsp("", "");
		}
	}

	@Controller
	private static class MyController {

		@RequestMapping(value = "/circuit/{id}", method = RequestMethod.GET)
		public String getCircuit(@PathVariable String id, Model model) {
			model.addAttribute("receivedId", id);
			return "result";
		}
	}

	@Component
	private static class HandlerMappingConfigurer implements BeanPostProcessor, PriorityOrdered {

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof RequestMappingHandlerMapping requestMappingHandlerMapping) {
				// URL decode after request mapping, not before.
				requestMappingHandlerMapping.setUrlDecode(false);
			}
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			return bean;
		}

		@Override
		public int getOrder() {
			return PriorityOrdered.HIGHEST_PRECEDENCE;
		}
	}

}
