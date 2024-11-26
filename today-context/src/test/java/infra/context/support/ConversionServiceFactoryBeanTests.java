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

package infra.context.support;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import infra.context.ConfigurableApplicationContext;
import infra.core.TypeDescriptor;
import infra.core.conversion.ConversionService;
import infra.core.conversion.Converter;
import infra.core.conversion.ConverterFactory;
import infra.core.conversion.GenericConverter;
import infra.core.io.ClassPathResource;
import infra.core.io.FileSystemResource;
import infra.lang.Nullable;
import infra.tests.sample.beans.ResourceTestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 */
public class ConversionServiceFactoryBeanTests {

  @Test
  void createDefaultConversionService() {
    ConversionServiceFactoryBean factory = new ConversionServiceFactoryBean();
    factory.afterPropertiesSet();
    ConversionService service = factory.getObject();
    assertThat(service.canConvert(String.class, Integer.class)).isTrue();
  }

  @Test
  void createDefaultConversionServiceWithSupplements() {
    ConversionServiceFactoryBean factory = new ConversionServiceFactoryBean();
    Set<Object> converters = new HashSet<>();
    // The following String -> Foo Converter cannot be implemented as a lambda
    // due to type erasure of the source and target types.
    converters.add(new Converter<String, Foo>() {
      @Override
      public Foo convert(String source) {
        return new Foo();
      }
    });
    converters.add(new ConverterFactory<String, Bar>() {
      @Override
      public <T extends Bar> Converter<String, T> getConverter(Class<T> targetType) {
        return new Converter<>() {
          @SuppressWarnings("unchecked")
          @Override
          public T convert(String source) {
            return (T) new Bar();
          }
        };
      }
    });
    converters.add(new GenericConverter() {
      @Override
      public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(String.class, Baz.class));
      }

      @Override
      @Nullable
      public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        return new Baz();
      }
    });
    factory.setConverters(converters);
    factory.afterPropertiesSet();
    ConversionService service = factory.getObject();
    assertThat(service.canConvert(String.class, Integer.class)).isTrue();
    assertThat(service.canConvert(String.class, Foo.class)).isTrue();
    assertThat(service.canConvert(String.class, Bar.class)).isTrue();
    assertThat(service.canConvert(String.class, Baz.class)).isTrue();
  }

  @Test
  void createDefaultConversionServiceWithInvalidSupplements() {
    ConversionServiceFactoryBean factory = new ConversionServiceFactoryBean();
    Set<Object> converters = new HashSet<>();
    converters.add("bogus");
    factory.setConverters(converters);
    assertThatIllegalArgumentException().isThrownBy(factory::afterPropertiesSet);
  }

  @Test
  void conversionServiceInApplicationContext() {
    doTestConversionServiceInApplicationContext("conversionService.xml", ClassPathResource.class);
  }

  @Test
  void conversionServiceInApplicationContextWithResourceOverriding() {
    doTestConversionServiceInApplicationContext("conversionServiceWithResourceOverriding.xml", FileSystemResource.class);
  }

  private void doTestConversionServiceInApplicationContext(String fileName, Class<?> resourceClass) {
    ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(fileName, getClass());
    ResourceTestBean tb = ctx.getBean("resourceTestBean", ResourceTestBean.class);
    assertThat(tb.getResource()).isInstanceOf(resourceClass);
    assertThat(tb.getResourceArray()).hasSize(1);
    assertThat(tb.getResourceArray()[0]).isInstanceOf(resourceClass);
    assertThat(tb.getResourceMap()).hasSize(1);
    assertThat(tb.getResourceMap().get("key1")).isInstanceOf(resourceClass);
    assertThat(tb.getResourceArrayMap()).hasSize(1);
    assertThat(tb.getResourceArrayMap().get("key1")).isNotEmpty();
    assertThat(tb.getResourceArrayMap().get("key1")[0]).isInstanceOf(resourceClass);
    ctx.close();
  }

  static class Foo {
  }

  static class Bar {
  }

  static class Baz {
  }

  static class ComplexConstructorArgument {

    ComplexConstructorArgument(Map<String, Class<?>> map) {
      assertThat(map.isEmpty()).isFalse();
      assertThat(map.keySet().iterator().next()).isInstanceOf(String.class);
      assertThat(map.values().iterator().next()).isInstanceOf(Class.class);
    }
  }

}
