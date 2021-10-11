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
package cn.taketoday.beans.factory;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;

import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;

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

      BeanDefinition beanDefinition = new DefaultBeanDefinition("testBean", BeanDefinitionTests.class);

      beanDefinition.setDestroyMethods("destory")//
              .setInitMethods(BeanDefinitionTests.class.getDeclaredMethod("init"));

      final Field test = BeanDefinitionTests.class.getDeclaredField("test");
      final Field testInt = BeanDefinitionTests.class.getDeclaredField("testInt");
      final Field testDouble = BeanDefinitionTests.class.getDeclaredField("testDouble");

      HashSet<PropertySetter> propertySetters = new HashSet<>();
      beanDefinition.addPropertySetter(propertySetters);

      beanDefinition.setPropertyValues(null);

      propertySetters.add(new DefaultPropertySetter(123, testInt));

      beanDefinition.addPropertySetter(propertySetters);

      beanDefinition.setPropertyValues(null);

      beanDefinition.addPropertySetter();

      beanDefinition.addPropertySetter(
              new DefaultPropertySetter("TEST_STRING", test),
              new DefaultPropertySetter(123.123, testDouble)//
      );

      beanDefinition.addPropertySetter(
              new DefaultPropertySetter("TEST_STRING", test), //
              new DefaultPropertySetter(123.123, testDouble)//
      );

      beanDefinition.getPropertyValue("test");
      assert beanDefinition.isSingleton();

      try {
        beanDefinition.getPropertyValue("test1");
        assert false;
      }
      catch (Exception e) {
        assert true;
      }

      beanDefinition.addPropertySetter(propertySetters);

      applicationContext.registerBean("testBean", beanDefinition);

      final Object bean = applicationContext.getBean("testBean");

      final BeanDefinitionTests beanDefinitionTest = applicationContext.getBean(getClass());

      assert beanDefinitionTest.testInt == 123;
      assert beanDefinitionTest.testDouble == 123.123;
      assert beanDefinitionTest.test.equals("TEST_STRING");
      assert beanDefinitionTest == bean;

      System.err.println(beanDefinition);
    }
  }

}
