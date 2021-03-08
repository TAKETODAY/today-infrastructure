/**
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context;

import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import cn.taketoday.context.ApplicationContextTest.RequiredTest.Bean1;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.factory.BeanDefinition;
import cn.taketoday.context.factory.DisposableBean;
import cn.taketoday.context.factory.PropertyValue;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.StringUtils;
import test.demo.config.Config;
import test.demo.config.ConfigFactoryBean;
import test.demo.config.ConfigurationBean;
import test.demo.config.User;
import test.demo.repository.impl.DefaultUserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Today
 * 2018年7月3日 下午10:05:21
 */
public class ApplicationContextTest {
  private static final Logger log = LoggerFactory.getLogger(ApplicationContextTest.class);

  /**
   * test ApplicationContext
   *
   * @throws NoSuchBeanDefinitionException
   */
  @Test
  public void testApplicationContext() throws NoSuchBeanDefinitionException {
    try (ApplicationContext applicationContext = new StandardApplicationContext("")) {
      applicationContext.loadContext("test.demo.repository");

      boolean containsBean = applicationContext.containsBeanDefinition(DefaultUserRepository.class);
      assertThat(toString(applicationContext)).isEqualTo(applicationContext.toString());
      assert containsBean : "UserDaoImpl load error.";
    }
  }

  static String toString(ApplicationContext context) {
    StringBuilder sb = new StringBuilder(ObjectUtils.toHexString(context));
    sb.append(": defining beans [");
    sb.append(StringUtils.collectionToString(context.getBeanDefinitions().keySet()));
    sb.append("], state: [");
    sb.append(context.getState());
    sb.append("], on startup date: ");
    sb.append(new Date(context.getStartupDate()));
    return sb.toString();
  }

  /**
   * test load context and get singleton.
   *
   * @throws NoSuchBeanDefinitionException
   */
  @Test
  public void testLoadSingleton() throws NoSuchBeanDefinitionException {
    try (ApplicationContext applicationContext = new StandardApplicationContext()) {
      applicationContext.loadContext("test.demo.config");
      Config config = applicationContext.getBean(Config.class);
      Config config_ = applicationContext.getBean(Config.class);

      assert config == config_ : "singleton error.";

      String copyright = config.getCopyright();

      assert copyright != null : "properties file load error.";
    }
  }

  /**
   * test load FactoryBean.
   *
   * @throws NoSuchBeanDefinitionException
   */
  @Test
  public void testLoadFactoryBean() throws NoSuchBeanDefinitionException {

    try (ApplicationContext applicationContext = new StandardApplicationContext("")) {
      applicationContext.loadContext("test.demo.config");
      Config config = applicationContext.getBean("FactoryBean-Config", Config.class);
      Config config_ = applicationContext.getBean("FactoryBean-Config", Config.class);

      BeanDefinition beanDefinition = applicationContext.getBeanDefinition("FactoryBean-Config");
      PropertyValue propertyValue = beanDefinition.getPropertyValue("pro");
      ConfigFactoryBean bean = applicationContext.getBean("$FactoryBean-Config", ConfigFactoryBean.class);

      assertNotNull(bean); // @Prototype 
      assertNotEquals(config, config_);

      log.debug("{}", config.hashCode());
      log.debug("{}", config_.hashCode());
      log.debug("{}", bean);
      log.debug("{}", propertyValue);
    }
  }

  /**
   * Manual Loading.
   */
  @Test
  public void testManualLoadContext() throws NoSuchBeanDefinitionException, BeanDefinitionStoreException {

    try (StandardApplicationContext applicationContext = new StandardApplicationContext("")) {

      applicationContext.registerBean(User.class);
      applicationContext.registerBean("user", User.class);
      applicationContext.registerBean("user_", User.class);
      //			applicationContext.onRefresh(); // init bean

      Map<String, BeanDefinition> beanDefinitionsMap = applicationContext.getBeanDefinitions();

      Object bean = applicationContext.getBean("user");
      assert beanDefinitionsMap.size() == 2;
      assert bean != null : "error";
    }
  }

  @Test
  public void testloadFromCollection() throws NoSuchBeanDefinitionException, BeanDefinitionStoreException {

    try (ApplicationContext applicationContext = //
            new StandardApplicationContext(Arrays.asList(ConfigurationBean.class))) {

      long start = System.currentTimeMillis();

      User bean = applicationContext.getBean(User.class);
//      System.err.println(System.currentTimeMillis() - start + "ms");
//      System.err.println(applicationContext.getEnvironment().getBeanDefinitionRegistry().getBeanDefinitions());

      bean.setAge(12);

      User user = applicationContext.getBean(User.class);

      assert bean != user;
    }
  }

  // RequiredTest
  // -------------------------------------
  public static class RequiredTest {

    @Autowired(required = false, value = "requiredTestBean")
    private Bean bean;

    @Autowired(required = true, value = "requiredTestBean1")
    private Bean1 bean1;

    public static class Bean {

    }

    public static class Bean1 implements DisposableBean {

      @Override
      public void destroy() throws Exception {
        System.err.println("Bean destroy");
      }
    }
  }

  @Test
  public void testRequired() throws NoSuchBeanDefinitionException, BeanDefinitionStoreException {

    try (ApplicationContext applicationContext = new StandardApplicationContext()) {

      applicationContext.registerBean("requiredTest", RequiredTest.class);

      // applicationContext.registerBean("requiredTestBean1", Bean.class);
      applicationContext.registerBean("requiredTestBean1", Bean1.class);

      RequiredTest requiredTest = applicationContext.getBean(RequiredTest.class);

      assertNull(requiredTest.bean);
      assertNotNull(requiredTest.bean1);
    }
  }

}
