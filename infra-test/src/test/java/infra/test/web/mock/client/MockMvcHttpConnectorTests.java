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

package infra.test.web.mock.client;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.http.HttpCookie;
import infra.http.HttpMethod;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.MvcResult;
import infra.test.web.mock.RequestBuilder;
import infra.test.web.mock.ResultActions;
import infra.test.web.mock.request.MockHttpRequestBuilder;
import infra.test.web.reactive.server.ExchangeResult;
import infra.test.web.reactive.server.MockServerClientHttpResponse;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;
import infra.web.mock.DefaultMockContext;
import infra.web.mock.MockServerHttpExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MockMvcHttpConnectorTests {

  @Test
  void requestAdaptationPreservesBodyCookiesAndCustomizers() throws Exception {
    MockMvc mvc = mock(MockMvc.class);
    MvcResult result = mock(MvcResult.class);
    MockRequest serverRequest = new MockRequest();
    MockResponse serverResponse = new MockResponse();
    given(result.getRequest()).willReturn(serverRequest);
    given(result.getResponse()).willReturn(serverResponse);
    given(result.getExchange()).willReturn(new MockServerHttpExchange(null, serverRequest, serverResponse));
    ResultActions actions = ResultActions.forMvcResult(result);
    given(mvc.perform(any())).willAnswer(invocation -> {
      MockHttpRequestBuilder builder = invocation.getArgument(0);
      MockRequest request = builder.buildRequest(new DefaultMockContext());
      builder.customize(request, new MockServerHttpExchange(request));
      assertThat(request.getContentAsByteArray()).isEqualTo("payload".getBytes(StandardCharsets.UTF_8));
      assertThat(request.getHeader("X-Test")).isEqualTo("header");
      assertThat(request.getAttribute("customized")).isEqualTo(true);
      assertThat(request.getCookies()).extracting("name").containsExactly("session", "session");
      return actions;
    });
    var connector = new MockMvcHttpConnector(mvc).with(List.of((request, context) -> {
      request.setAttribute("customized", true);
    }));
    var response = connector.connect(HttpMethod.POST, URI.create("/body"), request -> {
      request.getHeaders().add("X-Test", "header");
      request.getCookies().add("session", new HttpCookie("session", "one"));
      request.getCookies().add("session", new HttpCookie("session", "two"));
      return request.writeWith(Mono.just(DefaultDataBufferFactory.sharedInstance
              .wrap("payload".getBytes(StandardCharsets.UTF_8))));
    }).block();
    assertThat(response).isNotNull();
    MvcResult adapted = (MvcResult) ((MockServerClientHttpResponse) response).getServerResult();
    adapted.getUnresolvedException();
    adapted.getAsyncResult();
    adapted.getAsyncResult(1);
    verify(result).getUnresolvedException();
    verify(result).getAsyncResult();
    verify(result).getAsyncResult(1);
    assertThat(adapted.toString()).contains("HttpRequest:", "HttpResponse:", "Status = 200");
  }

  @Test
  void emptyRequestBodyAndMvcFailureArePropagated() throws Exception {
    MockMvc mvc = mock(MockMvc.class);
    given(mvc.perform(any())).willAnswer(invocation -> {
      MockRequest request = invocation.<RequestBuilder>getArgument(0).buildRequest(new DefaultMockContext());
      assertThat(request.getContentAsByteArray()).isNullOrEmpty();
      throw new IllegalStateException("perform failed");
    });
    var connector = new MockMvcHttpConnector(mvc);
    assertThatIllegalStateException().isThrownBy(() -> connector.connect(HttpMethod.GET, URI.create("/empty"),
            request -> request.setComplete()).block()).withMessage("perform failed");
  }

  @Test
  void resultActionsRejectNonMvcServerResults() {
    ExchangeResult result = mock(ExchangeResult.class);
    assertThatIllegalArgumentException().isThrownBy(() -> MockMvcWebTestClient.resultActionsFor(result))
            .withMessageEndingWith("null");
    given(result.getMockServerResult()).willReturn("wrong result");
    assertThatIllegalArgumentException().isThrownBy(() -> MockMvcWebTestClient.resultActionsFor(result))
            .withMessageEndingWith("java.lang.String");
    assertThat(MockMvcWebTestClient.bindTo(mock(MockMvc.class))).isNotNull();
  }

}
