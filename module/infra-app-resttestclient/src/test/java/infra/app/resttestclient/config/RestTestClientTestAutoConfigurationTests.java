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

package infra.app.resttestclient.config;

import org.assertj.core.extractor.Extractors;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import infra.app.test.context.runner.WebApplicationContextRunner;
import infra.app.test.http.server.LocalTestWebServer;
import infra.app.test.http.server.LocalTestWebServer.Scheme;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.core.annotation.Order;
import infra.http.converter.HttpMessageConverters.ClientBuilder;
import infra.http.converter.config.ClientHttpMessageConvertersCustomizer;
import infra.test.classpath.resources.WithResource;
import infra.test.context.FilteredClassLoader;
import infra.test.web.mock.client.RestTestClient;
import infra.web.client.RestClient;
import infra.web.util.UriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RestTestClientTestAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class RestTestClientTestAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(RestTestClientTestAutoConfiguration.class));

	@Test
	void registersRestTestClient() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(RestTestClient.class));
	}

	@Test
	void shouldNotRegisterRestTestClientIfRestClientIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(RestClient.class))
			.run((context) -> assertThat(context).doesNotHaveBean(RestTestClient.class));
	}

	@Test
	void shouldApplyRestTestClientCustomizers() {
		this.contextRunner.withUserConfiguration(RestTestClientCustomConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(RestTestClient.class);
			assertThat(context).hasBean("myRestTestClientCustomizer");
			then(context.getBean("myRestTestClientCustomizer", RestTestClientBuilderCustomizer.class)).should()
				.customize(any(RestTestClient.Builder.class));
		});
	}

	@Test
	@WithResource(name = "META-INF/today.strategies",
			content = """
					infra.app.test.http.server.LocalTestWebServer$Provider=\
					infra.app.resttestclient.config.RestTestClientTestAutoConfigurationTests$TestLocalTestWebServerProvider
					""")
	void shouldDefineRestTestClientBoundToWebServer() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(RestTestClient.class).hasBean("restTestClient");
			RestTestClient client = context.getBean(RestTestClient.class);
			UriBuilderFactory uiBuilderFactory = (UriBuilderFactory) Extractors
				.byName("restTestClientBuilder.restClientBuilder.uriBuilderFactory")
				.apply(client);
			assertThat(uiBuilderFactory.uriString("").toUriString()).isEqualTo("https://localhost:8182");
		});
	}

	@Test
	void clientHttpMessageConverterCustomizersAreAppliedInOrder() {
		this.contextRunner.withUserConfiguration(ClientHttpMessageConverterCustomizersConfiguration.class)
			.run((context) -> {
				ClientHttpMessageConvertersCustomizer customizer1 = context.getBean("customizer1",
						ClientHttpMessageConvertersCustomizer.class);
				ClientHttpMessageConvertersCustomizer customizer2 = context.getBean("customizer2",
						ClientHttpMessageConvertersCustomizer.class);
				ClientHttpMessageConvertersCustomizer customizer3 = context.getBean("customizer3",
						ClientHttpMessageConvertersCustomizer.class);
				InOrder inOrder = inOrder(customizer1, customizer2, customizer3);
				inOrder.verify(customizer3).customize(any(ClientBuilder.class));
				inOrder.verify(customizer1).customize(any(ClientBuilder.class));
				inOrder.verify(customizer2).customize(any(ClientBuilder.class));
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class RestTestClientCustomConfig {

		@Bean
		RestTestClientBuilderCustomizer myRestTestClientCustomizer() {
			return mock(RestTestClientBuilderCustomizer.class);
		}

	}

	@SuppressWarnings("unused")
	static class TestLocalTestWebServerProvider implements LocalTestWebServer.Provider {

		@Override
		public @Nullable LocalTestWebServer getLocalTestWebServer() {
			return LocalTestWebServer.of(Scheme.HTTPS, 8182);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ClientHttpMessageConverterCustomizersConfiguration {

		@Bean
		@Order(-5)
		ClientHttpMessageConvertersCustomizer customizer1() {
			return mock(ClientHttpMessageConvertersCustomizer.class);
		}

		@Bean
		@Order(5)
		ClientHttpMessageConvertersCustomizer customizer2() {
			return mock(ClientHttpMessageConvertersCustomizer.class);
		}

		@Bean
		@Order(-10)
		ClientHttpMessageConvertersCustomizer customizer3() {
			return mock(ClientHttpMessageConvertersCustomizer.class);
		}

	}

}
