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

package cn.taketoday.annotation.config.http;

import com.fasterxml.jackson.databind.ObjectMapper;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.DisableDIAutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnBean;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.http.codec.CodecConfigurer;
import cn.taketoday.http.codec.CodecCustomizer;
import cn.taketoday.http.codec.json.Jackson2JsonDecoder;
import cn.taketoday.http.codec.json.Jackson2JsonEncoder;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.DataSize;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.PropertyMapper;
import cn.taketoday.web.client.reactive.WebClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link cn.taketoday.core.codec.Encoder Encoders} and
 * {@link cn.taketoday.core.codec.Decoder Decoders}.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@DisableDIAutoConfiguration
@ConditionalOnClass({ CodecConfigurer.class, WebClient.class })
@EnableConfigurationProperties(CodecProperties.class)
public class CodecsAutoConfiguration {

  private static final MimeType[] EMPTY_MIME_TYPES = {};

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(ObjectMapper.class)
  static class JacksonCodecConfiguration {

    @Order(0)
    @Component
    @ConditionalOnBean(ObjectMapper.class)
    static CodecCustomizer jacksonCodecCustomizer(ObjectMapper objectMapper) {
      return configurer -> {
        CodecConfigurer.DefaultCodecs defaults = configurer.defaultCodecs();
        defaults.jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper, EMPTY_MIME_TYPES));
        defaults.jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper, EMPTY_MIME_TYPES));
      };
    }

  }

  @Component
  @Order(0)
  static CodecCustomizer defaultCodecCustomizer(CodecProperties codecProperties) {
    return configurer -> {
      PropertyMapper map = PropertyMapper.get();
      CodecConfigurer.DefaultCodecs defaultCodecs = configurer.defaultCodecs();
      defaultCodecs.enableLoggingRequestDetails(codecProperties.isLogRequestDetails());
      map.from(codecProperties.getMaxInMemorySize())
              .whenNonNull().asInt(DataSize::toBytes)
              .to(defaultCodecs::maxInMemorySize);
    };
  }

}
