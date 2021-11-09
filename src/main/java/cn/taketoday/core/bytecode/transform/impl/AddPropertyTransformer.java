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

import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.core.EmitUtils;
import cn.taketoday.core.bytecode.transform.ClassEmitterTransformer;

import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * @author TODAY
 */
public class AddPropertyTransformer extends ClassEmitterTransformer {
  private final String[] names;
  private final Type[] types;

  public AddPropertyTransformer(Map<String, Type> props) {
    int size = props.size();
    names = props.keySet().toArray(new String[size]);
    types = new Type[size];
    for (int i = 0; i < size; i++) {
      types[i] = props.get(names[i]);
    }
  }

  public AddPropertyTransformer(String[] names, Type[] types) {
    this.names = names;
    this.types = types;
  }

  @Override
  public void endClass() {
    if (!Modifier.isAbstract(getAccess())) {
      EmitUtils.addProperties(this, names, types);
    }
    super.endClass();
  }
}
