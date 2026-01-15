/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.mail.config;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Properties;

import infra.app.mail.config.MailProperties.Ssl;
import infra.beans.factory.ObjectProvider;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnProperty;
import infra.core.ssl.SslBundle;
import infra.core.ssl.SslBundles;
import infra.lang.Assert;
import infra.mail.MailSender;
import infra.mail.javamail.JavaMailSender;
import infra.mail.javamail.JavaMailSenderImpl;
import infra.util.StringUtils;

/**
 * Auto-configure a {@link MailSender} based on properties configuration.
 *
 * @author Oliver Gierke
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty("mail.host")
class MailSenderPropertiesConfiguration {

  @Bean
  @ConditionalOnMissingBean(JavaMailSender.class)
  JavaMailSenderImpl mailSender(MailProperties properties, ObjectProvider<SslBundles> sslBundles) {
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    applyProperties(properties, sender, sslBundles.getIfAvailable());
    return sender;
  }

  private void applyProperties(MailProperties properties, JavaMailSenderImpl sender,
          @Nullable SslBundles sslBundles) {
    sender.setHost(properties.getHost());
    if (properties.getPort() != null) {
      sender.setPort(properties.getPort());
    }
    sender.setUsername(properties.getUsername());
    sender.setPassword(properties.getPassword());
    sender.setProtocol(properties.getProtocol());
    if (properties.getDefaultEncoding() != null) {
      sender.setDefaultEncoding(properties.getDefaultEncoding().name());
    }
    Properties javaMailProperties = asProperties(properties.getProperties());
    String protocol = properties.getProtocol();
    protocol = StringUtils.isEmpty(protocol) ? "smtp" : protocol;
    Ssl ssl = properties.getSsl();
    if (ssl.isEnabled()) {
      javaMailProperties.setProperty("mail." + protocol + ".ssl.enable", "true");
    }
    if (ssl.getBundle() != null) {
      Assert.state(sslBundles != null, "'sslBundles' must not be null");
      SslBundle sslBundle = sslBundles.getBundle(ssl.getBundle());
      javaMailProperties.put("mail." + protocol + ".ssl.socketFactory",
              sslBundle.createSslContext().getSocketFactory());
    }
    if (!javaMailProperties.isEmpty()) {
      sender.setJavaMailProperties(javaMailProperties);
    }
  }

  private Properties asProperties(Map<String, String> source) {
    Properties properties = new Properties();
    properties.putAll(source);
    return properties;
  }

}
