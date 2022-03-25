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

package cn.taketoday.context.support;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.conversion.ConverterFactory;
import cn.taketoday.core.conversion.GenericConverter;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.FileSystemResource;
import cn.taketoday.lang.Nullable;
import cn.taketoday.tests.sample.beans.ResourceTestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 */
public class ConversionServiceFactoryBeanTests {

  @Test
  public void createDefaultConversionService() {
    ConversionServiceFactoryBean factory = new ConversionServiceFactoryBean();
    factory.afterPropertiesSet();
    ConversionService service = factory.getObject();
    assertThat(service.canConvert(String.class, Integer.class)).isTrue();
  }

  @Test
  public void createDefaultConversionServiceWithSupplements() {
    ConversionServiceFactoryBean factory = new ConversionServiceFactoryBean();
    Set<Object> converters = new HashSet<>();
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
  public void createDefaultConversionServiceWithInvalidSupplements() {
    ConversionServiceFactoryBean factory = new ConversionServiceFactoryBean();
    Set<Object> converters = new HashSet<>();
    converters.add("bogus");
    factory.setConverters(converters);
    assertThatIllegalArgumentException().isThrownBy(factory::afterPropertiesSet);
  }

  @Test
  public void conversionServiceInApplicationContext() {
    doTestConversionServiceInApplicationContext("conversionService.xml", ClassPathResource.class);
  }

  @Test
  public void conversionServiceInApplicationContextWithResourceOverriding() {
    doTestConversionServiceInApplicationContext("conversionServiceWithResourceOverriding.xml", FileSystemResource.class);
  }

  private void doTestConversionServiceInApplicationContext(String fileName, Class<?> resourceClass) {
    ApplicationContext ctx = new ClassPathXmlApplicationContext(fileName, getClass());
    ResourceTestBean tb = ctx.getBean("resourceTestBean", ResourceTestBean.class);
    assertThat(resourceClass.isInstance(tb.getResource())).isTrue();
    assertThat(tb.getResourceArray().length > 0).isTrue();
    assertThat(resourceClass.isInstance(tb.getResourceArray()[0])).isTrue();
    assertThat(tb.getResourceMap().size() == 1).isTrue();
    assertThat(resourceClass.isInstance(tb.getResourceMap().get("key1"))).isTrue();
    assertThat(tb.getResourceArrayMap().size() == 1).isTrue();
    assertThat(tb.getResourceArrayMap().get("key1").length > 0).isTrue();
    assertThat(resourceClass.isInstance(tb.getResourceArrayMap().get("key1")[0])).isTrue();
  }

  public static class Foo {
  }

  public static class Bar {
  }

  public static class Baz {
  }

  public static class ComplexConstructorArgument {

    public ComplexConstructorArgument(Map<String, Class<?>> map) {
      assertThat(!map.isEmpty()).isTrue();
      assertThat(map.keySet().iterator().next()).isInstanceOf(String.class);
      assertThat(map.values().iterator().next()).isInstanceOf(Class.class);
    }
  }

}
