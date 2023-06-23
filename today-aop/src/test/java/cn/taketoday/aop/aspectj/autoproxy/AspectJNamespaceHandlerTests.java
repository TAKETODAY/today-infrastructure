/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aop.aspectj.autoproxy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.aop.config.AopConfigUtils;
import cn.taketoday.aop.config.AopNamespaceUtils;
import cn.taketoday.beans.testfixture.beans.CollectingReaderEventListener;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.parsing.PassThroughSourceExtractor;
import cn.taketoday.beans.factory.parsing.SourceExtractor;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.xml.ParserContext;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.beans.factory.xml.XmlReaderContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Chris Beams
 */
public class AspectJNamespaceHandlerTests {

  private ParserContext parserContext;

  private CollectingReaderEventListener readerEventListener = new CollectingReaderEventListener();

  private BeanDefinitionRegistry registry = new StandardBeanFactory();

  @BeforeEach
  public void setUp() throws Exception {
    SourceExtractor sourceExtractor = new PassThroughSourceExtractor();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.registry);
    XmlReaderContext readerContext =
            new XmlReaderContext(null, null, this.readerEventListener, sourceExtractor, reader, null);
    this.parserContext = new ParserContext(readerContext, null);
  }

  @Test
  public void testRegisterAutoProxyCreator() throws Exception {
    AopNamespaceUtils.registerAutoProxyCreatorIfNecessary(this.parserContext, null);
    assertThat(registry.getBeanDefinitionCount()).as("Incorrect number of definitions registered").isEqualTo(1);

    AopNamespaceUtils.registerAspectJAutoProxyCreatorIfNecessary(this.parserContext, null);
    assertThat(registry.getBeanDefinitionCount()).as("Incorrect number of definitions registered").isEqualTo(1);
  }

  @Test
  public void testRegisterAspectJAutoProxyCreator() throws Exception {
    AopNamespaceUtils.registerAspectJAutoProxyCreatorIfNecessary(this.parserContext, null);
    assertThat(registry.getBeanDefinitionCount()).as("Incorrect number of definitions registered").isEqualTo(1);

    AopNamespaceUtils.registerAspectJAutoProxyCreatorIfNecessary(this.parserContext, null);
    assertThat(registry.getBeanDefinitionCount()).as("Incorrect number of definitions registered").isEqualTo(1);

    BeanDefinition definition = registry.getBeanDefinition(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
    assertThat(definition.getBeanClassName()).as("Incorrect APC class").isEqualTo(AspectJAwareAdvisorAutoProxyCreator.class.getName());
  }

  @Test
  public void testRegisterAspectJAutoProxyCreatorWithExistingAutoProxyCreator() throws Exception {
    AopNamespaceUtils.registerAutoProxyCreatorIfNecessary(this.parserContext, null);
    assertThat(registry.getBeanDefinitionCount()).isEqualTo(1);

    AopNamespaceUtils.registerAspectJAutoProxyCreatorIfNecessary(this.parserContext, null);
    assertThat(registry.getBeanDefinitionCount()).as("Incorrect definition count").isEqualTo(1);

    BeanDefinition definition = registry.getBeanDefinition(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
    assertThat(definition.getBeanClassName()).as("APC class not switched").isEqualTo(AspectJAwareAdvisorAutoProxyCreator.class.getName());
  }

  @Test
  public void testRegisterAutoProxyCreatorWhenAspectJAutoProxyCreatorAlreadyExists() throws Exception {
    AopNamespaceUtils.registerAspectJAutoProxyCreatorIfNecessary(this.parserContext, null);
    assertThat(registry.getBeanDefinitionCount()).isEqualTo(1);

    AopNamespaceUtils.registerAutoProxyCreatorIfNecessary(this.parserContext, null);
    assertThat(registry.getBeanDefinitionCount()).as("Incorrect definition count").isEqualTo(1);

    BeanDefinition definition = registry.getBeanDefinition(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
    assertThat(definition.getBeanClassName()).as("Incorrect APC class").isEqualTo(AspectJAwareAdvisorAutoProxyCreator.class.getName());
  }

}
