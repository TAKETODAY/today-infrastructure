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

package infra.annotation.config.validation;

import java.util.Collection;

import infra.beans.factory.ObjectProvider;
import infra.beans.factory.config.BeanDefinition;
import infra.context.ApplicationContext;
import infra.context.annotation.Import;
import infra.context.annotation.Role;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnResource;
import infra.context.condition.SearchStrategy;
import infra.core.env.Environment;
import infra.stereotype.Component;
import infra.validation.MessageInterpolatorFactory;
import infra.validation.beanvalidation.FilteredMethodValidationPostProcessor;
import infra.validation.beanvalidation.LocalValidatorFactoryBean;
import infra.validation.beanvalidation.MethodValidationExcludeFilter;
import infra.validation.beanvalidation.MethodValidationPostProcessor;
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

  private ValidationAutoConfiguration() {
  }

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
  public static MethodValidationPostProcessor methodValidationPostProcessor(Environment environment,
          ObjectProvider<Validator> validator, ValidationProperties validationProperties,
          Collection<MethodValidationExcludeFilter> excludeFilters) {

    var processor = new FilteredMethodValidationPostProcessor(excludeFilters);
    boolean proxyTargetClass = environment.getFlag("infra.aop.proxy-target-class", true);
    processor.setProxyTargetClass(proxyTargetClass);
    processor.setValidatorProvider(validator);
    processor.setAdaptConstraintViolations(validationProperties.getMethod().isAdaptConstraintViolations());
    return processor;
  }

}
