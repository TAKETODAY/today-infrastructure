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

package infra.annotation.config.http;

import infra.beans.factory.annotation.DisableAllDependencyInjection;
import infra.beans.factory.annotation.DisableDependencyInjection;
import infra.context.annotation.Bean;
import infra.context.annotation.Conditional;
import infra.context.annotation.Configuration;
import infra.context.annotation.Lazy;
import infra.context.condition.AnyNestedCondition;
import infra.context.condition.ConditionalOnBean;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnProperty;
import infra.core.annotation.Order;
import infra.http.converter.HttpMessageConverters.ClientBuilder;
import infra.http.converter.HttpMessageConverters.ServerBuilder;
import infra.http.converter.json.JsonbHttpMessageConverter;
import jakarta.json.bind.Jsonb;

import static infra.annotation.config.http.GsonHttpMessageConvertersConfiguration.GsonHttpConvertersCustomizer;
import static infra.annotation.config.http.JacksonHttpMessageConvertersConfiguration.JacksonJsonHttpMessageConvertersCustomizer;

/**
 * Configuration for HTTP Message converters that use JSON-B.
 *
 * @author Eddú Meléndez
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Lazy
@DisableDependencyInjection
@DisableAllDependencyInjection
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Jsonb.class)
class JsonbHttpMessageConvertersConfiguration {

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBean(Jsonb.class)
  @Conditional(PreferJsonbOrMissingJacksonAndGsonCondition.class)
  static class JsonbHttpMessageConverterConfiguration {

    @Bean
    @Order(0)
    @ConditionalOnMissingBean(JsonbHttpMessageConverter.class)
    JsonbHttpMessageConvertersCustomizer jsonbHttpMessageConvertersCustomizer(Jsonb jsonb) {
      return new JsonbHttpMessageConvertersCustomizer(jsonb);
    }

  }

  static class JsonbHttpMessageConvertersCustomizer
          implements ClientHttpMessageConvertersCustomizer, ServerHttpMessageConvertersCustomizer {

    private final JsonbHttpMessageConverter converter;

    JsonbHttpMessageConvertersCustomizer(Jsonb jsonb) {
      this.converter = new JsonbHttpMessageConverter(jsonb);
    }

    @Override
    public void customize(ClientBuilder builder) {
      builder.withJsonConverter(this.converter);
    }

    @Override
    public void customize(ServerBuilder builder) {
      builder.withJsonConverter(this.converter);
    }

  }

  private static class PreferJsonbOrMissingJacksonAndGsonCondition extends AnyNestedCondition {

    PreferJsonbOrMissingJacksonAndGsonCondition() {
      super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnProperty(name = HttpMessageConvertersAutoConfiguration.PREFERRED_MAPPER_PROPERTY,
            havingValue = "jsonb")
    static class JsonbPreferred {

    }

    @ConditionalOnMissingBean({ JacksonJsonHttpMessageConvertersCustomizer.class, GsonHttpConvertersCustomizer.class })
    static class JacksonAndGsonMissing {

    }

  }

}
