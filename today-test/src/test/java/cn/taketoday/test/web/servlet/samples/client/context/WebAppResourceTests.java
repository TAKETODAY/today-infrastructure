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

package cn.taketoday.test.web.servlet.samples.client.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextHierarchy;
import cn.taketoday.test.context.junit.jupiter.ApplicationExtension;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.test.web.reactive.server.EntityExchangeResult;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.servlet.client.MockMvcWebTestClient;
import cn.taketoday.web.context.WebApplicationContext;
import cn.taketoday.web.servlet.resource.DefaultServletHttpRequestHandler;

import static org.hamcrest.Matchers.containsString;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.handler;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link cn.taketoday.test.web.servlet.samples.context.WebAppResourceTests}.
 *
 * @author Rossen Stoyanchev
 */
@ExtendWith(ApplicationExtension.class)
@WebAppConfiguration("src/test/resources/META-INF/web-resources")
@ContextHierarchy({
	@ContextConfiguration("../../context/root-context.xml"),
	@ContextConfiguration("../../context/servlet-context.xml")
})
public class WebAppResourceTests {

	@Autowired
	private WebApplicationContext wac;

	private WebTestClient testClient;


	@BeforeEach
	public void setup() {
		this.testClient = MockMvcWebTestClient.bindToApplicationContext(this.wac).build();
	}

	// Resources served via <mvc:resources/>

	@Test
	public void resourceRequest() {
		testClient.get().uri("/resources/Spring.js")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType("application/javascript")
				.expectBody(String.class).value(containsString("Spring={};"));
	}

	// Forwarded to the "default" servlet via <mvc:default-servlet-handler/>

	@Test
	public void resourcesViaDefaultServlet() throws Exception {
		EntityExchangeResult<Void> result = testClient.get().uri("/unknown/resource")
				.exchange()
				.expectStatus().isOk()
				.expectBody().isEmpty();

		MockMvcWebTestClient.resultActionsFor(result)
				.andExpect(handler().handlerType(DefaultServletHttpRequestHandler.class))
				.andExpect(forwardedUrl("default"));
	}

}
