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

package cn.taketoday.aop;

import org.junit.Test;

import cn.taketoday.aop.proxy.ProxyConfig;
import cn.taketoday.aop.proxy.ProxyCreator;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.factory.BeanDefinition;
import cn.taketoday.context.factory.DefaultPropertyValue;
import cn.taketoday.context.factory.PropertyValue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/3/18 21:53
 */
public class AutoProxyConfigurationTests {

  @EnableAspectAutoProxy(proxyTargetClass = false, exposeProxy = true)
  static class AopConfig {

  }

  @Test
  public void test() {
    try (final StandardApplicationContext context = new StandardApplicationContext()) {
      context.importBeans(AopConfig.class);

      final BeanDefinition proxyCreatorDef = context.getBeanDefinition(ProxyCreator.class);

      final PropertyValue exposeProxy = proxyCreatorDef.getPropertyValue("exposeProxy");
      final PropertyValue proxyTargetClass = proxyCreatorDef.getPropertyValue("proxyTargetClass");


      assertThat(exposeProxy).isInstanceOf(DefaultPropertyValue.class);
      assertThat(proxyTargetClass).isInstanceOf(DefaultPropertyValue.class);

      assertThat(exposeProxy.getName()).isEqualTo("exposeProxy");
      assertThat(proxyTargetClass.getName()).isEqualTo("proxyTargetClass");

      DefaultPropertyValue _exposeProxy = (DefaultPropertyValue) exposeProxy;
      DefaultPropertyValue _proxyTargetClass = (DefaultPropertyValue) proxyTargetClass;

      final ProxyConfig proxyCreator = context.getBean(ProxyConfig.class);

      assertThat(_exposeProxy.getValue()).isInstanceOf(Boolean.class).isEqualTo(true).isEqualTo(proxyCreator.isExposeProxy());
      assertThat(_proxyTargetClass.getValue()).isInstanceOf(Boolean.class).isEqualTo(false).isEqualTo(proxyCreator.isProxyTargetClass());

    }
  }

}
