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
package cn.taketoday.util;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.beans.support.ArgumentsResolver;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ContextUtils;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.Env;
import cn.taketoday.context.Props;
import cn.taketoday.beans.Singleton;
import cn.taketoday.context.Value;
import cn.taketoday.context.Environment;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.StandardBeanDefinition;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import static org.assertj.core.api.Assertions.fail;

/**
 * @author Today <br>
 * 2018-07-12 20:46:41
 */
public class ContextUtilsTest {

  @Test
  public void test_FindInProperties() throws ConfigurationException {

    try (ApplicationContext applicationContext = new StandardApplicationContext("", "cn.taketoday.util")) {

      Properties properties = applicationContext.getEnvironment().getProperties();

      properties.setProperty("Name", "#{siteName}");
      properties.setProperty("siteName", "#{site.name}");

      String name = ContextUtils.resolvePlaceholder(properties, "/#{Name}\\");
      String findInProperties = ContextUtils.resolvePlaceholder(properties, "/#{site.name}\\");
      String findInProperties_ = ContextUtils.resolvePlaceholder(properties, "/TODAY BLOG\\");

      assert findInProperties.equals(findInProperties_);
      assert findInProperties.equals(name);
      assert name.equals(findInProperties_);

      System.err.println(name);
      System.out.println(findInProperties);
      System.out.println(findInProperties_);
    }
  }

  @Test
  public void test_GetResourceAsStream() throws IOException {
    InputStream resourceAsStream = ContextUtils.getResourceAsStream("info.properties");

    assert resourceAsStream != null;
  }

  @Test
  public void test_GetResourceAsProperties() throws IOException {
    Properties resourceAsProperties = ContextUtils.getResourceAsProperties("info.properties");
    assert "TODAY BLOG".equals(resourceAsProperties.getProperty("site.name"));
  }

  @Test
  public void test_GetUrlAsStream() throws IOException {
    URL resource = ClassUtils.getClassLoader().getResource("info.properties");

    InputStream urlAsStream = ContextUtils.getUrlAsStream(resource.getProtocol() + ":" + resource.getPath());

    assert resource.getProtocol().equals("file");
    assert urlAsStream != null;
  }

  @Test
  public void test_GetUrlAsProperties() throws IOException {
    URL resource = ClassUtils.getClassLoader().getResource("info.properties");
    Properties properties = ContextUtils.getUrlAsProperties(resource.getProtocol() + ":" + resource.getPath());

    assert resource.getProtocol().equals("file");
    assert "TODAY BLOG".equals(properties.getProperty("site.name"));
  }

  @Props(prefix = "site.")
  Config test;

  Config none;

  @Test
  @Props
  public void testResolveProps() throws NoSuchFieldException, SecurityException, IOException, NoSuchMethodException {

    Field declaredField = ContextUtilsTest.class.getDeclaredField("test");
    Props declaredAnnotation = declaredField.getDeclaredAnnotation(Props.class);

    URL resource = ClassUtils.getClassLoader().getResource("info.properties");
    Properties properties = ContextUtils.getUrlAsProperties(resource.getProtocol() + ":" + resource.getPath());
    properties.list(System.err);
    Config resolveProps = ContextUtils.resolveProps(declaredAnnotation, Config.class, properties);

    System.err.println(resolveProps);

    assert "TODAY BLOG".equals(resolveProps.getDescription());
    assert "https://cdn.taketoday.cn".equals(resolveProps.getCdn());

    assert 21 == resolveProps.getAdmin().getAge();
    assert "666".equals(resolveProps.getAdmin().getUserId());
    assert "TODAY".equals(resolveProps.getAdmin().getUserName());

    assert ContextUtils.resolveProps(ContextUtilsTest.class.getDeclaredField("none"), properties).equals(Collections.emptyList());

    ContextUtils.resolveProps(ContextUtilsTest.class.getMethod("testResolveProps"), properties);
  }

  @Test
  @Ignore
  @SuppressWarnings("unchecked")
  public void testResolveParameter() throws Exception {

    ClassUtils.clearCache();
    try (ApplicationContext applicationContext = new StandardApplicationContext("", "cn.taketoday.util")) {
      final BeanFactory beanFactory = applicationContext.getBeanFactory();
      final Environment environment = applicationContext.getEnvironment();
      // placeHolder
      final Properties properties = environment.getProperties();
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
      ContextUtils.setLastStartupContext(applicationContext);

      Object[] parameters = ArgumentsResolver.getSharedInstance().resolve(constructor, beanFactory);

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
            @Props(prefix = "site.admin.") UserModel model, //
            @Props(prefix = "site.") Properties properties, //
            Properties emptyProperties, //
            @Env("placeHolder") int placeHolderEnv,
            @Value("#{placeHolder}") int placeHolder,

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

    public Config() {}

    @Props UserModel admin;

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

  @Singleton
  public static class TestBean {

  }

  @Test
  public void testBuildBeanDefinitions() throws NoSuchFieldException, SecurityException, IOException {
    try (ApplicationContext applicationContext = new StandardApplicationContext("", "test.context.utils")) {

      List<BeanDefinition> beanDefinitions = ContextUtils.createBeanDefinitions(null, getClass());
      assert beanDefinitions.size() == 1;

      beanDefinitions = ContextUtils.createBeanDefinitions(null, TestBean.class);
      assert beanDefinitions.size() == 1;

      final BeanDefinition beanDefinition = beanDefinitions.get(0);
      beanDefinition.setDestroyMethods(null);
      beanDefinition.setInitMethods((Method[]) null);
      beanDefinition.setScope(null);
      beanDefinition.setPropertyValues(null);

      try {
        ContextUtils.validateBeanDefinition(beanDefinition);
        fail("beanDefinition");
      }
      catch (ConfigurationException e) {
        assert true;
      }

      StandardBeanDefinition standardBeanDefinition = new StandardBeanDefinition("", (Class<?>) null);
      try {
        ContextUtils.validateBeanDefinition(standardBeanDefinition);
        fail("standardBeanDefinition");
      }
      catch (ConfigurationException e) {
        assert true;
      }
      try {
        ContextUtils.validateBeanDefinition(standardBeanDefinition.setDeclaringName("test"));
        fail("setDeclaringName");
      }
      catch (ConfigurationException e) {
        assert true;
      }
    }

  }

}
