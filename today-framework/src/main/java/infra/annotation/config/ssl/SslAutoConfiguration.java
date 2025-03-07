/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.annotation.config.ssl;

import java.util.List;

import infra.context.annotation.Lazy;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.io.ResourceLoader;
import infra.core.ssl.DefaultSslBundleRegistry;
import infra.core.ssl.SslBundleRegistry;
import infra.core.ssl.SslBundles;
import infra.stereotype.Component;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for SSL.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Lazy
@DisableDIAutoConfiguration
@EnableConfigurationProperties(SslProperties.class)
public class SslAutoConfiguration {

  private SslAutoConfiguration() {
  }

  @Component
  public static SslPropertiesBundleRegistrar sslPropertiesSslBundleRegistrar(SslProperties properties, ResourceLoader resourceLoader) {
    FileWatcher fileWatcher = new FileWatcher(properties.getBundle().getWatch().getFile().getQuietPeriod());
    return new SslPropertiesBundleRegistrar(properties, fileWatcher, resourceLoader);
  }

  @Component
  @ConditionalOnMissingBean({ SslBundleRegistry.class, SslBundles.class })
  public static DefaultSslBundleRegistry sslBundleRegistry(List<SslBundleRegistrar> sslBundleRegistrars) {
    DefaultSslBundleRegistry registry = new DefaultSslBundleRegistry();
    for (SslBundleRegistrar registrar : sslBundleRegistrars) {
      registrar.registerBundles(registry);
    }
    return registry;
  }

}
