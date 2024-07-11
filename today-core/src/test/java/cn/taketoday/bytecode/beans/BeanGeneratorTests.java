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
package cn.taketoday.bytecode.beans;

import org.junit.jupiter.api.Test;

import java.beans.PropertyDescriptor;

import cn.taketoday.bytecode.core.CglibReflectUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Juozas Baliuka, Chris Nokleberg
 */
class BeanGeneratorTests {

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
