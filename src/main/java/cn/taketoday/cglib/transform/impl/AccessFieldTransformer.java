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
package cn.taketoday.cglib.transform.impl;

import cn.taketoday.asm.Opcodes;
import cn.taketoday.asm.Type;
import cn.taketoday.asm.commons.MethodSignature;
import cn.taketoday.cglib.core.CodeEmitter;
import cn.taketoday.cglib.transform.ClassEmitterTransformer;
import cn.taketoday.core.Constant;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY <br>
 * 2019-09-04 19:57
 */
public class AccessFieldTransformer extends ClassEmitterTransformer {

  private final Callback callback;

  public AccessFieldTransformer(Callback callback) {
    this.callback = callback;
  }

  public interface Callback {
    String getPropertyName(Type owner, String fieldName);
  }

  public void declare_field(int access, final String name, Type type, Object value) {
    super.declare_field(access, name, type, value);

    String property = StringUtils.capitalize(callback.getPropertyName(getClassType(), name));
    if (property != null) {
      CodeEmitter e;
      e = beginMethod(Opcodes.ACC_PUBLIC, new MethodSignature(type, "get" + property, Constant.TYPES_EMPTY_ARRAY));
      e.load_this();
      e.getField(name);
      e.return_value();
      e.end_method();

      e = beginMethod(Opcodes.ACC_PUBLIC, new MethodSignature(Type.VOID_TYPE, "set" + property, type));
      e.load_this();
      e.load_arg(0);
      e.putField(name);
      e.return_value();
      e.end_method();
    }
  }
}
