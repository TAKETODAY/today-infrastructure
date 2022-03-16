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

package cn.taketoday.web.framework.server.light;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.properties.Props;
import cn.taketoday.lang.Experimental;
import cn.taketoday.web.multipart.MultipartConfiguration;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 还没有写完功能残缺
 *
 * @author TODAY 2021/4/13 19:30
 */
@Experimental
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
@Import(LightHttpConfiguration.class)
public @interface EnableLightHttpHandling {

}

@Configuration(proxyBeanMethods = false)
class LightHttpConfiguration {

  /**
   * Default {@link LightWebServer} object
   * <p>
   * framework will auto inject properties start with 'server.' or 'server.light.'
   * </p>
   *
   * @return returns a default {@link LightWebServer} object
   */
  @MissingBean(value = WebServer.class)
  @Props("server.light")
  LightWebServer lightWebServer(LightHttpConfig lightHttpConfig) {
    return new LightWebServer(lightHttpConfig);
  }

  @MissingBean
  LightHttpConfig lightHttpConfig(MultipartConfiguration multipartConfig) {
    final LightHttpConfig lightHttpConfig = LightHttpConfig.defaultConfig();
    lightHttpConfig.setMultipartConfig(multipartConfig);
    return lightHttpConfig;
  }
}
