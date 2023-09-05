/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.annotation.config.validation;

import java.util.Collection;

import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.context.annotation.config.DisableDIAutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnResource;
import cn.taketoday.context.condition.SearchStrategy;
import cn.taketoday.core.env.Environment;
import cn.taketoday.stereotype.Component;
import cn.taketoday.validation.MessageInterpolatorFactory;
import cn.taketoday.validation.beanvalidation.FilteredMethodValidationPostProcessor;
import cn.taketoday.validation.beanvalidation.LocalValidatorFactoryBean;
import cn.taketoday.validation.beanvalidation.MethodValidationExcludeFilter;
import cn.taketoday.validation.beanvalidation.MethodValidationPostProcessor;
import cn.taketoday.validation.beanvalidation.SuppliedValidator;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to configure the validation
 * infrastructure.
 *
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@DisableDIAutoConfiguration
@ConditionalOnClass(ExecutableValidator.class)
@ConditionalOnResource("classpath:META-INF/services/jakarta.validation.spi.ValidationProvider")
@Import(PrimaryDefaultValidatorPostProcessor.class)
public class ValidationAutoConfiguration {

  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(Validator.class)
  public static LocalValidatorFactoryBean defaultValidator(
          ApplicationContext applicationContext,
          ObjectProvider<ValidationConfigurationCustomizer> customizers) {

    LocalValidatorFactoryBean factoryBean = new LocalValidatorFactoryBean();

    factoryBean.setConfigurationInitializer(configuration -> {
      for (ValidationConfigurationCustomizer customizer : customizers) {
        customizer.customize(configuration);
      }
    });

    factoryBean.setMessageInterpolator(new MessageInterpolatorFactory(applicationContext).get());
    return factoryBean;
  }

  @Component
  @ConditionalOnMissingBean(search = SearchStrategy.CURRENT)
  public static MethodValidationPostProcessor methodValidationPostProcessor(
          Environment environment, ObjectProvider<Validator> validator,
          Collection<MethodValidationExcludeFilter> excludeFilters) {

    var processor = new FilteredMethodValidationPostProcessor(excludeFilters);
    boolean proxyTargetClass = environment.getFlag("infra.aop.proxy-target-class", true);
    processor.setProxyTargetClass(proxyTargetClass);
    processor.setValidator(new SuppliedValidator(validator));
    return processor;
  }

}
