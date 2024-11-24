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

package infra.jmx.export.assembler;

import org.junit.jupiter.api.Test;

import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Chris Beams
 */
public class InterfaceBasedMBeanInfoAssemblerCustomTests extends AbstractJmxAssemblerTests {

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
    InterfaceBasedMBeanInfoAssembler assembler = new InterfaceBasedMBeanInfoAssembler();
    assembler.setManagedInterfaces(new Class<?>[] { ICustomJmxBean.class });
    return assembler;
  }

  @Test
  public void testGetAgeIsReadOnly() throws Exception {
    ModelMBeanInfo info = getMBeanInfoFromAssembler();
    ModelMBeanAttributeInfo attr = info.getAttribute(AGE_ATTRIBUTE);

    assertThat(attr.isReadable()).isTrue();
    assertThat(attr.isWritable()).isFalse();
  }

  @Override
  protected String getApplicationContextPath() {
    return "infra/jmx/export/assembler/interfaceAssemblerCustom.xml";
  }

}
