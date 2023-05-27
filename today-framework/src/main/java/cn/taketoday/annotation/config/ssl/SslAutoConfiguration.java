/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.annotation.config.ssl;

import java.util.List;

import cn.taketoday.context.annotation.config.AutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.ssl.DefaultSslBundleRegistry;
import cn.taketoday.core.ssl.SslBundleRegistry;
import cn.taketoday.core.ssl.SslBundles;
import cn.taketoday.stereotype.Component;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for SSL.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@AutoConfiguration
@EnableConfigurationProperties(SslProperties.class)
public class SslAutoConfiguration {

  @Component
  public SslPropertiesBundleRegistrar sslPropertiesSslBundleRegistrar(SslProperties sslProperties) {
    return new SslPropertiesBundleRegistrar(sslProperties);
  }

  @Component
  @ConditionalOnMissingBean({ SslBundleRegistry.class, SslBundles.class })
  public DefaultSslBundleRegistry sslBundleRegistry(List<SslBundleRegistrar> sslBundleRegistrars) {
    DefaultSslBundleRegistry registry = new DefaultSslBundleRegistry();
    sslBundleRegistrars.forEach((registrar) -> registrar.registerBundles(registry));
    return registry;
  }

}
