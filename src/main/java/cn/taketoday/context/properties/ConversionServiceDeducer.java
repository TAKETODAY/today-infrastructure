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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.context.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.beans.factory.ListableBeanFactory;
import cn.taketoday.beans.factory.annotation.BeanFactoryAnnotationUtils;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.boot.convert.ApplicationConversionService;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.convert.ConversionService;
import cn.taketoday.core.convert.converter.Converter;
import cn.taketoday.core.convert.converter.GenericConverter;
import cn.taketoday.format.Formatter;
import cn.taketoday.format.FormatterRegistry;

/**
 * Utility to deduce the {@link ConversionService} to use for configuration properties
 * binding.
 *
 * @author Phillip Webb
 */
class ConversionServiceDeducer {

  private final ApplicationContext applicationContext;

  ConversionServiceDeducer(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  List<ConversionService> getConversionServices() {
    if (hasUserDefinedConfigurationServiceBean()) {
      return Collections.singletonList(this.applicationContext
              .getBean(ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
    }
    if (this.applicationContext instanceof ConfigurableApplicationContext) {
      return getConversionServices((ConfigurableApplicationContext) this.applicationContext);
    }
    return null;
  }

  private List<ConversionService> getConversionServices(ConfigurableApplicationContext applicationContext) {
    List<ConversionService> conversionServices = new ArrayList<>();
    if (applicationContext.getBeanFactory().getConversionService() != null) {
      conversionServices.add(applicationContext.getBeanFactory().getConversionService());
    }
    ConverterBeans converterBeans = new ConverterBeans(applicationContext);
    if (!converterBeans.isEmpty()) {
      ApplicationConversionService beansConverterService = new ApplicationConversionService();
      converterBeans.addTo(beansConverterService);
      conversionServices.add(beansConverterService);
    }
    return conversionServices;
  }

  private boolean hasUserDefinedConfigurationServiceBean() {
    String beanName = ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME;
    return this.applicationContext.containsBean(beanName) && this.applicationContext.getAutowireCapableBeanFactory()
            .isTypeMatch(beanName, ConversionService.class);
  }

  private static class ConverterBeans {

    @SuppressWarnings("rawtypes")
    private final List<Converter> converters;

    private final List<GenericConverter> genericConverters;

    @SuppressWarnings("rawtypes")
    private final List<Formatter> formatters;

    ConverterBeans(ConfigurableApplicationContext applicationContext) {
      ConfigurableBeanFactory beanFactory = applicationContext.getBeanFactory();
      this.converters = beans(Converter.class, ConfigurationPropertiesBinding.VALUE, beanFactory);
      this.genericConverters = beans(GenericConverter.class, ConfigurationPropertiesBinding.VALUE, beanFactory);
      this.formatters = beans(Formatter.class, ConfigurationPropertiesBinding.VALUE, beanFactory);
    }

    private <T> List<T> beans(Class<T> type, String qualifier, ListableBeanFactory beanFactory) {
      return new ArrayList<>(
              BeanFactoryAnnotationUtils.qualifiedBeansOfType(beanFactory, type, qualifier).values());
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
