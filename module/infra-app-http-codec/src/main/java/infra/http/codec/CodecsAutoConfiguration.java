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

package infra.http.codec;

import org.jspecify.annotations.Nullable;

import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnBean;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnProperty;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.core.codec.Decoder;
import infra.core.codec.Encoder;
import infra.http.codec.json.JacksonJsonDecoder;
import infra.http.codec.json.JacksonJsonEncoder;
import infra.stereotype.Component;
import infra.util.DataSize;
import infra.web.client.reactive.WebClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link Encoder Encoders} and
 * {@link Decoder Decoders}.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@DisableDIAutoConfiguration
@ConditionalOnClass({ CodecConfigurer.class, WebClient.class })
@EnableConfigurationProperties(HttpCodecProperties.class)
public final class CodecsAutoConfiguration {

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(JsonMapper.class)
  @ConditionalOnProperty(name = "http.codecs.preferred-json-mapper", havingValue = "jackson", matchIfMissing = true)
  static class JacksonJsonCodecConfiguration {

    @Bean
    @Order(0)
    @ConditionalOnBean(JsonMapper.class)
    static CodecCustomizer jacksonCodecCustomizer(JsonMapper jsonMapper) {
      return (configurer) -> {
        var defaults = configurer.defaultCodecs();
        defaults.jacksonJsonDecoder(new JacksonJsonDecoder(jsonMapper));
        defaults.jacksonJsonEncoder(new JacksonJsonEncoder(jsonMapper));
      };
    }

  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(HttpCodecProperties.class)
  static class DefaultCodecsConfiguration {

    @Component
    static DefaultCodecCustomizer defaultCodecCustomizer(HttpCodecProperties properties) {
      return new DefaultCodecCustomizer(properties.logRequestDetails, properties.maxInMemorySize);
    }

    static final class DefaultCodecCustomizer implements CodecCustomizer, Ordered {

      private final boolean logRequestDetails;

      private final @Nullable DataSize maxInMemorySize;

      DefaultCodecCustomizer(boolean logRequestDetails, @Nullable DataSize maxInMemorySize) {
        this.logRequestDetails = logRequestDetails;
        this.maxInMemorySize = maxInMemorySize;
      }

      @Override
      public void customize(CodecConfigurer configurer) {
        CodecConfigurer.DefaultCodecs defaultCodecs = configurer.defaultCodecs();
        defaultCodecs.enableLoggingRequestDetails(this.logRequestDetails);
        if (maxInMemorySize != null) {
          defaultCodecs.maxInMemorySize(maxInMemorySize.toBytesInt());
        }
      }

      @Override
      public int getOrder() {
        return 0;
      }

    }

  }

}
