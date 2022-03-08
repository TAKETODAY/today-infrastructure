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

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.annotation.BeanFactoryAnnotationUtils;
import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.conversion.GenericConverter;
import cn.taketoday.format.Formatter;
import cn.taketoday.format.FormatterRegistry;
import cn.taketoday.format.support.ApplicationConversionService;
import cn.taketoday.lang.Nullable;

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

  ConversionServiceDeducer(ApplicationContext applicationContext) {
    this.context = applicationContext;
  }

  @Nullable
  List<ConversionService> getConversionServices() {
    if (hasUserDefinedConfigurationServiceBean()) {
      return Collections.singletonList(
              context.getBean(ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
    }
    if (this.context instanceof ConfigurableApplicationContext) {
      return getConversionServices((ConfigurableApplicationContext) this.context);
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
    return this.context.containsBean(beanName)
            && this.context.getAutowireCapableBeanFactory().isTypeMatch(beanName, ConversionService.class);
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
