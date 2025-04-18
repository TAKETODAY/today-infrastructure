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

package infra.expression.spel;

import java.util.Map;

import infra.bytecode.MethodVisitor;
import infra.bytecode.core.CodeFlow;
import infra.expression.AccessException;
import infra.expression.EvaluationContext;
import infra.expression.TypedValue;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * This is a local COPY of {@link infra.context.expression.MapAccessor}.
 *
 * @author Juergen Hoeller
 * @author Andy Clement
 */
public class CompilableMapAccessor implements CompilablePropertyAccessor {

  private final boolean allowWrite;

  /**
   * Create a new map accessor for reading as well as writing.
   *
   * @see #CompilableMapAccessor(boolean)
   */
  public CompilableMapAccessor() {
    this(true);
  }

  /**
   * Create a new map accessor for reading and possibly also writing.
   *
   * @param allowWrite whether to allow write operations on a target instance
   * @see #canWrite
   */
  public CompilableMapAccessor(boolean allowWrite) {
    this.allowWrite = allowWrite;
  }

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
    return (this.allowWrite && target instanceof Map);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue)
          throws AccessException {

    Assert.state(target instanceof Map, "Target must be of type Map");
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
