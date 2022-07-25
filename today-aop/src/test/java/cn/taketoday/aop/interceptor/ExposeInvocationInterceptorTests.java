/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.aop.interceptor;

import org.junit.jupiter.api.Test;

import cn.taketoday.aop.testfixture.beans.ITestBean;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;

import static cn.taketoday.aop.testfixture.ResourceTestUtils.qualifiedResource;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Non-XML tests are in AbstractAopProxyTests
 *
 * @author Rod Johnson
 * @author Chris Beams
 */
public class ExposeInvocationInterceptorTests {

  @Test
  public void testXmlConfig() {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
            qualifiedResource(ExposeInvocationInterceptorTests.class, "context.xml"));
    ITestBean tb = (ITestBean) bf.getBean("proxy");
    String name = "tony";
    tb.setName(name);
    // Fires context checks
    assertThat(tb.getName()).isEqualTo(name);
  }

}
