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
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.List;

import cn.taketoday.beans.InitializingBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.aware.ApplicationContextSupport;
import cn.taketoday.web.MessageBodyConverter;
import cn.taketoday.web.ObjectNotationProcessor;
import cn.taketoday.web.handler.JacksonObjectNotationProcessor;
import cn.taketoday.web.support.JacksonMessageBodyConverter;

/**
 * @author TODAY 2021/3/26 20:16
 * @since 3.0
 */
@Configuration
public class JacksonConfiguration
        extends ApplicationContextSupport implements InitializingBean {

  /**
   * construct a default ObjectMapper
   *
   * @see ObjectMapper
   * @see SerializationFeature#FAIL_ON_EMPTY_BEANS
   */
  @MissingBean
  ObjectMapper objectMapper() {
    final ObjectMapper objectMapper = createObjectMapper();
    // disable fail on empty beans
    objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    return objectMapper;
  }

  protected ObjectMapper createObjectMapper() {
    return new ObjectMapper();
  }

  @MissingBean(type = ObjectNotationProcessor.class)
  JacksonObjectNotationProcessor jacksonObjectNotationProcessor(ObjectMapper mapper) {
    return new JacksonObjectNotationProcessor(mapper);
  }

  @MissingBean(type = MessageBodyConverter.class)
  JacksonMessageBodyConverter jacksonMessageBodyConverter(ObjectMapper mapper) {
    return new JacksonMessageBodyConverter(mapper);
  }

  @Override
  public void afterPropertiesSet() {
    final ApplicationContext context = obtainApplicationContext();
    final ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
    final List<ObjectMapperCustomizer> mapperCustomizers = context.getBeans(ObjectMapperCustomizer.class);
    for (final ObjectMapperCustomizer customizer : mapperCustomizers) {
      customizer.customize(objectMapper);
    }

  }
}
