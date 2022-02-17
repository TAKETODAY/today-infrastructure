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

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.beans.factory.annotation.Autowired;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Tests cornering bug SPR-8514.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 4.0
 */
public class ConfigurationWithFactoryBeanAndAutowiringTests {

  @Test
  public void withConcreteFactoryBeanImplementationAsReturnType() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(AppConfig.class);
    ctx.register(ConcreteFactoryBeanImplementationConfig.class);
    ctx.refresh();
  }

  @Test
  public void withParameterizedFactoryBeanImplementationAsReturnType() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(AppConfig.class);
    ctx.register(ParameterizedFactoryBeanImplementationConfig.class);
    ctx.refresh();
  }

  @Test
  public void withParameterizedFactoryBeanInterfaceAsReturnType() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(AppConfig.class);
    ctx.register(ParameterizedFactoryBeanInterfaceConfig.class);
    ctx.refresh();
  }

  @Test
  public void withNonPublicParameterizedFactoryBeanInterfaceAsReturnType() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(AppConfig.class);
    ctx.register(NonPublicParameterizedFactoryBeanInterfaceConfig.class);
    ctx.refresh();
  }

  @Test
  public void withRawFactoryBeanInterfaceAsReturnType() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(AppConfig.class);
    ctx.register(RawFactoryBeanInterfaceConfig.class);
    ctx.refresh();
  }

  @Test
  public void withWildcardParameterizedFactoryBeanInterfaceAsReturnType() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(AppConfig.class);
    ctx.register(WildcardParameterizedFactoryBeanInterfaceConfig.class);
    ctx.refresh();
  }

  @Test
  public void withFactoryBeanCallingBean() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(AppConfig.class);
    ctx.register(FactoryBeanCallingConfig.class);
    ctx.refresh();
    assertThat(ctx.getBean("myString")).isEqualTo("true");
  }

  static class DummyBean {
  }

  static class MyFactoryBean implements FactoryBean<String>, InitializingBean {

    private boolean initialized = false;

    @Override
    public void afterPropertiesSet() throws Exception {
      this.initialized = true;
    }

    @Override
    public String getObject() throws Exception {
      return "foo";
    }

    @Override
    public Class<String> getObjectType() {
      return String.class;
    }

    @Override
    public boolean isSingleton() {
      return true;
    }

    public String getString() {
      return Boolean.toString(this.initialized);
    }
  }

  static class MyParameterizedFactoryBean<T> implements FactoryBean<T> {

    private final T obj;

    public MyParameterizedFactoryBean(T obj) {
      this.obj = obj;
    }

    @Override
    public T getObject() throws Exception {
      return obj;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<T> getObjectType() {
      return (Class<T>) obj.getClass();
    }

    @Override
    public boolean isSingleton() {
      return true;
    }
  }

  @Configuration
  static class AppConfig {

    @Bean
    public DummyBean dummyBean() {
      return new DummyBean();
    }
  }

  @Configuration
  static class ConcreteFactoryBeanImplementationConfig {

    @Autowired
    private DummyBean dummyBean;

    @Bean
    public MyFactoryBean factoryBean() {
      Assert.notNull(dummyBean, "DummyBean was not injected.");
      return new MyFactoryBean();
    }
  }

  @Configuration
  static class ParameterizedFactoryBeanImplementationConfig {

    @Autowired
    private DummyBean dummyBean;

    @Bean
    public MyParameterizedFactoryBean<String> factoryBean() {
      Assert.notNull(dummyBean, "DummyBean was not injected.");
      return new MyParameterizedFactoryBean<>("whatev");
    }
  }

  @Configuration
  static class ParameterizedFactoryBeanInterfaceConfig {

    @Autowired
    private DummyBean dummyBean;

    @Bean
    public FactoryBean<String> factoryBean() {
      Assert.notNull(dummyBean, "DummyBean was not injected.");
      return new MyFactoryBean();
    }
  }

  @Configuration
  static class NonPublicParameterizedFactoryBeanInterfaceConfig {

    @Autowired
    private DummyBean dummyBean;

    @Bean
    FactoryBean<String> factoryBean() {
      Assert.notNull(dummyBean, "DummyBean was not injected.");
      return new MyFactoryBean();
    }
  }

  @Configuration
  static class RawFactoryBeanInterfaceConfig {

    @Autowired
    private DummyBean dummyBean;

    @Bean
    @SuppressWarnings("rawtypes")
    public FactoryBean factoryBean() {
      Assert.notNull(dummyBean, "DummyBean was not injected.");
      return new MyFactoryBean();
    }
  }

  @Configuration
  static class WildcardParameterizedFactoryBeanInterfaceConfig {

    @Autowired
    private DummyBean dummyBean;

    @Bean
    public FactoryBean<?> factoryBean() {
      Assert.notNull(dummyBean, "DummyBean was not injected.");
      return new MyFactoryBean();
    }
  }

  @Configuration
  static class FactoryBeanCallingConfig {

    @Autowired
    private DummyBean dummyBean;

    @Bean
    public MyFactoryBean factoryBean() {
      Assert.notNull(dummyBean, "DummyBean was not injected.");
      return new MyFactoryBean();
    }

    @Bean
    public String myString() {
      return factoryBean().getString();
    }
  }

}
