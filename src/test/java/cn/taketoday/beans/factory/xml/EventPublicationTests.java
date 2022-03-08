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

package cn.taketoday.beans.factory.xml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import java.util.List;

import cn.taketoday.beans.factory.parsing.AliasDefinition;
import cn.taketoday.beans.factory.parsing.BeanComponentDefinition;
import cn.taketoday.beans.factory.parsing.ComponentDefinition;
import cn.taketoday.beans.factory.parsing.ImportDefinition;
import cn.taketoday.beans.factory.parsing.PassThroughSourceExtractor;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.config.TypedStringValue;
import cn.taketoday.beans.testfixture.beans.CollectingReaderEventListener;
import cn.taketoday.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
@SuppressWarnings("rawtypes")
public class EventPublicationTests {

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  private final CollectingReaderEventListener eventListener = new CollectingReaderEventListener();

  @BeforeEach
  public void setUp() throws Exception {
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
    reader.setEventListener(this.eventListener);
    reader.setSourceExtractor(new PassThroughSourceExtractor());
    reader.loadBeanDefinitions(new ClassPathResource("beanEvents.xml", getClass()));
  }

  @Test
  public void defaultsEventReceived() throws Exception {
    List defaultsList = this.eventListener.getDefaults();
    boolean condition2 = !defaultsList.isEmpty();
    assertThat(condition2).isTrue();
    boolean condition1 = defaultsList.get(0) instanceof DocumentDefaultsDefinition;
    assertThat(condition1).isTrue();
    DocumentDefaultsDefinition defaults = (DocumentDefaultsDefinition) defaultsList.get(0);
    assertThat(defaults.getLazyInit()).isEqualTo("true");
    assertThat(defaults.getAutowire()).isEqualTo("constructor");
    assertThat(defaults.getInitMethod()).isEqualTo("myInit");
    assertThat(defaults.getDestroyMethod()).isEqualTo("myDestroy");
    assertThat(defaults.getMerge()).isEqualTo("true");
    boolean condition = defaults.getSource() instanceof Element;
    assertThat(condition).isTrue();
  }

  @Test
  public void beanEventReceived() throws Exception {
    ComponentDefinition componentDefinition1 = this.eventListener.getComponentDefinition("testBean");
    boolean condition3 = componentDefinition1 instanceof BeanComponentDefinition;
    assertThat(condition3).isTrue();
    assertThat(componentDefinition1.getBeanDefinitions().length).isEqualTo(1);
    BeanDefinition beanDefinition1 = componentDefinition1.getBeanDefinitions()[0];
    assertThat(beanDefinition1.getConstructorArgumentValues().getGenericArgumentValue(String.class).getValue()).isEqualTo(new TypedStringValue("Rob Harrop"));
    assertThat(componentDefinition1.getBeanReferences().length).isEqualTo(1);
    assertThat(componentDefinition1.getBeanReferences()[0].getBeanName()).isEqualTo("testBean2");
    assertThat(componentDefinition1.getInnerBeanDefinitions().length).isEqualTo(1);
    BeanDefinition innerBd1 = componentDefinition1.getInnerBeanDefinitions()[0];
    assertThat(innerBd1.getConstructorArgumentValues().getGenericArgumentValue(String.class).getValue()).isEqualTo(new TypedStringValue("ACME"));
    boolean condition2 = componentDefinition1.getSource() instanceof Element;
    assertThat(condition2).isTrue();

    ComponentDefinition componentDefinition2 = this.eventListener.getComponentDefinition("testBean2");
    boolean condition1 = componentDefinition2 instanceof BeanComponentDefinition;
    assertThat(condition1).isTrue();
    assertThat(componentDefinition1.getBeanDefinitions().length).isEqualTo(1);
    BeanDefinition beanDefinition2 = componentDefinition2.getBeanDefinitions()[0];
    assertThat(beanDefinition2.propertyValues().getPropertyValue("name")).isEqualTo(new TypedStringValue("Juergen Hoeller"));
    assertThat(componentDefinition2.getBeanReferences().length).isEqualTo(0);
    assertThat(componentDefinition2.getInnerBeanDefinitions().length).isEqualTo(1);
    BeanDefinition innerBd2 = componentDefinition2.getInnerBeanDefinitions()[0];
    assertThat(innerBd2.propertyValues().getPropertyValue("name")).isEqualTo(new TypedStringValue("Eva Schallmeiner"));
    boolean condition = componentDefinition2.getSource() instanceof Element;
    assertThat(condition).isTrue();
  }

  @Test
  public void aliasEventReceived() throws Exception {
    List aliases = this.eventListener.getAliases("testBean");
    assertThat(aliases.size()).isEqualTo(2);
    AliasDefinition aliasDefinition1 = (AliasDefinition) aliases.get(0);
    assertThat(aliasDefinition1.getAlias()).isEqualTo("testBeanAlias1");
    boolean condition1 = aliasDefinition1.getSource() instanceof Element;
    assertThat(condition1).isTrue();
    AliasDefinition aliasDefinition2 = (AliasDefinition) aliases.get(1);
    assertThat(aliasDefinition2.getAlias()).isEqualTo("testBeanAlias2");
    boolean condition = aliasDefinition2.getSource() instanceof Element;
    assertThat(condition).isTrue();
  }

  @Test
  public void importEventReceived() throws Exception {
    List imports = this.eventListener.getImports();
    assertThat(imports.size()).isEqualTo(1);
    ImportDefinition importDefinition = (ImportDefinition) imports.get(0);
    assertThat(importDefinition.getImportedResource()).isEqualTo("beanEventsImported.xml");
    boolean condition = importDefinition.getSource() instanceof Element;
    assertThat(condition).isTrue();
  }

}
