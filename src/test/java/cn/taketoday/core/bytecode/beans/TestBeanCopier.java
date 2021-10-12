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
          return new Integer(((Number) value).intValue() + 1);
        }
        return value;
      }
    });
    assertEquals(43, bean2.getIntP());
  }

}
