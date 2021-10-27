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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.beans.factory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.aware.ApplicationContextSupport;
import cn.taketoday.lang.Singleton;

/**
 * @author Today <br>
 *
 * 2018-12-25 19:09
 */
class FactoryBeanTests {

  // bean
  // --------------------------------------
  private static class TEST {

  }

  private static class TESTFactoryBean extends AbstractFactoryBean<TEST> {

    @Override
    protected TEST createBeanInstance() {
      return new TEST();
    }

    @Override
    public Class<TEST> getBeanClass() {
      return TEST.class;
    }
  }

  // @Configuration bean
  // ---------------------------

  static class FactoryBeanConfiguration extends ApplicationContextSupport {

    @Singleton
    public TESTFactoryBean testFactoryBean() {
      return new TESTFactoryBean();
    }
  }

  @Import(FactoryBeanConfiguration.class)
  static class FactoryBeanConfigurationImporter {

  }

  // test
  // --------------------------------------------

  @Test
  public void testFactoryBean() throws NoSuchBeanDefinitionException {

    try (StandardApplicationContext applicationContext = new StandardApplicationContext()) {
      applicationContext.registerBean("testFactoryBean", TESTFactoryBean.class);
      applicationContext.refresh();

      Map<String, BeanDefinition> beanDefinitions = applicationContext.getBeanDefinitions();

      Assertions.assertFalse(beanDefinitions.isEmpty());

      Object testFactoryBean = applicationContext.getBean("testFactoryBean");

      TEST bean = applicationContext.getBean(TEST.class);

      SingletonBeanRegistry singletonBeanRegistry =
              applicationContext.unwrapFactory(SingletonBeanRegistry.class);

      System.err.println(singletonBeanRegistry.getSingletons());

      Assertions.assertEquals(bean, testFactoryBean);

      Assertions.assertSame(testFactoryBean, bean);
      Assertions.assertNotNull(applicationContext.getBean("$testFactoryBean"));
    }
  }

//    @Test
//    public void testPrototypeFactoryBean() throws NoSuchBeanDefinitionException {
//
//        try (ApplicationContext applicationContext = new StandardApplicationContext()) {
//
//            List<BeanDefinition> definitions = //
//                    ContextUtils.createBeanDefinitions("testFactoryBean-prototype", TESTFactoryBean.class);
//
//            assertFalse(definitions.isEmpty());
//
//            BeanDefinition beanDefinition = definitions.get(0);
//            beanDefinition.setScope(Scope.PROTOTYPE);
//
//            applicationContext.registerBean(beanDefinition);
//
//            Map<String, BeanDefinition> beanDefinitions = applicationContext.getBeanDefinitions();
//
//            assertFalse(beanDefinitions.isEmpty());
//
//            Object testFactoryBean = applicationContext.getBean("testFactoryBean-prototype");
//
//            TEST bean = applicationContext.getBean(TEST.class);
//
//            assertNotEquals(bean, testFactoryBean);
//
//            final Object $testFactoryBean = applicationContext.getBean("$testFactoryBean-prototype");
//            assertNotNull($testFactoryBean);
//        }
//    }

  @Test
  public void testConfigurationFactoryBean() throws NoSuchBeanDefinitionException {

    try (StandardApplicationContext applicationContext = new StandardApplicationContext()) {

      applicationContext.register(TESTFactoryBean.class);
      applicationContext.registerBean("factoryBeanConfigurationImporter", FactoryBeanConfigurationImporter.class);
      applicationContext.refresh();

      FactoryBeanConfiguration bean = applicationContext.getBean(FactoryBeanConfiguration.class);
      Object testFactoryBean = applicationContext.getBean("testFactoryBean");

      Assertions.assertNotNull(bean);
      Assertions.assertNotNull(testFactoryBean);
      Assertions.assertTrue(testFactoryBean instanceof TEST);

      Assertions.assertNotNull(applicationContext.getBean("$testFactoryBean"));
      Assertions.assertTrue(applicationContext.getBean("$testFactoryBean") instanceof TESTFactoryBean);
    }
  }

}
