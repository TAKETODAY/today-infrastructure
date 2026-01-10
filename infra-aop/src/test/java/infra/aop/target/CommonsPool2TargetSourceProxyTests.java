/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
