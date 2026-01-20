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

package infra.http.converter.config;

import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.condition.ConditionalOnBean;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnProperty;
import infra.core.annotation.Order;
import infra.http.converter.HttpMessageConverters.ClientBuilder;
import infra.http.converter.HttpMessageConverters.ServerBuilder;
import infra.http.converter.json.JacksonJsonHttpMessageConverter;
import infra.http.converter.xml.JacksonXmlHttpMessageConverter;
import infra.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 * Configuration for HTTP message converters that use Jackson.
 *
 * @author Andy Wilkinson
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
@Configuration(proxyBeanMethods = false)
class JacksonHttpMessageConvertersConfiguration {

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(JsonMapper.class)
  @ConditionalOnBean(JsonMapper.class)
  @ConditionalOnProperty(name = HttpMessageConvertersAutoConfiguration.PREFERRED_MAPPER_PROPERTY,
          havingValue = "jackson", matchIfMissing = true)
  static class JacksonJsonHttpMessageConverterConfiguration {

    @Bean
    @Order(0)
    @ConditionalOnMissingBean(JacksonJsonHttpMessageConverter.class)
    static JacksonJsonHttpMessageConvertersCustomizer jacksonJsonHttpMessageConvertersCustomizer(JsonMapper jsonMapper) {
      return new JacksonJsonHttpMessageConvertersCustomizer(jsonMapper);
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(XmlMapper.class)
  @ConditionalOnBean(XmlMapper.class)
  protected static class JacksonXmlHttpMessageConverterConfiguration {

    @Component
    @Order(0)
    @ConditionalOnMissingBean(JacksonXmlHttpMessageConverter.class)
    static JacksonXmlHttpMessageConvertersCustomizer jacksonXmlHttpMessageConvertersCustomizer(XmlMapper xmlMapper) {
      return new JacksonXmlHttpMessageConvertersCustomizer(xmlMapper);
    }

  }

  static class JacksonJsonHttpMessageConvertersCustomizer
          implements ClientHttpMessageConvertersCustomizer, ServerHttpMessageConvertersCustomizer {

    private final JsonMapper jsonMapper;

    JacksonJsonHttpMessageConvertersCustomizer(JsonMapper jsonMapper) {
      this.jsonMapper = jsonMapper;
    }

    @Override
    public void customize(ClientBuilder builder) {
      builder.withJsonConverter(new JacksonJsonHttpMessageConverter(this.jsonMapper));
    }

    @Override
    public void customize(ServerBuilder builder) {
      builder.withJsonConverter(new JacksonJsonHttpMessageConverter(this.jsonMapper));
    }

  }

  static class JacksonXmlHttpMessageConvertersCustomizer
          implements ClientHttpMessageConvertersCustomizer, ServerHttpMessageConvertersCustomizer {

    private final XmlMapper xmlMapper;

    JacksonXmlHttpMessageConvertersCustomizer(XmlMapper xmlMapper) {
      this.xmlMapper = xmlMapper;
    }

    @Override
    public void customize(ClientBuilder builder) {
      builder.withXmlConverter(new JacksonXmlHttpMessageConverter(this.xmlMapper));
    }

    @Override
    public void customize(ServerBuilder builder) {
      builder.withXmlConverter(new JacksonXmlHttpMessageConverter(this.xmlMapper));
    }

  }

}
