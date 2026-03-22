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

package infra.mail.config;

import org.junit.jupiter.api.Test;

import infra.app.diagnostics.FailureAnalysis;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.annotation.config.AutoConfigurations;
import infra.mail.MailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

/**
 * Tests for {@link NoSuchMailSenderBeanFailureAnalyzer}.
 *
 * @author MJY (answndud)
 * @author Andy Wilkinson
 */
class NoSuchMailSenderBeanFailureAnalyzerTests {

  @Test
  void analyzeWhenNotNoSuchBeanDefinitionExceptionShouldReturnNull() {
    new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
            .run((context) -> {
              ConfigurableBeanFactory beanFactory = context.getBeanFactory();
              FailureAnalysis analysis = new NoSuchMailSenderBeanFailureAnalyzer(beanFactory)
                      .analyze(new Exception());
              assertThat(analysis).isNull();
            });
  }

  @Test
  void analyzeWhenNoSuchBeanDefinitionExceptionForDifferentTypeShouldReturnNull() {
    new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
            .run((context) -> {
              ConfigurableBeanFactory beanFactory = context.getBeanFactory();
              assertThatException().isThrownBy(() -> context.getBean(String.class)).satisfies((ex) -> {
                FailureAnalysis analysis = new NoSuchMailSenderBeanFailureAnalyzer(beanFactory).analyze(ex);
                assertThat(analysis).isNull();
              });
            });
  }

  @Test
  void analyzeWithoutMailSenderAutoConfigurationShouldReturnNull() {
    new ApplicationContextRunner().run((context) -> {
      ConfigurableBeanFactory beanFactory = context.getBeanFactory();
      assertThatException().isThrownBy(() -> context.getBean(MailSender.class)).satisfies((ex) -> {
        FailureAnalysis analysis = new NoSuchMailSenderBeanFailureAnalyzer(beanFactory).analyze(ex);
        assertThat(analysis).isNull();
      });
    });
  }

  @Test
  void analyzeWhenMailSenderBeanIsMissingAndMailSenderConditionDidNotMatchShouldProvideGuidance() {
    new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
            .run((context) -> {
              ConfigurableBeanFactory beanFactory = context.getBeanFactory();
              assertThatException().isThrownBy(() -> context.getBean(MailSender.class)).satisfies((ex) -> {
                FailureAnalysis analysis = new NoSuchMailSenderBeanFailureAnalyzer(beanFactory).analyze(ex);
                assertThat(analysis).isNotNull();
                assertThat(analysis.getDescription()).contains("A MailSender bean could not be found")
                        .contains("mail.host")
                        .contains("mail.jndi-name");
                assertThat(analysis.getAction()).contains("mail.host")
                        .contains("mail.jndi-name")
                        .contains("MailSender bean");
              });
            });
  }

}
