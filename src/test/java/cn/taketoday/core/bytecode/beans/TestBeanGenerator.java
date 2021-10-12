/*
 * Copyright 2003 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.core.bytecode.beans;

import org.junit.jupiter.api.Test;

import java.beans.PropertyDescriptor;

import cn.taketoday.core.bytecode.core.CglibReflectUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Juozas Baliuka, Chris Nokleberg
 */
public class TestBeanGenerator {
  @Test
  public void testSimple() throws Exception {
    BeanGenerator bg = new BeanGenerator();
    bg.addProperty("sin", Double.TYPE);
    Object bean = bg.create();

    PropertyDescriptor[] pds = CglibReflectUtils.getBeanProperties(bean.getClass());
    assertEquals(1, pds.length);
    assertEquals("sin", pds[0].getName());
    assertEquals(pds[0].getPropertyType(), Double.TYPE);
  }

  @Test
  public void testSuperclass() throws Exception {
    BeanGenerator bg = new BeanGenerator();
    bg.setSuperclass(MA.class);
    bg.addProperty("sin", Double.TYPE);
    Object bean = bg.create();

    assertTrue(bean instanceof MA);
    assertTrue(BeanMap.create(bean).keySet().contains("sin"));
  }

}
