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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.annotation.PreDestroy;

import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.BeanDefinitionBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author TODAY <br>
 * 2020-04-02 21:15
 */
class ScopeTests {

  static class ScopeBean {

    @PreDestroy
    void destory() {
      assertTrue(true);
      System.err.println("destory()");
    }
  }

  @Test
  void testSimpleThreadScope() {

    SimpleThreadScope thread = new SimpleThreadScope();

    try (StandardApplicationContext context = new StandardApplicationContext()) {
      context.getBeanFactory()
              .registerScope("thread", thread);

      final BeanDefinition def = BeanDefinitionBuilder.defaults("scopeBean", ScopeBean.class);
      context.registerBean(def);

      def.setScope("thread");
      final Object bean = context.getBean(def);
      final Object bean2 = context.getBean(def);
      assertEquals(bean, bean2);

      new Thread() {
        public void run() {
          final Object bean2 = context.getBean(def);
          System.err.println(bean2);
          Assertions.assertNotEquals(bean2, bean);
        }
      }.start();

      context.getBeanFactory()
              .destroyScopedBean("scopeBean");
      System.err.println(bean);
      Assertions.assertNotEquals(bean, context.getBean(def));
    }
  }

  @Test
  public void testCustomScopeConfigurer() {

    CustomScopeConfigurer configurer = new CustomScopeConfigurer();
    SimpleThreadScope thread = new SimpleThreadScope();
    configurer.addScope("thread", thread);

    try (StandardApplicationContext context = new StandardApplicationContext()) {

      final BeanDefinition def = BeanDefinitionBuilder.defaults("scopeBean", ScopeBean.class);
      context.registerBean(def);

      def.setScope("thread");
      try {
        context.getBean(def);
      }
      catch (Exception e) {
        assertTrue(true);
      }
      context.addBeanFactoryPostProcessor(configurer);

      context.scan();

      final Object bean = context.getBean(def);
      final Object bean2 = context.getBean(def);
      Assertions.assertEquals(bean, bean2);

      new Thread() {
        public void run() {
          final Object bean2 = context.getBean(def);
          System.err.println(bean2);
          assertNotEquals(bean2, bean);
        }

        ;
      }.start();

      context.getBeanFactory().destroyScopedBean("scopeBean");
      System.err.println(bean);
      assertNotEquals(bean, context.getBean(def));
    }
  }

}
