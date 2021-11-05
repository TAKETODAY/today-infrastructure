/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.http.client.reactive;

import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.Message;
import org.reactivestreams.Publisher;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.web.http.HttpHeaders;
import cn.taketoday.web.http.HttpStatus;
import cn.taketoday.web.http.ResponseCookie;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import reactor.core.publisher.Flux;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link ClientHttpResponse} implementation for the Apache HttpComponents HttpClient 5.x.
 *
 * @author Martin Tarjányi
 * @author Arjen Poutsma
 * @since 4.0
 * @see <a href="https://hc.apache.org/index.html">Apache HttpComponents</a>
 */
class HttpComponentsClientHttpResponse implements ClientHttpResponse {

	private final DataBufferFactory dataBufferFactory;

	private final Message<HttpResponse, Publisher<ByteBuffer>> message;

	private final HttpHeaders headers;

	private final HttpClientContext context;

	private final AtomicBoolean rejectSubscribers = new AtomicBoolean();


	public HttpComponentsClientHttpResponse(DataBufferFactory dataBufferFactory,
			Message<HttpResponse, Publisher<ByteBuffer>> message, HttpClientContext context) {

		this.dataBufferFactory = dataBufferFactory;
		this.message = message;
		this.context = context;

		MultiValueMap<String, String> adapter = new HttpComponentsHeadersAdapter(message.getHead());
		this.headers = HttpHeaders.readOnlyHttpHeaders(adapter);
	}


	@Override
	public HttpStatus getStatusCode() {
		return HttpStatus.valueOf(this.message.getHead().getCode());
	}

	@Override
	public int getRawStatusCode() {
		return this.message.getHead().getCode();
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		LinkedMultiValueMap<String, ResponseCookie> result = new LinkedMultiValueMap<>();
		this.context.getCookieStore().getCookies().forEach(cookie ->
				result.add(cookie.getName(),
						ResponseCookie.fromClientResponse(cookie.getName(), cookie.getValue())
								.domain(cookie.getDomain())
								.path(cookie.getPath())
								.maxAge(getMaxAgeSeconds(cookie))
								.secure(cookie.isSecure())
								.httpOnly(cookie.containsAttribute("httponly"))
								.sameSite(cookie.getAttribute("samesite"))
								.build()));
		return result;
	}

	private long getMaxAgeSeconds(Cookie cookie) {
		String maxAgeAttribute = cookie.getAttribute(Cookie.MAX_AGE_ATTR);
		return (maxAgeAttribute != null ? Long.parseLong(maxAgeAttribute) : -1);
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return Flux.from(this.message.getBody())
				.doOnSubscribe(s -> {
					if (!this.rejectSubscribers.compareAndSet(false, true)) {
						throw new IllegalStateException("The client response body can only be consumed once.");
					}
				})
				.map(this.dataBufferFactory::wrap);
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

}
