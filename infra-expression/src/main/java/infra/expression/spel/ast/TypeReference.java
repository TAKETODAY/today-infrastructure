/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.expression.spel.ast;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.Locale;

import infra.bytecode.MethodVisitor;
import infra.bytecode.Type;
import infra.expression.spel.CodeFlow;
import infra.expression.EvaluationException;
import infra.expression.TypedValue;
import infra.expression.spel.ExpressionState;
import infra.lang.Assert;

/**
 * Represents a reference to a type, for example
 * {@code "T(String)" or "T(com.somewhere.Foo)"}.
 *
 * @author Andy Clement
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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
      TypeCode tc = TypeCode.valueOf(typeName.toUpperCase(Locale.ROOT));
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
    Assert.state(this.type != null, "No type available");
    if (this.type.isPrimitive()) {
      if (this.type == boolean.class) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean", "TYPE", "Ljava/lang/Class;");
      }
      else if (this.type == byte.class) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/Byte", "TYPE", "Ljava/lang/Class;");
      }
      else if (this.type == char.class) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/Character", "TYPE", "Ljava/lang/Class;");
      }
      else if (this.type == double.class) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/Double", "TYPE", "Ljava/lang/Class;");
      }
      else if (this.type == float.class) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/Float", "TYPE", "Ljava/lang/Class;");
      }
      else if (this.type == int.class) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/Integer", "TYPE", "Ljava/lang/Class;");
      }
      else if (this.type == long.class) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/Long", "TYPE", "Ljava/lang/Class;");
      }
      else if (this.type == short.class) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/Short", "TYPE", "Ljava/lang/Class;");
      }
    }
    else {
      mv.visitLdcInsn(Type.forClass(this.type));
    }
    cf.pushDescriptor(this.exitTypeDescriptor);
  }

}
