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

package cn.taketoday.test.web.reactive.server.samples;

import org.junit.jupiter.api.Test;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.web.bind.annotation.CookieValue;
import cn.taketoday.web.bind.annotation.GetMapping;
import cn.taketoday.web.bind.annotation.RequestHeader;
import cn.taketoday.web.bind.annotation.RestController;

/**
 * Tests with headers and cookies.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class HeaderAndCookieTests {

	private final WebTestClient client = WebTestClient.bindToController(new TestController()).build();


	@Test
	void requestResponseHeaderPair() {
		this.client.get().uri("/header-echo").header("h1", "in")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals("h1", "in-out");
	}

	@Test
	void headerMultipleValues() {
		this.client.get().uri("/header-multi-value")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals("h1", "v1", "v2", "v3");
	}

	@Test
	void setCookies() {
		this.client.get().uri("/cookie-echo")
				.cookies(cookies -> cookies.add("k1", "v1"))
				.exchange()
				.expectHeader().valueMatches("Set-Cookie", "k1=v1");
	}


	@RestController
	static class TestController {

		@GetMapping("header-echo")
		ResponseEntity<Void> handleHeader(@RequestHeader("h1") String myHeader) {
			String value = myHeader + "-out";
			return ResponseEntity.ok().header("h1", value).build();
		}

		@GetMapping("header-multi-value")
		ResponseEntity<Void> multiValue() {
			return ResponseEntity.ok().header("h1", "v1", "v2", "v3").build();
		}

		@GetMapping("cookie-echo")
		ResponseEntity<Void> handleCookie(@CookieValue("k1") String cookieValue) {
			HttpHeaders headers = HttpHeaders.create();
			headers.set("Set-Cookie", "k1=" + cookieValue);
			return new ResponseEntity<>(headers, HttpStatus.OK);
		}
	}

}
