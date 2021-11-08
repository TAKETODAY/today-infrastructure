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
import cn.taketoday.core.TypeReference;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.util.MediaType;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.http.converter.GenericHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageNotReadableException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test fixture for {@link HttpMessageConverter}.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author Sam Brannen
 */
class HttpMessageConverterExtractorTests {

	@SuppressWarnings("unchecked")
	private final HttpMessageConverter<String> converter = mock(HttpMessageConverter.class);
	private final HttpMessageConverterExtractor<?> extractor = new HttpMessageConverterExtractor<>(String.class, asList(converter));
	private final MediaType contentType = MediaType.TEXT_PLAIN;
	private final HttpHeaders responseHeaders = HttpHeaders.create();
	private final ClientHttpResponse response = mock(ClientHttpResponse.class);


	@Test
	void constructorPreconditions() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new HttpMessageConverterExtractor<>(String.class, (List<HttpMessageConverter<?>>) null))
				.withMessage("'messageConverters' must not be empty");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new HttpMessageConverterExtractor<>(String.class, Arrays.asList(null, this.converter)))
				.withMessage("'messageConverters' must not contain null elements");
	}

	@Test
	void noContent() throws IOException {
		given(response.getRawStatusCode()).willReturn(HttpStatus.NO_CONTENT.value());

		Object result = extractor.extractData(response);
		assertThat(result).isNull();
	}

	@Test
	void notModified() throws IOException {
		given(response.getRawStatusCode()).willReturn(HttpStatus.NOT_MODIFIED.value());

		Object result = extractor.extractData(response);
		assertThat(result).isNull();
	}

	@Test
	void informational() throws IOException {
		given(response.getRawStatusCode()).willReturn(HttpStatus.CONTINUE.value());

		Object result = extractor.extractData(response);
		assertThat(result).isNull();
	}

	@Test
	void zeroContentLength() throws IOException {
		responseHeaders.setContentLength(0);
		given(response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		given(response.getHeaders()).willReturn(responseHeaders);

		Object result = extractor.extractData(response);
		assertThat(result).isNull();
	}

	@Test
	void emptyMessageBody() throws IOException {
		given(response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream("".getBytes()));

		Object result = extractor.extractData(response);
		assertThat(result).isNull();
	}

	@Test // gh-22265
	void nullMessageBody() throws IOException {
		given(response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(null);

		Object result = extractor.extractData(response);
		assertThat(result).isNull();
	}

	@Test
	void normal() throws IOException {
		responseHeaders.setContentType(contentType);
		String expected = "Foo";
		given(response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream(expected.getBytes()));
		given(converter.canRead(String.class, contentType)).willReturn(true);
		given(converter.read(eq(String.class), any(HttpInputMessage.class))).willReturn(expected);

		Object result = extractor.extractData(response);
		assertThat(result).isEqualTo(expected);
	}

	@Test
	void cannotRead() throws IOException {
		responseHeaders.setContentType(contentType);
		given(response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream("Foobar".getBytes()));
		given(converter.canRead(String.class, contentType)).willReturn(false);
		assertThatExceptionOfType(RestClientException.class).isThrownBy(() -> extractor.extractData(response));
	}

	@Test
	@SuppressWarnings("unchecked")
	void generics() throws IOException {
		responseHeaders.setContentType(contentType);
		String expected = "Foo";
		TypeReference<List<String>> reference = new TypeReference<List<String>>() {};
		Type type = reference.getType();

		GenericHttpMessageConverter<String> converter = mock(GenericHttpMessageConverter.class);
		HttpMessageConverterExtractor<?> extractor = new HttpMessageConverterExtractor<List<String>>(type, asList(converter));

		given(response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream(expected.getBytes()));
		given(converter.canRead(type, null, contentType)).willReturn(true);
		given(converter.read(eq(type), eq(null), any(HttpInputMessage.class))).willReturn(expected);

		Object result = extractor.extractData(response);
		assertThat(result).isEqualTo(expected);
	}

	@Test  // SPR-13592
	void converterThrowsIOException() throws IOException {
		responseHeaders.setContentType(contentType);
		given(response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream("Foobar".getBytes()));
		given(converter.canRead(String.class, contentType)).willReturn(true);
		given(converter.read(eq(String.class), any(HttpInputMessage.class))).willThrow(IOException.class);
		assertThatExceptionOfType(RestClientException.class).isThrownBy(() -> extractor.extractData(response))
			.withMessageContaining("Error while extracting response for type [class java.lang.String] and content type [text/plain]")
			.withCauseInstanceOf(IOException.class);
	}

	@Test  // SPR-13592
	void converterThrowsHttpMessageNotReadableException() throws IOException {
		responseHeaders.setContentType(contentType);
		given(response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream("Foobar".getBytes()));
		given(converter.canRead(String.class, contentType)).willThrow(HttpMessageNotReadableException.class);
		assertThatExceptionOfType(RestClientException.class).isThrownBy(() -> extractor.extractData(response))
			.withMessageContaining("Error while extracting response for type [class java.lang.String] and content type [text/plain]")
			.withCauseInstanceOf(HttpMessageNotReadableException.class);
	}

	@Test
	void unknownContentTypeExceptionContainsCorrectResponseBody() throws IOException {
		responseHeaders.setContentType(contentType);
		given(response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream("Foobar".getBytes())  {
			@Override
			public boolean markSupported() {
				return false;
			}
		});
		given(converter.canRead(String.class, contentType)).willReturn(false);

		assertThatExceptionOfType(UnknownContentTypeException.class).isThrownBy(() -> extractor.extractData(response))
			.satisfies(exception -> assertThat(exception.getResponseBodyAsString()).isEqualTo("Foobar"));
	}

}
