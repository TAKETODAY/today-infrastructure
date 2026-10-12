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

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;

import infra.bytecode.Label;
import infra.bytecode.MethodVisitor;
import infra.bytecode.Type;
import infra.core.TypeDescriptor;
import infra.expression.EvaluationException;
import infra.expression.Operation;
import infra.expression.TypeConverter;
import infra.expression.TypedValue;
import infra.expression.spel.CodeFlow;
import infra.expression.spel.ExpressionState;
import infra.expression.spel.SpelCompilerMode;
import infra.expression.spel.SpelEvaluationException;
import infra.expression.spel.SpelMessage;
import infra.util.Assert;
import infra.util.NumberUtils;

/**
 * The plus operator will:
 * <ul>
 * <li>add numbers
 * <li>concatenate strings
 * </ul>
 *
 * <p>It can be used as a unary operator for numbers.
 * The standard promotions are performed when the operand types vary (double+int=double).
 * For other options it defers to the registered overloader.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Ivo Smid
 * @author Giovanni Dall'Oglio Risso
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class OpPlus extends Operator {

  /**
   * Maximum number of characters permitted in a concatenated string.
   */
  private static final int MAX_CONCATENATED_STRING_LENGTH = 100_000;

  /**
   * The non-String operand of the most recent String concatenation, or
   * {@code null} if the most recent evaluation was not such a concatenation.
   *
   * @since 5.0
   */
  private volatile @Nullable SpelNodeImpl nonStringOperand;

  /**
   * The type of the most recent non-null value converted to a String.
   * <p>Unless the operand descriptor guarantees this type, compiled code
   * verifies that subsequent non-null values have exactly this type.
   *
   * @since 5.0
   */
  private volatile @Nullable Class<?> nonStringOperandType;

  /**
   * The type of the converter that most recently converted a non-null operand.
   * <p>Tracking the type avoids retaining a converter instance. Switching between
   * instances of the same type that convert differently is detected only when
   * the expression may be compiled automatically.
   *
   * @since 5.0
   */
  private volatile @Nullable Class<?> nonStringOperandTypeConverterType;

  /**
   * Whether conversion differs from {@link String#valueOf(Object)} or the
   * operand descriptor carries annotations that may affect formatting.
   * <p>This one-way latch prevents compilation once set, since compiled code
   * uses StringBuilder append overloads rather than the configured converter.
   *
   * @since 5.0
   */
  private volatile boolean typeConversionDiffersFromToString;

  public OpPlus(int startPos, int endPos, SpelNodeImpl... operands) {
    super("+", startPos, endPos, operands);
    Assert.notEmpty(operands, "Operands must not be empty");
  }

  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    SpelNodeImpl leftOp = getLeftOperand();

    if (this.children.length < 2) {  // if only one operand, then this is unary plus
      Object operandOne = leftOp.getValueInternal(state).getValue();
      if (operandOne instanceof Number) {
        if (operandOne instanceof Double) {
          this.exitTypeDescriptor = "D";
        }
        else if (operandOne instanceof Float) {
          this.exitTypeDescriptor = "F";
        }
        else if (operandOne instanceof Long) {
          this.exitTypeDescriptor = "J";
        }
        else if (operandOne instanceof Integer) {
          this.exitTypeDescriptor = "I";
        }
        return new TypedValue(operandOne);
      }
      return state.operate(Operation.ADD, operandOne, null);
    }

    TypedValue operandOneValue = leftOp.getValueInternal(state);
    Object leftOperand = operandOneValue.getValue();
    TypedValue operandTwoValue = getRightOperand().getValueInternal(state);
    Object rightOperand = operandTwoValue.getValue();

    if (leftOperand instanceof Number leftNumber && rightOperand instanceof Number rightNumber) {
      if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
        BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
        BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
        return new TypedValue(leftBigDecimal.add(rightBigDecimal));
      }
      else if (leftNumber instanceof Double || rightNumber instanceof Double) {
        this.exitTypeDescriptor = "D";
        return new TypedValue(leftNumber.doubleValue() + rightNumber.doubleValue());
      }
      else if (leftNumber instanceof Float || rightNumber instanceof Float) {
        this.exitTypeDescriptor = "F";
        return new TypedValue(leftNumber.floatValue() + rightNumber.floatValue());
      }
      else if (leftNumber instanceof BigInteger || rightNumber instanceof BigInteger) {
        BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
        BigInteger rightBigInteger = NumberUtils.convertNumberToTargetClass(rightNumber, BigInteger.class);
        return new TypedValue(leftBigInteger.add(rightBigInteger));
      }
      else if (leftNumber instanceof Long || rightNumber instanceof Long) {
        this.exitTypeDescriptor = "J";
        return new TypedValue(leftNumber.longValue() + rightNumber.longValue());
      }
      else if (CodeFlow.isIntegerForNumericOp(leftNumber) || CodeFlow.isIntegerForNumericOp(rightNumber)) {
        this.exitTypeDescriptor = "I";
        return new TypedValue(leftNumber.intValue() + rightNumber.intValue());
      }
      else {
        // Unknown Number subtypes -> best guess is double addition
        return new TypedValue(leftNumber.doubleValue() + rightNumber.doubleValue());
      }
    }

    if (leftOperand instanceof String leftString && rightOperand instanceof String rightString) {
      this.exitTypeDescriptor = "Ljava/lang/String";
      if (this.nonStringOperand != null) {
        this.nonStringOperand = null;
      }
      checkStringLength(leftString);
      checkStringLength(rightString);
      return concatenate(leftString, rightString);
    }

    if (leftOperand instanceof String leftString) {
      checkStringLength(leftString);
      String rightString = convertNonStringOperandToString(operandTwoValue, state);
      checkStringLength(rightString);
      this.exitTypeDescriptor = "Ljava/lang/String";
      SpelNodeImpl rightOp = getRightOperand();
      if (this.nonStringOperand != rightOp) {
        this.nonStringOperand = rightOp;
      }
      return concatenate(leftString, rightString);
    }

    if (rightOperand instanceof String rightString) {
      checkStringLength(rightString);
      String leftString = convertNonStringOperandToString(operandOneValue, state);
      checkStringLength(leftString);
      this.exitTypeDescriptor = "Ljava/lang/String";
      if (this.nonStringOperand != leftOp) {
        this.nonStringOperand = leftOp;
      }
      return concatenate(leftString, rightString);
    }

    return state.operate(Operation.ADD, leftOperand, rightOperand);
  }

  private void checkStringLength(String string) {
    checkStringLength(string.length());
  }

  private void checkStringLength(int stringLength) {
    if (stringLength > MAX_CONCATENATED_STRING_LENGTH) {
      throw new SpelEvaluationException(getStartPosition(),
              SpelMessage.MAX_CONCATENATED_STRING_LENGTH_EXCEEDED, MAX_CONCATENATED_STRING_LENGTH);
    }
  }

  private TypedValue concatenate(String leftString, String rightString) {
    checkStringLength(leftString.length() + rightString.length());
    return new TypedValue(leftString + rightString);
  }

  @Override
  public String toStringAST() {
    if (this.children.length < 2) {  // unary plus
      return "+" + getLeftOperand().toStringAST();
    }
    return super.toStringAST();
  }

  @Override
  public SpelNodeImpl getRightOperand() {
    if (this.children.length < 2) {
      throw new IllegalStateException("No right operand");
    }
    return this.children[1];
  }

  /**
   * Convert the value of a non-{@code String} operand to a {@code String} for use
   * in a {@code String + non-String} (or {@code non-String + String}) concatenation,
   * using the registered {@link TypeConverter} if it can convert the value and
   * {@link String#valueOf(Object)} otherwise.
   * <p>In order to determine whether this node can be compiled, this method also
   * tracks the type of the value and whether the {@code TypeConverter} produced a
   * result that differs from {@code String.valueOf()}.
   * <p>If the expression may be compiled automatically, the result is compared
   * for every evaluation. Otherwise, the result is only compared if the type of
   * the value or the type of the {@code TypeConverter} changes, in order to avoid
   * converting the value to a {@code String} twice for every evaluation in
   * interpreted mode, while still supporting explicit compilation.
   *
   * @since 5.0
   */
  private String convertNonStringOperandToString(TypedValue typedValue, ExpressionState state) {
    Object value = typedValue.getValue();
    if (value == null) {
      return "null";
    }
    Class<?> type = value.getClass();
    TypeConverter typeConverter = state.getEvaluationContext().getTypeConverter();
    Class<?> converterType = typeConverter.getClass();
    boolean compare = !this.typeConversionDiffersFromToString &&
            (isCompilationEnabled(state) || type != this.nonStringOperandType ||
                    converterType != this.nonStringOperandTypeConverterType);
    TypeDescriptor sourceType = typedValue.getTypeDescriptor();
    TypeDescriptor targetType = TypeDescriptor.valueOf(String.class);
    String result;
    if (typeConverter.canConvert(sourceType, targetType)) {
      result = String.valueOf(typeConverter.convertValue(value, sourceType, targetType));
      if (compare && ((sourceType != null && sourceType.getAnnotations().length > 0)
              || !result.equals(String.valueOf(value)))) {
        this.typeConversionDiffersFromToString = true;
      }
    }
    else {
      result = String.valueOf(value);
    }
    if (type != this.nonStringOperandType) {
      this.nonStringOperandType = type;
    }
    if (converterType != this.nonStringOperandTypeConverterType) {
      this.nonStringOperandTypeConverterType = converterType;
    }
    return result;
  }

  /**
   * Determine whether the configured compiler mode and evaluation context
   * permit automatic compilation.
   *
   * @since 5.0
   */
  private static boolean isCompilationEnabled(ExpressionState state) {
    return state.getConfiguration().getCompilerMode() != SpelCompilerMode.OFF
            && state.getEvaluationContext().isCompilationSupported();
  }

  @Override
  public boolean isCompilable() {
    if (!getLeftOperand().isCompilable()) {
      return false;
    }
    if (this.children.length > 1) {
      if (!getRightOperand().isCompilable()) {
        return false;
      }
    }
    String exitDesc = this.exitTypeDescriptor;
    if (exitDesc == null) {
      return false;
    }
    SpelNodeImpl operand = this.nonStringOperand;
    if ("Ljava/lang/String".equals(exitDesc) && operand != null) {
      String descriptor = operand.exitTypeDescriptor;
      Class<?> type = this.nonStringOperandType;
      return !this.typeConversionDiffersFromToString && type != null && descriptor != null
              && !"V".equals(descriptor) && (CodeFlow.isPrimitive(descriptor) || isAccessible(type));
    }
    return true;
  }

  /**
   * Determine whether a type is public and unconditionally exported by its module.
   *
   * @since 5.0
   */
  private static boolean isAccessible(Class<?> type) {
    return Modifier.isPublic(type.getModifiers()) && type.getModule().isExported(type.getPackageName());
  }

  /**
   * Walk through a possible tree of nodes that combine strings and append
   * them all to the same (on stack) StringBuilder.
   */
  private static void walk(MethodVisitor mv, CodeFlow cf, OpPlus concatenation, @Nullable SpelNodeImpl operand) {
    if (operand instanceof OpPlus plus && "Ljava/lang/String".equals(plus.exitTypeDescriptor)) {
      walk(mv, cf, plus, plus.getLeftOperand());
      walk(mv, cf, plus, plus.getRightOperand());
    }
    else if (operand != null) {
      cf.enterCompilationScope();
      operand.generateCode(mv, cf);
      String descriptor = cf.lastDescriptor();
      cf.exitCompilationScope();
      if (operand == concatenation.nonStringOperand) {
        concatenation.appendNonStringOperand(mv, descriptor);
      }
      else {
        if (!"Ljava/lang/String".equals(descriptor)) {
          mv.visitTypeInsn(CHECKCAST, "java/lang/String");
        }
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
      }
    }
  }

  @Override
  public void generateCode(MethodVisitor mv, CodeFlow cf) {
    if ("Ljava/lang/String".equals(this.exitTypeDescriptor)) {
      mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
      mv.visitInsn(DUP);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
      walk(mv, cf, this, getLeftOperand());
      walk(mv, cf, this, getRightOperand());
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
    }
    else {
      this.children[0].generateCode(mv, cf);
      String leftDesc = this.children[0].exitTypeDescriptor;
      String exitDesc = this.exitTypeDescriptor;
      Assert.state(exitDesc != null, "No exit type descriptor");
      char targetDesc = exitDesc.charAt(0);
      CodeFlow.insertNumericUnboxOrPrimitiveTypeCoercion(mv, leftDesc, targetDesc);
      if (this.children.length > 1) {
        cf.enterCompilationScope();
        this.children[1].generateCode(mv, cf);
        String rightDesc = this.children[1].exitTypeDescriptor;
        cf.exitCompilationScope();
        CodeFlow.insertNumericUnboxOrPrimitiveTypeCoercion(mv, rightDesc, targetDesc);
        switch (targetDesc) {
          case 'I' -> mv.visitInsn(IADD);
          case 'J' -> mv.visitInsn(LADD);
          case 'F' -> mv.visitInsn(FADD);
          case 'D' -> mv.visitInsn(DADD);
          default -> throw new IllegalStateException(
                  "Unrecognized exit type descriptor: '" + this.exitTypeDescriptor + "'");
        }
      }
    }
    cf.pushDescriptor(this.exitTypeDescriptor);
  }

  /**
   * Emit the appropriate StringBuilder append overload for a non-String operand.
   * <p>Primitive operands use dedicated overloads; references use append(Object)
   * for null-safe String.valueOf() semantics. Unless a final static descriptor
   * guarantees the tracked type, verify exact runtime type equality and throw
   * IllegalStateException on a change, allowing MIXED mode to resume interpretation.
   *
   * @since 5.0
   */
  private void appendNonStringOperand(MethodVisitor mv, @Nullable String descriptor) {
    if (CodeFlow.isPrimitive(descriptor)) {
      String appendDescriptor = switch (descriptor.charAt(0)) {
        case 'B', 'S', 'I' -> "I";
        case 'J' -> "J";
        case 'F' -> "F";
        case 'D' -> "D";
        case 'Z' -> "Z";
        case 'C' -> "C";
        default -> throw new IllegalStateException("Unexpected primitive descriptor '" + descriptor + "'");
      };
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
              "(" + appendDescriptor + ")Ljava/lang/StringBuilder;", false);
      return;
    }
    Class<?> type = this.nonStringOperandType;
    Assert.state(type != null, "No type available for non-String operand");
    if (!Modifier.isFinal(type.getModifiers()) || !CodeFlow.toDescriptor(type).equals(descriptor)) {
      Label verified = new Label();
      mv.visitInsn(DUP);
      mv.visitJumpInsn(IFNULL, verified);
      mv.visitInsn(DUP);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
      mv.visitLdcInsn(Type.forClass(type));
      mv.visitJumpInsn(IF_ACMPEQ, verified);
      mv.visitTypeInsn(NEW, "java/lang/IllegalStateException");
      mv.visitInsn(DUP);
      mv.visitLdcInsn("Operand of compiled String concatenation is not of type " + type.getName());
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "(Ljava/lang/String;)V", false);
      mv.visitInsn(ATHROW);
      mv.visitLabel(verified);
    }
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
            "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
  }

}
