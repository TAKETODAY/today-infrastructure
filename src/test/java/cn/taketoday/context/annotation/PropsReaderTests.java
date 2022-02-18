/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.env.MapPropertyResolver;
import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.lang.Singleton;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author TODAY 2021/10/3 14:10
 */
class PropsReaderTests {

  @Test
  void illegalArgumentException() {
    assertThatThrownBy(() -> new PropsReader((PropertyResolver) null)).hasMessage("PropertyResolver must not be null");
    assertThatThrownBy(() -> new PropsReader((ApplicationContext) null)).hasMessage("ApplicationContext must not be null");
    assertThrows(IllegalArgumentException.class, () -> new PropsReader((PropertyResolver) null));
    assertThrows(IllegalArgumentException.class, () -> new PropsReader((ApplicationContext) null));

    PropsReader propsReader = new PropsReader();
//    assertThatThrownBy(() -> propsReader.read((AnnotatedElement)null)).hasMessage("AnnotatedElement must not be null");
  }

  @Data
  public static class PropsReaderConfig {
    @Props
    PropsReaderNested nested;

    private String cdn;
    private String description;
  }

  @Data
  public static class PropsReaderNested {

    private String userId;
    private String userName;
    private Integer age;
  }

  // -------------------------

  @Singleton
  static class TestBean {

  }

  @Props(prefix = "test.")
  PropsReaderConfig test;

  PropsReaderConfig none;

  @Test
  void readClassAsBean() throws Exception {
    HashMap<String, Object> keyValues = new HashMap<>();
    MapPropertyResolver propertyResolver = new MapPropertyResolver(keyValues);

    PropsReader propsReader = new PropsReader(propertyResolver);
    propsReader.setBeanFactory(new StandardBeanFactory());

    Field declaredField = getClass().getDeclaredField("test");
    Props declaredAnnotation = declaredField.getDeclaredAnnotation(Props.class);

    keyValues.put("test.description", "TODAY BLOG");
    keyValues.put("test.cdn", "https://cdn.taketoday.cn");
    keyValues.put("test.nested.age", "23");
    keyValues.put("test.nested.userId", "666");
    keyValues.put("test.nested.userName", "TODAY");

    PropsReaderConfig bean = propsReader.read(declaredAnnotation, PropsReaderConfig.class);
    assertThat(bean).isNotNull();
    assertThat(bean.description).isEqualTo("TODAY BLOG");
    assertThat(bean.cdn).isEqualTo("https://cdn.taketoday.cn");

    assertThat(bean.nested).isNotNull();
    assertThat(bean.nested.age).isEqualTo(23);
    assertThat(bean.nested.userId).isEqualTo("666");
    assertThat(bean.nested.userName).isEqualTo("TODAY");

//    List<DependencySetter> none = propsReader.read(getClass().getDeclaredField("none"));
//    assertThat(none).isNotNull().isEmpty();
  }

  @Test
  void readClassAsBeanPlaceholder() throws Exception {
    HashMap<String, Object> keyValues = new HashMap<>();
    MapPropertyResolver propertyResolver = new MapPropertyResolver(keyValues);

    PropsReader propsReader = new PropsReader(propertyResolver);
    propsReader.setBeanFactory(new StandardBeanFactory());

    Field declaredField = getClass().getDeclaredField("test");
    Props declaredAnnotation = declaredField.getDeclaredAnnotation(Props.class);

    keyValues.put("test.description", "TODAY BLOG #{1+1}");
    keyValues.put("test.cdn", "${test.cdn.placeholder}");
    keyValues.put("test.cdn.placeholder", "https://cdn.taketoday.cn");
    keyValues.put("test.nested.age", "${age:30}");
    keyValues.put("age", "23");
    keyValues.put("test.nested.userId", "666");
    keyValues.put("test.nested.userName", "${name:lost-TODAY}");

    PropsReaderConfig bean = propsReader.read(declaredAnnotation, PropsReaderConfig.class);
    assertThat(bean).isNotNull();
    assertThat(bean.description).isEqualTo("TODAY BLOG 2");
    assertThat(bean.cdn).isEqualTo("https://cdn.taketoday.cn");

    assertThat(bean.nested).isNotNull();
    assertThat(bean.nested.age).isEqualTo(23);
    assertThat(bean.nested.userId).isEqualTo("666");
    assertThat(bean.nested.userName).isEqualTo("lost-TODAY");

  }

  //

  @Test
  void systemProperties() {
    String value = "best programming language in the world";
    System.setProperty("java", value);

    PropsReader propsReader = new PropsReader(); // use default systemProperties
    DefaultProps defaultProps = new DefaultProps();
    propsReader.setBeanFactory(new StandardBeanFactory());

    PropsReaderBean read = propsReader.read(defaultProps, PropsReaderBean.class);
    assertThat(read.java).isEqualTo(value);
  }

  @Data
  static class PropsReaderBean {
    String java;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  static class PrefixPropsReaderBean extends PropsReaderBean {
  }

  @Test
  void read() {
    String value = "best programming language in the world";
    HashMap<String, Object> keyValues = new HashMap<>();
    keyValues.put("java", value);
    MapPropertyResolver propertyResolver = new MapPropertyResolver(keyValues);

    PropsReader propsReader = new PropsReader(propertyResolver);
    propsReader.setBeanFactory(new StandardBeanFactory());
    DefaultProps defaultProps = new DefaultProps();
    PropsReaderBean read = propsReader.read(defaultProps, PropsReaderBean.class);

    assertThat(read.java).isEqualTo(value);
    keyValues.clear();

    // PrefixPropsReaderBean

    DefaultProps prefix = new DefaultProps();
    prefix.setPrefix("prefix.");

    keyValues.put("prefix.java", value);

    PrefixPropsReaderBean prefixBean = propsReader.read(prefix, PrefixPropsReaderBean.class);
    assertThat(prefixBean.java).isEqualTo(value);

  }

  @Setter
  @Getter
  static class TypeConversionBean {
    String java;
    int intValue;
    float floatValue;
    byte byteValue;
    boolean booleanValue;
  }

  @Test
  void typeConversion() {
    String value = "best programming language in the world";
    HashMap<String, Object> keyValues = new HashMap<>();
    keyValues.put("java", value);
    keyValues.put("intValue", 11);
    keyValues.put("floatValue", 11.11f);
    keyValues.put("byteValue", 1);
    keyValues.put("booleanValue", 1);

    MapPropertyResolver propertyResolver = new MapPropertyResolver(keyValues);
    PropsReader propsReader = new PropsReader(propertyResolver);
    propsReader.setBeanFactory(new StandardBeanFactory());

    TypeConversionBean prefixBean = propsReader.read(new DefaultProps(), TypeConversionBean.class);
    assertThat(prefixBean.java).isEqualTo(value);
    assertThat(prefixBean.intValue).isEqualTo(11);
    assertThat(prefixBean.floatValue).isEqualTo(11.11f);
    assertThat(prefixBean.byteValue).isEqualTo((byte) 1);
    assertThat(prefixBean.booleanValue).isTrue();

    keyValues.put("booleanValue", "false");
    assertThat(propsReader.read(new DefaultProps(), TypeConversionBean.class).booleanValue).isFalse();

    keyValues.put("booleanValue", "true");
    assertThat(propsReader.read(new DefaultProps(), TypeConversionBean.class).booleanValue).isTrue();

    keyValues.put("booleanValue", "1");
    assertThat(propsReader.read(new DefaultProps(), TypeConversionBean.class).booleanValue).isTrue();

    keyValues.put("booleanValue", "yes");
    assertThat(propsReader.read(new DefaultProps(), TypeConversionBean.class).booleanValue).isTrue();

    keyValues.put("booleanValue", "no");
    assertThat(propsReader.read(new DefaultProps(), TypeConversionBean.class).booleanValue).isFalse();

    keyValues.put("booleanValue", "0");
    assertThat(propsReader.read(new DefaultProps(), TypeConversionBean.class).booleanValue).isFalse();

  }
  //

}
