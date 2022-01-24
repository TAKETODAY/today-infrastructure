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

package cn.taketoday.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.aware.ApplicationContextSupport;

/**
 * @author TODAY 2021/3/26 20:16
 * @since 3.0
 */
@Configuration(proxyBeanMethods = false)
@DisableAllDependencyInjection
@Deprecated
public class JacksonConfiguration
        extends ApplicationContextSupport implements InitializingBean {

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
