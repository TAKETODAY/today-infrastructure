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

package cn.taketoday.aop;

import org.junit.jupiter.api.Test;

import cn.taketoday.aop.proxy.ProxyConfig;
import cn.taketoday.aop.proxy.ProxyCreator;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.DefaultPropertySetter;
import cn.taketoday.beans.factory.PropertyValue;
import cn.taketoday.context.StandardApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/3/18 21:53
 */
class AutoProxyConfigurationTests {

  @EnableAspectAutoProxy(proxyTargetClass = false, exposeProxy = true)
  static class AopConfig {

  }

  @Test
  void testEnableAspectAutoProxy() {
    try (StandardApplicationContext context = new StandardApplicationContext()) {
      context.register(AopConfig.class);

      BeanDefinition proxyCreatorDef = context.getBeanDefinition(ProxyCreator.class);

      Object exposeProxy = proxyCreatorDef.getPropertyValue("exposeProxy");
      Object proxyTargetClass = proxyCreatorDef.getPropertyValue("proxyTargetClass");

      ProxyConfig proxyCreator = context.getBean(ProxyConfig.class);

      assertThat(exposeProxy).isInstanceOf(Boolean.class).isEqualTo(true).isEqualTo(proxyCreator.isExposeProxy());
      assertThat(proxyTargetClass).isInstanceOf(Boolean.class).isEqualTo(false).isEqualTo(proxyCreator.isProxyTargetClass());

    }
  }

}
