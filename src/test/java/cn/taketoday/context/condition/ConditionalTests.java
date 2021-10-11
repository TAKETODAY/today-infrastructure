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
package cn.taketoday.context.condition;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.lang.Singleton;

/**
 * @author TODAY <br>
 * 2019-09-16 22:39
 */
class ConditionalTests {

  @Singleton
  @ConditionalOnClass("javax.inject.Inject")
  public static class ConditionalClass {

  }

  @Singleton
  @ConditionalOnClass("Inject")
  public static class ConditionalMissingClass {

  }

  //
  @Singleton
  @ConditionalOnMissingClass("Inject")
  public static class ConditionalOnMissing {

  }

  @Singleton
  @ConditionalOnMissingClass("javax.inject.Inject")
  public static class ConditionalMissed {

  }

  // ConditionalOnClass
  // ------------------------------

  @Test
  void testConditionalOnClass() {

    try (final ApplicationContext applicationContext = //
            new StandardApplicationContext("info.properties", "cn.taketoday.context.condition")) {
      BeanDefinitionRegistry registry = applicationContext.unwrapFactory(BeanDefinitionRegistry.class);

      Assertions.assertTrue(registry.containsBeanDefinition(ConditionalClass.class));
      Assertions.assertTrue(registry.containsBeanDefinition(ConditionalOnMissing.class));
      Assertions.assertTrue(!registry.containsBeanDefinition(ConditionalMissingClass.class));
      Assertions.assertTrue(!registry.containsBeanDefinition(ConditionalMissed.class));
    }
  }

  // ConditionalOnExpression
  // ------------------------------------------------

  @Singleton
  @ConditionalOnExpression("${1+1==2}")
  public static class ConditionalExpression_ {

  }

  @Singleton
  @ConditionalOnExpression("${1+1!=2}")
  public static class ConditionalExpression__ {

  }

  @Test
  void conditionalOnExpression() {

    try (final ApplicationContext applicationContext = //
            new StandardApplicationContext("info.properties", "cn.taketoday.context.condition")) {

      BeanDefinitionRegistry registry = applicationContext.unwrapFactory(BeanDefinitionRegistry.class);

      Assertions.assertTrue(registry.containsBeanDefinition(ConditionalExpression_.class));
      Assertions.assertTrue(!registry.containsBeanDefinition(ConditionalExpression__.class));
    }
  }

  // ConditionalOnProperty
  // ---------------------------------

  @Singleton
  @ConditionalOnProperty("test.property")
  public static class ConditionalOnProperty_ {

  }

  @Singleton
  @ConditionalOnProperty("test.none.property")
  public static class ConditionalOnProperty__ {

  }

  @Singleton
  @ConditionalOnProperty(value = "property", prefix = "test.")
  public static class ConditionalOnProperty___ {

  }

  @Singleton
  @ConditionalOnProperty(value = "property", prefix = "test.none")
  public static class ConditionalOnProperty____ {

  }

  @Test
  public void testConditionalOnProperty() throws Exception {

    try (final ApplicationContext applicationContext = //
            new StandardApplicationContext("info.properties", "cn.taketoday.context.condition")) {
      BeanDefinitionRegistry registry = applicationContext.unwrapFactory(BeanDefinitionRegistry.class);

      Assertions.assertTrue(registry.containsBeanDefinition(ConditionalOnProperty_.class));
      Assertions.assertTrue(!registry.containsBeanDefinition(ConditionalOnProperty__.class));
      Assertions.assertTrue(registry.containsBeanDefinition(ConditionalOnProperty___.class));
      Assertions.assertTrue(!registry.containsBeanDefinition(ConditionalOnProperty____.class));
    }
  }

  // ConditionalOnResource
  // ----------------------------
  @Singleton
  @ConditionalOnResource("none")
  public static class ConditionalOnResource_ {

  }

  @Singleton
  @ConditionalOnResource("info.properties")
  public static class ConditionalOnResource__ {

  }

  @Singleton
  @ConditionalOnResource("classpath:/info.properties")
  public static class ConditionalOnResource___ {

  }

  @Singleton
  @ConditionalOnResource("classpath:info.properties")
  public static class ConditionalOnResource____ {

  }

  @Test
  public void testConditionalOnResource() throws Exception {

    try (final ApplicationContext applicationContext = //
            new StandardApplicationContext("info.properties", "cn.taketoday.context.condition")) {
      BeanDefinitionRegistry registry = applicationContext.unwrapFactory(BeanDefinitionRegistry.class);
      Assertions.assertFalse(registry.containsBeanDefinition(ConditionalOnResource_.class));

      Assertions.assertTrue(registry.containsBeanDefinition(ConditionalOnResource__.class));
      Assertions.assertTrue(registry.containsBeanDefinition(ConditionalOnResource___.class));
      Assertions.assertTrue(registry.containsBeanDefinition(ConditionalOnResource____.class));
    }
  }

}
