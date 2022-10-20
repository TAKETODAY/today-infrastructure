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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.core.CodeFlow;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.expression.AccessException;
import cn.taketoday.expression.ConstructorExecutor;
import cn.taketoday.expression.ConstructorResolver;
import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.TypeConverter;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.common.ExpressionUtils;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.expression.spel.SpelMessage;
import cn.taketoday.expression.spel.SpelNode;
import cn.taketoday.expression.spel.support.ReflectiveConstructorExecutor;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Represents the invocation of a constructor. Either a constructor on a regular type or
 * construction of an array. When an array is constructed, an initializer can be specified.
 *
 * <p>Examples:<br>
 * new String('hello world')<br>
 * new int[]{1,2,3,4}<br>
 * new int[3] new int[3]{1,2,3}
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
public class ConstructorReference extends SpelNodeImpl {

  /**
   * Maximum number of elements permitted in an array declaration, applying
   * to one-dimensional as well as multi-dimensional arrays.
   */
  private static final int MAX_ARRAY_ELEMENTS = 256 * 1024; // 256K

  private final boolean isArrayConstructor;

  @Nullable
  private SpelNodeImpl[] dimensions;

  // TODO is this caching safe - passing the expression around will mean this executor is also being passed around
  /** The cached executor that may be reused on subsequent evaluations. */
  @Nullable
  private volatile ConstructorExecutor cachedExecutor;

  /**
   * Create a constructor reference. The first argument is the type, the rest are the parameters to the constructor
   * call
   */
  public ConstructorReference(int startPos, int endPos, SpelNodeImpl... arguments) {
    super(startPos, endPos, arguments);
    this.isArrayConstructor = false;
  }

  /**
   * Create a constructor reference. The first argument is the type, the rest are the parameters to the constructor
   * call
   */
  public ConstructorReference(int startPos, int endPos, SpelNodeImpl[] dimensions, SpelNodeImpl... arguments) {
    super(startPos, endPos, arguments);
    this.isArrayConstructor = true;
    this.dimensions = dimensions;
  }

  /**
   * Implements getValue() - delegating to the code for building an array or a simple type.
   */
  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    if (this.isArrayConstructor) {
      return createArray(state);
    }
    else {
      return createNewInstance(state);
    }
  }

  /**
   * Create a new ordinary object and return it.
   *
   * @param state the expression state within which this expression is being evaluated
   * @return the new object
   * @throws EvaluationException if there is a problem creating the object
   */
  private TypedValue createNewInstance(ExpressionState state) throws EvaluationException {
    Object[] arguments = new Object[getChildCount() - 1];
    List<TypeDescriptor> argumentTypes = new ArrayList<>(getChildCount() - 1);
    for (int i = 0; i < arguments.length; i++) {
      TypedValue childValue = this.children[i + 1].getValueInternal(state);
      Object value = childValue.getValue();
      arguments[i] = value;
      argumentTypes.add(TypeDescriptor.fromObject(value));
    }

    ConstructorExecutor executorToUse = this.cachedExecutor;
    if (executorToUse != null) {
      try {
        return executorToUse.execute(state.getEvaluationContext(), arguments);
      }
      catch (AccessException ex) {
        // Two reasons this can occur:
        // 1. the method invoked actually threw a real exception
        // 2. the method invoked was not passed the arguments it expected and has become 'stale'

        // In the first case we should not retry, in the second case we should see if there is a
        // better suited method.

        // To determine which situation it is, the AccessException will contain a cause.
        // If the cause is an InvocationTargetException, a user exception was thrown inside the constructor.
        // Otherwise the constructor could not be invoked.
        if (ex.getCause() instanceof InvocationTargetException) {
          // User exception was the root cause - exit now
          Throwable rootCause = ex.getCause().getCause();
          if (rootCause instanceof RuntimeException runtimeException) {
            throw runtimeException;
          }
          else {
            String typeName = (String) this.children[0].getValueInternal(state).getValue();
            throw new SpelEvaluationException(getStartPosition(), rootCause,
                    SpelMessage.CONSTRUCTOR_INVOCATION_PROBLEM, typeName,
                    FormatHelper.formatMethodForMessage("", argumentTypes));
          }
        }

        // At this point we know it wasn't a user problem so worth a retry if a better candidate can be found
        this.cachedExecutor = null;
      }
    }

    // Either there was no accessor or it no longer exists
    String typeName = (String) this.children[0].getValueInternal(state).getValue();
    Assert.state(typeName != null, "No type name");
    executorToUse = findExecutorForConstructor(typeName, argumentTypes, state);
    try {
      this.cachedExecutor = executorToUse;
      if (executorToUse instanceof ReflectiveConstructorExecutor reflectiveConstructorExecutor) {
        this.exitTypeDescriptor = CodeFlow.toDescriptor(
                reflectiveConstructorExecutor.getConstructor().getDeclaringClass());

      }
      return executorToUse.execute(state.getEvaluationContext(), arguments);
    }
    catch (AccessException ex) {
      throw new SpelEvaluationException(getStartPosition(), ex,
              SpelMessage.CONSTRUCTOR_INVOCATION_PROBLEM, typeName,
              FormatHelper.formatMethodForMessage("", argumentTypes));
    }
  }

  /**
   * Go through the list of registered constructor resolvers and see if any can find a
   * constructor that takes the specified set of arguments.
   *
   * @param typeName the type trying to be constructed
   * @param argumentTypes the types of the arguments supplied that the constructor must take
   * @param state the current state of the expression
   * @return a reusable ConstructorExecutor that can be invoked to run the constructor or null
   * @throws SpelEvaluationException if there is a problem locating the constructor
   */
  private ConstructorExecutor findExecutorForConstructor(String typeName,
          List<TypeDescriptor> argumentTypes, ExpressionState state) throws SpelEvaluationException {

    EvaluationContext evalContext = state.getEvaluationContext();
    List<ConstructorResolver> ctorResolvers = evalContext.getConstructorResolvers();
    for (ConstructorResolver ctorResolver : ctorResolvers) {
      try {
        ConstructorExecutor ce = ctorResolver.resolve(state.getEvaluationContext(), typeName, argumentTypes);
        if (ce != null) {
          return ce;
        }
      }
      catch (AccessException ex) {
        throw new SpelEvaluationException(getStartPosition(), ex,
                SpelMessage.CONSTRUCTOR_INVOCATION_PROBLEM, typeName,
                FormatHelper.formatMethodForMessage("", argumentTypes));
      }
    }
    throw new SpelEvaluationException(getStartPosition(), SpelMessage.CONSTRUCTOR_NOT_FOUND, typeName,
            FormatHelper.formatMethodForMessage("", argumentTypes));
  }

  @Override
  public String toStringAST() {
    StringBuilder sb = new StringBuilder("new ");
    int index = 0;
    sb.append(getChild(index++).toStringAST());
    sb.append('(');
    for (int i = index; i < getChildCount(); i++) {
      if (i > index) {
        sb.append(',');
      }
      sb.append(getChild(i).toStringAST());
    }
    sb.append(')');
    return sb.toString();
  }

  /**
   * Create an array and return it.
   *
   * @param state the expression state within which this expression is being evaluated
   * @return the new array
   * @throws EvaluationException if there is a problem creating the array
   */
  private TypedValue createArray(ExpressionState state) throws EvaluationException {
    // First child gives us the array type which will either be a primitive or reference type
    Object intendedArrayType = getChild(0).getValue(state);
    if (!(intendedArrayType instanceof String type)) {
      throw new SpelEvaluationException(getChild(0).getStartPosition(),
              SpelMessage.TYPE_NAME_EXPECTED_FOR_ARRAY_CONSTRUCTION,
              FormatHelper.formatClassNameForMessage(
                      intendedArrayType != null ? intendedArrayType.getClass() : null));
    }

    if (state.getEvaluationContext().getConstructorResolvers().isEmpty()) {
      // No constructor resolver -> no array construction either (as of 6.0)
      throw new SpelEvaluationException(getStartPosition(), SpelMessage.CONSTRUCTOR_NOT_FOUND,
              type + "[]", "[]");
    }

    Class<?> componentType;
    TypeCode arrayTypeCode = TypeCode.forName(type);
    if (arrayTypeCode == TypeCode.OBJECT) {
      componentType = state.findType(type);
    }
    else {
      componentType = arrayTypeCode.getType();
    }

    Object newArray = null;
    if (!hasInitializer()) {
      // Confirm all dimensions were specified (for example [3][][5] is missing the 2nd dimension)
      if (this.dimensions != null) {
        for (SpelNodeImpl dimension : this.dimensions) {
          if (dimension == null) {
            throw new SpelEvaluationException(getStartPosition(), SpelMessage.MISSING_ARRAY_DIMENSION);
          }
        }
        TypeConverter typeConverter = state.getEvaluationContext().getTypeConverter();
        if (this.dimensions.length == 1) {
          // Shortcut for 1-dimensional
          TypedValue o = this.dimensions[0].getTypedValue(state);
          int arraySize = ExpressionUtils.toInt(typeConverter, o);
          checkNumElements(arraySize);
          newArray = Array.newInstance(componentType, arraySize);
        }
        else {
          // Multi-dimensional - hold onto your hat!
          int[] dims = new int[this.dimensions.length];
          long numElements = 1;
          for (int d = 0; d < this.dimensions.length; d++) {
            TypedValue o = this.dimensions[d].getTypedValue(state);
            int arraySize = ExpressionUtils.toInt(typeConverter, o);
            dims[d] = arraySize;
            numElements *= arraySize;
            checkNumElements(numElements);
          }
          newArray = Array.newInstance(componentType, dims);
        }
      }
    }
    else {
      // There is an initializer
      if (this.dimensions == null || this.dimensions.length > 1) {
        // There is an initializer but this is a multi-dimensional array (e.g. new int[][]{{1,2},{3,4}}) - this
        // is not currently supported
        throw new SpelEvaluationException(getStartPosition(),
                SpelMessage.MULTIDIM_ARRAY_INITIALIZER_NOT_SUPPORTED);
      }
      TypeConverter typeConverter = state.getEvaluationContext().getTypeConverter();
      InlineList initializer = (InlineList) getChild(1);
      // If a dimension was specified, check it matches the initializer length
      if (this.dimensions[0] != null) {
        TypedValue dValue = this.dimensions[0].getTypedValue(state);
        int i = ExpressionUtils.toInt(typeConverter, dValue);
        if (i != initializer.getChildCount()) {
          throw new SpelEvaluationException(getStartPosition(), SpelMessage.INITIALIZER_LENGTH_INCORRECT);
        }
      }
      // Build the array and populate it
      int arraySize = initializer.getChildCount();
      newArray = Array.newInstance(componentType, arraySize);
      if (arrayTypeCode == TypeCode.OBJECT) {
        populateReferenceTypeArray(state, newArray, typeConverter, initializer, componentType);
      }
      else if (arrayTypeCode == TypeCode.BOOLEAN) {
        populateBooleanArray(state, newArray, typeConverter, initializer);
      }
      else if (arrayTypeCode == TypeCode.BYTE) {
        populateByteArray(state, newArray, typeConverter, initializer);
      }
      else if (arrayTypeCode == TypeCode.CHAR) {
        populateCharArray(state, newArray, typeConverter, initializer);
      }
      else if (arrayTypeCode == TypeCode.DOUBLE) {
        populateDoubleArray(state, newArray, typeConverter, initializer);
      }
      else if (arrayTypeCode == TypeCode.FLOAT) {
        populateFloatArray(state, newArray, typeConverter, initializer);
      }
      else if (arrayTypeCode == TypeCode.INT) {
        populateIntArray(state, newArray, typeConverter, initializer);
      }
      else if (arrayTypeCode == TypeCode.LONG) {
        populateLongArray(state, newArray, typeConverter, initializer);
      }
      else if (arrayTypeCode == TypeCode.SHORT) {
        populateShortArray(state, newArray, typeConverter, initializer);
      }
      else {
        throw new IllegalStateException(arrayTypeCode.name());
      }
    }
    return new TypedValue(newArray);
  }

  private void checkNumElements(long numElements) {
    if (numElements >= MAX_ARRAY_ELEMENTS) {
      throw new SpelEvaluationException(getStartPosition(),
              SpelMessage.MAX_ARRAY_ELEMENTS_THRESHOLD_EXCEEDED, MAX_ARRAY_ELEMENTS);
    }
  }

  private void populateReferenceTypeArray(ExpressionState state, Object newArray, TypeConverter typeConverter,
          InlineList initializer, Class<?> componentType) {

    TypeDescriptor toTypeDescriptor = TypeDescriptor.valueOf(componentType);
    Object[] newObjectArray = (Object[]) newArray;
    for (int i = 0; i < newObjectArray.length; i++) {
      SpelNode elementNode = initializer.getChild(i);
      Object arrayEntry = elementNode.getValue(state);
      newObjectArray[i] = typeConverter.convertValue(arrayEntry,
              TypeDescriptor.fromObject(arrayEntry), toTypeDescriptor);
    }
  }

  private void populateByteArray(ExpressionState state, Object newArray, TypeConverter typeConverter,
          InlineList initializer) {

    byte[] newByteArray = (byte[]) newArray;
    for (int i = 0; i < newByteArray.length; i++) {
      TypedValue typedValue = initializer.getChild(i).getTypedValue(state);
      newByteArray[i] = ExpressionUtils.toByte(typeConverter, typedValue);
    }
  }

  private void populateFloatArray(ExpressionState state, Object newArray, TypeConverter typeConverter,
          InlineList initializer) {

    float[] newFloatArray = (float[]) newArray;
    for (int i = 0; i < newFloatArray.length; i++) {
      TypedValue typedValue = initializer.getChild(i).getTypedValue(state);
      newFloatArray[i] = ExpressionUtils.toFloat(typeConverter, typedValue);
    }
  }

  private void populateDoubleArray(ExpressionState state, Object newArray, TypeConverter typeConverter,
          InlineList initializer) {

    double[] newDoubleArray = (double[]) newArray;
    for (int i = 0; i < newDoubleArray.length; i++) {
      TypedValue typedValue = initializer.getChild(i).getTypedValue(state);
      newDoubleArray[i] = ExpressionUtils.toDouble(typeConverter, typedValue);
    }
  }

  private void populateShortArray(ExpressionState state, Object newArray, TypeConverter typeConverter,
          InlineList initializer) {

    short[] newShortArray = (short[]) newArray;
    for (int i = 0; i < newShortArray.length; i++) {
      TypedValue typedValue = initializer.getChild(i).getTypedValue(state);
      newShortArray[i] = ExpressionUtils.toShort(typeConverter, typedValue);
    }
  }

  private void populateLongArray(ExpressionState state, Object newArray, TypeConverter typeConverter,
          InlineList initializer) {

    long[] newLongArray = (long[]) newArray;
    for (int i = 0; i < newLongArray.length; i++) {
      TypedValue typedValue = initializer.getChild(i).getTypedValue(state);
      newLongArray[i] = ExpressionUtils.toLong(typeConverter, typedValue);
    }
  }

  private void populateCharArray(ExpressionState state,
          Object newArray, TypeConverter typeConverter, InlineList initializer) {

    char[] newCharArray = (char[]) newArray;
    for (int i = 0; i < newCharArray.length; i++) {
      TypedValue typedValue = initializer.getChild(i).getTypedValue(state);
      newCharArray[i] = ExpressionUtils.toChar(typeConverter, typedValue);
    }
  }

  private void populateBooleanArray(ExpressionState state,
          Object newArray, TypeConverter typeConverter, InlineList initializer) {

    boolean[] newBooleanArray = (boolean[]) newArray;
    for (int i = 0; i < newBooleanArray.length; i++) {
      TypedValue typedValue = initializer.getChild(i).getTypedValue(state);
      newBooleanArray[i] = ExpressionUtils.toBoolean(typeConverter, typedValue);
    }
  }

  private void populateIntArray(ExpressionState state,
          Object newArray, TypeConverter typeConverter, InlineList initializer) {

    int[] newIntArray = (int[]) newArray;
    for (int i = 0; i < newIntArray.length; i++) {
      TypedValue typedValue = initializer.getChild(i).getTypedValue(state);
      newIntArray[i] = ExpressionUtils.toInt(typeConverter, typedValue);
    }
  }

  private boolean hasInitializer() {
    return (getChildCount() > 1);
  }

  @Override
  public boolean isCompilable() {
    if (!(cachedExecutor instanceof ReflectiveConstructorExecutor executor)
            || exitTypeDescriptor == null) {
      return false;
    }

    if (getChildCount() > 1) {
      for (int c = 1, max = getChildCount(); c < max; c++) {
        if (!children[c].isCompilable()) {
          return false;
        }
      }
    }

    Constructor<?> constructor = executor.getConstructor();
    return Modifier.isPublic(constructor.getModifiers())
            && Modifier.isPublic(constructor.getDeclaringClass().getModifiers());
  }

  @Override
  public void generateCode(MethodVisitor mv, CodeFlow cf) {
    ReflectiveConstructorExecutor executor = ((ReflectiveConstructorExecutor) this.cachedExecutor);
    Assert.state(executor != null, "No cached executor");

    Constructor<?> constructor = executor.getConstructor();
    String classDesc = constructor.getDeclaringClass().getName().replace('.', '/');
    mv.visitTypeInsn(NEW, classDesc);
    mv.visitInsn(DUP);

    // children[0] is the type of the constructor, don't want to include that in argument processing
    SpelNodeImpl[] arguments = new SpelNodeImpl[this.children.length - 1];
    System.arraycopy(this.children, 1, arguments, 0, this.children.length - 1);
    generateCodeForArguments(mv, cf, constructor, arguments);
    mv.visitMethodInsn(INVOKESPECIAL, classDesc, "<init>", CodeFlow.createSignatureDescriptor(constructor), false);
    cf.pushDescriptor(this.exitTypeDescriptor);
  }

}
