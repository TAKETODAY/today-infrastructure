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

package cn.taketoday.aop.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanReference;
import cn.taketoday.beans.factory.parsing.BeanComponentDefinition;
import cn.taketoday.beans.factory.parsing.ComponentDefinition;
import cn.taketoday.beans.factory.parsing.CompositeComponentDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.beans.testfixture.beans.CollectingReaderEventListener;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.testfixture.io.ResourceTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class AopNamespaceHandlerEventTests {

  private static final Class<?> CLASS = AopNamespaceHandlerEventTests.class;

  private static final Resource CONTEXT = ResourceTestUtils.qualifiedResource(CLASS, "context.xml");
  private static final Resource POINTCUT_EVENTS_CONTEXT = ResourceTestUtils.qualifiedResource(CLASS, "pointcutEvents.xml");
  private static final Resource POINTCUT_REF_CONTEXT = ResourceTestUtils.qualifiedResource(CLASS, "pointcutRefEvents.xml");
  private static final Resource DIRECT_POINTCUT_EVENTS_CONTEXT = ResourceTestUtils.qualifiedResource(CLASS, "directPointcutEvents.xml");

  private CollectingReaderEventListener eventListener = new CollectingReaderEventListener();

  private StandardBeanFactory beanFactory = new StandardBeanFactory();

  private XmlBeanDefinitionReader reader;

  @BeforeEach
  void setup() {
    this.reader = new XmlBeanDefinitionReader(this.beanFactory);
    this.reader.setEventListener(this.eventListener);
  }

  @Test
  void pointcutEvents() {
    this.reader.loadBeanDefinitions(POINTCUT_EVENTS_CONTEXT);
    ComponentDefinition[] componentDefinitions = this.eventListener.getComponentDefinitions();
    assertThat(componentDefinitions).as("Incorrect number of events fired").hasSize(1);
    assertThat(componentDefinitions[0]).as("No holder with nested components").isInstanceOf(CompositeComponentDefinition.class);

    CompositeComponentDefinition compositeDef = (CompositeComponentDefinition) componentDefinitions[0];
    assertThat(compositeDef.getName()).isEqualTo("aop:config");

    ComponentDefinition[] nestedComponentDefs = compositeDef.getNestedComponents();
    assertThat(nestedComponentDefs).as("Incorrect number of inner components").hasSize(2);
    PointcutComponentDefinition pcd = null;
    for (ComponentDefinition componentDefinition : nestedComponentDefs) {
      if (componentDefinition instanceof PointcutComponentDefinition) {
        pcd = (PointcutComponentDefinition) componentDefinition;
        break;
      }
    }
    assertThat(pcd).as("PointcutComponentDefinition not found").isNotNull();
    assertThat(pcd.getBeanDefinitions()).as("Incorrect number of BeanDefinitions").hasSize(1);
  }

  @Test
  void advisorEventsWithPointcutRef() {
    this.reader.loadBeanDefinitions(POINTCUT_REF_CONTEXT);
    ComponentDefinition[] componentDefinitions = this.eventListener.getComponentDefinitions();
    assertThat(componentDefinitions).as("Incorrect number of events fired").hasSize(2);

    assertThat(componentDefinitions[0]).as("No holder with nested components").isInstanceOf(CompositeComponentDefinition.class);
    CompositeComponentDefinition compositeDef = (CompositeComponentDefinition) componentDefinitions[0];
    assertThat(compositeDef.getName()).isEqualTo("aop:config");

    ComponentDefinition[] nestedComponentDefs = compositeDef.getNestedComponents();
    assertThat(nestedComponentDefs).as("Incorrect number of inner components").hasSize(3);
    AdvisorComponentDefinition acd = null;
    for (ComponentDefinition componentDefinition : nestedComponentDefs) {
      if (componentDefinition instanceof AdvisorComponentDefinition) {
        acd = (AdvisorComponentDefinition) componentDefinition;
        break;
      }
    }
    assertThat(acd).as("AdvisorComponentDefinition not found").isNotNull();
    assertThat(acd.getBeanDefinitions()).hasSize(1);
    assertThat(acd.getBeanReferences()).hasSize(2);

    assertThat(componentDefinitions[1]).as("No advice bean found").isInstanceOf(BeanComponentDefinition.class);
    BeanComponentDefinition adviceDef = (BeanComponentDefinition) componentDefinitions[1];
    assertThat(adviceDef.getBeanName()).isEqualTo("countingAdvice");
  }

  @Test
  void advisorEventsWithDirectPointcut() {
    this.reader.loadBeanDefinitions(DIRECT_POINTCUT_EVENTS_CONTEXT);
    ComponentDefinition[] componentDefinitions = this.eventListener.getComponentDefinitions();
    assertThat(componentDefinitions).as("Incorrect number of events fired").hasSize(2);

    assertThat(componentDefinitions[0]).as("No holder with nested components").isInstanceOf(CompositeComponentDefinition.class);
    CompositeComponentDefinition compositeDef = (CompositeComponentDefinition) componentDefinitions[0];
    assertThat(compositeDef.getName()).isEqualTo("aop:config");

    ComponentDefinition[] nestedComponentDefs = compositeDef.getNestedComponents();
    assertThat(nestedComponentDefs).as("Incorrect number of inner components").hasSize(2);
    AdvisorComponentDefinition acd = null;
    for (ComponentDefinition componentDefinition : nestedComponentDefs) {
      if (componentDefinition instanceof AdvisorComponentDefinition) {
        acd = (AdvisorComponentDefinition) componentDefinition;
        break;
      }
    }
    assertThat(acd).as("AdvisorComponentDefinition not found").isNotNull();
    assertThat(acd.getBeanDefinitions()).hasSize(2);
    assertThat(acd.getBeanReferences()).hasSize(1);

    assertThat(componentDefinitions[1]).as("No advice bean found").isInstanceOf(BeanComponentDefinition.class);
    BeanComponentDefinition adviceDef = (BeanComponentDefinition) componentDefinitions[1];
    assertThat(adviceDef.getBeanName()).isEqualTo("countingAdvice");
  }

  @Test
  void aspectEvent() {
    this.reader.loadBeanDefinitions(CONTEXT);
    ComponentDefinition[] componentDefinitions = this.eventListener.getComponentDefinitions();
    assertThat(componentDefinitions).as("Incorrect number of events fired").hasSize(2);

    assertThat(componentDefinitions[0]).as("No holder with nested components").isInstanceOf(CompositeComponentDefinition.class);
    CompositeComponentDefinition compositeDef = (CompositeComponentDefinition) componentDefinitions[0];
    assertThat(compositeDef.getName()).isEqualTo("aop:config");

    ComponentDefinition[] nestedComponentDefs = compositeDef.getNestedComponents();
    assertThat(nestedComponentDefs).as("Incorrect number of inner components").hasSize(2);
    AspectComponentDefinition acd = null;
    for (ComponentDefinition componentDefinition : nestedComponentDefs) {
      if (componentDefinition instanceof AspectComponentDefinition) {
        acd = (AspectComponentDefinition) componentDefinition;
        break;
      }
    }

    assertThat(acd).as("AspectComponentDefinition not found").isNotNull();
    BeanDefinition[] beanDefinitions = acd.getBeanDefinitions();
    assertThat(beanDefinitions).hasSize(5);
    BeanReference[] beanReferences = acd.getBeanReferences();
    assertThat(beanReferences).hasSize(6);

    Set<String> expectedReferences = new HashSet<>();
    expectedReferences.add("pc");
    expectedReferences.add("countingAdvice");
    for (BeanReference beanReference : beanReferences) {
      expectedReferences.remove(beanReference.getBeanName());
    }
    assertThat(expectedReferences).as("Incorrect references found").isEmpty();

    Arrays.stream(componentDefinitions).skip(1).forEach(definition ->
            assertThat(definition).isInstanceOf(BeanComponentDefinition.class));

    ComponentDefinition[] nestedComponentDefs2 = acd.getNestedComponents();
    assertThat(nestedComponentDefs2).as("Inner PointcutComponentDefinition not found").hasSize(1);
    assertThat(nestedComponentDefs2[0]).isInstanceOf(PointcutComponentDefinition.class);
    PointcutComponentDefinition pcd = (PointcutComponentDefinition) nestedComponentDefs2[0];
    assertThat(pcd.getBeanDefinitions()).as("Incorrect number of BeanDefinitions").hasSize(1);
  }

}
