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

package cn.taketoday.test.web.reactive.server.samples.bind;

import org.junit.jupiter.api.Test;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.web.server.WebFilter;

import java.nio.charset.StandardCharsets;

import reactor.core.publisher.Mono;

/**
 * Tests for a {@link WebFilter}.
 * @author Rossen Stoyanchev
 */
public class WebFilterTests {

	@Test
	public void testWebFilter() throws Exception {

		WebFilter filter = (exchange, chain) -> {
			DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.allocateBuffer();
			buffer.write("It works!".getBytes(StandardCharsets.UTF_8));
			return exchange.getResponse().writeWith(Mono.just(buffer));
		};

		WebTestClient client = WebTestClient.bindToWebHandler(exchange -> Mono.empty())
				.webFilter(filter)
				.build();

		client.get().uri("/")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("It works!");
	}

}
