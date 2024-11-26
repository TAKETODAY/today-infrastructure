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

package infra.ejb.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.beans.factory.parsing.BeanComponentDefinition;
import infra.beans.factory.parsing.ComponentDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.beans.testfixture.beans.CollectingReaderEventListener;
import infra.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Torsten Juergeleit
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class JeeNamespaceHandlerEventTests {

  private final CollectingReaderEventListener eventListener = new CollectingReaderEventListener();

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  private XmlBeanDefinitionReader reader;

  @BeforeEach
  public void setup() throws Exception {
    this.reader = new XmlBeanDefinitionReader(this.beanFactory);
    this.reader.setEventListener(this.eventListener);
    this.reader.loadBeanDefinitions(new ClassPathResource("jeeNamespaceHandlerTests.xml", getClass()));
  }

  @Test
  public void testJndiLookupComponentEventReceived() {
    ComponentDefinition component = this.eventListener.getComponentDefinition("simple");
    boolean condition = component instanceof BeanComponentDefinition;
    assertThat(condition).isTrue();
  }

  @Test
  public void testLocalSlsbComponentEventReceived() {
    ComponentDefinition component = this.eventListener.getComponentDefinition("simpleLocalEjb");
    boolean condition = component instanceof BeanComponentDefinition;
    assertThat(condition).isTrue();
  }

  @Test
  public void testRemoteSlsbComponentEventReceived() {
    ComponentDefinition component = this.eventListener.getComponentDefinition("simpleRemoteEjb");
    boolean condition = component instanceof BeanComponentDefinition;
    assertThat(condition).isTrue();
  }

}
