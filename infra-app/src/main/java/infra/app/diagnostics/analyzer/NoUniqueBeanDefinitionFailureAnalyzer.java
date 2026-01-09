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

package infra.app.diagnostics.analyzer;

import org.jspecify.annotations.Nullable;

import infra.app.diagnostics.FailureAnalysis;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.NoUniqueBeanDefinitionException;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.util.StringUtils;

/**
 * An {@link AbstractInjectionFailureAnalyzer} that performs analysis of failures caused
 * by a {@link NoUniqueBeanDefinitionException}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class NoUniqueBeanDefinitionFailureAnalyzer extends AbstractInjectionFailureAnalyzer<NoUniqueBeanDefinitionException> {

  private final ConfigurableBeanFactory beanFactory;

  public NoUniqueBeanDefinitionFailureAnalyzer(ConfigurableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  @Nullable
  protected FailureAnalysis analyze(Throwable rootFailure, NoUniqueBeanDefinitionException cause, @Nullable String description) {
    if (description == null) {
      return null;
    }
    String[] beanNames = extractBeanNames(cause);
    if (beanNames == null) {
      return null;
    }
    StringBuilder message = new StringBuilder();
    message.append(String.format("%s required a single bean, but %d were found:%n", description, beanNames.length));
    for (String beanName : beanNames) {
      buildMessage(message, beanName);
    }
    MissingParameterNamesFailureAnalyzer.appendPossibility(message);
    StringBuilder action = new StringBuilder(
            "Consider marking one of the beans as @Primary, updating the consumer to accept multiple beans, "
                    + "or using @Qualifier to identify the bean that should be consumed");
    action.append("%n%n%s".formatted(MissingParameterNamesFailureAnalyzer.ACTION));
    return new FailureAnalysis(message.toString(), action.toString(), cause);
  }

  private void buildMessage(StringBuilder message, String beanName) {
    try {
      BeanDefinition definition = this.beanFactory.getMergedBeanDefinition(beanName);
      message.append(getDefinitionDescription(beanName, definition));
    }
    catch (NoSuchBeanDefinitionException ex) {
      message.append(String.format("\t- %s: a programmatically registered singleton%n", beanName));
    }
  }

  private String getDefinitionDescription(String beanName, BeanDefinition definition) {
    if (StringUtils.hasText(definition.getFactoryMethodName())) {
      return String.format("\t- %s: defined by method '%s' in %s%n", beanName, definition.getFactoryMethodName(),
              getResourceDescription(definition));
    }
    return String.format("\t- %s: defined in %s%n", beanName, getResourceDescription(definition));
  }

  private String getResourceDescription(BeanDefinition definition) {
    String resourceDescription = definition.getResourceDescription();
    return (resourceDescription != null) ? resourceDescription : "unknown location";
  }

  private String @Nullable [] extractBeanNames(NoUniqueBeanDefinitionException cause) {
    String message = cause.getMessage();
    if (message != null && message.contains("but found")) {
      return StringUtils.commaDelimitedListToStringArray(
              message.substring(message.lastIndexOf(':') + 1).trim());
    }
    return null;
  }

}
