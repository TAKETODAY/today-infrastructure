/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.web.mock.assertj;

import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.Test;
import infra.http.HttpHeaders;
import infra.mock.web.MockHttpResponseImpl;
import infra.test.json.JsonContent;
import infra.test.json.JsonConverterDelegate;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link infra.test.web.mock.assertj.AbstractMockHttpServletResponseAssert}.
 *
 * @author Stephane Nicoll
 */
public class AbstractMockHttpServletResponseAssertTests {

	@Test
	void bodyText() {
		MockHttpResponseImpl response = createResponse("OK");
		assertThat(fromResponse(response)).bodyText().isEqualTo("OK");
	}

	@Test
	void bodyJsonWithJsonPath() {
		MockHttpResponseImpl response = createResponse("{\"albumById\": {\"name\": \"Greatest hits\"}}");
		assertThat(fromResponse(response)).bodyJson()
				.extractingPath("$.albumById.name").isEqualTo("Greatest hits");
	}

	@Test
	void bodyJsonCanLoadResourceRelativeToClass() {
		MockHttpResponseImpl response = createResponse("{ \"name\" : \"Spring\", \"age\" : 123 }");
		// See org/springframework/test/json/example.json
		assertThat(fromResponse(response)).bodyJson().withResourceLoadClass(JsonContent.class)
				.isLenientlyEqualTo("example.json");
	}

	@Test
	void bodyWithByteArray() throws UnsupportedEncodingException {
		byte[] bytes = "OK".getBytes(StandardCharsets.UTF_8);
		MockHttpResponseImpl response = new MockHttpResponseImpl();
		response.getWriter().write("OK");
		response.setContentType(StandardCharsets.UTF_8.name());
		assertThat(fromResponse(response)).body().isEqualTo(bytes);
	}

	@Test
	void hasBodyTextEqualTo() throws UnsupportedEncodingException {
		MockHttpResponseImpl response = new MockHttpResponseImpl();
		response.getWriter().write("OK");
		response.setContentType(StandardCharsets.UTF_8.name());
		assertThat(fromResponse(response)).hasBodyTextEqualTo("OK");
	}

	@Test
	void hasForwardedUrl() {
		String forwardedUrl = "https://example.com/42";
		MockHttpResponseImpl response = new MockHttpResponseImpl();
		response.setForwardedUrl(forwardedUrl);
		assertThat(fromResponse(response)).hasForwardedUrl(forwardedUrl);
	}

	@Test
	void hasForwardedUrlWithWrongValue() {
		String forwardedUrl = "https://example.com/42";
		MockHttpResponseImpl response = new MockHttpResponseImpl();
		response.setForwardedUrl(forwardedUrl);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(fromResponse(response)).hasForwardedUrl("another"))
				.withMessageContainingAll("Forwarded URL", forwardedUrl, "another");
	}

	@Test
	void hasRedirectedUrl() {
		String redirectedUrl = "https://example.com/42";
		MockHttpResponseImpl response = new MockHttpResponseImpl();
		response.addHeader(HttpHeaders.LOCATION, redirectedUrl);
		assertThat(fromResponse(response)).hasRedirectedUrl(redirectedUrl);
	}

	@Test
	void hasRedirectedUrlWithWrongValue() {
		String redirectedUrl = "https://example.com/42";
		MockHttpResponseImpl response = new MockHttpResponseImpl();
		response.addHeader(HttpHeaders.LOCATION, redirectedUrl);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(fromResponse(response)).hasRedirectedUrl("another"))
				.withMessageContainingAll("Redirected URL", redirectedUrl, "another");
	}

	@Test
	void hasServletErrorMessage() throws Exception{
		MockHttpResponseImpl response = new MockHttpResponseImpl();
		response.sendError(403, "expected error message");
		assertThat(fromResponse(response)).hasErrorMessage("expected error message");
	}


	private MockHttpResponseImpl createResponse(String body) {
		try {
			MockHttpResponseImpl response = new MockHttpResponseImpl();
			response.setContentType(StandardCharsets.UTF_8.name());
			response.getWriter().write(body);
			return response;
		}
		catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static AssertProvider<ResponseAssert> fromResponse(MockHttpResponseImpl response) {
		return () -> new ResponseAssert(response);
	}


	private static final class ResponseAssert extends infra.test.web.mock.assertj.AbstractMockHttpServletResponseAssert<ResponseAssert, MockHttpResponseImpl> {

		ResponseAssert(MockHttpResponseImpl actual) {
			super((JsonConverterDelegate) null, actual, ResponseAssert.class);
		}

		@Override
		protected MockHttpResponseImpl getResponse() {
			return this.actual;
		}

	}

}
