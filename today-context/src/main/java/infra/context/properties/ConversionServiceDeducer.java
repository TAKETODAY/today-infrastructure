/*
 * Copyright 2017 - 2024 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.annotation.BeanFactoryAnnotationUtils;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;
import infra.core.conversion.ConversionService;
import infra.core.conversion.Converter;
import infra.core.conversion.GenericConverter;
import infra.core.conversion.support.DefaultConversionService;
import infra.format.Formatter;
import infra.format.FormatterRegistry;
import infra.format.support.ApplicationConversionService;
import infra.format.support.FormattingConversionService;
import infra.lang.Nullable;

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
      return Collections.singletonList(
              context.getBean(ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
    }
    if (context instanceof ConfigurableApplicationContext configurableContext) {
      return getConversionServices(configurableContext);
    }
    return null;
  }

  private List<ConversionService> getConversionServices(ConfigurableApplicationContext applicationContext) {
    List<ConversionService> conversionServices = new ArrayList<>();
    ConverterBeans converterBeans = new ConverterBeans(applicationContext);
    if (!converterBeans.isEmpty()) {
      FormattingConversionService beansConverterService = new FormattingConversionService();
      DefaultConversionService.addCollectionConverters(beansConverterService);
      converterBeans.addTo(beansConverterService);
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

  private boolean hasUserDefinedConfigurationServiceBean() {
    String beanName = ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME;
    return context.containsBean(beanName)
            && context.getAutowireCapableBeanFactory().isTypeMatch(beanName, ConversionService.class);
  }

  private static class ConverterBeans {

    @SuppressWarnings("rawtypes")
    private final List<Converter> converters;

    private final List<GenericConverter> genericConverters;

    @SuppressWarnings("rawtypes")
    private final List<Formatter> formatters;

    ConverterBeans(ConfigurableApplicationContext applicationContext) {
      ConfigurableBeanFactory beanFactory = applicationContext.getBeanFactory();
      this.converters = beans(Converter.class, beanFactory);
      this.genericConverters = beans(GenericConverter.class, beanFactory);
      this.formatters = beans(Formatter.class, beanFactory);
    }

    private <T> List<T> beans(Class<T> type, BeanFactory beanFactory) {
      return new ArrayList<>(BeanFactoryAnnotationUtils.qualifiedBeansOfType(
              beanFactory, type, ConfigurationPropertiesBinding.VALUE).values());
    }

    boolean isEmpty() {
      return this.converters.isEmpty() && this.genericConverters.isEmpty() && this.formatters.isEmpty();
    }

    void addTo(FormatterRegistry registry) {
      for (Converter<?, ?> converter : this.converters) {
        registry.addConverter(converter);
      }
      for (GenericConverter genericConverter : this.genericConverters) {
        registry.addConverter(genericConverter);
      }
      for (Formatter<?> formatter : this.formatters) {
        registry.addFormatter(formatter);
      }
    }

  }

}
