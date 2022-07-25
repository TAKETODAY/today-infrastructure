/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.http.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.stream.Collectors;

import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnBean;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnProperty;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverters;
import cn.taketoday.http.converter.json.MappingJackson2HttpMessageConverter;
import cn.taketoday.stereotype.Component;
import cn.taketoday.web.config.jackson.JacksonAutoConfiguration;

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
@AutoConfiguration(after = JacksonAutoConfiguration.class)
public class HttpMessageConvertersAutoConfiguration {
  static final String PREFERRED_MAPPER_PROPERTY = "web.mvc.converters.preferred-json-mapper";

  @Component
  @ConditionalOnMissingBean
  public HttpMessageConverters messageConverters(ObjectProvider<HttpMessageConverter<?>> converters) {
    return new HttpMessageConverters(converters.orderedStream().collect(Collectors.toList()));
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(ObjectMapper.class)
  @ConditionalOnBean(ObjectMapper.class)
  @ConditionalOnProperty(name = PREFERRED_MAPPER_PROPERTY, havingValue = "jackson", matchIfMissing = true)
  static class MappingJackson2HttpMessageConverterConfiguration {

    @Component
    @ConditionalOnMissingBean(MappingJackson2HttpMessageConverter.class)
    MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(ObjectMapper objectMapper) {
      return new MappingJackson2HttpMessageConverter(objectMapper);
    }

  }
}

