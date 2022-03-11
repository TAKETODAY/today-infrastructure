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

package cn.taketoday.context.expression;

import java.util.Map;

import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.expression.AccessException;
import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.CodeFlow;
import cn.taketoday.expression.spel.CompilablePropertyAccessor;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * EL property accessor that knows how to traverse the keys
 * of a standard {@link Map}.
 *
 * @author Juergen Hoeller
 * @author Andy Clement
 * @since 4.0
 */
public class MapAccessor implements CompilablePropertyAccessor {

  @Override
  public Class<?>[] getSpecificTargetClasses() {
    return new Class<?>[] { Map.class };
  }

  @Override
  public boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
    return (target instanceof Map<?, ?> map && map.containsKey(name));
  }

  @Override
  public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
    Assert.state(target instanceof Map, "Target must be of type Map");
    Map<?, ?> map = (Map<?, ?>) target;
    Object value = map.get(name);
    if (value == null && !map.containsKey(name)) {
      throw new MapAccessException(name);
    }
    return new TypedValue(value);
  }

  @Override
  public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
    return true;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue)
          throws AccessException {

    Assert.state(target instanceof Map, "Target must be a Map");
    Map<Object, Object> map = (Map<Object, Object>) target;
    map.put(name, newValue);
  }

  @Override
  public boolean isCompilable() {
    return true;
  }

  @Override
  public Class<?> getPropertyType() {
    return Object.class;
  }

  @Override
  public void generateCode(String propertyName, MethodVisitor mv, CodeFlow cf) {
    String descriptor = cf.lastDescriptor();
    if (descriptor == null || !descriptor.equals("Ljava/util/Map")) {
      if (descriptor == null) {
        cf.loadTarget(mv);
      }
      CodeFlow.insertCheckCast(mv, "Ljava/util/Map");
    }
    mv.visitLdcInsn(propertyName);
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
  }

  /**
   * Exception thrown from {@code read} in order to reset a cached
   * PropertyAccessor, allowing other accessors to have a try.
   */
  @SuppressWarnings("serial")
  private static class MapAccessException extends AccessException {

    private final String key;

    public MapAccessException(String key) {
      super("");
      this.key = key;
    }

    @Override
    public String getMessage() {
      return "Map does not contain a value for key '" + this.key + "'";
    }
  }

}
