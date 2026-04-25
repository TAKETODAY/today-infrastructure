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

package infra.app.restclient.test.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.app.test.context.PropertyMapping;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.app.restclient.test.MockServerRestClientCustomizer;
import infra.app.restclient.test.MockServerRestTemplateCustomizer;
import infra.test.web.client.MockRestServiceServer;
import infra.web.client.RestClient;
import infra.web.client.RestTemplate;
import infra.web.client.RestTemplateBuilder;

/**
 * Annotation that can be applied to a test class to enable and configure
 * auto-configuration of a single {@link MockRestServiceServer}. Only useful when a single
 * call is made to {@link RestTemplateBuilder} or {@link RestClient.Builder RestClient.Builder}. If
 * multiple {@link infra.web.client.RestTemplate RestTemplates} or
 * {@link infra.web.client.RestClient RestClients} are in use, inject a
 * {@link MockServerRestTemplateCustomizer} and use
 * {@link MockServerRestTemplateCustomizer#getServer(RestTemplate)
 * getServer(RestTemplate)}, or inject a {@link MockServerRestClientCustomizer} and use
 * {@link MockServerRestClientCustomizer#getServer(RestClient.Builder)
 * * getServer(RestClient.Builder)}, or bind a {@link MockRestServiceServer} directly.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @see MockServerRestTemplateCustomizer
 * @since 5.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
@PropertyMapping("infra.test.restclient.mockrestserviceserver")
public @interface AutoConfigureMockRestServiceServer {

  /**
   * If {@link MockServerRestTemplateCustomizer} and
   * {@link MockServerRestClientCustomizer} should be enabled and
   * {@link MockRestServiceServer} beans should be registered. Defaults to {@code true}
   *
   * @return if mock support is enabled
   */
  boolean enabled() default true;

}
