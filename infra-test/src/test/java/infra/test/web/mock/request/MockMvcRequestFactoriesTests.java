/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.test.web.mock.request;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import infra.http.HttpMethod;
import infra.test.web.mock.MvcResult;
import infra.web.mock.DefaultMockContext;
import infra.web.mock.MockRequest;
import infra.web.mock.api.DispatcherType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MockMvcRequestFactoriesTests {

  @Test
  void uriFactoriesPreserveMethodAndEncodedUri() {
    URI uri = URI.create("/items/a%20b?q=value");
    List<MockHttpRequestBuilder> builders = List.of(
            MockMvcRequestBuilders.get(uri), MockMvcRequestBuilders.post(uri),
            MockMvcRequestBuilders.put(uri), MockMvcRequestBuilders.patch(uri),
            MockMvcRequestBuilders.delete(uri), MockMvcRequestBuilders.options(uri),
            MockMvcRequestBuilders.head(uri), MockMvcRequestBuilders.request(HttpMethod.TRACE, uri),
            MockMvcRequestBuilders.request("GET", uri));
    List<String> methods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD", "TRACE", "GET");
    for (int i = 0; i < builders.size(); i++) {
      MockRequest request = builders.get(i).buildRequest(new DefaultMockContext());
      assertThat(request.getMethod()).isEqualTo(methods.get(i));
      assertThat(request.getRequestURI()).isEqualTo("/items/a%20b");
      assertThat(request.getQueryString()).isEqualTo("q=value");
    }
    assertThatIllegalArgumentException().isThrownBy(() -> MockMvcRequestBuilders.request("PROPFIND", uri))
            .withMessageContaining("PROPFIND");
  }

  @Test
  void templateFactoriesExpandVariablesAndMultipartMethods() {
    List<MockHttpRequestBuilder> builders = List.of(
            MockMvcRequestBuilders.head("/items/{id}", 42),
            MockMvcRequestBuilders.options("/items/{id}", 42),
            MockMvcRequestBuilders.request(HttpMethod.TRACE, "/items/{id}", 42),
            MockMvcRequestBuilders.multipart(HttpMethod.PUT, "/items/{id}", 42),
            MockMvcRequestBuilders.multipart(URI.create("/items/42")),
            MockMvcRequestBuilders.multipart(HttpMethod.PATCH, URI.create("/items/42")));
    List<String> methods = List.of("HEAD", "OPTIONS", "TRACE", "PUT", "POST", "PATCH");
    for (int i = 0; i < builders.size(); i++) {
      MockRequest request = builders.get(i).buildRequest(new DefaultMockContext());
      assertThat(request.getMethod()).isEqualTo(methods.get(i));
      assertThat(request.getRequestURI()).isEqualTo("/items/42");
    }
  }

  @Test
  void asyncDispatchReusesRequestAndClearsAsyncStarted() {
    MvcResult result = mock(MvcResult.class);
    MockRequest request = new MockRequest();
    request.setAsyncStarted(true);
    given(result.getRequest()).willReturn(request);
    given(result.getAsyncResult()).willReturn("ready");
    var builder = MockMvcRequestBuilders.asyncDispatch(result);
    verify(result).getAsyncResult();
    assertThat(builder.buildRequest(new DefaultMockContext())).isSameAs(request);
    assertThat(request.getDispatcherType()).isEqualTo(DispatcherType.ASYNC);
    assertThat(request.isAsyncStarted()).isFalse();
  }

}
