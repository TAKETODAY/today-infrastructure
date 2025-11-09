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

package infra.context.properties;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;
import infra.core.conversion.ConversionService;
import infra.core.conversion.support.DefaultConversionService;
import infra.format.support.ApplicationConversionService;
import infra.format.support.FormattingConversionService;

/**
 * Utility to deduce the {@link ConversionService} to use for configuration properties
 * binding.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Phillip Webb
 * @since 4.0
 */
class ConversionServiceDeducer {

  private final ApplicationContext context;

  ConversionServiceDeducer(ApplicationContext context) {
    this.context = context;
  }

  @Nullable
  List<ConversionService> getConversionServices() {
    if (hasUserDefinedConfigurationServiceBean()) {
      return Collections.singletonList(this.context
              .getBean(ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
    }
    if (this.context instanceof ConfigurableApplicationContext configurableContext) {
      return getConversionServices(configurableContext);
    }
    return null;
  }

  private List<ConversionService> getConversionServices(ConfigurableApplicationContext applicationContext) {
    List<ConversionService> conversionServices = new ArrayList<>();
    FormattingConversionService beansConverterService = new FormattingConversionService();
    Map<String, Object> converterBeans = addBeans(applicationContext, beansConverterService);
    if (!converterBeans.isEmpty()) {
      conversionServices.add(beansConverterService);
    }
    if (applicationContext.getBeanFactory().getConversionService() != null) {
      conversionServices.add(applicationContext.getBeanFactory().getConversionService());
    }
    if (!converterBeans.isEmpty()) {
      // Converters beans used to be added to a custom ApplicationConversionService
      // after the BeanFactory's ConversionService. For backwards compatibility, we
      // add an ApplicationConversationService as a fallback in the same place in
      // the list.
      conversionServices.add(ApplicationConversionService.getSharedInstance());
    }
    return conversionServices;
  }

  private Map<String, Object> addBeans(ConfigurableApplicationContext applicationContext,
          FormattingConversionService converterService) {
    DefaultConversionService.addCollectionConverters(converterService);
    converterService.addConverter(new ConfigurationPropertiesCharSequenceToObjectConverter(converterService));
    return ApplicationConversionService.addBeans(converterService, applicationContext.getBeanFactory(), ConfigurationPropertiesBinding.VALUE);
  }

  private boolean hasUserDefinedConfigurationServiceBean() {
    String beanName = ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME;
    return this.context.containsBean(beanName) && this.context.getAutowireCapableBeanFactory()
            .isTypeMatch(beanName, ConversionService.class);
  }

}
