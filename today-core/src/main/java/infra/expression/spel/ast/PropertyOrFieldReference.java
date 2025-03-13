/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.expression.spel.ast;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import infra.bytecode.Label;
import infra.bytecode.MethodVisitor;
import infra.bytecode.core.CodeFlow;
import infra.core.TypeDescriptor;
import infra.expression.AccessException;
import infra.expression.EvaluationContext;
import infra.expression.EvaluationException;
import infra.expression.PropertyAccessor;
import infra.expression.TypedValue;
import infra.expression.spel.CompilablePropertyAccessor;
import infra.expression.spel.ExpressionState;
import infra.expression.spel.SpelEvaluationException;
import infra.expression.spel.SpelMessage;
import infra.expression.spel.support.ReflectivePropertyAccessor;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.ReflectionUtils;

/**
 * Represents a simple public property or field reference.
 *
 * <h3>Null-safe Navigation</h3>
 *
 * <p>Null-safe navigation is supported via the {@code '?.'} operator. For example,
 * {@code 'user?.name'} will evaluate to {@code null} if {@code user} is {@code null}
 * and will otherwise evaluate to the name of the user.
 * null-safe navigation also applies when accessing a property or field on an
 * {@link Optional} target. For example, if {@code user} is of type
 * {@code Optional<User>}, the expression {@code 'user?.name'} will evaluate to
 * {@code null} if {@code user} is {@code null} or {@link Optional#isEmpty() empty}
 * and will otherwise evaluate to the name of the user, effectively
 * {@code user.get().getName()} or {@code user.get().name}.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Clark Duplichien
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class PropertyOrFieldReference extends SpelNodeImpl {

  private final boolean nullSafe;

  private final String name;

  @Nullable
  private String originalPrimitiveExitTypeDescriptor;

  @Nullable
  private volatile PropertyAccessor cachedReadAccessor;

  @Nullable
  private volatile PropertyAccessor cachedWriteAccessor;

  public PropertyOrFieldReference(boolean nullSafe, String propertyOrFieldName, int startPos, int endPos) {
    super(startPos, endPos);
    this.nullSafe = nullSafe;
    this.name = propertyOrFieldName;
  }

  /**
   * Does this node represent a null-safe property or field reference?
   */
  @Override
  public boolean isNullSafe() {
    return this.nullSafe;
  }

  /**
   * Get the name of the referenced property or field.
   */
  public String getName() {
    return this.name;
  }

  @Override
  public ValueRef getValueRef(ExpressionState state) throws EvaluationException {
    return new AccessorValueRef(this, state.getActiveContextObject(), state.getEvaluationContext(),
            state.getConfiguration().isAutoGrowNullReferences());
  }

  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    TypedValue tv = getValueInternal(state.getActiveContextObject(), state.getEvaluationContext(),
            state.getConfiguration().isAutoGrowNullReferences());
    PropertyAccessor accessorToUse = this.cachedReadAccessor;
    if (accessorToUse instanceof CompilablePropertyAccessor compilablePropertyAccessor) {
      setExitTypeDescriptor(CodeFlow.toDescriptor(compilablePropertyAccessor.getPropertyType()));
    }
    return tv;
  }

  private TypedValue getValueInternal(TypedValue contextObject, EvaluationContext evalContext,
          boolean isAutoGrowNullReferences) throws EvaluationException {

    TypedValue result = readProperty(contextObject, evalContext, this.name);

    // Dynamically create the objects if the user has requested that optional behavior
    if (result.getValue() == null && isAutoGrowNullReferences
            && nextChildIs(Indexer.class, PropertyOrFieldReference.class)) {
      TypeDescriptor resultDescriptor = result.getTypeDescriptor();
      Assert.state(resultDescriptor != null, "No result type");
      // Create a new collection or map ready for the indexer
      if (List.class == resultDescriptor.getType()) {
        if (isWritableProperty(this.name, contextObject, evalContext)) {
          var newList = new ArrayList<>();
          writeProperty(contextObject, evalContext, this.name, newList);
          result = readProperty(contextObject, evalContext, this.name);
        }
      }
      else if (Map.class == resultDescriptor.getType()) {
        if (isWritableProperty(this.name, contextObject, evalContext)) {
          var newMap = new HashMap<>();
          writeProperty(contextObject, evalContext, this.name, newMap);
          result = readProperty(contextObject, evalContext, this.name);
        }
      }
      else {
        // 'simple' object
        try {
          if (isWritableProperty(this.name, contextObject, evalContext)) {
            Class<?> clazz = resultDescriptor.getType();
            Object newObject = ReflectionUtils.accessibleConstructor(clazz).newInstance();
            writeProperty(contextObject, evalContext, this.name, newObject);
            result = readProperty(contextObject, evalContext, this.name);
          }
        }
        catch (InvocationTargetException ex) {
          throw new SpelEvaluationException(getStartPosition(), ex.getTargetException(),
                  SpelMessage.UNABLE_TO_DYNAMICALLY_CREATE_OBJECT, resultDescriptor.getType());
        }
        catch (Throwable ex) {
          throw new SpelEvaluationException(getStartPosition(), ex,
                  SpelMessage.UNABLE_TO_DYNAMICALLY_CREATE_OBJECT, resultDescriptor.getType());
        }
      }
    }
    return result;
  }

  @Override
  public TypedValue setValueInternal(ExpressionState state, Supplier<TypedValue> valueSupplier)
          throws EvaluationException {

    TypedValue typedValue = valueSupplier.get();
    writeProperty(state.getActiveContextObject(), state.getEvaluationContext(), this.name, typedValue.getValue());
    return typedValue;
  }

  @Override
  public boolean isWritable(ExpressionState state) throws EvaluationException {
    return isWritableProperty(this.name, state.getActiveContextObject(), state.getEvaluationContext());
  }

  @Override
  public String toStringAST() {
    return this.name;
  }

  /**
   * Attempt to read the named property from the current context object.
   *
   * @return the value of the property
   * @throws EvaluationException if any problem accessing the property, or if it cannot be found
   */
  private TypedValue readProperty(TypedValue contextObject, EvaluationContext evalContext, String name)
          throws EvaluationException {

    final Object originalTarget = contextObject.getValue();
    Object target = originalTarget;
    Optional<?> fallbackOptionalTarget = null;
    boolean isEmptyOptional = false;

    if (isNullSafe()) {
      if (target == null) {
        return TypedValue.NULL;
      }
      if (target instanceof Optional<?> optional) {
        if (optional.isPresent()) {
          target = optional.get();
          fallbackOptionalTarget = optional;
        }
        else {
          isEmptyOptional = true;
        }
      }
    }

    PropertyAccessor accessorToUse = this.cachedReadAccessor;
    if (accessorToUse != null) {
      if (evalContext.getPropertyAccessors().contains(accessorToUse)) {
        try {
          return accessorToUse.read(evalContext, target, name);
        }
        catch (Exception ex) {
          // This is OK - it may have gone stale due to a class change,
          // let's try to get a new one and call it before giving up...
        }
      }
      this.cachedReadAccessor = null;
    }

    List<PropertyAccessor> accessorsToTry =
            AccessorUtils.getAccessorsToTry(target, evalContext.getPropertyAccessors());
    // Go through the accessors that may be able to resolve it. If they are a cacheable accessor then
    // get the accessor and use it. If they are not cacheable but report they can read the property
    // then ask them to read it.
    try {
      for (PropertyAccessor accessor : accessorsToTry) {
        // First, attempt to find the property on the target object.
        if (accessor.canRead(evalContext, target, name)) {
          if (accessor instanceof ReflectivePropertyAccessor reflectivePropertyAccessor) {
            accessor = reflectivePropertyAccessor.createOptimalAccessor(
                    evalContext, target, name);
          }
          this.cachedReadAccessor = accessor;
          return accessor.read(evalContext, target, name);
        }
        // Second, attempt to find the property on the original Optional instance.
        else if (fallbackOptionalTarget != null && accessor.canRead(evalContext, fallbackOptionalTarget, name)) {
          if (accessor instanceof ReflectivePropertyAccessor reflectivePropertyAccessor) {
            accessor = reflectivePropertyAccessor.createOptimalAccessor(
                    evalContext, fallbackOptionalTarget, name);
          }
          this.cachedReadAccessor = accessor;
          return accessor.read(evalContext, fallbackOptionalTarget, name);
        }
      }
    }
    catch (Exception ex) {
      throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_DURING_PROPERTY_READ, name, ex.getMessage());
    }

    // If we got this far, that means we failed to find an accessor for both the
    // target and the fallback target. So, we return NULL if the original target
    // is a null-safe empty Optional.
    if (isEmptyOptional) {
      return TypedValue.NULL;
    }

    if (originalTarget == null) {
      throw new SpelEvaluationException(SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE_ON_NULL, name);
    }
    else {
      throw new SpelEvaluationException(getStartPosition(), SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE, name,
              FormatHelper.formatClassNameForMessage(getObjectClass(originalTarget)));
    }
  }

  private void writeProperty(TypedValue contextObject, EvaluationContext evalContext, String name, @Nullable Object newValue)
          throws EvaluationException {

    Object target = contextObject.getValue();
    if (target == null) {
      if (isNullSafe()) {
        return;
      }
      throw new SpelEvaluationException(
              getStartPosition(), SpelMessage.PROPERTY_OR_FIELD_NOT_WRITABLE_ON_NULL, name);
    }

    PropertyAccessor accessorToUse = this.cachedWriteAccessor;
    if (accessorToUse != null) {
      if (evalContext.getPropertyAccessors().contains(accessorToUse)) {
        try {
          accessorToUse.write(evalContext, target, name, newValue);
          return;
        }
        catch (Exception ex) {
          // This is OK - it may have gone stale due to a class change,
          // let's try to get a new one and call it before giving up...
        }
      }
      this.cachedWriteAccessor = null;
    }

    List<PropertyAccessor> accessorsToTry =
            AccessorUtils.getAccessorsToTry(target, evalContext.getPropertyAccessors());
    try {
      for (PropertyAccessor accessor : accessorsToTry) {
        if (accessor.canWrite(evalContext, target, name)) {
          this.cachedWriteAccessor = accessor;
          accessor.write(evalContext, target, name, newValue);
          return;
        }
      }
    }
    catch (AccessException ex) {
      throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_PROPERTY_WRITE,
              name, ex.getMessage());
    }

    throw new SpelEvaluationException(getStartPosition(), SpelMessage.PROPERTY_OR_FIELD_NOT_WRITABLE, name,
            FormatHelper.formatClassNameForMessage(getObjectClass(target)));
  }

  public boolean isWritableProperty(String name, TypedValue contextObject, EvaluationContext evalContext)
          throws EvaluationException {

    Object target = contextObject.getValue();
    if (target != null) {
      List<PropertyAccessor> accessorsToTry =
              AccessorUtils.getAccessorsToTry(target, evalContext.getPropertyAccessors());
      for (PropertyAccessor accessor : accessorsToTry) {
        try {
          if (accessor.canWrite(evalContext, target, name)) {
            return true;
          }
        }
        catch (AccessException ex) {
          // let others try
        }
      }
    }
    return false;
  }

  @Override
  public boolean isCompilable() {
    return (this.cachedReadAccessor instanceof CompilablePropertyAccessor compilablePropertyAccessor &&
            compilablePropertyAccessor.isCompilable());
  }

  @Override
  public void generateCode(MethodVisitor mv, CodeFlow cf) {
    PropertyAccessor accessorToUse = this.cachedReadAccessor;
    if (!(accessorToUse instanceof CompilablePropertyAccessor cpa)) {
      throw new IllegalStateException("Property accessor is not compilable: " + accessorToUse);
    }

    Label skipIfNull = null;
    if (isNullSafe()) {
      mv.visitInsn(DUP);
      skipIfNull = new Label();
      Label continueLabel = new Label();
      mv.visitJumpInsn(IFNONNULL, continueLabel);
      CodeFlow.insertCheckCast(mv, this.exitTypeDescriptor);
      mv.visitJumpInsn(GOTO, skipIfNull);
      mv.visitLabel(continueLabel);
    }

    cpa.generateCode(this.name, mv, cf);
    cf.pushDescriptor(this.exitTypeDescriptor);

    if (this.originalPrimitiveExitTypeDescriptor != null) {
      // The output of the accessor is a primitive but from the block above it might be null,
      // so to have a common stack element type at skipIfNull target it is necessary
      // to box the primitive
      CodeFlow.insertBoxIfNecessary(mv, this.originalPrimitiveExitTypeDescriptor);
    }
    if (skipIfNull != null) {
      mv.visitLabel(skipIfNull);
    }
  }

  void setExitTypeDescriptor(String descriptor) {
    // If this property or field access would return a primitive - and yet
    // it is also marked null safe - then the exit type descriptor must be
    // promoted to the box type to allow a null value to be passed on
    if (isNullSafe() && CodeFlow.isPrimitive(descriptor)) {
      this.originalPrimitiveExitTypeDescriptor = descriptor;
      this.exitTypeDescriptor = CodeFlow.toBoxedDescriptor(descriptor);
    }
    else {
      this.exitTypeDescriptor = descriptor;
    }
  }

  private static class AccessorValueRef implements ValueRef {

    private final PropertyOrFieldReference ref;

    private final TypedValue contextObject;

    private final EvaluationContext evalContext;

    private final boolean autoGrowNullReferences;

    public AccessorValueRef(PropertyOrFieldReference propertyOrFieldReference, TypedValue activeContextObject,
            EvaluationContext evalContext, boolean autoGrowNullReferences) {

      this.ref = propertyOrFieldReference;
      this.contextObject = activeContextObject;
      this.evalContext = evalContext;
      this.autoGrowNullReferences = autoGrowNullReferences;
    }

    @Override
    public TypedValue getValue() {
      TypedValue value =
              this.ref.getValueInternal(this.contextObject, this.evalContext, this.autoGrowNullReferences);
      if (this.ref.cachedReadAccessor instanceof CompilablePropertyAccessor compilablePropertyAccessor) {
        this.ref.setExitTypeDescriptor(CodeFlow.toDescriptor(compilablePropertyAccessor.getPropertyType()));
      }
      return value;
    }

    @Override
    public void setValue(@Nullable Object newValue) {
      this.ref.writeProperty(this.contextObject, this.evalContext, this.ref.name, newValue);
    }

    @Override
    public boolean isWritable() {
      return this.ref.isWritableProperty(this.ref.name, this.contextObject, this.evalContext);
    }
  }

}
