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

package cn.taketoday.test.web.reactive.server;

import org.junit.jupiter.api.Test;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.client.reactive.ClientHttpConnector;
import cn.taketoday.http.client.reactive.ClientHttpRequest;
import cn.taketoday.http.client.reactive.ClientHttpResponse;
import cn.taketoday.mock.http.client.reactive.MockClientHttpRequest;
import cn.taketoday.mock.http.client.reactive.MockClientHttpResponse;
import cn.taketoday.web.reactive.function.client.ClientRequest;
import cn.taketoday.web.reactive.function.client.ExchangeFunction;
import cn.taketoday.web.reactive.function.client.ExchangeFunctions;

import java.net.URI;
import java.time.Duration;

import reactor.core.publisher.Mono;

import static java.time.Duration.ofMillis;

/**
 * Unit tests for {@link WiretapConnector}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WiretapConnectorTests {

	@Test
	public void captureAndClaim() {
		ClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, "/test");
		ClientHttpResponse response = new MockClientHttpResponse(HttpStatus.OK);
		ClientHttpConnector connector = (method, uri, fn) -> fn.apply(request).then(Mono.just(response));

		ClientRequest clientRequest = ClientRequest.create(HttpMethod.GET, URI.create("/test"))
				.header(WebTestClient.WEBTESTCLIENT_REQUEST_ID, "1").build();

		WiretapConnector wiretapConnector = new WiretapConnector(connector);
		ExchangeFunction function = ExchangeFunctions.create(wiretapConnector);
		function.exchange(clientRequest).block(ofMillis(0));

		ExchangeResult result = wiretapConnector.getExchangeResult("1", null, Duration.ZERO);
		assertThat(result.getMethod()).isEqualTo(HttpMethod.GET);
		assertThat(result.getUrl().toString()).isEqualTo("/test");
	}

}
