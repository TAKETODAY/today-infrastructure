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

import infra.app.resttestclient.TestRestTemplate;
import infra.app.resttestclient.TestRestTemplate.HttpClientOption;
import infra.app.test.http.server.LocalTestWebServer;
import infra.beans.factory.ObjectProvider;
import infra.context.ApplicationContext;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.stereotype.Component;
import infra.web.client.RestTemplateBuilder;

/**
 * Test auto-configuration for {@link TestRestTemplate}.
 *
 * @author Andy Wilkinson
 * @see AutoConfigureTestRestTemplate
 */
@AutoConfiguration
final class TestRestTemplateTestAutoConfiguration {

  @Component(name = "infra.resttestclient.TestRestTemplate")
  @ConditionalOnMissingBean
  static TestRestTemplate testRestTemplate(ObjectProvider<RestTemplateBuilder> builderProvider,
          ApplicationContext applicationContext) {
    RestTemplateBuilder builder = builderProvider.getIfAvailable(RestTemplateBuilder::new);
    LocalTestWebServer localTestWebServer = LocalTestWebServer.obtain(applicationContext);
    TestRestTemplate template = new TestRestTemplate(builder, null, null,
            httpClientOptions(localTestWebServer.scheme()));
    template.setUriTemplateHandler(localTestWebServer.uriBuilderFactory());
    return template;
  }

  private static HttpClientOption[] httpClientOptions(LocalTestWebServer.Scheme scheme) {
    return switch (scheme) {
      case HTTP -> new HttpClientOption[] {};
      case HTTPS -> new HttpClientOption[] { HttpClientOption.SSL };
    };
  }

}
