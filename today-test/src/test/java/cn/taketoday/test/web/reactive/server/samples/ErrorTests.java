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
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.test.web.reactive.server.EntityExchangeResult;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.web.bind.annotation.GetMapping;
import cn.taketoday.web.bind.annotation.PostMapping;
import cn.taketoday.web.bind.annotation.RequestBody;
import cn.taketoday.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests with error status codes or error conditions.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class ErrorTests {

	private final WebTestClient client = WebTestClient.bindToController(new TestController()).build();


	@Test
	void notFound(){
		this.client.get().uri("/invalid")
				.exchange()
				.expectStatus().isNotFound()
				.expectBody(Void.class);
	}

	@Test
	void serverException() {
		this.client.get().uri("/server-error")
				.exchange()
				.expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
				.expectBody(Void.class);
	}

	@Test // SPR-17363
	void badRequestBeforeRequestBodyConsumed() {
		EntityExchangeResult<Void> result = this.client.post()
				.uri("/post")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(new Person("Dan"))
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody().isEmpty();

		byte[] content = result.getRequestBodyContent();
		assertThat(content).isNotNull();
		assertThat(new String(content, StandardCharsets.UTF_8)).isEqualTo("{\"name\":\"Dan\"}");
	}


	@RestController
	static class TestController {

		@GetMapping("/server-error")
		void handleAndThrowException() {
			throw new IllegalStateException("server error");
		}

		@PostMapping(path = "/post", params = "p")
		void handlePost(@RequestBody Person person) {
		}
	}

}
