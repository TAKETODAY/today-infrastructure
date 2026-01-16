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

package infra.validation.config;

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
public final class ValidationAutoConfiguration {

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
          ObjectProvider<Validator> validator, Collection<MethodValidationExcludeFilter> excludeFilters) {

    var processor = new FilteredMethodValidationPostProcessor(excludeFilters);
    boolean proxyTargetClass = environment.getFlag("infra.aop.proxy-target-class", true);
    boolean adaptConstraintViolations = environment.getFlag(
            "infra.validation.method.adapt-constraint-violations", false);

    processor.setProxyTargetClass(proxyTargetClass);
    processor.setValidatorProvider(validator);
    processor.setAdaptConstraintViolations(adaptConstraintViolations);
    return processor;
  }

}
