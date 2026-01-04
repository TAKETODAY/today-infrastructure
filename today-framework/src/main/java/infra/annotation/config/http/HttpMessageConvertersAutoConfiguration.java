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

import java.util.List;

import infra.annotation.ConditionalOnWebApplication;
import infra.annotation.ConditionalOnWebApplication.Type;
import infra.annotation.config.gson.GsonAutoConfiguration;
import infra.annotation.config.jackson.JacksonAutoConfiguration;
import infra.annotation.config.jsonb.JsonbAutoConfiguration;
import infra.context.annotation.Conditional;
import infra.context.annotation.Import;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.NoneNestedConditions;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.annotation.Order;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.HttpMessageConverters.ClientBuilder;
import infra.http.converter.HttpMessageConverters.ServerBuilder;
import infra.http.converter.StringHttpMessageConverter;
import infra.stereotype.Component;

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
@EnableConfigurationProperties(HttpMessageConvertersProperties.class)
@Conditional(HttpMessageConvertersAutoConfiguration.NotReactiveWebApplicationCondition.class)
@Import({ GsonHttpMessageConvertersConfiguration.class, JsonbHttpMessageConvertersConfiguration.class })
public final class HttpMessageConvertersAutoConfiguration {

  static final String PREFERRED_MAPPER_PROPERTY = "http.converters.preferred-json-mapper";

  @Order(0)
  @Component
  static DefaultHttpMessageConvertersCustomizer clientConvertersCustomizer(List<HttpMessageConverter<?>> converters) {
    return new DefaultHttpMessageConvertersCustomizer(converters);
  }

  @Component
  @ConditionalOnMissingBean(StringHttpMessageConverter.class)
  static StringHttpMessageConvertersCustomizer stringHttpMessageConvertersCustomizer(HttpMessageConvertersProperties properties) {
    return new StringHttpMessageConvertersCustomizer(properties);
  }

  static class StringHttpMessageConvertersCustomizer implements ClientHttpMessageConvertersCustomizer, ServerHttpMessageConvertersCustomizer {

    private final StringHttpMessageConverter converter;

    StringHttpMessageConvertersCustomizer(HttpMessageConvertersProperties properties) {
      this.converter = new StringHttpMessageConverter(properties.stringEncodingCharset);
      this.converter.setWriteAcceptCharset(false);
    }

    @Override
    public void customize(ClientBuilder builder) {
      builder.withStringConverter(this.converter);
    }

    @Override
    public void customize(ServerBuilder builder) {
      builder.withStringConverter(this.converter);
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

}

