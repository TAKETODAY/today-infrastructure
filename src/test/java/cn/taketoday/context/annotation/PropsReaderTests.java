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

import cn.taketoday.context.DefaultProps;
import cn.taketoday.context.Env;
import cn.taketoday.context.Props;
import cn.taketoday.context.Value;
import cn.taketoday.core.env.MapPropertyResolver;
import cn.taketoday.core.env.PropertiesPropertyResolver;
import cn.taketoday.core.io.PropertiesUtils;
import cn.taketoday.lang.Singleton;
import cn.taketoday.util.ClassUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/10/3 14:10
 */
class PropsReaderTests {

  @Getter
  @Setter
  @ToString
  public static class PropsReaderConfig {

    public PropsReaderConfig(
            @Props(prefix = "site.admin.") PropsReaderNested model, //
            @Props(prefix = "site.") Properties properties, //
            Properties emptyProperties, //
            @Env("placeHolder") int placeHolderEnv,
            @Value("#{placeHolder}") int placeHolder) {
      assert placeHolder == 12345;
      assert placeHolderEnv == 12345;
      System.err.println("model -> " + model);
      System.err.println(properties.getClass());
    }

    public PropsReaderConfig() { }

    @Props
    PropsReaderNested nested;

    private String cdn;
    private String icp;
    private String host;
    private File index;
    private File upload;
    private String keywords;
    private String siteName;
    private String copyright;
    private File serverPath;
    private String description;
  }

  @Getter
  @Setter
  @ToString
  @NoArgsConstructor
  public static class PropsReaderNested {

    private String userId;
    private String userName;
    private Integer age;
  }

  // -------------------------

  @Singleton
  public static class TestBean {

  }

  @Props(prefix = "site.")
  PropsReaderConfig test;

  PropsReaderConfig none;

  @Test
  void testResolveProps() throws NoSuchFieldException, SecurityException, IOException, NoSuchMethodException {
    Field declaredField = getClass().getDeclaredField("test");
    Props declaredAnnotation = declaredField.getDeclaredAnnotation(Props.class);

    URL resource = Objects.requireNonNull(ClassUtils.getDefaultClassLoader()).getResource("info.properties");
    Properties properties = PropertiesUtils.loadProperties(
            resource.getProtocol() + ":" + resource.getPath());
//    properties.list(System.err);

    PropsReader propsReader = new PropsReader(new PropertiesPropertyResolver(properties));

    PropsReaderConfig resolveProps = propsReader.read(declaredAnnotation, PropsReaderConfig.class);

//    System.err.println(resolveProps);

    assert "TODAY BLOG".equals(resolveProps.getDescription());
    assert "https://cdn.taketoday.cn".equals(resolveProps.getCdn());

    assert 21 == resolveProps.getNested().getAge();
    assert "666".equals(resolveProps.getNested().getUserId());
    assert "TODAY".equals(resolveProps.getNested().getUserName());

    assert propsReader.read(getClass().getDeclaredField("none")).equals(Collections.emptyList());

    propsReader.read(getClass().getMethod("testResolveProps"));
  }


  //

  @Test
  void systemProperties() {
    String value = "best programming language in the world";
    System.setProperty("java", value);

    PropsReader propsReader = new PropsReader(); // use default systemProperties
    DefaultProps defaultProps = new DefaultProps();
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

}
