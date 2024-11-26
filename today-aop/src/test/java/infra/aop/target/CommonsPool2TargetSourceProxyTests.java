/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.aop.target;

import org.junit.jupiter.api.Test;

import infra.aop.support.AopUtils;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.beans.testfixture.beans.ITestBean;
import infra.core.io.Resource;

import static infra.core.testfixture.io.ResourceTestUtils.qualifiedResource;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
public class CommonsPool2TargetSourceProxyTests {

  private static final Resource CONTEXT =
          qualifiedResource(CommonsPool2TargetSourceProxyTests.class, "context.xml");

  @Test
  public void testProxy() throws Exception {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
    reader.loadBeanDefinitions(CONTEXT);
    beanFactory.preInstantiateSingletons();
    ITestBean bean = (ITestBean) beanFactory.getBean("testBean");
    assertThat(AopUtils.isAopProxy(bean)).isTrue();
  }
}
