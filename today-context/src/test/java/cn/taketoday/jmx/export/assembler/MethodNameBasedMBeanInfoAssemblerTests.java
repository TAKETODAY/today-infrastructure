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

package cn.taketoday.jmx.export.assembler;

import org.junit.jupiter.api.Test;

import javax.management.MBeanOperationInfo;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author David Boden
 * @author Chris Beams
 */
public class MethodNameBasedMBeanInfoAssemblerTests extends AbstractJmxAssemblerTests {

  protected static final String OBJECT_NAME = "bean:name=testBean5";

  @Override
  protected String getObjectName() {
    return OBJECT_NAME;
  }

  @Override
  protected int getExpectedOperationCount() {
    return 5;
  }

  @Override
  protected int getExpectedAttributeCount() {
    return 2;
  }

  @Override
  protected MBeanInfoAssembler getAssembler() {
    MethodNameBasedMBeanInfoAssembler assembler = new MethodNameBasedMBeanInfoAssembler();
    assembler.setManagedMethods("add", "myOperation", "getName", "setName", "getAge");
    return assembler;
  }

  @Test
  public void testGetAgeIsReadOnly() throws Exception {
    ModelMBeanInfo info = getMBeanInfoFromAssembler();
    ModelMBeanAttributeInfo attr = info.getAttribute(AGE_ATTRIBUTE);

    assertThat(attr.isReadable()).isTrue();
    assertThat(attr.isWritable()).isFalse();
  }

  @Test
  public void testSetNameParameterIsNamed() throws Exception {
    ModelMBeanInfo info = getMBeanInfoFromAssembler();

    MBeanOperationInfo operationSetAge = info.getOperation("setName");
    assertThat(operationSetAge.getSignature()[0].getName()).isEqualTo("name");
  }

  @Override
  protected String getApplicationContextPath() {
    return "cn/taketoday/jmx/export/assembler/methodNameAssembler.xml";
  }

}
