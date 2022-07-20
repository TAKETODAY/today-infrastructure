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

package cn.taketoday.expression.spel.ast;

import java.lang.reflect.Array;

import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.core.bytecode.core.CodeFlow;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Represents a reference to a type, for example
 * {@code "T(String)" or "T(com.somewhere.Foo)"}.
 *
 * @author Andy Clement
 */
public class TypeReference extends SpelNodeImpl {

  private final int dimensions;

  @Nullable
  private transient Class<?> type;

  public TypeReference(int startPos, int endPos, SpelNodeImpl qualifiedId) {
    this(startPos, endPos, qualifiedId, 0);
  }

  public TypeReference(int startPos, int endPos, SpelNodeImpl qualifiedId, int dims) {
    super(startPos, endPos, qualifiedId);
    this.dimensions = dims;
  }

  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    // TODO possible optimization here if we cache the discovered type reference, but can we do that?
    String typeName = (String) this.children[0].getValueInternal(state).getValue();
    Assert.state(typeName != null, "No type name");
    if (!typeName.contains(".") && Character.isLowerCase(typeName.charAt(0))) {
      TypeCode tc = TypeCode.valueOf(typeName.toUpperCase());
      if (tc != TypeCode.OBJECT) {
        // It is a primitive type
        Class<?> clazz = makeArrayIfNecessary(tc.getType());
        this.exitTypeDescriptor = "Ljava/lang/Class";
        this.type = clazz;
        return new TypedValue(clazz);
      }
    }
    Class<?> clazz = state.findType(typeName);
    clazz = makeArrayIfNecessary(clazz);
    this.exitTypeDescriptor = "Ljava/lang/Class";
    this.type = clazz;
    return new TypedValue(clazz);
  }

  private Class<?> makeArrayIfNecessary(Class<?> clazz) {
    if (this.dimensions != 0) {
      for (int i = 0; i < this.dimensions; i++) {
        Object array = Array.newInstance(clazz, 0);
        clazz = array.getClass();
      }
    }
    return clazz;
  }

  @Override
  public String toStringAST() {
    StringBuilder sb = new StringBuilder("T(");
    sb.append(getChild(0).toStringAST());
    sb.append("[]".repeat(Math.max(0, this.dimensions)));
    sb.append(')');
    return sb.toString();
  }

  @Override
  public boolean isCompilable() {
    return (this.exitTypeDescriptor != null);
  }

  @Override
  public void generateCode(MethodVisitor mv, CodeFlow cf) {
    // TODO Future optimization - if followed by a static method call, skip generating code here
    Class<?> type = this.type;
    Assert.state(type != null, "No type available");
    if (type.isPrimitive()) {
      if (type == Boolean.TYPE) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean", "TYPE", "Ljava/lang/Class;");
      }
      else if (type == Byte.TYPE) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/Byte", "TYPE", "Ljava/lang/Class;");
      }
      else if (type == Character.TYPE) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/Character", "TYPE", "Ljava/lang/Class;");
      }
      else if (type == Double.TYPE) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/Double", "TYPE", "Ljava/lang/Class;");
      }
      else if (type == Float.TYPE) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/Float", "TYPE", "Ljava/lang/Class;");
      }
      else if (type == Integer.TYPE) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/Integer", "TYPE", "Ljava/lang/Class;");
      }
      else if (type == Long.TYPE) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/Long", "TYPE", "Ljava/lang/Class;");
      }
      else if (type == Short.TYPE) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/Short", "TYPE", "Ljava/lang/Class;");
      }
    }
    else {
      mv.visitLdcInsn(Type.fromClass(this.type));
    }
    cf.pushDescriptor(this.exitTypeDescriptor);
  }

}
