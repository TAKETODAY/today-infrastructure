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

import org.jspecify.annotations.Nullable;

import java.util.Map;

import infra.app.diagnostics.AbstractFailureAnalyzer;
import infra.app.diagnostics.FailureAnalysis;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.context.condition.ConditionEvaluationReport;
import infra.context.condition.ConditionEvaluationReport.ConditionAndOutcome;
import infra.context.condition.ConditionEvaluationReport.ConditionAndOutcomes;
import infra.core.Ordered;
import infra.mail.MailSender;

/**
 * An {@link AbstractFailureAnalyzer} that improves missing {@link MailSender} guidance
 * when {@link MailSenderAutoConfiguration} is present but did not match.
 *
 * @author MJY (answndud)
 * @author Andy Wilkinson
 */
class NoSuchMailSenderBeanFailureAnalyzer extends AbstractFailureAnalyzer<NoSuchBeanDefinitionException>
        implements Ordered {

  private static final String MAIL_HOST_PROPERTY = "mail.host";

  private static final String MAIL_JNDI_NAME_PROPERTY = "mail.jndi-name";

  private final BeanFactory beanFactory;

  NoSuchMailSenderBeanFailureAnalyzer(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  protected @Nullable FailureAnalysis analyze(Throwable rootFailure, NoSuchBeanDefinitionException cause) {
    if (!isMissingMailSenderBean(cause)) {
      return null;
    }
    ConditionAndOutcome conditionAndOutcome = findMailSenderConditionOutcome();
    if (conditionAndOutcome == null || conditionAndOutcome.outcome.isMatch()) {
      return null;
    }
    String description = "A MailSender bean could not be found because MailSenderAutoConfiguration "
            + "did not match. Neither '" + MAIL_HOST_PROPERTY + "' nor '" + MAIL_JNDI_NAME_PROPERTY
            + "' is configured.";
    String action = "Consider configuring '" + MAIL_HOST_PROPERTY + "' or '" + MAIL_JNDI_NAME_PROPERTY
            + "' to enable auto-configuration. If you want to use a custom mail sender, define a MailSender "
            + "bean in your configuration.";
    return new FailureAnalysis(description, action, cause);
  }

  private @Nullable ConditionAndOutcome findMailSenderConditionOutcome() {
    ConditionEvaluationReport report = ConditionEvaluationReport.find(this.beanFactory);
    if (report != null) {
      Map<String, ConditionAndOutcomes> conditionAndOutcomesBySource = report.getConditionAndOutcomesBySource();
      ConditionAndOutcomes conditionAndOutcomes = conditionAndOutcomesBySource.get(MailSenderAutoConfiguration.class.getName());
      if (conditionAndOutcomes != null) {
        return conditionAndOutcomes.stream()
                .filter((candidate) -> candidate.condition instanceof MailSenderAutoConfiguration.MailSenderCondition)
                .findFirst()
                .orElse(null);
      }
    }
    return null;
  }

  private boolean isMissingMailSenderBean(NoSuchBeanDefinitionException cause) {
    Class<?> beanType = cause.getBeanType();
    if (beanType == null && cause.getResolvableType() != null) {
      beanType = cause.getResolvableType().resolve();
    }
    return (beanType != null) && MailSender.class.isAssignableFrom(beanType);
  }

  @Override
  public int getOrder() {
    return 0;
  }

}
