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

package cn.taketoday.test.web.servlet.samples.client.standalone;

import org.junit.jupiter.api.Test;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.servlet.client.MockMvcWebTestClient;
import cn.taketoday.test.web.servlet.request.RequestPostProcessor;
import cn.taketoday.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import cn.taketoday.test.web.servlet.setup.MockMvcConfigurerAdapter;
import cn.taketoday.util.Assert;
import cn.taketoday.web.bind.annotation.RequestMapping;
import cn.taketoday.web.bind.annotation.ResponseBody;
import cn.taketoday.web.context.WebApplicationContext;

import java.security.Principal;

import static org.mockito.Mockito.mock;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link cn.taketoday.test.web.servlet.samples.standalone.FrameworkExtensionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class FrameworkExtensionTests {

	private final WebTestClient client =
			MockMvcWebTestClient.bindToController(new SampleController())
					.apply(defaultSetup())
					.build();


	@Test
	public void fooHeader() {
		this.client.get().uri("/")
				.header("Foo", "a=b")
				.exchange()
				.expectBody(String.class).isEqualTo("Foo");
	}

	@Test
	public void barHeader() {
		this.client.get().uri("/")
				.header("Bar", "a=b")
				.exchange()
				.expectBody(String.class).isEqualTo("Bar");
	}

	private static TestMockMvcConfigurer defaultSetup() {
		return new TestMockMvcConfigurer();
	}


	/**
	 * Test {@code MockMvcConfigurer}.
	 */
	private static class TestMockMvcConfigurer extends MockMvcConfigurerAdapter {

		@Override
		public void afterConfigurerAdded(ConfigurableMockMvcBuilder<?> builder) {
			builder.alwaysExpect(status().isOk());
		}

		@Override
		public RequestPostProcessor beforeMockMvcCreated(ConfigurableMockMvcBuilder<?> builder,
				WebApplicationContext context) {
			return request -> {
				request.setUserPrincipal(mock(Principal.class));
				return request;
			};
		}
	}


	@Controller
	@RequestMapping("/")
	private static class SampleController {

		@RequestMapping(headers = "Foo")
		@ResponseBody
		public String handleFoo(Principal principal) {
			Assert.notNull(principal, "Principal must not be null");
			return "Foo";
		}

		@RequestMapping(headers = "Bar")
		@ResponseBody
		public String handleBar(Principal principal) {
			Assert.notNull(principal, "Principal must not be null");
			return "Bar";
		}
	}

}
