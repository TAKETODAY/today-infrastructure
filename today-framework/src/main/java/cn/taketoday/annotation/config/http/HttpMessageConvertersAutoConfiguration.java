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

package cn.taketoday.annotation.config.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.util.stream.Collectors;

import cn.taketoday.annotation.config.gson.GsonAutoConfiguration;
import cn.taketoday.annotation.config.jackson.JacksonAutoConfiguration;
import cn.taketoday.annotation.config.jsonb.JsonbAutoConfiguration;
import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.config.AutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnBean;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnProperty;
import cn.taketoday.context.condition.NoneNestedConditions;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.core.env.Environment;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication.Type;
import cn.taketoday.framework.web.server.EncodingProperties;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverters;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.http.converter.json.Jackson2ObjectMapperBuilder;
import cn.taketoday.http.converter.json.MappingJackson2HttpMessageConverter;
import cn.taketoday.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import cn.taketoday.stereotype.Component;

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
@AutoConfiguration(after = {
        GsonAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        JsonbAutoConfiguration.class
})
@ConditionalOnClass(HttpMessageConverter.class)
@Conditional(HttpMessageConvertersAutoConfiguration.NotReactiveWebApplicationCondition.class)
@Import({
        GsonHttpMessageConvertersConfiguration.class,
        JsonbHttpMessageConvertersConfiguration.class
})
public class HttpMessageConvertersAutoConfiguration {
  static final String PREFERRED_MAPPER_PROPERTY = "web.mvc.converters.preferred-json-mapper";

  @Component
  @ConditionalOnMissingBean
  public HttpMessageConverters messageConverters(ObjectProvider<HttpMessageConverter<?>> converters) {
    return new HttpMessageConverters(converters.orderedStream().collect(Collectors.toList()));
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(StringHttpMessageConverter.class)
  protected static class StringHttpMessageConverterConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public StringHttpMessageConverter stringHttpMessageConverter(Environment environment) {
      var encoding = Binder.get(environment).bindOrCreate("server.encoding", EncodingProperties.class);
      StringHttpMessageConverter converter = new StringHttpMessageConverter(encoding.getCharset());
      converter.setWriteAcceptCharset(false);
      return converter;
    }

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

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(XmlMapper.class)
  @ConditionalOnBean(Jackson2ObjectMapperBuilder.class)
  protected static class MappingJackson2XmlHttpMessageConverterConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MappingJackson2XmlHttpMessageConverter mappingJackson2XmlHttpMessageConverter(
            Jackson2ObjectMapperBuilder builder) {
      return new MappingJackson2XmlHttpMessageConverter(builder.createXmlMapper(true).build());
    }

  }

  static class NotReactiveWebApplicationCondition extends NoneNestedConditions {

    NotReactiveWebApplicationCondition() {
      super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnWebApplication(type = Type.REACTIVE)
    private static class ReactiveWebApplication {

    }

  }
}

