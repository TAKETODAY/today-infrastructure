/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.annotation.config.http;

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
