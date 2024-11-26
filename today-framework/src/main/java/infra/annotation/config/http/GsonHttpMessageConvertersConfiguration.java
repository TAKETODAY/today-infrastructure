/*
 * Copyright 2017 - 2024 the original author or authors.
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

import com.google.gson.Gson;

import infra.beans.factory.annotation.DisableAllDependencyInjection;
import infra.beans.factory.annotation.DisableDependencyInjection;
import infra.context.annotation.Conditional;
import infra.context.annotation.Configuration;
import infra.context.annotation.Lazy;
import infra.context.condition.AnyNestedCondition;
import infra.context.condition.ConditionalOnBean;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnProperty;
import infra.context.condition.NoneNestedConditions;
import infra.http.converter.json.GsonHttpMessageConverter;
import infra.http.converter.json.MappingJackson2HttpMessageConverter;
import infra.stereotype.Component;

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

    @Component
    @ConditionalOnMissingBean
    static GsonHttpMessageConverter gsonHttpMessageConverter(Gson gson) {
      GsonHttpMessageConverter converter = new GsonHttpMessageConverter();
      converter.setGson(gson);
      return converter;
    }

  }

  private static class PreferGsonOrJacksonAndJsonbUnavailableCondition extends AnyNestedCondition {

    PreferGsonOrJacksonAndJsonbUnavailableCondition() {
      super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnProperty(
            name = HttpMessageConvertersAutoConfiguration.PREFERRED_MAPPER_PROPERTY, havingValue = "gson")
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

    @ConditionalOnBean(MappingJackson2HttpMessageConverter.class)
    static class JacksonAvailable {

    }

    @ConditionalOnProperty(name = HttpMessageConvertersAutoConfiguration.PREFERRED_MAPPER_PROPERTY,
                           havingValue = "jsonb")
    static class JsonbPreferred {

    }

  }

}
