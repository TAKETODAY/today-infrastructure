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
package cn.taketoday.beans.factory;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

import static org.assertj.core.api.Assertions.fail;

/**
 * @author TODAY <br>
 * 2019-06-12 20:48
 */
@SuppressWarnings("all")
class BeanDefinitionTests {
  private static final Logger log = LoggerFactory.getLogger(BeanDefinitionTests.class);

  public String test;
  public int testInt;
  public double testDouble;

  public void init() {
    assert true;
    log.debug("init");
  }

  public void destory() {
    assert true;
    log.debug("destory");
  }

  @Test
  void addPropertyValue() throws NoSuchMethodException, SecurityException, NoSuchFieldException {

    try (StandardApplicationContext applicationContext = new StandardApplicationContext()) {

      BeanDefinition beanDefinition = new BeanDefinition("testBean", BeanDefinitionTests.class);

      beanDefinition.setDestroyMethod("destory");
      beanDefinition.setInitMethods("init");

      beanDefinition.addPropertyValue("testInt", 123);
      beanDefinition.addPropertyValue("test", "TEST_STRING");
      beanDefinition.addPropertyValue("testDouble", 123.123);
      beanDefinition.addPropertyValue("testDouble", 123.123);

      assert beanDefinition.isSingleton();

      try {
        beanDefinition.getRequiredPropertyValue("test1");
        fail("getPropertyValue failed");
      }
      catch (Exception e) {
      }

      applicationContext.registerBeanDefinition("testBean", beanDefinition);
      applicationContext.refresh();

      Object bean = applicationContext.getBean("testBean");

      BeanDefinitionTests beanDefinitionTest = applicationContext.getBean(getClass());

      assert beanDefinitionTest.testInt == 123;
      assert beanDefinitionTest.testDouble == 123.123;
      assert beanDefinitionTest.test.equals("TEST_STRING");
      assert beanDefinitionTest == bean;

      System.err.println(beanDefinition);
    }
  }

}
