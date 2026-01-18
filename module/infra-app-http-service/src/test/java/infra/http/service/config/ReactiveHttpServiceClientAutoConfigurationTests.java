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

package infra.http.service.config;

import org.assertj.core.extractor.Extractors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import infra.aop.Advisor;
import infra.app.test.context.runner.ReactiveWebApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.http.HttpHeaders;
import infra.http.client.HttpClientSettings;
import infra.http.client.HttpRedirects;
import infra.http.client.config.HttpClientAutoConfiguration;
import infra.http.client.config.reactive.ReactiveHttpClientAutoConfiguration;
import infra.http.client.reactive.ClientHttpConnectorBuilder;
import infra.http.reactive.client.ClientHttpConnector;
import infra.test.util.ReflectionTestUtils;
import infra.web.client.WebClientCustomizer;
import infra.web.client.config.WebClientAutoConfiguration;
import infra.web.reactive.client.WebClient;
import infra.http.service.annotation.GetExchange;
import infra.http.service.registry.HttpServiceGroup;
import infra.http.service.registry.HttpServiceGroup.ClientType;
import infra.http.service.registry.HttpServiceGroupConfigurer.ClientCallback;
import infra.http.service.registry.HttpServiceGroupConfigurer.Groups;
import infra.http.service.registry.HttpServiceProxyRegistry;
import infra.http.service.registry.ImportHttpServices;
import infra.http.service.support.WebClientHttpServiceGroupConfigurer;
import infra.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReactiveHttpServiceClientAutoConfiguration},
 * {@link PropertiesWebClientHttpServiceGroupConfigurer} and
 * {@link WebClientCustomizerHttpServiceGroupConfigurer}.
 *
 * @author Phillip Webb
 */
class ReactiveHttpServiceClientAutoConfigurationTests {

  private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ReactiveHttpServiceClientAutoConfiguration.class,
                  ReactiveHttpClientAutoConfiguration.class, WebClientAutoConfiguration.class));

  @Test
  void configuresClientFromProperties() {
    this.contextRunner
            .withPropertyValues("http.service-client.one.base-uri=https://example.com/one",
                    "http.service-client.one.default-header.test=true",
                    "http.service-client.two.base-uri=https://example.com/two",
                    "http.service-client.two.default-header.test=true",
                    "http.service-client.two.default-header.two=iam2")
            .withUserConfiguration(HttpClientConfiguration.class)
            .run((context) -> {
              HttpServiceProxyRegistry serviceProxyRegistry = context.getBean(HttpServiceProxyRegistry.class);
              assertThat(serviceProxyRegistry.getGroupNames()).containsOnly("one", "two");
              TestClientOne clientOne = context.getBean(TestClientOne.class);
              WebClient webClientOne = getWebClient(clientOne);
              assertThat(getUriComponentsBuilder(webClientOne).toUriString()).isEqualTo("https://example.com/one");
              assertThat(getHttpHeaders(webClientOne).entrySet())
                      .containsExactlyInAnyOrder(Map.entry("test", List.of("true")));
              TestClientTwo clientTwo = context.getBean(TestClientTwo.class);
              WebClient webClientTwo = getWebClient(clientTwo);
              assertThat(getUriComponentsBuilder(webClientTwo).toUriString()).isEqualTo("https://example.com/two");
              assertThat(getHttpHeaders(webClientTwo).entrySet())
                      .containsExactlyInAnyOrder(Map.entry("test", List.of("true")), Map.entry("two", List.of("iam2")));
            });
  }

  @Test
    // gh-46915
  void configuresClientFromPropertiesWhenHasHttpConnectorAutoConfiguration() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(HttpClientAutoConfiguration.class))
            .withPropertyValues("http.service-client.one.connect-timeout=5s",
                    "http.service-client.two.connect-timeout=10s", "http.clients.reactive.connector=jdk")
            .withUserConfiguration(HttpClientConfiguration.class)
            .run((context) -> {
              PropertiesWebClientHttpServiceGroupConfigurer configurer = context
                      .getBean(PropertiesWebClientHttpServiceGroupConfigurer.class);
              Groups<WebClient.Builder> groups = mock();
              configurer.configureGroups(groups);
              ArgumentCaptor<ClientCallback<WebClient.Builder>> callbackCaptor = ArgumentCaptor.captor();
              then(groups).should().forEachClient(callbackCaptor.capture());
              ClientCallback<WebClient.Builder> callback = callbackCaptor.getValue();
              assertConnectTimeout(callback, "one", Duration.ofSeconds(5));
              assertConnectTimeout(callback, "two", Duration.ofSeconds(10));
            });
  }

  private void assertConnectTimeout(ClientCallback<WebClient.Builder> callback, String name,
          Duration expectedReadTimeout) {
    HttpServiceGroup group = mock();
    given(group.name()).willReturn(name);
    WebClient.Builder builder = mock();
    callback.withClient(group, builder);
    ArgumentCaptor<ClientHttpConnector> connectorCaptor = ArgumentCaptor.captor();
    then(builder).should().clientConnector(connectorCaptor.capture());
    ClientHttpConnector client = connectorCaptor.getValue();
    HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(client, "httpClient");
    assertThat(httpClient).isNotNull();
    assertThat(httpClient.connectTimeout()).contains(expectedReadTimeout);
  }

  @Test
  void whenHasUserDefinedHttpConnectorBuilder() {
    this.contextRunner.withPropertyValues("http.reactiveclient.service.base-uri=https://example.com")
            .withUserConfiguration(HttpClientConfiguration.class, HttpConnectorBuilderConfiguration.class)
            .run((context) -> {
              TestClientOne clientOne = context.getBean(TestClientOne.class);
              assertThat(getJdkHttpClient(clientOne).followRedirects()).isEqualTo(Redirect.NEVER);
            });
  }

  @Test
  void whenHasUserDefinedRequestFactorySettings() {
    this.contextRunner
            .withPropertyValues("http.service-client.one.base-uri=https://example.com",
                    "http.clients.reactive.connector=jdk")
            .withUserConfiguration(HttpClientConfiguration.class, HttpConnectorSettingsConfiguration.class)
            .run((context) -> {
              TestClientOne clientOne = context.getBean(TestClientOne.class);
              assertThat(getJdkHttpClient(clientOne).followRedirects()).isEqualTo(Redirect.NEVER);
            });
  }

  @Test
  void whenHasUserDefinedWebClientCustomizer() {
    this.contextRunner.withPropertyValues("http.service-client.one.base-uri=https://example.com")
            .withUserConfiguration(HttpClientConfiguration.class, WebClientCustomizerConfiguration.class)
            .run((context) -> {
              TestClientOne clientOne = context.getBean(TestClientOne.class);
              WebClient webClientOne = getWebClient(clientOne);
              assertThat(getHttpHeaders(webClientOne).entrySet())
                      .containsExactlyInAnyOrder(Map.entry("customized", List.of("true")));
            });
  }

  @Test
  void whenHasUserDefinedHttpServiceGroupConfigurer() {
    this.contextRunner.withPropertyValues("http.service-client.one.base-uri=https://example.com")
            .withUserConfiguration(HttpClientConfiguration.class, HttpServiceGroupConfigurerConfiguration.class)
            .run((context) -> {
              TestClientOne clientOne = context.getBean(TestClientOne.class);
              WebClient webClientOne = getWebClient(clientOne);
              assertThat(getHttpHeaders(webClientOne).entrySet())
                      .containsExactlyInAnyOrder(Map.entry("customizedgroup", List.of("true")));
            });
  }

  @Test
  void whenHasNoHttpServiceProxyRegistryBean() {
    this.contextRunner.withPropertyValues("http.service-client.one.base-uri=https://example.com")
            .run((context) -> assertThat(context).doesNotHaveBean(HttpServiceProxyRegistry.class));
  }

  private HttpClient getJdkHttpClient(Object proxy) {
    return (HttpClient) Extractors.byName("builder.connector.httpClient").apply(getWebClient(proxy));
  }

  private HttpHeaders getHttpHeaders(WebClient webClient) {
    return (HttpHeaders) Extractors.byName("defaultHeaders").apply(webClient);
  }

  private UriComponentsBuilder getUriComponentsBuilder(WebClient webClient) {
    return (UriComponentsBuilder) Extractors.byName("uriBuilderFactory.baseUri").apply(webClient);
  }

  private WebClient getWebClient(Object proxy) {
    InvocationHandler handler = Proxy.getInvocationHandler(proxy);
    Advisor[] advisors = (Advisor[]) Extractors.byName("advised.advisors").apply(handler);
    Map<?, ?> serviceMethods = (Map<?, ?>) Extractors.byName("advice.httpServiceMethods").apply(advisors[0]);
    Object serviceMethod = serviceMethods.values().iterator().next();
    return (WebClient) Extractors.byName("requestExecution.arg$1.webClient").apply(serviceMethod);
  }

  @Configuration(proxyBeanMethods = false)
  @ImportHttpServices(group = "one", types = TestClientOne.class, clientType = ClientType.WEB_CLIENT)
  @ImportHttpServices(group = "two", types = TestClientTwo.class, clientType = ClientType.WEB_CLIENT)
  static class HttpClientConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  static class HttpConnectorBuilderConfiguration {

    @Bean
    ClientHttpConnectorBuilder<?> httpConnectorBuilder() {
      return ClientHttpConnectorBuilder.jdk()
              .withHttpClientCustomizer((httpClient) -> httpClient.followRedirects(Redirect.NEVER));
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class HttpConnectorSettingsConfiguration {

    @Bean
    HttpClientSettings httpClientSettings() {
      return HttpClientSettings.defaults().withRedirects(HttpRedirects.DONT_FOLLOW);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class WebClientCustomizerConfiguration {

    @Bean
    WebClientCustomizer webClientCustomizer() {
      return (builder) -> builder.defaultHeader("customized", "true");
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class HttpServiceGroupConfigurerConfiguration {

    @Bean
    WebClientHttpServiceGroupConfigurer restClientHttpServiceGroupConfigurer() {
      return (groups) -> groups.filterByName("one")
              .forEachClient((group, builder) -> builder.defaultHeader("customizedgroup", "true"));
    }

  }

  interface TestClientOne {

    @GetExchange("/hello")
    String hello();

  }

  interface TestClientTwo {

    @GetExchange("/there")
    String there();

  }

}
