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

package infra.mail.config;

import javax.naming.NamingException;

import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnJndi;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnProperty;
import infra.jndi.JndiLocatorDelegate;
import infra.lang.Assert;
import infra.mail.MailSender;
import infra.mail.javamail.JavaMailSenderImpl;
import jakarta.mail.Session;

/**
 * Auto-configuration a {@link MailSender} based on a {@link Session} available on JNDI.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
@ConditionalOnJndi
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Session.class)
@ConditionalOnProperty("mail.jndi-name")
class MailSenderJndiConfiguration {

  @Bean
  JavaMailSenderImpl mailSender(Session session, MailProperties properties) {
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setDefaultEncoding(properties.defaultEncoding.name());
    sender.setSession(session);
    return sender;
  }

  @Bean
  @ConditionalOnMissingBean
  static Session session(MailProperties properties) {
    String jndiName = properties.jndiName;
    Assert.state(jndiName != null, "'jndiName' is required");
    try {
      return JndiLocatorDelegate.createDefaultResourceRefLocator().lookup(jndiName, Session.class);
    }
    catch (NamingException ex) {
      throw new IllegalStateException(String.format("Unable to find Session in JNDI location %s", jndiName), ex);
    }
  }

}
