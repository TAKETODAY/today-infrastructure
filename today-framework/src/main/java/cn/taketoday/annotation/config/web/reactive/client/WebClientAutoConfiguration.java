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

package cn.taketoday.annotation.config.web.reactive.client;

import java.util.List;

import cn.taketoday.annotation.config.http.CodecsAutoConfiguration;
import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Scope;
import cn.taketoday.context.annotation.config.AutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnBean;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.core.ssl.SslBundles;
import cn.taketoday.http.config.CodecCustomizer;
import cn.taketoday.stereotype.Component;
import cn.taketoday.web.reactive.function.client.WebClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link WebClient}.
 * <p>
 * This will produce a
 * {@link cn.taketoday.web.reactive.function.client.WebClient.Builder
 * WebClient.Builder} bean with the {@code prototype} scope, meaning each injection point
 * will receive a newly cloned instance of the builder.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@AutoConfiguration(after = { CodecsAutoConfiguration.class, ClientHttpConnectorAutoConfiguration.class })
@ConditionalOnClass(WebClient.class)
public class WebClientAutoConfiguration {

  @Component
  @Scope("prototype")
  @ConditionalOnMissingBean
  public WebClient.Builder webClientBuilder(ObjectProvider<WebClientCustomizer> customizerProvider) {
    WebClient.Builder builder = WebClient.builder();
    customizerProvider.orderedStream().forEach((customizer) -> customizer.customize(builder));
    return builder;
  }

  @Component
  @ConditionalOnMissingBean(WebClientSsl.class)
  @ConditionalOnBean(SslBundles.class)
  AutoConfiguredWebClientSsl webClientSsl(ClientHttpConnectorFactory<?> clientHttpConnectorFactory,
          SslBundles sslBundles) {
    return new AutoConfiguredWebClientSsl(clientHttpConnectorFactory, sslBundles);
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBean(CodecCustomizer.class)
  protected static class WebClientCodecsConfiguration {

    @Component
    @ConditionalOnMissingBean
    @Order(0)
    public WebClientCodecCustomizer exchangeStrategiesCustomizer(List<CodecCustomizer> codecCustomizers) {
      return new WebClientCodecCustomizer(codecCustomizers);
    }

  }

}
