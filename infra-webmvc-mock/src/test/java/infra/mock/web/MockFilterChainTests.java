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

import infra.web.mock.MockFilterChain;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;
import infra.web.mock.api.MockHandler;
import infra.web.Filter;
import infra.web.FilterChain;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;
import infra.web.mock.MockUtils;

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
    this.request = new MockRequest();
    this.response = new MockResponse();
  }

  @Test
  void constructorNullMock() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new MockFilterChain(null));
  }

  @Test
  void constructorNullFilter() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new MockFilterChain(mock(MockHandler.class), (Filter) null));
  }

  @Test
  void doFilterNullRequest() throws Exception {
    MockFilterChain chain = new MockFilterChain();
    assertThatIllegalArgumentException().isThrownBy(() ->
            chain.doFilter(null));
  }

  @Test
  void doFilterEmptyChain() throws Exception {
    MockFilterChain chain = new MockFilterChain();
    chain.doFilter(new MockRequestContext(request, response));

    assertThat(chain.getRequest()).isEqualTo(request);
    assertThat(chain.getResponse()).isEqualTo(response);

    assertThatIllegalStateException().isThrownBy(() ->
                    chain.doFilter(new MockRequestContext(request, response)))
            .withMessage("This FilterChain has already been called!");
  }

  @Test
  void doFilterWith() throws Exception {
    MockHandler mockHandler = mock(MockHandler.class);
    MockFilterChain chain = new MockFilterChain(mockHandler);
    chain.doFilter(new MockRequestContext(request, response));
    verify(mockHandler).service(request, response);
    assertThatIllegalStateException().isThrownBy(() ->
                    chain.doFilter(new MockRequestContext(request, response)))
            .withMessage("This FilterChain has already been called!");
  }

  @Test
  void doFilterWithMockAndFilters() throws Exception {
    MockHandler mockHandler = mock(MockHandler.class);

    MockFilter filter2 = new MockFilter(mockHandler);
    MockFilter filter1 = new MockFilter(null);
    MockFilterChain chain = new MockFilterChain(mockHandler, filter1, filter2);

    chain.doFilter(new MockRequestContext(request, response));

    assertThat(filter1.invoked).isTrue();
    assertThat(filter2.invoked).isTrue();

    verify(mockHandler).service(request, response);

    assertThatIllegalStateException().isThrownBy(() ->
                    chain.doFilter(new MockRequestContext(request, response)))
            .withMessage("This FilterChain has already been called!");
  }

  private static class MockFilter implements Filter {

    private final MockHandler mockHandler;

    private boolean invoked;

    public MockFilter(MockHandler mockHandler) {
      this.mockHandler = mockHandler;
    }

    @Override
    public void doFilter(RequestContext request, FilterChain chain) throws Exception {
      this.invoked = true;
      if (this.mockHandler != null) {
        this.mockHandler.service(MockUtils.getMockRequest(request), MockUtils.getMockResponse(request));
      }
      else {
        chain.doFilter(request);
      }
    }

  }

}
