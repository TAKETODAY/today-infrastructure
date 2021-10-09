/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import cn.taketoday.core.io.PropertiesUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collections;
import java.util.Objects;
import java.util.Properties;

import cn.taketoday.context.ContextUtils;
import cn.taketoday.context.Env;
import cn.taketoday.context.Props;
import cn.taketoday.context.Value;
import cn.taketoday.core.env.PropertiesPropertyResolver;
import cn.taketoday.util.ClassUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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

}
