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

import java.util.List;

import infra.app.config.ConditionalOnWebApplication;
import infra.app.config.ConditionalOnWebApplication.Type;
import infra.app.jackson.config.JacksonAutoConfiguration;
import infra.jsonb.config.JsonbAutoConfiguration;
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

import static infra.http.converter.config.HttpMessageConvertersAutoConfiguration.NotReactiveWebApplicationCondition;

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
@DisableDIAutoConfiguration(afterName = "infra.gson.config.GsonAutoConfiguration", after = {
        JacksonAutoConfiguration.class,
        JsonbAutoConfiguration.class
})
@ConditionalOnClass(HttpMessageConverter.class)
@Conditional(NotReactiveWebApplicationCondition.class)
@EnableConfigurationProperties(HttpMessageConvertersProperties.class)
@Import({ JacksonHttpMessageConvertersConfiguration.class,
        GsonHttpMessageConvertersConfiguration.class, JsonbHttpMessageConvertersConfiguration.class })
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

