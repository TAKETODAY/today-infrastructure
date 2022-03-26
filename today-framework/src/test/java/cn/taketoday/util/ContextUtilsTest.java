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
package cn.taketoday.util;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.beans.factory.support.DependencyInjector;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.PropertiesPropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.io.PropertiesUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Today <br>
 * 2018-07-12 20:46:41
 */
public class ContextUtilsTest {

  @Test
  public void test_GetResourceAsProperties() throws IOException {
    Properties resourceAsProperties = PropertiesUtils.loadProperties("info.properties");
    assert "TODAY BLOG".equals(resourceAsProperties.getProperty("site.name"));
  }

  @Test
  public void test_GetUrlAsStream() throws IOException {
    URL resource = ClassUtils.getDefaultClassLoader().getResource("info.properties");
    InputStream urlAsStream = ResourceUtils.getResourceAsStream(resource.getProtocol() + ":" + resource.getPath());

    assert resource.getProtocol().equals("file");
    assert urlAsStream != null;
  }

  @Test
  public void test_GetUrlAsProperties() throws IOException {
    URL resource = ClassUtils.getDefaultClassLoader().getResource("info.properties");
    Properties properties = PropertiesUtils.loadProperties(resource.getProtocol() + ":" + resource.getPath());

    assert resource.getProtocol().equals("file");
    assert "TODAY BLOG".equals(properties.getProperty("site.name"));
  }

  @Test
  @Disabled
  @SuppressWarnings("unchecked")
  public void testResolveParameter() throws Exception {

    try (StandardApplicationContext applicationContext = new StandardApplicationContext("cn.taketoday.util")) {

      final BeanFactory beanFactory = applicationContext.getBeanFactory();
      final ConfigurableEnvironment environment = applicationContext.getEnvironment();
      // placeHolder

      PropertySources propertySources = environment.getPropertySources();
      Properties properties = new Properties();
      propertySources.addLast(new PropertiesPropertySource(
              "properties", properties
      ));

      properties.setProperty("placeHolder", "12345");

      final Constructor<Config>[] declaredConstructors = (Constructor<Config>[]) Config.class.getDeclaredConstructors();
      Constructor<Config> constructor = null;
      for (final Constructor<Config> declaredConstructor : declaredConstructors) {
        if (declaredConstructor.getParameterCount() > 0) {
          constructor = declaredConstructor;
        }
      }

//      properties.list(System.err);
//      System.err.println(properties.get("placeHolder"));

      Object[] parameters = new DependencyInjector(beanFactory).resolveArguments(constructor);

      Config newInstance = constructor.newInstance(parameters);

      assert parameters.length == 14;

      assert parameters[0] instanceof UserModel;

      UserModel userModel = (UserModel) parameters[0];
      assert userModel.getAge() == 21;
      assert userModel.getUserId().equals("666");
      assert userModel.getUserName().equals("TODAY");

      assert parameters[1] instanceof Properties;

      // collection > 5
      final List<Object> objectList = applicationContext.getBeans(Object.class);
      final int allSize = objectList.size();

      final Object collectionParameter = parameters[5];
      assert collectionParameter instanceof Collection;
      final int size = ((Collection<?>) collectionParameter).size();
      assert size == allSize;

      final Object setParameter = parameters[6];
      assert setParameter instanceof Set;
      assert ((Set<?>) setParameter).size() == allSize;

      final Object hashSetParameter = parameters[7];
      assert hashSetParameter instanceof HashSet;
      assert ((Set<?>) hashSetParameter).size() == allSize;

      final Object listParameter = parameters[8];
      assert listParameter instanceof List;
      assert ((List<?>) listParameter).size() == allSize;

      final Object arrayListParameter = parameters[9];
      assert arrayListParameter instanceof ArrayList;
      assert ((ArrayList<?>) arrayListParameter).size() == allSize;

      final Object mapParameter = parameters[10];
      assert mapParameter instanceof Map;
      assert ((Map) mapParameter).size() == allSize;

      final Object hashMapParameter = parameters[11];
      assert hashMapParameter instanceof HashMap;
      assert ((HashMap) hashMapParameter).size() == allSize;

    }
  }

  @Getter
  @Setter
  @ToString
  public static class Config {

    public Config(
            UserModel model, //
            Properties properties, //
            Properties emptyProperties, //
            @Value("${placeHolder}") int placeHolderEnv,
            @Value("${placeHolder}") int placeHolder,

            Collection<Object> objects,
            Set<Object> setObjects,
            HashSet<Object> hashSetObjects,
            List<Object> listObjects,
            ArrayList<Object> arrayListObjects,
            Map<String, Object> mapObjects,
            HashMap<String, Object> hashMapObjects,
            Object[] array,
            UserModel[] none

    ) {
      assert placeHolder == 12345;
      assert placeHolderEnv == 12345;
      System.err.println("model -> " + model);
      System.err.println(properties.getClass());
    }

    public Config() { }

    UserModel admin;

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
  public static class UserModel {

    private String userId;
    private String userName;
    private Integer age;
  }

  // -------------------------

}
