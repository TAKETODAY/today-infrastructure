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

package cn.taketoday.jmx.export.annotation;

import org.junit.jupiter.api.Test;

import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import cn.taketoday.jmx.IJmxTestBean;
import cn.taketoday.jmx.export.assembler.AbstractMetadataAssemblerTests;
import cn.taketoday.jmx.export.metadata.JmxAttributeSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Chris Beams
 */
public class AnnotationMetadataAssemblerTests extends AbstractMetadataAssemblerTests {

  private static final String OBJECT_NAME = "bean:name=testBean4";

  @Test
  public void testAttributeFromInterface() throws Exception {
    ModelMBeanInfo inf = getMBeanInfoFromAssembler();
    ModelMBeanAttributeInfo attr = inf.getAttribute("Colour");
    assertThat(attr.isWritable()).as("The name attribute should be writable").isTrue();
    assertThat(attr.isReadable()).as("The name attribute should be readable").isTrue();
  }

  @Test
  public void testOperationFromInterface() throws Exception {
    ModelMBeanInfo inf = getMBeanInfoFromAssembler();
    ModelMBeanOperationInfo op = inf.getOperation("fromInterface");
    assertThat(op).isNotNull();
  }

  @Test
  public void testOperationOnGetter() throws Exception {
    ModelMBeanInfo inf = getMBeanInfoFromAssembler();
    ModelMBeanOperationInfo op = inf.getOperation("getExpensiveToCalculate");
    assertThat(op).isNotNull();
  }

  @Test
  public void testRegistrationOnInterface() throws Exception {
    Object bean = getContext().getBean("testInterfaceBean");
    ModelMBeanInfo inf = getAssembler().getMBeanInfo(bean, "bean:name=interfaceTestBean");
    assertThat(inf).isNotNull();
    assertThat(inf.getDescription()).isEqualTo("My Managed Bean");

    ModelMBeanOperationInfo op = inf.getOperation("foo");
    assertThat(op).as("foo operation not exposed").isNotNull();
    assertThat(op.getDescription()).isEqualTo("invoke foo");

    assertThat(inf.getOperation("doNotExpose")).as("doNotExpose operation should not be exposed").isNull();

    ModelMBeanAttributeInfo attr = inf.getAttribute("Bar");
    assertThat(attr).as("bar attribute not exposed").isNotNull();
    assertThat(attr.getDescription()).isEqualTo("Bar description");

    ModelMBeanAttributeInfo attr2 = inf.getAttribute("CacheEntries");
    assertThat(attr2).as("cacheEntries attribute not exposed").isNotNull();
    assertThat(attr2.getDescriptor().getFieldValue("metricType")).as("Metric Type should be COUNTER").isEqualTo("COUNTER");
  }

  @Override
  protected JmxAttributeSource getAttributeSource() {
    return new AnnotationJmxAttributeSource();
  }

  @Override
  protected String getObjectName() {
    return OBJECT_NAME;
  }

  @Override
  protected IJmxTestBean createJmxTestBean() {
    return new AnnotationTestSubBean();
  }

  @Override
  protected String getApplicationContextPath() {
    return "cn/taketoday/jmx/export/annotation/annotations.xml";
  }

  @Override
  protected int getExpectedAttributeCount() {
    return super.getExpectedAttributeCount() + 1;
  }

  @Override
  protected int getExpectedOperationCount() {
    return super.getExpectedOperationCount() + 4;
  }
}
