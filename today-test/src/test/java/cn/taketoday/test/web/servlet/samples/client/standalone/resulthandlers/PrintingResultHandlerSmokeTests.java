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

package cn.taketoday.test.web.servlet.samples.client.standalone.resulthandlers;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import cn.taketoday.http.MediaType;
import cn.taketoday.test.web.reactive.server.EntityExchangeResult;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.servlet.client.MockMvcWebTestClient;
import cn.taketoday.web.bind.annotation.PostMapping;
import cn.taketoday.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link cn.taketoday.test.web.servlet.samples.standalone.resulthandlers.PrintingResultHandlerSmokeTests}.
 *
 * @author Rossen Stoyanchev
 */
@Disabled
public class PrintingResultHandlerSmokeTests {

	private final WebTestClient testClient =
			MockMvcWebTestClient.bindToController(new SimpleController()).build();


	// Not intended to be executed with the build.
	// Comment out class-level @Disabled to see the output.

	@Test
	public void printViaConsumer() {
		testClient.post().uri("/")
				.contentType(MediaType.TEXT_PLAIN)
				.bodyValue("Hello Request".getBytes(StandardCharsets.UTF_8))
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.consumeWith(System.out::println);
	}

	@Test
	public void returnResultAndPrint() {
		EntityExchangeResult<String> result = testClient.post().uri("/")
				.contentType(MediaType.TEXT_PLAIN)
				.bodyValue("Hello Request".getBytes(StandardCharsets.UTF_8))
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.returnResult();

		System.out.println(result);
	}


	@RestController
	private static class SimpleController {

		@PostMapping("/")
		public String hello(HttpServletResponse response) {
			response.addCookie(new Cookie("enigma", "42"));
			return "Hello Response";
		}
	}
}
