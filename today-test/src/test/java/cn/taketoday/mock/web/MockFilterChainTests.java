/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.mock.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import cn.taketoday.mock.api.Filter;
import cn.taketoday.mock.api.FilterChain;
import cn.taketoday.mock.api.FilterConfig;
import cn.taketoday.mock.api.MockApi;
import cn.taketoday.mock.api.MockException;
import cn.taketoday.mock.api.MockRequest;
import cn.taketoday.mock.api.MockResponse;

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
