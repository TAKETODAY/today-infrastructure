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

import com.google.gson.Gson;

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
import infra.context.condition.NoneNestedConditions;
import infra.core.annotation.Order;
import infra.http.converter.HttpMessageConverters.ClientBuilder;
import infra.http.converter.HttpMessageConverters.ServerBuilder;
import infra.http.converter.config.JacksonHttpMessageConvertersConfiguration.JacksonJsonHttpMessageConvertersCustomizer;
import infra.http.converter.json.GsonHttpMessageConverter;

/**
 * Configuration for HTTP Message converters that use Gson.
 *
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Lazy
@DisableDependencyInjection
@DisableAllDependencyInjection
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Gson.class)
class GsonHttpMessageConvertersConfiguration {

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBean(Gson.class)
  @Conditional(PreferGsonOrJacksonAndJsonbUnavailableCondition.class)
  static class GsonHttpMessageConverterConfiguration {

    @Bean
    @Order(0)
    @ConditionalOnMissingBean(GsonHttpMessageConverter.class)
    static GsonHttpConvertersCustomizer gsonHttpMessageConvertersCustomizer(Gson gson) {
      return new GsonHttpConvertersCustomizer(gson);
    }

  }

  static class GsonHttpConvertersCustomizer
          implements ClientHttpMessageConvertersCustomizer, ServerHttpMessageConvertersCustomizer {

    private final GsonHttpMessageConverter converter;

    GsonHttpConvertersCustomizer(Gson gson) {
      this.converter = new GsonHttpMessageConverter(gson);
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

  private static class PreferGsonOrJacksonAndJsonbUnavailableCondition extends AnyNestedCondition {

    PreferGsonOrJacksonAndJsonbUnavailableCondition() {
      super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnProperty(name = HttpMessageConvertersAutoConfiguration.PREFERRED_MAPPER_PROPERTY,
            havingValue = "gson")
    static class GsonPreferred {

    }

    @Conditional(JacksonAndJsonbUnavailableCondition.class)
    static class JacksonJsonbUnavailable {

    }

  }

  private static class JacksonAndJsonbUnavailableCondition extends NoneNestedConditions {

    JacksonAndJsonbUnavailableCondition() {
      super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnBean(JacksonJsonHttpMessageConvertersCustomizer.class)
    static class JacksonAvailable {

    }

    @ConditionalOnProperty(name = HttpMessageConvertersAutoConfiguration.PREFERRED_MAPPER_PROPERTY,
            havingValue = "jsonb")
    static class JsonbPreferred {

    }

  }

}
