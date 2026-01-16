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

import infra.app.health.config.contributor.CompositeHealthContributorConfiguration;
import infra.app.health.config.contributor.ConditionalOnEnabledHealthIndicator;
import infra.app.health.contributor.HealthContributor;
import infra.app.mail.health.MailHealthIndicator;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.annotation.Bean;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnBean;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.mail.javamail.JavaMailSenderImpl;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link MailHealthIndicator}.
 *
 * @author Johannes Edmeier
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@DisableDIAutoConfiguration(after = MailSenderAutoConfiguration.class)
@ConditionalOnClass({ JavaMailSenderImpl.class, MailHealthIndicator.class, ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnBean(JavaMailSenderImpl.class)
@ConditionalOnEnabledHealthIndicator("mail")
public final class MailHealthContributorAutoConfiguration extends CompositeHealthContributorConfiguration<MailHealthIndicator, JavaMailSenderImpl> {

  MailHealthContributorAutoConfiguration() {
    super(MailHealthIndicator::new);
  }

  @Bean
  @ConditionalOnMissingBean(name = { "mailHealthIndicator", "mailHealthContributor" })
  HealthContributor mailHealthContributor(ConfigurableBeanFactory beanFactory) {
    return createContributor(beanFactory, JavaMailSenderImpl.class);
  }

}
