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

package cn.taketoday.expression.spel.support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.core.CodeFlow;
import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.IndexAccessor;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.CompilableIndexAccessor;
import cn.taketoday.expression.spel.SpelNode;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * A flexible {@link cn.taketoday.expression.IndexAccessor IndexAccessor}
 * that uses reflection to read from and optionally write to an indexed structure
 * of a target object.
 *
 * <p>The indexed structure can be accessed through a public read-method (when
 * being read) or a public write-method (when being written). The relationship
 * between the read-method and write-method is based on a convention that is
 * applicable for typical implementations of indexed structures. See the example
 * below for details.
 *
 * <p>{@code ReflectiveIndexAccessor} also implements {@link CompilableIndexAccessor}
 * in order to support compilation to bytecode for read access. Note, however,
 * that the configured read-method must be invokable via a public class or public
 * interface for compilation to succeed.
 *
 * <h3>Example</h3>
 *
 * <p>The {@code FruitMap} class (the {@code targetType}) represents a structure
 * that is indexed via the {@code Color} enum (the {@code indexType}). The name
 * of the read-method is {@code "getFruit"}, and that method returns a
 * {@code String} (the {@code indexedValueType}). The name of the write-method
 * is {@code "setFruit"}, and that method accepts a {@code Color} enum (the
 * {@code indexType}) and a {@code String} (the {@code indexedValueType} which
 * must match the return type of the read-method).
 *
 * <p>A read-only {@code IndexAccessor} for {@code FruitMap} can be created via
 * {@code new ReflectiveIndexAccessor(FruitMap.class, Color.class, "getFruit")}.
 * With that accessor registered and a {@code FruitMap} registered as a variable
 * named {@code #fruitMap}, the SpEL expression {@code #fruitMap[T(example.Color).RED]}
 * will evaluate to {@code "cherry"}.
 *
 * <p>A read-write {@code IndexAccessor} for {@code FruitMap} can be created via
 * {@code new ReflectiveIndexAccessor(FruitMap.class, Color.class, "getFruit", "setFruit")}.
 * With that accessor registered and a {@code FruitMap} registered as a variable
 * named {@code #fruitMap}, the SpEL expression
 * {@code #fruitMap[T(example.Color).RED] = 'strawberry'} can be used to change
 * the fruit mapping for the color red from {@code "cherry"} to {@code "strawberry"}.
 *
 * <pre class="code">
 * package example;
 *
 * public enum Color {
 *     RED, ORANGE, YELLOW
 * }</pre>
 *
 * <pre class="code">
 * public class FruitMap {
 *
 *     private final Map&lt;Color, String&gt; map = new HashMap&lt;&gt;();
 *
 *     public FruitMap() {
 *         this.map.put(Color.RED, "cherry");
 *         this.map.put(Color.ORANGE, "orange");
 *         this.map.put(Color.YELLOW, "banana");
 *     }
 *
 *     public String getFruit(Color color) {
 *         return this.map.get(color);
 *     }
 *
 *     public void setFruit(Color color, String fruit) {
 *         this.map.put(color, fruit);
 *     }
 * }</pre>
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see IndexAccessor
 * @see CompilableIndexAccessor
 * @see StandardEvaluationContext
 * @see SimpleEvaluationContext
 * @since 5.0
 */
public class ReflectiveIndexAccessor implements CompilableIndexAccessor {

  private final Class<?> targetType;

  private final Class<?> indexType;

  private final Method readMethod;

  private final Method readMethodToInvoke;

  @Nullable
  private final Method writeMethodToInvoke;

  /**
   * Construct a new {@code ReflectiveIndexAccessor} for read-only access.
   * <p>See {@linkplain ReflectiveIndexAccessor class-level documentation} for
   * further details and an example.
   *
   * @param targetType the type of indexed structure which serves as the target
   * of index operations
   * @param indexType the type of index used to read from the indexed structure
   * @param readMethodName the name of the method used to read from the indexed
   * structure
   */
  public ReflectiveIndexAccessor(Class<?> targetType, Class<?> indexType, String readMethodName) {
    this(targetType, indexType, readMethodName, null);
  }

  /**
   * Construct a new {@code ReflectiveIndexAccessor} for read-write access.
   * <p>See {@linkplain ReflectiveIndexAccessor class-level documentation} for
   * further details and an example.
   *
   * @param targetType the type of indexed structure which serves as the target
   * of index operations
   * @param indexType the type of index used to read from or write to the indexed
   * structure
   * @param readMethodName the name of the method used to read from the indexed
   * structure
   * @param writeMethodName the name of the method used to write to the indexed
   * structure, or {@code null} if writing is not supported
   */
  public ReflectiveIndexAccessor(Class<?> targetType, Class<?> indexType, String readMethodName,
          @Nullable String writeMethodName) {

    this.targetType = targetType;
    this.indexType = indexType;

    try {
      this.readMethod = targetType.getMethod(readMethodName, indexType);
    }
    catch (Exception ex) {
      throw new IllegalArgumentException("Failed to find public read-method '%s(%s)' in class '%s'."
              .formatted(readMethodName, getName(indexType), getName(targetType)));
    }

    this.readMethodToInvoke = ReflectionUtils.getPubliclyAccessibleMethodIfPossible(this.readMethod, targetType);
    ReflectionUtils.makeAccessible(this.readMethodToInvoke);

    if (writeMethodName != null) {
      Class<?> indexedValueType = this.readMethod.getReturnType();
      Method writeMethod;
      try {
        writeMethod = targetType.getMethod(writeMethodName, indexType, indexedValueType);
      }
      catch (Exception ex) {
        throw new IllegalArgumentException("Failed to find public write-method '%s(%s, %s)' in class '%s'."
                .formatted(writeMethodName, getName(indexType), getName(indexedValueType),
                        getName(targetType)));
      }
      this.writeMethodToInvoke = ReflectionUtils.getPubliclyAccessibleMethodIfPossible(writeMethod, targetType);
      ReflectionUtils.makeAccessible(this.writeMethodToInvoke);
    }
    else {
      this.writeMethodToInvoke = null;
    }
  }

  /**
   * Return an array containing the {@code targetType} configured via the constructor.
   */
  @Override
  public Class<?>[] getSpecificTargetClasses() {
    return new Class<?>[] { this.targetType };
  }

  /**
   * Return {@code true} if the supplied {@code target} and {@code index} can
   * be assigned to the {@code targetType} and {@code indexType} configured
   * via the constructor.
   * <p>Considers primitive wrapper classes as assignable to the corresponding
   * primitive types.
   */
  @Override
  public boolean canRead(EvaluationContext context, Object target, Object index) {
    return (ClassUtils.isAssignableValue(this.targetType, target) &&
            ClassUtils.isAssignableValue(this.indexType, index));
  }

  /**
   * Invoke the configured read-method via reflection and return the result
   * wrapped in a {@link TypedValue}.
   */
  @Override
  public TypedValue read(EvaluationContext context, Object target, Object index) {
    Object value = ReflectionUtils.invokeMethod(this.readMethodToInvoke, target, index);
    return new TypedValue(value);
  }

  /**
   * Return {@code true} if a write-method has been configured and
   * {@link #canRead} returns {@code true} for the same arguments.
   */
  @Override
  public boolean canWrite(EvaluationContext context, Object target, Object index) {
    return (this.writeMethodToInvoke != null && canRead(context, target, index));
  }

  /**
   * Invoke the configured write-method via reflection.
   * <p>Should only be invoked if {@link #canWrite} returns {@code true} for the
   * same arguments.
   */
  @Override
  public void write(EvaluationContext context, Object target, Object index, @Nullable Object newValue) {
    Assert.state(this.writeMethodToInvoke != null, "Write-method cannot be null");
    ReflectionUtils.invokeMethod(this.writeMethodToInvoke, target, index, newValue);
  }

  @Override
  public boolean isCompilable() {
    return true;
  }

  /**
   * Get the return type of the configured read-method.
   */
  @Override
  public Class<?> getIndexedValueType() {
    return this.readMethod.getReturnType();
  }

  @Override
  public void generateCode(SpelNode index, MethodVisitor mv, CodeFlow cf) {
    // Find the public declaring class.
    Class<?> publicDeclaringClass = this.readMethodToInvoke.getDeclaringClass();
    if (!Modifier.isPublic(publicDeclaringClass.getModifiers())) {
      throw new IllegalStateException("Failed to find public declaring class for read-method: " + this.readMethod);
    }

    String classDesc = publicDeclaringClass.getName().replace('.', '/');

    // Ensure the current object on the stack is the required type.
    String lastDesc = cf.lastDescriptor();
    if (lastDesc == null || !classDesc.equals(lastDesc.substring(1))) {
      mv.visitTypeInsn(CHECKCAST, classDesc);
    }

    // Push the index onto the stack.
    cf.generateCodeForArgument(mv, index, this.indexType);

    // Invoke the read-method.
    String methodName = this.readMethod.getName();
    String methodDescr = CodeFlow.createSignatureDescriptor(this.readMethod);
    boolean isInterface = publicDeclaringClass.isInterface();
    int opcode = (isInterface ? INVOKEINTERFACE : INVOKEVIRTUAL);
    mv.visitMethodInsn(opcode, classDesc, methodName, methodDescr, isInterface);
  }

  private static String getName(Class<?> clazz) {
    String canonicalName = clazz.getCanonicalName();
    return (canonicalName != null ? canonicalName : clazz.getName());
  }

}
