/*
 * Copyright 2017 - 2023 the original author or authors.
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

import cn.taketoday.aop.framework.ProxyFactoryBean;
import cn.taketoday.aop.target.CommonsPool2TargetSource;

/**
 * @author Juergen Hoeller
 */
public class Spr15042Tests {

  @Test
  public void poolingTargetSource() {
    new AnnotationConfigApplicationContext(PoolingTargetSourceConfig.class);
  }

  @Configuration
  static class PoolingTargetSourceConfig {

    @Bean
    @Scope(value = "request"/*, proxyMode = ScopedProxyMode.TARGET_CLASS*/)
    public ProxyFactoryBean myObject() {
      ProxyFactoryBean pfb = new ProxyFactoryBean();
      pfb.setTargetSource(poolTargetSource());
      return pfb;
    }

    @Bean
    public CommonsPool2TargetSource poolTargetSource() {
      CommonsPool2TargetSource pool = new CommonsPool2TargetSource();
      pool.setMaxSize(3);
      pool.setTargetBeanName("myObjectTarget");
      return pool;
    }

    @Bean(name = "myObjectTarget")
    @Scope(scopeName = "prototype")
    public Object myObjectTarget() {
      return new Object();
    }
  }

}
