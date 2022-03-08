/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.diagnostics.analyzer;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.NoUniqueBeanDefinitionException;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.framework.diagnostics.FailureAnalysis;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * An {@link AbstractInjectionFailureAnalyzer} that performs analysis of failures caused
 * by a {@link NoUniqueBeanDefinitionException}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class NoUniqueBeanDefinitionFailureAnalyzer extends AbstractInjectionFailureAnalyzer<NoUniqueBeanDefinitionException>
        implements BeanFactoryAware {

  private ConfigurableBeanFactory beanFactory;

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    Assert.isInstanceOf(ConfigurableBeanFactory.class, beanFactory);
    this.beanFactory = (ConfigurableBeanFactory) beanFactory;
  }

  @Override
  @Nullable
  protected FailureAnalysis analyze(
          Throwable rootFailure, NoUniqueBeanDefinitionException cause, @Nullable String description) {
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
    return new FailureAnalysis(message.toString(),
            "Consider marking one of the beans as @Primary, updating the consumer to"
                    + " accept multiple beans, or using @Qualifier to identify the"
                    + " bean that should be consumed", cause);
  }

  private void buildMessage(StringBuilder message, String beanName) {
    try {
      BeanDefinition definition = BeanFactoryUtils.requiredDefinition(beanFactory, beanName);
      message.append(getDefinitionDescription(beanName, definition));
    }
    catch (NoSuchBeanDefinitionException ex) {
      message.append(String.format("\t- %s: a programmatically registered singleton", beanName));
    }
  }

  private String getDefinitionDescription(String beanName, BeanDefinition definition) {
    if (StringUtils.hasText(definition.getFactoryMethodName())) {
      return String.format("\t- %s: defined by method '%s' in %s%n", beanName, definition.getFactoryMethodName(),
              definition.getResourceDescription());
    }
    return String.format("\t- %s: defined in %s%n", beanName, definition.getResourceDescription());
  }

  @Nullable
  private String[] extractBeanNames(NoUniqueBeanDefinitionException cause) {
    if (cause.getMessage().contains("but found")) {
      return StringUtils.commaDelimitedListToStringArray(
              cause.getMessage().substring(cause.getMessage().lastIndexOf(':') + 1).trim());
    }
    return null;
  }

}
