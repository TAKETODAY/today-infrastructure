/*
 * Copyright 2017 - 2025 the original author or authors.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.util.List;

import infra.annotation.ConditionalOnWebApplication;
import infra.annotation.ConditionalOnWebApplication.Type;
import infra.annotation.config.gson.GsonAutoConfiguration;
import infra.annotation.config.jackson.JacksonAutoConfiguration;
import infra.annotation.config.jsonb.JsonbAutoConfiguration;
import infra.context.annotation.Conditional;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.ImportRuntimeHints;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.condition.ConditionalOnBean;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnProperty;
import infra.context.condition.NoneNestedConditions;
import infra.context.properties.EnableConfigurationProperties;
import infra.context.properties.bind.BindableRuntimeHintsRegistrar;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.HttpMessageConverters;
import infra.http.converter.StringHttpMessageConverter;
import infra.http.converter.json.Jackson2ObjectMapperBuilder;
import infra.http.converter.json.MappingJackson2HttpMessageConverter;
import infra.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import infra.stereotype.Component;
import infra.web.server.EncodingProperties;
import infra.web.server.ServerProperties;

/**
 * Auto-configuration for {@link HttpMessageConverter}s.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Piotr Maj
 * @author Oliver Gierke
 * @author David Liu
 * @author Andy Wilkinson
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 15:10
 */
@DisableDIAutoConfiguration(after = {
        GsonAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        JsonbAutoConfiguration.class
})
@ConditionalOnClass(HttpMessageConverter.class)
@Conditional(HttpMessageConvertersAutoConfiguration.NotReactiveWebApplicationCondition.class)
@Import({ GsonHttpMessageConvertersConfiguration.class, JsonbHttpMessageConvertersConfiguration.class })
@ImportRuntimeHints(HttpMessageConvertersAutoConfiguration.Hints.class)
public class HttpMessageConvertersAutoConfiguration {

  static final String PREFERRED_MAPPER_PROPERTY = "web.mvc.converters.preferred-json-mapper";

  private HttpMessageConvertersAutoConfiguration() {
  }

  @Component
  @ConditionalOnMissingBean
  public static HttpMessageConverters messageConverters(List<HttpMessageConverter<?>> converters) {
    return new HttpMessageConverters(converters);
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(ServerProperties.class)
  @ConditionalOnClass(StringHttpMessageConverter.class)
  protected static class StringHttpMessageConverterConfiguration {

    @Component
    @ConditionalOnMissingBean
    static StringHttpMessageConverter stringHttpMessageConverter(ServerProperties serverProperties) {
      StringHttpMessageConverter converter = new StringHttpMessageConverter(serverProperties.encoding.getCharset());
      converter.setWriteAcceptCharset(false);
      return converter;
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(ObjectMapper.class)
  @ConditionalOnBean(ObjectMapper.class)
  @ConditionalOnProperty(name = PREFERRED_MAPPER_PROPERTY, havingValue = "jackson", matchIfMissing = true)
  static class MappingJackson2HttpMessageConverterConfiguration {

    @Component
    @ConditionalOnMissingBean(MappingJackson2HttpMessageConverter.class)
    static MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(ObjectMapper objectMapper) {
      return new MappingJackson2HttpMessageConverter(objectMapper);
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(XmlMapper.class)
  @ConditionalOnBean(Jackson2ObjectMapperBuilder.class)
  protected static class MappingJackson2XmlHttpMessageConverterConfiguration {

    @Component
    @ConditionalOnMissingBean
    static MappingJackson2XmlHttpMessageConverter mappingJackson2XmlHttpMessageConverter(
            Jackson2ObjectMapperBuilder builder) {
      return new MappingJackson2XmlHttpMessageConverter(builder.createXmlMapper(true).build());
    }

  }

  static class NotReactiveWebApplicationCondition extends NoneNestedConditions {

    NotReactiveWebApplicationCondition() {
      super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnWebApplication(type = Type.REACTIVE)
    private static final class ReactiveWebApplication {

    }

  }

  static class Hints extends BindableRuntimeHintsRegistrar {

    Hints() {
      super(EncodingProperties.class);
    }

  }
}

