/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.core.bytecode.beans;

import org.junit.jupiter.api.Test;

import cn.taketoday.core.bytecode.core.Converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author baliuka
 */
public class TestBeanCopier {

  @Test
  public void testSimple() {
    BeanCopier copier = BeanCopier.create(MA.class, MA.class, false);
    MA bean1 = new MA();
    bean1.setIntP(42);
    MA bean2 = new MA();
    copier.copy(bean1, bean2, null);
    assertEquals(42, bean2.getIntP());
  }

  @Test
  public void testOneWay() {
    BeanCopier copier = BeanCopier.create(SampleGetter.class, SampleSetter.class, false);
    SampleGetter sampleGetter = new SampleGetter();
    sampleGetter.foo = 42;
    SampleSetter sampleSetter = new SampleSetter();
    copier.copy(sampleGetter, sampleSetter, null);
    assertEquals(42, sampleSetter.foo);
  }

  @Test
  public void testConvert() {
    BeanCopier copier = BeanCopier.create(MA.class, MA.class, true);
    MA bean1 = new MA();
    bean1.setIntP(42);
    MA bean2 = new MA();
    copier.copy(bean1, bean2, new Converter() {
      public Object convert(Object value, Class target, Object context) {
        if (target.equals(Integer.TYPE)) {
          return ((Number) value).intValue() + 1;
        }
        return value;
      }
    });
    assertEquals(43, bean2.getIntP());
  }

}
