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

package cn.taketoday.context.properties;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.format.support.ApplicationConversionService;
import cn.taketoday.format.support.FormattingConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConversionServiceDeducer}.
 *
 * @author Phillip Webb
 */
class ConversionServiceDeducerTests {

  @Test
  void getConversionServicesWhenHasConversionServiceBeanContainsOnlyBean() {
    ApplicationContext applicationContext = new AnnotationConfigApplicationContext(
            CustomConverterServiceConfiguration.class);
    ConversionServiceDeducer deducer = new ConversionServiceDeducer(applicationContext);
    TestApplicationConversionService expected = applicationContext.getBean(TestApplicationConversionService.class);
    assertThat(deducer.getConversionServices()).containsExactly(expected);
  }

  @Test
  void getConversionServiceWhenHasNoConversionServiceBeanAndNoQualifiedBeansAndNoBeanFactoryConversionServiceReturnsEmptyList() {
    ApplicationContext applicationContext = new AnnotationConfigApplicationContext(EmptyConfiguration.class);
    ConversionServiceDeducer deducer = new ConversionServiceDeducer(applicationContext);
    assertThat(deducer.getConversionServices()).isEmpty();
  }

  @Test
  void getConversionServiceWhenHasNoConversionServiceBeanAndNoQualifiedBeansAndBeanFactoryConversionServiceContainsOnlyBeanFactoryInstance() {
    ConfigurableApplicationContext applicationContext = new AnnotationConfigApplicationContext(
            EmptyConfiguration.class);
    ConversionService conversionService = new ApplicationConversionService();
    applicationContext.getBeanFactory().setConversionService(conversionService);
    ConversionServiceDeducer deducer = new ConversionServiceDeducer(applicationContext);
    List<ConversionService> conversionServices = deducer.getConversionServices();
    assertThat(conversionServices).containsOnly(conversionService);
    assertThat(conversionServices.get(0)).isSameAs(conversionService);
  }

  @Test
  void getConversionServiceWhenHasQualifiedConverterBeansContainsCustomizedFormattingService() {
    ApplicationContext applicationContext = new AnnotationConfigApplicationContext(
            CustomConverterConfiguration.class);
    ConversionServiceDeducer deducer = new ConversionServiceDeducer(applicationContext);
    List<ConversionService> conversionServices = deducer.getConversionServices();
    assertThat(conversionServices).hasSize(2);
    assertThat(conversionServices.get(0)).isExactlyInstanceOf(FormattingConversionService.class);
    assertThat(conversionServices.get(0).canConvert(InputStream.class, OutputStream.class)).isTrue();
    assertThat(conversionServices.get(1)).isSameAs(ApplicationConversionService.getSharedInstance());
  }

  @Configuration(proxyBeanMethods = false)
  static class CustomConverterServiceConfiguration {

    @Bean(ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME)
    TestApplicationConversionService conversionService() {
      return new TestApplicationConversionService();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class EmptyConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomConverterConfiguration {

    @Bean
    @ConfigurationPropertiesBinding
    TestConverter testConverter() {
      return new TestConverter();
    }

  }

  private static class TestApplicationConversionService extends ApplicationConversionService {

  }

  private static class TestConverter implements Converter<InputStream, OutputStream> {

    @Override
    public OutputStream convert(InputStream source) {
      throw new UnsupportedOperationException();
    }

  }

}
