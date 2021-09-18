/*
 * Copyright 2003,2004 The Apache Software Foundation
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
package cn.taketoday.cglib.core;

import cn.taketoday.asm.Opcodes;
import cn.taketoday.asm.Type;
import cn.taketoday.asm.commons.MethodSignature;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * @author TODAY <br>
 * 2019-09-03 14:19
 */
public abstract class TypeUtils {

  public static Type[] add(Type[] types, Type extra) {
    return add(types, extra, false);
  }

  public static Type[] add(Type[] types, Type extra, boolean justAdd) {

    if (ObjectUtils.isEmpty(types)) {
      return new Type[] { extra };
    }

    if (!justAdd && CollectionUtils.contains(types, extra)) {
      return types;
    }
    final Type[] copy = new Type[types.length + 1];
    System.arraycopy(types, 0, copy, 0, types.length);
    copy[types.length] = extra;
    return copy;
  }

  public static Type[] add(Type[] t1, Type... t2) {
    if (ObjectUtils.isEmpty(t2)) {
      return t1;
    }
    // TODO: set semantics?
    Type[] all = new Type[t1.length + t2.length];
    System.arraycopy(t1, 0, all, 0, t1.length);
    System.arraycopy(t2, 0, all, t1.length, t2.length);
    return all;
  }

  public static Type fromInternalName(String name) {
    // TODO; primitives?
    return Type.fromDescriptor('L' + name + ';');
  }

  public static Type[] fromInternalNames(String[] names) {
    if (names == null) {
      return null;
    }
    Type[] types = new Type[names.length];
    for (int i = 0; i < names.length; i++) {
      types[i] = fromInternalName(names[i]);
    }
    return types;
  }

  public static boolean isConstructor(MethodInfo method) {
    return method.getSignature().getName().equals(MethodSignature.CONSTRUCTOR_NAME);
  }

  public static Type[] getTypes(Class<?>[] classes) {
    if (classes == null) {
      return null;
    }
    Type[] types = new Type[classes.length];
    for (int i = 0; i < classes.length; i++) {
      types[i] = Type.fromClass(classes[i]);
    }
    return types;
  }

  public static int iconst(int value) {
    switch (value) //@off
        { 
            case -1 :   return Opcodes.ICONST_M1;
            case 0 :    return Opcodes.ICONST_0;
            case 1 :    return Opcodes.ICONST_1;
            case 2 :    return Opcodes.ICONST_2;
            case 3 :    return Opcodes.ICONST_3;
            case 4 :    return Opcodes.ICONST_4;
            case 5 :    return Opcodes.ICONST_5;
            default:
              return -1; // error @on
        }
  }

  public static int lconst(long value) {
    if (value == 0L) {
      return Opcodes.LCONST_0;
    }
    if (value == 1L) {
      return Opcodes.LCONST_1;
    }
    return -1; // error
  }

  public static int fconst(float value) {
    if (value == 0f) {
      return Opcodes.FCONST_0;
    }
    if (value == 1f) {
      return Opcodes.FCONST_1;
    }
    if (value == 2f) {
      return Opcodes.FCONST_2;
    }
    return -1; // error
  }

  public static int dconst(double value) {
    if (value == 0d) {
      return Opcodes.DCONST_0;
    }
    if (value == 1d) {
      return Opcodes.DCONST_1;
    }
    return -1; // error
  }

  public static int newArray(Type type) {
    switch (type.getSort()) { //@off
            case Type.BYTE :    return Opcodes.T_BYTE;
            case Type.CHAR :    return Opcodes.T_CHAR;
            case Type.DOUBLE :  return Opcodes.T_DOUBLE;
            case Type.FLOAT :   return Opcodes.T_FLOAT;
            case Type.INT :     return Opcodes.T_INT;
            case Type.LONG :    return Opcodes.T_LONG;
            case Type.SHORT :   return Opcodes.T_SHORT;
            case Type.BOOLEAN : return Opcodes.T_BOOLEAN;
            default:
              return -1; // error @on
    }
  }

  public static String escapeType(String s) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0, len = s.length(); i < len; i++) {
      char c = s.charAt(i);
      switch (c) {
        case '$':
          sb.append("$24");
          break;
        case '.':
          sb.append("$2E");
          break;
        case '[':
          sb.append("$5B");
          break;
        case ';':
          sb.append("$3B");
          break;
        case '(':
          sb.append("$28");
          break;
        case ')':
          sb.append("$29");
          break;
        case '/':
          sb.append("$2F");
          break;
        default:
          sb.append(c);
          break;
      }
    }
    return sb.toString();
  }

}
