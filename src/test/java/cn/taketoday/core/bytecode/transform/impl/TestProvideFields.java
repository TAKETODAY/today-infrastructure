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

package cn.taketoday.core.bytecode.transform.impl;



import org.junit.jupiter.api.Test;

import cn.taketoday.core.bytecode.transform.AbstractTransformTest;
import cn.taketoday.core.bytecode.transform.ClassTransformer;
import cn.taketoday.core.bytecode.transform.ClassTransformerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author baliuka
 */
public class TestProvideFields extends AbstractTransformTest {

  String field = "test";

  @Test
  public void test() throws Exception {
    Object o = new TestProvideFields().transform().newInstance();
    FieldProvider provider = (FieldProvider) o;
    assertEquals(field, provider.getField("field"));
    String value = "tst2";
    provider.setField("field", value);
    assertEquals(field, value);
  }

  protected ClassTransformerFactory getTransformer() throws Exception {

    return new ClassTransformerFactory() {

      public ClassTransformer newTransformer() {
        return new FieldProviderTransformer();
      }
    };
  }


}
