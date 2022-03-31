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
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.web.bind.annotation.GetMapping;
import cn.taketoday.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for {@link WebTestClient} with soft assertions.
 *
 * @author Michał Rowicki
 * @author Sam Brannen
 * @since 4.0
 */
class SoftAssertionTests {

	private final WebTestClient webTestClient = WebTestClient.bindToController(new TestController()).build();


	@Test
	void expectAll() {
		this.webTestClient.get().uri("/test").exchange()
			.expectAll(
				responseSpec -> responseSpec.expectStatus().isOk(),
				responseSpec -> responseSpec.expectBody(String.class).isEqualTo("hello")
			);
	}

	@Test
	void expectAllWithMultipleFailures() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() ->
						this.webTestClient.get().uri("/test").exchange()
								.expectAll(
										responseSpec -> responseSpec.expectStatus().isBadRequest(),
										responseSpec -> responseSpec.expectStatus().isOk(),
										responseSpec -> responseSpec.expectBody(String.class).isEqualTo("bogus")
								)
				)
				.withMessage("Multiple Exceptions (2):\n" +
						"Status expected:<400 BAD_REQUEST> but was:<200 OK>\n" +
						"Response body expected:<bogus> but was:<hello>");
	}


	@RestController
	static class TestController {

		@GetMapping("/test")
		String handle() {
			return "hello";
		}
	}

}
