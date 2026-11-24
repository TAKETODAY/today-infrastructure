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

package infra.test.app.web.reactive.server;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import infra.app.ApplicationType;
import infra.aot.AotDetector;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.ConfigurableApplicationContext;
import infra.context.support.GenericApplicationContext;
import infra.http.MediaType;
import infra.http.codec.CodecCustomizer;
import infra.mock.env.MockEnvironment;
import infra.test.app.context.InfraTest;
import infra.test.context.MergedContextConfiguration;
import infra.test.web.reactive.server.WebTestClient;
import infra.test.web.reactive.server.HttpHandlerConnector;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class WebTestClientFactoryTests {

  @Test
  void factoryCachesClientAndAppliesBuilderAndCodecCustomizers() throws Exception {
    AtomicInteger codecCalls = new AtomicInteger();
    try (GenericApplicationContext context = new GenericApplicationContext()) {
      context.setEnvironment(new MockEnvironment().withProperty("local.server.port", "9123"));
        context.registerBean("builder", WebTestClientBuilderCustomizer.class, () -> builder -> builder.clientConnector(
                new HttpHandlerConnector((request, response) -> {
                  assertThat(request.getURI().toString()).isEqualTo("http://localhost:9123/hello");
                  response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
                  return response.writeWith(Mono.just(response.bufferFactory().wrap("custom".getBytes(java.nio.charset.StandardCharsets.UTF_8))));
                })));
        context.registerBean("codec", CodecCustomizer.class, () -> codecs -> codecCalls.incrementAndGet());
        context.refresh();
        WebTestClientContextCustomizer.WebTestClientFactory factory = new WebTestClientContextCustomizer.WebTestClientFactory();
        factory.setApplicationContext(context);
        assertThat(factory.isSingleton()).isTrue();
        assertThat(factory.getObjectType()).isEqualTo(WebTestClient.class);
        WebTestClient client = factory.getObject();
        assertThat(factory.getObject()).isSameAs(client);
        assertThat(codecCalls).hasValue(1);
        client.get().uri("/hello").exchange().expectBody(String.class).isEqualTo("custom");
    }
  }

  @Test
  void defaultFactoryWorksWithoutCustomizersAndRegistrarPreservesExistingClient() throws Exception {
    try (GenericApplicationContext context = new GenericApplicationContext()) {
      context.refresh();
      WebTestClientContextCustomizer.WebTestClientFactory factory = new WebTestClientContextCustomizer.WebTestClientFactory();
      factory.setApplicationContext(context);
      assertThat(factory.getObject()).isNotNull();
      assertThat(WebTestClientContextCustomizer.WebTestClientFactory.deduceFromApplicationContext(context.getClass()))
              .isEqualTo(ApplicationType.NORMAL);
    }
    try (GenericApplicationContext context = new GenericApplicationContext()) {
      WebTestClient client = WebTestClient.bindToServer().build();
      context.getBeanFactory().registerSingleton("existing", client);
      var registrar = new WebTestClientContextCustomizer.WebTestClientRegistrar();
      registrar.setBeanFactory(context.getBeanFactory());
      registrar.postProcessBeanDefinitionRegistry(context);
      registrar.postProcessBeanFactory(context.getBeanFactory());
      assertThat(context.containsBeanDefinition(WebTestClient.class.getName())).isFalse();
    }
  }

  @Test
  void nonEmbeddedContextIsUnchangedAndCustomizersHaveValueEquality() {
    var first = new WebTestClientContextCustomizer();
    assertThat(first).isEqualTo(first).isEqualTo(new WebTestClientContextCustomizer()).isNotEqualTo(null).isNotEqualTo("other");
    assertThat(first.hashCode()).isEqualTo(new WebTestClientContextCustomizer().hashCode());
    try (GenericApplicationContext context = new GenericApplicationContext()) {
      first.customizeContext(context, new MergedContextConfiguration(NonEmbedded.class, null, null, null, null));
      assertThat(context.getBeanDefinitionCount()).isZero();
    }
  }

  @Test
  void aotContextsSkipRegistrationAndOrdinaryEmbeddedContextsRegisterAClient() {
    try (GenericApplicationContext context = new GenericApplicationContext()) {
      var customizer = new WebTestClientContextCustomizer();
      var config = new MergedContextConfiguration(Embedded.class, null, null, null, null);
      try (var aot = mockStatic(AotDetector.class)) {
        aot.when(AotDetector::useGeneratedArtifacts).thenReturn(true);
        customizer.customizeContext(context, config);
        var registrar = new WebTestClientContextCustomizer.WebTestClientRegistrar();
        registrar.postProcessBeanDefinitionRegistry(context);
        assertThat(context.getBeanDefinitionCount()).isZero();
      }
      customizer.customizeContext(context, config);
      context.refresh();
      assertThat(context.getBean(WebTestClient.class)).isNotNull();
      var registrar = new WebTestClientContextCustomizer.WebTestClientRegistrar();
      assertThat(registrar.getOrder()).isEqualTo(Integer.MAX_VALUE);
    }
    var context = mock(ConfigurableApplicationContext.class);
    given(context.getBeanFactory()).willReturn(mock(ConfigurableBeanFactory.class));
    new WebTestClientContextCustomizer().customizeContext(context,
            new MergedContextConfiguration(Embedded.class, null, null, null, null));
  }

  @InfraTest(webEnvironment = InfraTest.WebEnvironment.NONE)
  static class NonEmbedded { }

  @InfraTest(webEnvironment = InfraTest.WebEnvironment.RANDOM_PORT)
  static class Embedded { }

}
