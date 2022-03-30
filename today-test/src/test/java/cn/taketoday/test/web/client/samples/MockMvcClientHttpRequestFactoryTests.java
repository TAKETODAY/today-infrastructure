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

package cn.taketoday.test.web.client.samples;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.ComponentScan;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit.jupiter.ApplicationExtension;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.test.web.client.MockMvcClientHttpRequestFactory;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.setup.MockMvcBuilders;
import cn.taketoday.web.bind.annotation.RequestMapping;
import cn.taketoday.web.bind.annotation.RequestMethod;
import cn.taketoday.web.bind.annotation.ResponseBody;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.context.WebApplicationContext;
import cn.taketoday.web.servlet.config.annotation.EnableWebMvc;
import cn.taketoday.web.servlet.config.annotation.WebMvcConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests that use a {@link RestTemplate} configured with a
 * {@link MockMvcClientHttpRequestFactory} that is in turn configured with a
 * {@link MockMvc} instance that uses a {@link WebApplicationContext} loaded by
 * the TestContext framework.
 *
 * @author Rossen Stoyanchev
 */
@ExtendWith(ApplicationExtension.class)
@WebAppConfiguration
@ContextConfiguration
public class MockMvcClientHttpRequestFactoryTests {

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;


	@BeforeEach
	public void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).alwaysExpect(status().isOk()).build();
	}

	@Test
	public void test() {
		RestTemplate template = new RestTemplate(new MockMvcClientHttpRequestFactory(this.mockMvc));
		String result = template.getForObject("/foo", String.class);
		assertThat(result).isEqualTo("bar");
	}


	@EnableWebMvc
	@Configuration
	@ComponentScan(basePackageClasses=MockMvcClientHttpRequestFactoryTests.class)
	static class MyWebConfig implements WebMvcConfigurer {
	}

	@Controller
	static class MyController {

		@RequestMapping(value="/foo", method=RequestMethod.GET)
		@ResponseBody
		public String handle() {
			return "bar";
		}
	}

}
