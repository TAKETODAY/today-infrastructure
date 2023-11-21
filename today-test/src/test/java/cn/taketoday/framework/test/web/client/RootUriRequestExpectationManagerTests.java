/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.framework.test.web.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.http.client.support.HttpRequestDecorator;
import cn.taketoday.test.web.client.ExpectedCount;
import cn.taketoday.test.web.client.MockRestServiceServer;
import cn.taketoday.test.web.client.RequestExpectationManager;
import cn.taketoday.test.web.client.RequestMatcher;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.client.config.RestTemplateBuilder;

import static cn.taketoday.test.web.client.match.MockRestRequestMatchers.requestTo;
import static cn.taketoday.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RootUriRequestExpectationManager}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class RootUriRequestExpectationManagerTests {

  private final String uri = "https://example.com";

  @Mock
  private RequestExpectationManager delegate;

  private RootUriRequestExpectationManager manager;

  @BeforeEach
  void setup() {
    this.manager = new RootUriRequestExpectationManager(this.uri, this.delegate);
  }

  @Test
  void createWhenRootUriIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new RootUriRequestExpectationManager(null, this.delegate))
            .withMessageContaining("RootUri is required");
  }

  @Test
  void createWhenExpectationManagerIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new RootUriRequestExpectationManager(this.uri, null))
            .withMessageContaining("ExpectationManager is required");
  }

  @Test
  void expectRequestShouldDelegateToExpectationManager() {
    ExpectedCount count = ExpectedCount.once();
    RequestMatcher requestMatcher = mock(RequestMatcher.class);
    this.manager.expectRequest(count, requestMatcher);
    then(this.delegate).should().expectRequest(count, requestMatcher);
  }

  @Test
  void validateRequestWhenUriDoesNotStartWithRootUriShouldDelegateToExpectationManager() throws Exception {
    ClientHttpRequest request = mock(ClientHttpRequest.class);
    given(request.getURI()).willReturn(new URI("https://spring.io/test"));
    this.manager.validateRequest(request);
    then(this.delegate).should().validateRequest(request);
  }

  @Test
  void validateRequestWhenUriStartsWithRootUriShouldReplaceUri() throws Exception {
    ClientHttpRequest request = mock(ClientHttpRequest.class);
    given(request.getURI()).willReturn(new URI(this.uri + "/hello"));
    this.manager.validateRequest(request);
    URI expectedURI = new URI("/hello");
    then(this.delegate).should()
            .validateRequest(assertArg((actual) -> assertThat(actual).isInstanceOfSatisfying(HttpRequestDecorator.class,
                    (requestWrapper) -> {
                      assertThat(requestWrapper.getRequest()).isSameAs(request);
                      assertThat(requestWrapper.getURI()).isEqualTo(expectedURI);
                    })));

  }

  @Test
  void validateRequestWhenRequestUriAssertionIsThrownShouldReplaceUriInMessage() throws Exception {
    ClientHttpRequest request = mock(ClientHttpRequest.class);
    given(request.getURI()).willReturn(new URI(this.uri + "/hello"));
    given(this.delegate.validateRequest(any(ClientHttpRequest.class)))
            .willThrow(new AssertionError("Request URI expected:</hello> was:<https://example.com/bad>"));
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> this.manager.validateRequest(request))
            .withMessageContaining("Request URI expected:<https://example.com/hello>");
  }

  @Test
  void resetRequestShouldDelegateToExpectationManager() {
    this.manager.reset();
    then(this.delegate).should().reset();
  }

  @Test
  void bindToShouldReturnMockRestServiceServer() {
    RestTemplate restTemplate = new RestTemplateBuilder().build();
    MockRestServiceServer bound = RootUriRequestExpectationManager.bindTo(restTemplate);
    assertThat(bound).isNotNull();
  }

  @Test
  void bindToWithExpectationManagerShouldReturnMockRestServiceServer() {
    RestTemplate restTemplate = new RestTemplateBuilder().build();
    MockRestServiceServer bound = RootUriRequestExpectationManager.bindTo(restTemplate, this.delegate);
    assertThat(bound).isNotNull();
  }

  @Test
  void forRestTemplateWhenUsingRootUriTemplateHandlerShouldReturnRootUriRequestExpectationManager() {
    RestTemplate restTemplate = new RestTemplateBuilder().rootUri(this.uri).build();
    RequestExpectationManager actual = RootUriRequestExpectationManager.forRestTemplate(restTemplate,
            this.delegate);
    assertThat(actual).isInstanceOf(RootUriRequestExpectationManager.class);
    assertThat(actual).extracting("rootUri").isEqualTo(this.uri);
  }

  @Test
  void forRestTemplateWhenNotUsingRootUriTemplateHandlerShouldReturnOriginalRequestExpectationManager() {
    RestTemplate restTemplate = new RestTemplateBuilder().build();
    RequestExpectationManager actual = RootUriRequestExpectationManager.forRestTemplate(restTemplate,
            this.delegate);
    assertThat(actual).isSameAs(this.delegate);
  }

  @Test
  void boundRestTemplateShouldPrefixRootUri() {
    RestTemplate restTemplate = new RestTemplateBuilder().rootUri("https://example.com").build();
    MockRestServiceServer server = RootUriRequestExpectationManager.bindTo(restTemplate);
    server.expect(requestTo("/hello")).andRespond(withSuccess());
    restTemplate.getForEntity("/hello", String.class);
  }

  @Test
  void boundRestTemplateWhenUrlIncludesDomainShouldNotPrefixRootUri() {
    RestTemplate restTemplate = new RestTemplateBuilder().rootUri("https://example.com").build();
    MockRestServiceServer server = RootUriRequestExpectationManager.bindTo(restTemplate);
    server.expect(requestTo("/hello")).andRespond(withSuccess());
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> restTemplate.getForEntity("https://spring.io/hello", String.class))
            .withMessageContaining("expected:<https://example.com/hello> but was:<https://spring.io/hello>");
  }

}
