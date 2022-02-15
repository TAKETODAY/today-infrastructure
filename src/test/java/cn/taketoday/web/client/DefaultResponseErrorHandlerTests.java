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

package cn.taketoday.web.client;

import org.junit.jupiter.api.Test;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link DefaultResponseErrorHandler}.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Denys Ivano
 */
public class DefaultResponseErrorHandlerTests {

	private final DefaultResponseErrorHandler handler = new DefaultResponseErrorHandler();

	private final ClientHttpResponse response = mock(ClientHttpResponse.class);


	@Test
	public void hasErrorTrue() throws Exception {
		given(response.getRawStatusCode()).willReturn(HttpStatus.NOT_FOUND.value());
		assertThat(handler.hasError(response)).isTrue();
	}

	@Test
	public void hasErrorFalse() throws Exception {
		given(response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		assertThat(handler.hasError(response)).isFalse();
	}

	@Test
	public void handleError() throws Exception {
		HttpHeaders headers = HttpHeaders.create();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getRawStatusCode()).willReturn(HttpStatus.NOT_FOUND.value());
		given(response.getStatusText()).willReturn("Not Found");
		given(response.getHeaders()).willReturn(headers);
		given(response.getBody()).willReturn(new ByteArrayInputStream("Hello World".getBytes(StandardCharsets.UTF_8)));

		assertThatExceptionOfType(HttpClientErrorException.class)
				.isThrownBy(() -> handler.handleError(response))
				.withMessage("404 Not Found: \"Hello World\"")
				.satisfies(ex -> assertThat(ex.getResponseHeaders()).isSameAs(headers));
	}

	@Test
	public void handleErrorIOException() throws Exception {
		HttpHeaders headers = HttpHeaders.create();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getRawStatusCode()).willReturn(HttpStatus.NOT_FOUND.value());
		given(response.getStatusText()).willReturn("Not Found");
		given(response.getHeaders()).willReturn(headers);
		given(response.getBody()).willThrow(new IOException());

		assertThatExceptionOfType(HttpClientErrorException.class).isThrownBy(() -> handler.handleError(response));
	}

	@Test
	public void handleErrorNullResponse() throws Exception {
		HttpHeaders headers = HttpHeaders.create();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getRawStatusCode()).willReturn(HttpStatus.NOT_FOUND.value());
		given(response.getStatusText()).willReturn("Not Found");
		given(response.getHeaders()).willReturn(headers);

		assertThatExceptionOfType(HttpClientErrorException.class).isThrownBy(() ->
				handler.handleError(response));
	}

	@Test  // SPR-16108
	public void hasErrorForUnknownStatusCode() throws Exception {
		HttpHeaders headers = HttpHeaders.create();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getRawStatusCode()).willReturn(999);
		given(response.getStatusText()).willReturn("Custom status code");
		given(response.getHeaders()).willReturn(headers);

		assertThat(handler.hasError(response)).isFalse();
	}

	@Test // SPR-9406
	public void handleErrorUnknownStatusCode() throws Exception {
		HttpHeaders headers = HttpHeaders.create();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getRawStatusCode()).willReturn(999);
		given(response.getStatusText()).willReturn("Custom status code");
		given(response.getHeaders()).willReturn(headers);

		assertThatExceptionOfType(UnknownHttpStatusCodeException.class).isThrownBy(() ->
				handler.handleError(response));
	}

	@Test  // SPR-17461
	public void hasErrorForCustomClientError() throws Exception {
		HttpHeaders headers = HttpHeaders.create();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getRawStatusCode()).willReturn(499);
		given(response.getStatusText()).willReturn("Custom status code");
		given(response.getHeaders()).willReturn(headers);

		assertThat(handler.hasError(response)).isTrue();
	}

	@Test
	public void handleErrorForCustomClientError() throws Exception {
		int statusCode = 499;
		String statusText = "Custom status code";

		HttpHeaders headers = HttpHeaders.create();
		headers.setContentType(MediaType.TEXT_PLAIN);

		String responseBody = "Hello World";
		TestByteArrayInputStream body = new TestByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8));

		given(response.getRawStatusCode()).willReturn(statusCode);
		given(response.getStatusText()).willReturn(statusText);
		given(response.getHeaders()).willReturn(headers);
		given(response.getBody()).willReturn(body);

		Throwable throwable = catchThrowable(() -> handler.handleError(response));

		// validate exception
		assertThat(throwable).isInstanceOf(UnknownHttpStatusCodeException.class);
		UnknownHttpStatusCodeException actualUnknownHttpStatusCodeException = (UnknownHttpStatusCodeException) throwable;
		assertThat(actualUnknownHttpStatusCodeException.getRawStatusCode()).isEqualTo(statusCode);
		assertThat(actualUnknownHttpStatusCodeException.getStatusText()).isEqualTo(statusText);
		assertThat(actualUnknownHttpStatusCodeException.getResponseHeaders()).isEqualTo(headers);
		assertThat(actualUnknownHttpStatusCodeException.getMessage()).contains(responseBody);
		assertThat(actualUnknownHttpStatusCodeException.getResponseBodyAsString()).isEqualTo(responseBody);
	}

	@Test  // SPR-17461
	public void hasErrorForCustomServerError() throws Exception {
		HttpHeaders headers = HttpHeaders.create();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getRawStatusCode()).willReturn(599);
		given(response.getStatusText()).willReturn("Custom status code");
		given(response.getHeaders()).willReturn(headers);

		assertThat(handler.hasError(response)).isTrue();
	}

	@Test
	public void handleErrorForCustomServerError() throws Exception {
		int statusCode = 599;
		String statusText = "Custom status code";

		HttpHeaders headers = HttpHeaders.create();
		headers.setContentType(MediaType.TEXT_PLAIN);

		String responseBody = "Hello World";
		TestByteArrayInputStream body = new TestByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8));

		given(response.getRawStatusCode()).willReturn(statusCode);
		given(response.getStatusText()).willReturn(statusText);
		given(response.getHeaders()).willReturn(headers);
		given(response.getBody()).willReturn(body);

		Throwable throwable = catchThrowable(() -> handler.handleError(response));

		// validate exception
		assertThat(throwable).isInstanceOf(UnknownHttpStatusCodeException.class);
		UnknownHttpStatusCodeException actualUnknownHttpStatusCodeException = (UnknownHttpStatusCodeException) throwable;
		assertThat(actualUnknownHttpStatusCodeException.getRawStatusCode()).isEqualTo(statusCode);
		assertThat(actualUnknownHttpStatusCodeException.getStatusText()).isEqualTo(statusText);
		assertThat(actualUnknownHttpStatusCodeException.getResponseHeaders()).isEqualTo(headers);
		assertThat(actualUnknownHttpStatusCodeException.getMessage()).contains(responseBody);
		assertThat(actualUnknownHttpStatusCodeException.getResponseBodyAsString()).isEqualTo(responseBody);
	}

	@Test  // SPR-16604
	public void bodyAvailableAfterHasErrorForUnknownStatusCode() throws Exception {
		HttpHeaders headers = HttpHeaders.create();
		headers.setContentType(MediaType.TEXT_PLAIN);
		TestByteArrayInputStream body = new TestByteArrayInputStream("Hello World".getBytes(StandardCharsets.UTF_8));

		given(response.getRawStatusCode()).willReturn(999);
		given(response.getStatusText()).willReturn("Custom status code");
		given(response.getHeaders()).willReturn(headers);
		given(response.getBody()).willReturn(body);

		assertThat(handler.hasError(response)).isFalse();
		assertThat(body.isClosed()).isFalse();
		assertThat(StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8)).isEqualTo("Hello World");
	}


	private static class TestByteArrayInputStream extends ByteArrayInputStream {

		private boolean closed;

		public TestByteArrayInputStream(byte[] buf) {
			super(buf);
			this.closed = false;
		}

		public boolean isClosed() {
			return closed;
		}

		@Override
		public boolean markSupported() {
			return false;
		}

		@Override
		public synchronized void mark(int readlimit) {
			throw new UnsupportedOperationException("mark/reset not supported");
		}

		@Override
		public synchronized void reset() {
			throw new UnsupportedOperationException("mark/reset not supported");
		}

		@Override
		public void close() throws IOException {
			super.close();
			this.closed = true;
		}
	}

}
