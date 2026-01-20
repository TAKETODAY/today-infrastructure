/*
 * Copyright 2012-present the original author or authors.
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

package infra.web.client;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.core5.function.Resolver;
import org.apache.hc.core5.util.Timeout;
import org.assertj.core.extractor.Extractors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.MediaType;
import infra.http.client.BufferingClientHttpRequestFactory;
import infra.http.client.ClientHttpRequest;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.ClientHttpRequestInitializer;
import infra.http.client.ClientHttpRequestInterceptor;
import infra.http.client.HttpClientSettings;
import infra.http.client.HttpComponentsClientHttpRequestFactory;
import infra.http.client.HttpRedirects;
import infra.http.client.InterceptingClientHttpRequestFactory;
import infra.http.client.JdkClientHttpRequestFactory;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.ResourceHttpMessageConverter;
import infra.http.converter.StringHttpMessageConverter;
import infra.test.util.ReflectionTestUtils;
import infra.test.web.client.MockRestServiceServer;
import infra.web.util.UriTemplateHandler;

import static infra.test.web.client.match.MockRestRequestMatchers.requestTo;
import static infra.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/2 15:33
 */
@ExtendWith(MockitoExtension.class)
class RestTemplateBuilderTests {

  private final RestTemplateBuilder builder = new RestTemplateBuilder();

  @Mock
  @SuppressWarnings("NullAway.Init")
  private HttpMessageConverter<Object> messageConverter;

  @Mock
  @SuppressWarnings("NullAway.Init")
  private ClientHttpRequestInterceptor interceptor;

  @Test
  @SuppressWarnings("NullAway")
  void createWhenCustomizersAreNullShouldThrowException() {
    RestTemplateCustomizer[] customizers = null;
    assertThatIllegalArgumentException().isThrownBy(() -> new RestTemplateBuilder(customizers))
            .withMessageContaining("'customizers' is required");
  }

  @Test
  void createWithCustomizersShouldApplyCustomizers() {
    RestTemplateCustomizer customizer = mock(RestTemplateCustomizer.class);
    RestTemplate template = new RestTemplateBuilder(customizer).build();
    then(customizer).should().customize(template);
  }

  @Test
  void buildShouldDetectRequestFactory() {
    RestTemplate restTemplate = this.builder.build();
    assertThat(restTemplate.getRequestFactory()).isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
  }

  @Test
  void rootUriShouldApply() {
    RestTemplate restTemplate = this.builder.rootUri("https://example.com").build();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    server.expect(MockRestRequestMatchers.requestTo("https://example.com/hello")).andRespond(MockRestResponseCreators.withSuccess());
    restTemplate.getForEntity("/hello", String.class);
    server.verify();
  }

  @Test
  void rootUriShouldApplyAfterUriTemplateHandler() {
    UriTemplateHandler uriTemplateHandler = mock(UriTemplateHandler.class);
    RestTemplate template = this.builder.uriTemplateHandler(uriTemplateHandler)
            .rootUri("https://example.com")
            .build();
    UriTemplateHandler handler = template.getUriTemplateHandler();
    handler.expand("/hello");
    assertThat(handler).isInstanceOf(RootUriBuilderFactory.class);
    then(uriTemplateHandler).should().expand("https://example.com/hello");
  }

  @Test
  @SuppressWarnings("NullAway")
  void messageConvertersWhenConvertersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.messageConverters((HttpMessageConverter<?>[]) null))
            .withMessageContaining("'messageConverters' is required");
  }

  @Test
  @SuppressWarnings("NullAway")
  void messageConvertersCollectionWhenConvertersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.messageConverters((Set<HttpMessageConverter<?>>) null))
            .withMessageContaining("'messageConverters' is required");
  }

  @Test
  void messageConvertersShouldApply() {
    RestTemplate template = this.builder.messageConverters(this.messageConverter).build();
    assertThat(template.getMessageConverters()).containsOnly(this.messageConverter);
  }

  @Test
  void messageConvertersShouldReplaceExisting() {
    RestTemplate template = this.builder.messageConverters(new ResourceHttpMessageConverter())
            .messageConverters(Collections.singleton(this.messageConverter))
            .build();
    assertThat(template.getMessageConverters()).containsOnly(this.messageConverter);
  }

  @Test
  @SuppressWarnings("NullAway")
  void additionalMessageConvertersWhenConvertersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.additionalMessageConverters((HttpMessageConverter<?>[]) null))
            .withMessageContaining("'messageConverters' is required");
  }

  @Test
  @SuppressWarnings("NullAway")
  void additionalMessageConvertersCollectionWhenConvertersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.additionalMessageConverters((Set<HttpMessageConverter<?>>) null))
            .withMessageContaining("'messageConverters' is required");
  }

  @Test
  void additionalMessageConvertersShouldAddToExisting() {
    HttpMessageConverter<?> resourceConverter = new ResourceHttpMessageConverter();
    RestTemplate template = this.builder.messageConverters(resourceConverter)
            .additionalMessageConverters(this.messageConverter)
            .build();
    assertThat(template.getMessageConverters()).containsOnly(resourceConverter, this.messageConverter);
  }

  @Test
  void defaultMessageConvertersShouldSetDefaultList() {
    RestTemplate template = new RestTemplate(Collections.singletonList(new StringHttpMessageConverter()));
    this.builder.defaultMessageConverters().configure(template);
    assertThat(template.getMessageConverters()).hasSameSizeAs(new RestTemplate().getMessageConverters());
  }

  @Test
  void defaultMessageConvertersShouldClearExisting() {
    RestTemplate template = new RestTemplate(Collections.singletonList(new StringHttpMessageConverter()));
    this.builder.additionalMessageConverters(this.messageConverter).defaultMessageConverters().configure(template);
    assertThat(template.getMessageConverters()).hasSameSizeAs(new RestTemplate().getMessageConverters());
  }

  @Test
  @SuppressWarnings("NullAway")
  void interceptorsWhenInterceptorsAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.interceptors((ClientHttpRequestInterceptor[]) null))
            .withMessageContaining("'interceptors' is required");
  }

  @Test
  @SuppressWarnings("NullAway")
  void interceptorsCollectionWhenInterceptorsAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.interceptors((Set<ClientHttpRequestInterceptor>) null))
            .withMessageContaining("'interceptors' is required");
  }

  @Test
  void interceptorsShouldApply() {
    RestTemplate template = this.builder.interceptors(this.interceptor).build();
    assertThat(template.getInterceptors()).containsOnly(this.interceptor);
  }

  @Test
  void interceptorsShouldReplaceExisting() {
    RestTemplate template = this.builder.interceptors(mock(ClientHttpRequestInterceptor.class))
            .interceptors(Collections.singleton(this.interceptor))
            .build();
    assertThat(template.getInterceptors()).containsOnly(this.interceptor);
  }

  @Test
  @SuppressWarnings("NullAway")
  void additionalInterceptorsWhenInterceptorsAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.additionalInterceptors((ClientHttpRequestInterceptor[]) null))
            .withMessageContaining("'interceptors' is required");
  }

  @Test
  @SuppressWarnings("NullAway")
  void additionalInterceptorsCollectionWhenInterceptorsAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.additionalInterceptors((Set<ClientHttpRequestInterceptor>) null))
            .withMessageContaining("'interceptors' is required");
  }

  @Test
  void additionalInterceptorsShouldAddToExisting() {
    ClientHttpRequestInterceptor interceptor = mock(ClientHttpRequestInterceptor.class);
    RestTemplate template = this.builder.interceptors(interceptor).additionalInterceptors(this.interceptor).build();
    assertThat(template.getInterceptors()).containsOnly(interceptor, this.interceptor);
  }

  @Test
  @SuppressWarnings("NullAway")
  void requestFactoryClassWhenFactoryIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.requestFactory((Class<ClientHttpRequestFactory>) null))
            .withMessageContaining("'requestFactoryType' is required");
  }

  @Test
  void requestFactoryPackagePrivateClassShouldApply() {
    RestTemplate template = this.builder.requestFactory(TestClientHttpRequestFactory.class).build();
    assertThat(template.getRequestFactory()).isInstanceOf(TestClientHttpRequestFactory.class);
  }

  @Test
  @SuppressWarnings("NullAway")
  void requestFactoryWhenSupplierIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.requestFactory((Supplier<ClientHttpRequestFactory>) null))
            .withMessageContaining("requestFactorySupplier' is required");
  }

  @Test
  void requestFactoryShouldApply() {
    ClientHttpRequestFactory requestFactory = mock(ClientHttpRequestFactory.class);
    RestTemplate template = this.builder.requestFactory(() -> requestFactory).build();
    assertThat(template.getRequestFactory()).isSameAs(requestFactory);
  }

  @Test
  @SuppressWarnings("NullAway")
  void uriTemplateHandlerWhenHandlerIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.builder.uriTemplateHandler(null))
            .withMessageContaining("'uriTemplateHandler' is required");
  }

  @Test
  void uriTemplateHandlerShouldApply() {
    UriTemplateHandler uriTemplateHandler = mock(UriTemplateHandler.class);
    RestTemplate template = this.builder.uriTemplateHandler(uriTemplateHandler).build();
    assertThat(template.getUriTemplateHandler()).isSameAs(uriTemplateHandler);
  }

  @Test
  @SuppressWarnings("NullAway")
  void errorHandlerWhenHandlerIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.builder.errorHandler(null))
            .withMessageContaining("'errorHandler' is required");
  }

  @Test
  void errorHandlerShouldApply() {
    ResponseErrorHandler errorHandler = mock(ResponseErrorHandler.class);
    RestTemplate template = this.builder.errorHandler(errorHandler).build();
    assertThat(template.getErrorHandler()).isSameAs(errorHandler);
  }

  @Test
  void basicAuthenticationShouldApply() {
    RestTemplate template = this.builder.basicAuthentication("spring", "boot", StandardCharsets.UTF_8).build();
    ClientHttpRequest request = createRequest(template);
    assertThat(request.getHeaders().keySet()).containsOnly(HttpHeaders.AUTHORIZATION);
    assertThat(request.getHeaders().get(HttpHeaders.AUTHORIZATION)).containsExactly("Basic c3ByaW5nOmJvb3Q=");
  }

  @Test
  void defaultHeaderAddsHeader() {
    RestTemplate template = this.builder.defaultHeader("spring", "boot").build();
    ClientHttpRequest request = createRequest(template);
    assertThat(request.getHeaders().entrySet()).contains(entry("spring", Collections.singletonList("boot")));
  }

  @Test
  void defaultHeaderAddsHeaderValues() {
    String name = HttpHeaders.ACCEPT;
    String[] values = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE };
    RestTemplate template = this.builder.defaultHeader(name, values).build();
    ClientHttpRequest request = createRequest(template);
    assertThat(request.getHeaders().entrySet()).contains(entry(name, Arrays.asList(values)));
  }

  @Test
    // gh-17885
  void defaultHeaderWhenUsingMockRestServiceServerAddsHeader() {
    RestTemplate template = this.builder.defaultHeader("spring", "boot").build();
    MockRestServiceServer.bindTo(template).build();
    ClientHttpRequest request = createRequest(template);
    assertThat(request.getHeaders().entrySet()).contains(entry("spring", Collections.singletonList("boot")));
  }

  @Test
  @SuppressWarnings("unchecked")
  void clientSettingsAppliesSettings() {
    HttpClientSettings settings = HttpClientSettings.defaults()
            .withConnectTimeout(Duration.ofSeconds(1))
            .withReadTimeout(Duration.ofSeconds(2));
    RestTemplate template = this.builder.clientSettings(settings).build();
    Resolver<HttpRoute, ConnectionConfig> resolver = (Resolver<HttpRoute, ConnectionConfig>) Extractors
            .byName("httpClient.connManager.connectionConfigResolver")
            .apply(template.getRequestFactory());
    ConnectionConfig config = resolver.resolve(mock());
    assertThat(config.getConnectTimeout()).isEqualTo(Timeout.of(Duration.ofSeconds(1)));
    assertThat(config.getSocketTimeout()).isEqualTo(Timeout.of(Duration.ofSeconds(2)));
  }

  @Test
  void requestCustomizersAddsCustomizers() {
    RestTemplate template = this.builder
            .requestCustomizers((request) -> request.getHeaders().add("spring", "framework"))
            .build();
    ClientHttpRequest request = createRequest(template);
    assertThat(request.getHeaders().entrySet()).contains(entry("spring", Collections.singletonList("framework")));
  }

  @Test
  void additionalRequestCustomizersAddsCustomizers() {
    RestTemplate template = this.builder
            .requestCustomizers((request) -> request.getHeaders().add("spring", "framework"))
            .additionalRequestCustomizers((request) -> request.getHeaders().add("for", "java"))
            .build();
    ClientHttpRequest request = createRequest(template);
    assertThat(request.getHeaders().entrySet()).contains(entry("spring", Collections.singletonList("framework")))
            .contains(entry("for", Collections.singletonList("java")));
  }

  @Test
  @SuppressWarnings("NullAway")
  void customizersWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.builder.customizers((RestTemplateCustomizer[]) null))
            .withMessageContaining("'customizers' is required");
  }

  @Test
  @SuppressWarnings("NullAway")
  void customizersCollectionWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.customizers((Set<RestTemplateCustomizer>) null))
            .withMessageContaining("'customizers' is required");
  }

  @Test
  void customizersShouldApply() {
    RestTemplateCustomizer customizer = mock(RestTemplateCustomizer.class);
    RestTemplate template = this.builder.customizers(customizer).build();
    then(customizer).should().customize(template);
  }

  @Test
  void customizersShouldBeAppliedLast() {
    RestTemplate template = spy(new RestTemplate());
    this.builder.additionalCustomizers(
            (restTemplate) -> then(restTemplate).should().setRequestFactory(any(ClientHttpRequestFactory.class)));
    this.builder.configure(template);
  }

  @Test
  void customizersShouldReplaceExisting() {
    RestTemplateCustomizer customizer1 = mock(RestTemplateCustomizer.class);
    RestTemplateCustomizer customizer2 = mock(RestTemplateCustomizer.class);
    RestTemplate template = this.builder.customizers(customizer1)
            .customizers(Collections.singleton(customizer2))
            .build();
    then(customizer1).shouldHaveNoInteractions();
    then(customizer2).should().customize(template);
  }

  @Test
  @SuppressWarnings("NullAway")
  void additionalCustomizersWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.additionalCustomizers((RestTemplateCustomizer[]) null))
            .withMessageContaining("'customizers' is required");
  }

  @Test
  @SuppressWarnings("NullAway")
  void additionalCustomizersCollectionWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.additionalCustomizers((Set<RestTemplateCustomizer>) null))
            .withMessageContaining("customizers' is required");
  }

  @Test
  void additionalCustomizersShouldAddToExisting() {
    RestTemplateCustomizer customizer1 = mock(RestTemplateCustomizer.class);
    RestTemplateCustomizer customizer2 = mock(RestTemplateCustomizer.class);
    RestTemplate template = this.builder.customizers(customizer1).additionalCustomizers(customizer2).build();
    InOrder ordered = inOrder(customizer1, customizer2);
    ordered.verify(customizer1).customize(template);
    ordered.verify(customizer2).customize(template);
  }

  @Test
  void customizerShouldBeAppliedAtTheEnd() {
    ResponseErrorHandler errorHandler = mock(ResponseErrorHandler.class);
    ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
    this.builder.interceptors(this.interceptor)
            .messageConverters(this.messageConverter)
            .rootUri("http://localhost:8080")
            .errorHandler(errorHandler)
            .basicAuthentication("spring", "boot")
            .requestFactory(() -> requestFactory)
            .customizers((restTemplate) -> {
              assertThat(restTemplate.getInterceptors()).hasSize(1);
              assertThat(restTemplate.getMessageConverters()).contains(this.messageConverter);
              assertThat(restTemplate.getUriTemplateHandler()).isInstanceOf(RootUriBuilderFactory.class);
              assertThat(restTemplate.getErrorHandler()).isEqualTo(errorHandler);
              ClientHttpRequestFactory actualRequestFactory = restTemplate.getRequestFactory();
              assertThat(actualRequestFactory).isInstanceOf(InterceptingClientHttpRequestFactory.class);
              ClientHttpRequestInitializer initializer = restTemplate.getHttpRequestInitializers().get(0);
              assertThat(initializer).isInstanceOf(RestTemplateBuilderClientHttpRequestInitializer.class);
            })
            .build();
  }

  @Test
  void buildShouldReturnRestTemplate() {
    RestTemplate template = this.builder.build();
    assertThat(template.getClass()).isEqualTo(RestTemplate.class);
  }

  @Test
  void buildClassShouldReturnClassInstance() {
    RestTemplateSubclass template = this.builder.build(RestTemplateSubclass.class);
    assertThat(template.getClass()).isEqualTo(RestTemplateSubclass.class);
  }

  @Test
  void configureShouldApply() {
    RestTemplate template = new RestTemplate();
    this.builder.configure(template);
    assertThat(template.getRequestFactory()).isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
  }

  @Test
  void unwrappingDoesNotAffectRequestFactoryThatIsSetOnTheBuiltTemplate() {
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
    RestTemplate template = this.builder.requestFactory(() -> new BufferingClientHttpRequestFactory(requestFactory))
            .build();
    assertThat(template.getRequestFactory()).isInstanceOf(BufferingClientHttpRequestFactory.class);
  }

  @Test
  void configureRedirects() {
    assertThat(this.builder.redirects(HttpRedirects.DONT_FOLLOW)).extracting("clientSettings")
            .extracting("redirects")
            .isSameAs(HttpRedirects.DONT_FOLLOW);
  }

  private ClientHttpRequest createRequest(RestTemplate template) {
    ClientHttpRequest request = ReflectionTestUtils.invokeMethod(template, "createRequest",
            URI.create("http://localhost"), HttpMethod.GET);
    assertThat(request).isNotNull();
    return request;
  }

  static class RestTemplateSubclass extends RestTemplate {

  }

  static class TestClientHttpRequestFactory extends JdkClientHttpRequestFactory {

  }

  static class TestHttpComponentsClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory {

  }

}