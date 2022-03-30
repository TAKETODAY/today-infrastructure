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

package cn.taketoday.test.web.servlet.samples.client.standalone.resultmatches;

import org.junit.jupiter.api.Test;
import cn.taketoday.http.MediaType;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.servlet.client.MockMvcWebTestClient;
import cn.taketoday.web.bind.annotation.RequestMapping;
import cn.taketoday.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link cn.taketoday.test.web.servlet.samples.standalone.resultmatchers.ContentAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class ContentAssertionTests {

	private final WebTestClient testClient =
			MockMvcWebTestClient.bindToController(new SimpleController()).build();

	@Test
	public void testContentType() {
		testClient.get().uri("/handle").accept(MediaType.TEXT_PLAIN)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.valueOf("text/plain;charset=ISO-8859-1"))
				.expectHeader().contentType("text/plain;charset=ISO-8859-1")
				.expectHeader().contentTypeCompatibleWith("text/plain")
				.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN);

		testClient.get().uri("/handleUtf8")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.valueOf("text/plain;charset=UTF-8"))
				.expectHeader().contentType("text/plain;charset=UTF-8")
				.expectHeader().contentTypeCompatibleWith("text/plain")
				.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN);
	}

	@Test
	public void testContentAsString() {

		testClient.get().uri("/handle").accept(MediaType.TEXT_PLAIN)
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("Hello world!");

		testClient.get().uri("/handleUtf8").accept(MediaType.TEXT_PLAIN)
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01");

		// Hamcrest matchers...
		testClient.get().uri("/handle").accept(MediaType.TEXT_PLAIN)
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).value(equalTo("Hello world!"));
		testClient.get().uri("/handleUtf8")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).value(equalTo("\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01"));
	}

	@Test
	public void testContentAsBytes() {

		testClient.get().uri("/handle").accept(MediaType.TEXT_PLAIN)
				.exchange()
				.expectStatus().isOk()
				.expectBody(byte[].class).isEqualTo(
				"Hello world!".getBytes(StandardCharsets.ISO_8859_1));

		testClient.get().uri("/handleUtf8")
				.exchange()
				.expectStatus().isOk()
				.expectBody(byte[].class).isEqualTo(
				"\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void testContentStringMatcher() {
		testClient.get().uri("/handle").accept(MediaType.TEXT_PLAIN)
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).value(containsString("world"));
	}

	@Test
	public void testCharacterEncoding() {

		testClient.get().uri("/handle").accept(MediaType.TEXT_PLAIN)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType("text/plain;charset=ISO-8859-1")
				.expectBody(String.class).value(containsString("world"));

		testClient.get().uri("/handleUtf8")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType("text/plain;charset=UTF-8")
				.expectBody(byte[].class)
				.isEqualTo("\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01".getBytes(StandardCharsets.UTF_8));
	}


	@Controller
	private static class SimpleController {

		@RequestMapping(value="/handle", produces="text/plain")
		@ResponseBody
		public String handle() {
			return "Hello world!";
		}

		@RequestMapping(value="/handleUtf8", produces="text/plain;charset=UTF-8")
		@ResponseBody
		public String handleWithCharset() {
			return "\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01";	// "Hello world! (Japanese)
		}
	}

}
