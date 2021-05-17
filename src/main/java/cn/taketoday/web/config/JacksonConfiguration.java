/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.condition.ConditionalOnClass;
import cn.taketoday.web.handler.JacksonObjectNotationProcessor;
import cn.taketoday.web.handler.ObjectNotationProcessor;
import cn.taketoday.web.view.JacksonMessageConverter;
import cn.taketoday.web.view.MessageConverter;

/**
 * @author TODAY 2021/3/26 20:16
 * @since 3.0
 */
@Configuration
@ConditionalOnClass("com.fasterxml.jackson.databind.ObjectMapper")
public class JacksonConfiguration {

  @MissingBean(type = MessageConverter.class)
  JacksonMessageConverter jacksonMessageConverter(
          ObjectMapper objectMapper, List<ObjectMapperCustomizer> customizers) {

    for (final ObjectMapperCustomizer customizer : customizers) {
      customizer.customize(objectMapper);
    }

    return new JacksonMessageConverter(objectMapper);
  }

  protected ObjectMapper createObjectMapper() {
    return new ObjectMapper();
  }

  @MissingBean
  ObjectMapper objectMapper() {
    return createObjectMapper();
  }

  @MissingBean(type = ObjectNotationProcessor.class)
  JacksonObjectNotationProcessor jacksonObjectNotationProcessor(ObjectMapper mapper) {
    return new JacksonObjectNotationProcessor(mapper);
  }

}
