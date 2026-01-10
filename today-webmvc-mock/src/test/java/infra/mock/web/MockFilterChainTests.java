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

package infra.mock.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import infra.mock.api.Filter;
import infra.mock.api.FilterChain;
import infra.mock.api.FilterConfig;
import infra.mock.api.MockApi;
import infra.mock.api.MockException;
import infra.mock.api.MockRequest;
import infra.mock.api.MockResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test fixture for {@link MockFilterChain}.
 *
 * @author Rob Winch
 */
class MockFilterChainTests {

	private MockRequest request;

	private MockResponse response;

	@BeforeEach
	void setup() {
		this.request = new HttpMockRequestImpl();
		this.response = new MockHttpResponseImpl();
	}

	@Test
	void constructorNullMock() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new MockFilterChain(null));
	}

	@Test
	void constructorNullFilter() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new MockFilterChain(mock(MockApi.class), (Filter) null));
	}

	@Test
	void doFilterNullRequest() throws Exception {
		MockFilterChain chain = new MockFilterChain();
		assertThatIllegalArgumentException().isThrownBy(() ->
				chain.doFilter(null, this.response));
	}

	@Test
	void doFilterNullResponse() throws Exception {
		MockFilterChain chain = new MockFilterChain();
		assertThatIllegalArgumentException().isThrownBy(() ->
				chain.doFilter(this.request, null));
	}

	@Test
	void doFilterEmptyChain() throws Exception {
		MockFilterChain chain = new MockFilterChain();
		chain.doFilter(this.request, this.response);

		assertThat(chain.getRequest()).isEqualTo(request);
		assertThat(chain.getResponse()).isEqualTo(response);

		assertThatIllegalStateException().isThrownBy(() ->
				chain.doFilter(this.request, this.response))
			.withMessage("This FilterChain has already been called!");
	}

	@Test
	void doFilterWith() throws Exception {
		MockApi mockApi = mock(MockApi.class);
		MockFilterChain chain = new MockFilterChain(mockApi);
		chain.doFilter(this.request, this.response);
		verify(mockApi).service(this.request, this.response);
		assertThatIllegalStateException().isThrownBy(() ->
				chain.doFilter(this.request, this.response))
			.withMessage("This FilterChain has already been called!");
	}

	@Test
	void doFilterWithMockAndFilters() throws Exception {
		MockApi mockApi = mock(MockApi.class);

		MockFilter filter2 = new MockFilter(mockApi);
		MockFilter filter1 = new MockFilter(null);
		MockFilterChain chain = new MockFilterChain(mockApi, filter1, filter2);

		chain.doFilter(this.request, this.response);

		assertThat(filter1.invoked).isTrue();
		assertThat(filter2.invoked).isTrue();

		verify(mockApi).service(this.request, this.response);

		assertThatIllegalStateException().isThrownBy(() ->
				chain.doFilter(this.request, this.response))
			.withMessage("This FilterChain has already been called!");
	}


	private static class MockFilter implements Filter {

		private final MockApi mockApi;

		private boolean invoked;

		public MockFilter(MockApi mockApi) {
			this.mockApi = mockApi;
		}

		@Override
		public void doFilter(MockRequest request, MockResponse response, FilterChain chain)
				throws IOException, MockException {

			this.invoked = true;

			if (this.mockApi != null) {
				this.mockApi.service(request, response);
			}
			else {
				chain.doFilter(request, response);
			}
		}

		@Override
		public void init(FilterConfig filterConfig) throws MockException {
		}

		@Override
		public void destroy() {
		}
	}

}
