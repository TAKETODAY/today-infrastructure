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

import infra.context.annotation.Configuration;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnBean;
import infra.context.condition.ConditionalOnClass;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.annotation.Order;
import infra.core.codec.Decoder;
import infra.core.codec.Encoder;
import infra.http.codec.CodecConfigurer;
import infra.http.codec.CodecCustomizer;
import infra.stereotype.Component;
import infra.util.DataSize;
import infra.util.MimeType;
import infra.util.PropertyMapper;
import infra.web.client.reactive.WebClient;
import tools.jackson.databind.ObjectMapper;

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
@EnableConfigurationProperties(CodecProperties.class)
public final class CodecsAutoConfiguration {

  private static final MimeType[] EMPTY_MIME_TYPES = {};

  @Component
  @Order(0)
  public static CodecCustomizer defaultCodecCustomizer(CodecProperties codecProperties) {
    return configurer -> {
      PropertyMapper map = PropertyMapper.get();
      CodecConfigurer.DefaultCodecs defaultCodecs = configurer.defaultCodecs();
      defaultCodecs.enableLoggingRequestDetails(codecProperties.isLogRequestDetails());
      map.from(codecProperties.getMaxInMemorySize())
              .whenNonNull().asInt(DataSize::toBytes)
              .to(defaultCodecs::maxInMemorySize);
    };
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(ObjectMapper.class)
  public static class JacksonCodecConfiguration {

    private JacksonCodecConfiguration() {
    }

    @Order(0)
    @Component
    @ConditionalOnBean(ObjectMapper.class)
    public static CodecCustomizer jacksonCodecCustomizer(ObjectMapper objectMapper) {
      return configurer -> {
        CodecConfigurer.DefaultCodecs defaults = configurer.defaultCodecs();
//        defaults.jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper, EMPTY_MIME_TYPES));
//        defaults.jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper, EMPTY_MIME_TYPES));
      };
    }

  }

}
