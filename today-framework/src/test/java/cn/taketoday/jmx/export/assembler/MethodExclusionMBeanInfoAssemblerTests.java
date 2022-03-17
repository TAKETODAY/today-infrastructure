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

import java.lang.reflect.Method;
import java.util.Properties;

import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;

import cn.taketoday.jmx.JmxTestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Rick Evans
 * @author Chris Beams
 */
public class MethodExclusionMBeanInfoAssemblerTests extends AbstractJmxAssemblerTests {

  private static final String OBJECT_NAME = "bean:name=testBean5";

  @Override
  protected String getObjectName() {
    return OBJECT_NAME;
  }

  @Override
  protected int getExpectedOperationCount() {
    return 9;
  }

  @Override
  protected int getExpectedAttributeCount() {
    return 4;
  }

  @Override
  protected String getApplicationContextPath() {
    return "cn/taketoday/jmx/export/assembler/methodExclusionAssembler.xml";
  }

  @Override
  protected MBeanInfoAssembler getAssembler() {
    MethodExclusionMBeanInfoAssembler assembler = new MethodExclusionMBeanInfoAssembler();
    assembler.setIgnoredMethods(new String[] { "dontExposeMe", "setSuperman" });
    return assembler;
  }

  @Test
  public void testSupermanIsReadOnly() throws Exception {
    ModelMBeanInfo info = getMBeanInfoFromAssembler();
    ModelMBeanAttributeInfo attr = info.getAttribute("Superman");

    assertThat(attr.isReadable()).isTrue();
    assertThat(attr.isWritable()).isFalse();
  }

  /*
   * https://opensource.atlassian.com/projects/spring/browse/SPR-2754
   */
  @Test
  public void testIsNotIgnoredDoesntIgnoreUnspecifiedBeanMethods() throws Exception {
    final String beanKey = "myTestBean";
    MethodExclusionMBeanInfoAssembler assembler = new MethodExclusionMBeanInfoAssembler();
    Properties ignored = new Properties();
    ignored.setProperty(beanKey, "dontExposeMe,setSuperman");
    assembler.setIgnoredMethodMappings(ignored);
    Method method = JmxTestBean.class.getMethod("dontExposeMe");
    assertThat(assembler.isNotIgnored(method, beanKey)).isFalse();
    // this bean does not have any ignored methods on it, so must obviously not be ignored...
    assertThat(assembler.isNotIgnored(method, "someOtherBeanKey")).isTrue();
  }

}
