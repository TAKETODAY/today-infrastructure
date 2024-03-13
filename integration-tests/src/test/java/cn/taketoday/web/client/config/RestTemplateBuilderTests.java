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

package cn.taketoday.web.client.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.client.BufferingClientHttpRequestFactory;
import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.http.client.ClientHttpRequestFactory;
import cn.taketoday.http.client.ClientHttpRequestInitializer;
import cn.taketoday.http.client.ClientHttpRequestInterceptor;
import cn.taketoday.http.client.HttpComponentsClientHttpRequestFactory;
import cn.taketoday.http.client.InterceptingClientHttpRequestFactory;
import cn.taketoday.http.client.JdkClientHttpRequestFactory;
import cn.taketoday.http.client.SimpleClientHttpRequestFactory;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.ResourceHttpMessageConverter;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.test.util.ReflectionTestUtils;
import cn.taketoday.test.web.client.MockRestServiceServer;
import cn.taketoday.web.client.ResponseErrorHandler;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.util.UriTemplateHandler;

import static cn.taketoday.test.web.client.match.MockRestRequestMatchers.requestTo;
import static cn.taketoday.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link RestTemplateBuilder}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Dmytro Nosan
 * @author Kevin Strijbos
 * @author Ilya Lukyanovich
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/28 13:47
 */
@ExtendWith(MockitoExtension.class)
class RestTemplateBuilderTests {

  private final RestTemplateBuilder builder = new RestTemplateBuilder();

  @Mock
  private HttpMessageConverter<Object> messageConverter;

  @Mock
  private ClientHttpRequestInterceptor interceptor;

  @Test
  void createWhenCustomizersAreNullShouldThrowException() {
    RestTemplateCustomizer[] customizers = null;
    assertThatIllegalArgumentException().isThrownBy(() -> new RestTemplateBuilder(customizers))
            .withMessageContaining("Customizers is required");
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
  void detectRequestFactoryWhenFalseShouldDisableDetection() {
    RestTemplate restTemplate = this.builder.detectRequestFactory(false).build();
    assertThat(restTemplate.getRequestFactory()).isInstanceOf(JdkClientHttpRequestFactory.class);
  }

  @Test
  void rootUriShouldApply() {
    RestTemplate restTemplate = this.builder.rootUri("https://example.com").build();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    server.expect(requestTo("https://example.com/hello")).andRespond(withSuccess());
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
  void messageConvertersWhenConvertersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.messageConverters((HttpMessageConverter<?>[]) null))
            .withMessageContaining("MessageConverters is required");
  }

  @Test
  void messageConvertersCollectionWhenConvertersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.messageConverters((Set<HttpMessageConverter<?>>) null))
            .withMessageContaining("MessageConverters is required");
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
  void additionalMessageConvertersWhenConvertersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.additionalMessageConverters((HttpMessageConverter<?>[]) null))
            .withMessageContaining("MessageConverters is required");
  }

  @Test
  void additionalMessageConvertersCollectionWhenConvertersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.additionalMessageConverters((Set<HttpMessageConverter<?>>) null))
            .withMessageContaining("MessageConverters is required");
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
  void interceptorsWhenInterceptorsAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.interceptors((ClientHttpRequestInterceptor[]) null))
            .withMessageContaining("interceptors is required");
  }

  @Test
  void interceptorsCollectionWhenInterceptorsAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.interceptors((Set<ClientHttpRequestInterceptor>) null))
            .withMessageContaining("interceptors is required");
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
  void additionalInterceptorsWhenInterceptorsAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.additionalInterceptors((ClientHttpRequestInterceptor[]) null))
            .withMessageContaining("interceptors is required");
  }

  @Test
  void additionalInterceptorsCollectionWhenInterceptorsAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.additionalInterceptors((Set<ClientHttpRequestInterceptor>) null))
            .withMessageContaining("interceptors is required");
  }

  @Test
  void additionalInterceptorsShouldAddToExisting() {
    ClientHttpRequestInterceptor interceptor = mock(ClientHttpRequestInterceptor.class);
    RestTemplate template = this.builder.interceptors(interceptor).additionalInterceptors(this.interceptor).build();
    assertThat(template.getInterceptors()).containsOnly(interceptor, this.interceptor);
  }

  @Test
  void requestFactoryClassWhenFactoryIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.requestFactory((Class<ClientHttpRequestFactory>) null))
            .withMessageContaining("RequestFactoryType is required");
  }

  @Test
  void requestFactoryClassShouldApply() {
    RestTemplate template = this.builder.requestFactory(SimpleClientHttpRequestFactory.class).build();
    assertThat(template.getRequestFactory()).isInstanceOf(SimpleClientHttpRequestFactory.class);
  }

  @Test
  void requestFactoryPackagePrivateClassShouldApply() {
    RestTemplate template = this.builder.requestFactory(TestClientHttpRequestFactory.class).build();
    assertThat(template.getRequestFactory()).isInstanceOf(TestClientHttpRequestFactory.class);
  }

  @Test
  void requestFactoryWhenSupplierIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.requestFactory((Supplier<ClientHttpRequestFactory>) null))
            .withMessageContaining("RequestFactory supplier is required");
  }

  @Test
  void requestFactoryWhenFunctionIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder
                    .requestFactory((Function<ClientHttpRequestFactorySettings, ClientHttpRequestFactory>) null))
            .withMessageContaining("RequestFactoryFunction is required");
  }

  @Test
  void requestFactoryShouldApply() {
    ClientHttpRequestFactory requestFactory = mock(ClientHttpRequestFactory.class);
    RestTemplate template = this.builder.requestFactory(() -> requestFactory).build();
    assertThat(template.getRequestFactory()).isSameAs(requestFactory);
  }

  @Test
  void uriTemplateHandlerWhenHandlerIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.builder.uriTemplateHandler(null))
            .withMessageContaining("UriTemplateHandler is required");
  }

  @Test
  void uriTemplateHandlerShouldApply() {
    UriTemplateHandler uriTemplateHandler = mock(UriTemplateHandler.class);
    RestTemplate template = this.builder.uriTemplateHandler(uriTemplateHandler).build();
    assertThat(template.getUriTemplateHandler()).isSameAs(uriTemplateHandler);
  }

  @Test
  void errorHandlerWhenHandlerIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.builder.errorHandler(null))
            .withMessageContaining("ErrorHandler is required");
  }

  @Test
  void errorHandlerShouldApply() {
    ResponseErrorHandler errorHandler = mock(ResponseErrorHandler.class);
    RestTemplate template = this.builder.errorHandler(errorHandler).build();
    assertThat(template.getErrorHandler()).isSameAs(errorHandler);
  }

  @Test
  void basicAuthenticationShouldApply() {
    RestTemplate template = this.builder.basicAuthentication("spring", "boot", StandardCharsets.UTF_8)
            .build();
    ClientHttpRequest request = createRequest(template);
    assertThat(request.getHeaders()).containsKey(HttpHeaders.AUTHORIZATION);
    assertThat(request.getHeaders().get(HttpHeaders.AUTHORIZATION)).containsExactly("Basic c3ByaW5nOmJvb3Q=");
  }

  @Test
  void defaultHeaderAddsHeader() {
    RestTemplate template = this.builder.defaultHeader("spring", "boot").build();
    ClientHttpRequest request = createRequest(template);
    assertThat(request.getHeaders()).contains(entry("spring", Collections.singletonList("boot")));
  }

  @Test
  void defaultHeaderAddsHeaderValues() {
    String name = HttpHeaders.ACCEPT;
    String[] values = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE };
    RestTemplate template = this.builder.defaultHeader(name, values).build();
    ClientHttpRequest request = createRequest(template);
    assertThat(request.getHeaders()).contains(entry(name, Arrays.asList(values)));
  }

  @Test
    // gh-17885
  void defaultHeaderWhenUsingMockRestServiceServerAddsHeader() {
    RestTemplate template = this.builder.defaultHeader("spring", "boot").build();
    MockRestServiceServer.bindTo(template).build();
    ClientHttpRequest request = createRequest(template);
    assertThat(request.getHeaders()).contains(entry("spring", Collections.singletonList("boot")));
  }

  @Test
  void requestCustomizersAddsCustomizers() {
    RestTemplate template = this.builder
            .requestCustomizers((request) -> request.getHeaders().add("spring", "framework"))
            .build();
    ClientHttpRequest request = createRequest(template);
    assertThat(request.getHeaders()).contains(entry("spring", Collections.singletonList("framework")));
  }

  @Test
  void additionalRequestCustomizersAddsCustomizers() {
    RestTemplate template = this.builder
            .requestCustomizers((request) -> request.getHeaders().add("spring", "framework"))
            .additionalRequestCustomizers((request) -> request.getHeaders().add("for", "java"))
            .build();
    ClientHttpRequest request = createRequest(template);
    assertThat(request.getHeaders()).contains(entry("spring", Collections.singletonList("framework")))
            .contains(entry("for", Collections.singletonList("java")));
  }

  @Test
  void customizersWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.builder.customizers((RestTemplateCustomizer[]) null))
            .withMessageContaining("Customizers is required");
  }

  @Test
  void customizersCollectionWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.customizers((Set<RestTemplateCustomizer>) null))
            .withMessageContaining("Customizers is required");
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
  void additionalCustomizersWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.additionalCustomizers((RestTemplateCustomizer[]) null))
            .withMessageContaining("Customizers is required");
  }

  @Test
  void additionalCustomizersCollectionWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.additionalCustomizers((Set<RestTemplateCustomizer>) null))
            .withMessageContaining("RestTemplateCustomizers is required");
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
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    RestTemplate template = this.builder.requestFactory(() -> new BufferingClientHttpRequestFactory(requestFactory))
            .build();
    assertThat(template.getRequestFactory()).isInstanceOf(BufferingClientHttpRequestFactory.class);
  }

  private ClientHttpRequest createRequest(RestTemplate template) {
    return ReflectionTestUtils.invokeMethod(template, "createRequest",
            URI.create("http://localhost"), HttpMethod.GET);
  }

  static class RestTemplateSubclass extends RestTemplate {

  }

  static class TestClientHttpRequestFactory extends SimpleClientHttpRequestFactory {

  }

  static class TestHttpComponentsClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory {

  }

}
