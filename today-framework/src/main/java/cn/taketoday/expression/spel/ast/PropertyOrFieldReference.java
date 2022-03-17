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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.bytecode.Label;
import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.expression.AccessException;
import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.PropertyAccessor;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.CodeFlow;
import cn.taketoday.expression.spel.CompilablePropertyAccessor;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.expression.spel.SpelMessage;
import cn.taketoday.expression.spel.support.ReflectivePropertyAccessor;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

/**
 * Represents a simple property or field reference.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Clark Duplichien
 * @author Sam Brannen
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

  public boolean isNullSafe() {
    return this.nullSafe;
  }

  public String getName() {
    return this.name;
  }

  @Override
  public ValueRef getValueRef(ExpressionState state) throws EvaluationException {
    return new AccessorLValue(this, state.getActiveContextObject(), state.getEvaluationContext(),
            state.getConfiguration().isAutoGrowNullReferences());
  }

  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    TypedValue tv = getValueInternal(state.getActiveContextObject(), state.getEvaluationContext(),
            state.getConfiguration().isAutoGrowNullReferences());
    PropertyAccessor accessorToUse = this.cachedReadAccessor;
    if (accessorToUse instanceof CompilablePropertyAccessor accessor) {
      setExitTypeDescriptor(CodeFlow.toDescriptor(accessor.getPropertyType()));
    }
    return tv;
  }

  private TypedValue getValueInternal(TypedValue contextObject, EvaluationContext evalContext,
          boolean isAutoGrowNullReferences) throws EvaluationException {

    TypedValue result = readProperty(contextObject, evalContext, this.name);

    // Dynamically create the objects if the user has requested that optional behavior
    if (isAutoGrowNullReferences
            && result.getValue() == null
            && nextChildIs(Indexer.class, PropertyOrFieldReference.class)) {
      TypeDescriptor resultDescriptor = result.getTypeDescriptor();
      Assert.state(resultDescriptor != null, "No result type");
      // Create a new collection or map ready for the indexer
      if (List.class == resultDescriptor.getType()) {
        if (isWritableProperty(this.name, contextObject, evalContext)) {
          ArrayList<?> newList = new ArrayList<>();
          writeProperty(contextObject, evalContext, this.name, newList);
          result = readProperty(contextObject, evalContext, this.name);
        }
      }
      else if (Map.class == resultDescriptor.getType()) {
        if (isWritableProperty(this.name, contextObject, evalContext)) {
          HashMap<?, ?> newMap = new HashMap<>();
          writeProperty(contextObject, evalContext, this.name, newMap);
          result = readProperty(contextObject, evalContext, this.name);
        }
      }
      else {
        // 'simple' object
        try {
          if (isWritableProperty(this.name, contextObject, evalContext)) {
            Class<?> clazz = result.getTypeDescriptor().getType();
            Object newObject = ReflectionUtils.accessibleConstructor(clazz).newInstance();
            writeProperty(contextObject, evalContext, this.name, newObject);
            result = readProperty(contextObject, evalContext, this.name);
          }
        }
        catch (InvocationTargetException ex) {
          throw new SpelEvaluationException(getStartPosition(), ex.getTargetException(),
                  SpelMessage.UNABLE_TO_DYNAMICALLY_CREATE_OBJECT, result.getTypeDescriptor().getType());
        }
        catch (Throwable ex) {
          throw new SpelEvaluationException(getStartPosition(), ex,
                  SpelMessage.UNABLE_TO_DYNAMICALLY_CREATE_OBJECT, result.getTypeDescriptor().getType());
        }
      }
    }
    return result;
  }

  @Override
  public void setValue(ExpressionState state, @Nullable Object newValue) throws EvaluationException {
    writeProperty(state.getActiveContextObject(), state.getEvaluationContext(), this.name, newValue);
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
   * @throws EvaluationException if any problem accessing the property or it cannot be found
   */
  private TypedValue readProperty(TypedValue contextObject, EvaluationContext evalContext, String name)
          throws EvaluationException {

    Object targetObject = contextObject.getValue();
    if (targetObject == null && this.nullSafe) {
      return TypedValue.NULL;
    }

    PropertyAccessor accessorToUse = this.cachedReadAccessor;
    if (accessorToUse != null) {
      if (evalContext.getPropertyAccessors().contains(accessorToUse)) {
        try {
          return accessorToUse.read(evalContext, contextObject.getValue(), name);
        }
        catch (Exception ex) {
          // This is OK - it may have gone stale due to a class change,
          // let's try to get a new one and call it before giving up...
        }
      }
      this.cachedReadAccessor = null;
    }

    List<PropertyAccessor> accessorsToTry =
            getPropertyAccessorsToTry(contextObject.getValue(), evalContext.getPropertyAccessors());
    // Go through the accessors that may be able to resolve it. If they are a cacheable accessor then
    // get the accessor and use it. If they are not cacheable but report they can read the property
    // then ask them to read it
    try {
      for (PropertyAccessor accessor : accessorsToTry) {
        if (accessor.canRead(evalContext, contextObject.getValue(), name)) {
          if (accessor instanceof ReflectivePropertyAccessor reflectivePropertyAccessor) {
            accessor = reflectivePropertyAccessor.createOptimalAccessor(
                    evalContext, contextObject.getValue(), name);
          }
          this.cachedReadAccessor = accessor;
          return accessor.read(evalContext, contextObject.getValue(), name);
        }
      }
    }
    catch (Exception ex) {
      throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_DURING_PROPERTY_READ, name, ex.getMessage());
    }

    if (contextObject.getValue() == null) {
      throw new SpelEvaluationException(SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE_ON_NULL, name);
    }
    else {
      throw new SpelEvaluationException(getStartPosition(), SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE, name,
              FormatHelper.formatClassNameForMessage(getObjectClass(contextObject.getValue())));
    }
  }

  private void writeProperty(
          TypedValue contextObject, EvaluationContext evalContext, String name, @Nullable Object newValue)
          throws EvaluationException {

    if (contextObject.getValue() == null && this.nullSafe) {
      return;
    }
    if (contextObject.getValue() == null) {
      throw new SpelEvaluationException(getStartPosition(), SpelMessage.PROPERTY_OR_FIELD_NOT_WRITABLE_ON_NULL, name);
    }

    PropertyAccessor accessorToUse = this.cachedWriteAccessor;
    if (accessorToUse != null) {
      if (evalContext.getPropertyAccessors().contains(accessorToUse)) {
        try {
          accessorToUse.write(evalContext, contextObject.getValue(), name, newValue);
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
            getPropertyAccessorsToTry(contextObject.getValue(), evalContext.getPropertyAccessors());
    try {
      for (PropertyAccessor accessor : accessorsToTry) {
        if (accessor.canWrite(evalContext, contextObject.getValue(), name)) {
          this.cachedWriteAccessor = accessor;
          accessor.write(evalContext, contextObject.getValue(), name, newValue);
          return;
        }
      }
    }
    catch (AccessException ex) {
      throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_PROPERTY_WRITE,
              name, ex.getMessage());
    }

    throw new SpelEvaluationException(getStartPosition(), SpelMessage.PROPERTY_OR_FIELD_NOT_WRITABLE, name,
            FormatHelper.formatClassNameForMessage(getObjectClass(contextObject.getValue())));
  }

  public boolean isWritableProperty(String name, TypedValue contextObject, EvaluationContext evalContext)
          throws EvaluationException {

    Object value = contextObject.getValue();
    if (value != null) {
      List<PropertyAccessor> accessorsToTry =
              getPropertyAccessorsToTry(contextObject.getValue(), evalContext.getPropertyAccessors());
      for (PropertyAccessor accessor : accessorsToTry) {
        try {
          if (accessor.canWrite(evalContext, value, name)) {
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

  /**
   * Determines the set of property resolvers that should be used to try and access a property
   * on the specified target type. The resolvers are considered to be in an ordered list,
   * however in the returned list any that are exact matches for the input target type (as
   * opposed to 'general' resolvers that could work for any type) are placed at the start of the
   * list. In addition, there are specific resolvers that exactly name the class in question
   * and resolvers that name a specific class but it is a supertype of the class we have.
   * These are put at the end of the specific resolvers set and will be tried after exactly
   * matching accessors but before generic accessors.
   *
   * @param contextObject the object upon which property access is being attempted
   * @return a list of resolvers that should be tried in order to access the property
   */
  private List<PropertyAccessor> getPropertyAccessorsToTry(
          @Nullable Object contextObject, List<PropertyAccessor> propertyAccessors) {

    Class<?> targetType = (contextObject != null ? contextObject.getClass() : null);

    ArrayList<PropertyAccessor> specificAccessors = new ArrayList<>();
    ArrayList<PropertyAccessor> generalAccessors = new ArrayList<>();
    for (PropertyAccessor resolver : propertyAccessors) {
      Class<?>[] targets = resolver.getSpecificTargetClasses();
      if (targets == null) {
        // generic resolver that says it can be used for any type
        generalAccessors.add(resolver);
      }
      else if (targetType != null) {
        for (Class<?> clazz : targets) {
          if (clazz == targetType) {
            specificAccessors.add(resolver);
            break;
          }
          else if (clazz.isAssignableFrom(targetType)) {
            generalAccessors.add(resolver);
          }
        }
      }
    }
    ArrayList<PropertyAccessor> resolvers = new ArrayList<>(specificAccessors);
    generalAccessors.removeAll(specificAccessors);
    resolvers.addAll(generalAccessors);
    return resolvers;
  }

  @Override
  public boolean isCompilable() {
    return (this.cachedReadAccessor instanceof CompilablePropertyAccessor compilablePropertyAccessor &&
            compilablePropertyAccessor.isCompilable());
  }

  @Override
  public void generateCode(MethodVisitor mv, CodeFlow cf) {
    PropertyAccessor accessorToUse = this.cachedReadAccessor;
    if (!(accessorToUse instanceof CompilablePropertyAccessor)) {
      throw new IllegalStateException("Property accessor is not compilable: " + accessorToUse);
    }

    Label skipIfNull = null;
    if (this.nullSafe) {
      mv.visitInsn(DUP);
      skipIfNull = new Label();
      Label continueLabel = new Label();
      mv.visitJumpInsn(IFNONNULL, continueLabel);
      CodeFlow.insertCheckCast(mv, this.exitTypeDescriptor);
      mv.visitJumpInsn(GOTO, skipIfNull);
      mv.visitLabel(continueLabel);
    }

    ((CompilablePropertyAccessor) accessorToUse).generateCode(this.name, mv, cf);
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
    if (this.nullSafe && CodeFlow.isPrimitive(descriptor)) {
      this.originalPrimitiveExitTypeDescriptor = descriptor;
      this.exitTypeDescriptor = CodeFlow.toBoxedDescriptor(descriptor);
    }
    else {
      this.exitTypeDescriptor = descriptor;
    }
  }

  private static class AccessorLValue implements ValueRef {

    private final PropertyOrFieldReference ref;

    private final TypedValue contextObject;

    private final EvaluationContext evalContext;

    private final boolean autoGrowNullReferences;

    public AccessorLValue(PropertyOrFieldReference propertyOrFieldReference, TypedValue activeContextObject,
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
