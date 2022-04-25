/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.http.config;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.config.AutoConfigureAfter;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.core.env.Environment;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.lang.Component;
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
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter({ JacksonAutoConfiguration.class })
@Import(JacksonHttpMessageConvertersConfiguration.class)
public class HttpMessageConvertersAutoConfiguration {

  @Component
  @ConditionalOnMissingBean
  public HttpMessageConverters messageConverters(ObjectProvider<HttpMessageConverter<?>> converters) {
    return new HttpMessageConverters(converters.orderedStream().collect(Collectors.toList()));
  }

  @Component
  @ConditionalOnMissingBean
  public StringHttpMessageConverter stringHttpMessageConverter(Environment environment) {
    Charset charset;
    String encoding = environment.getProperty("server.encoding");
    if (encoding != null) {
      charset = Charset.forName(encoding);
    }
    else {
      charset = StandardCharsets.UTF_8;
    }
    StringHttpMessageConverter converter = new StringHttpMessageConverter(charset);
    converter.setWriteAcceptCharset(false);
    return converter;
  }

}

