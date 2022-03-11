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

import java.lang.reflect.Modifier;

import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.CodeFlow;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.lang.Nullable;

/**
 * Represents a variable reference, eg. #someVar. Note this is different to a *local*
 * variable like $someVar
 *
 * @author Andy Clement
 * @since 4.0
 */
public class VariableReference extends SpelNodeImpl {

  // Well known variables:
  private static final String THIS = "this";  // currently active context object

  private static final String ROOT = "root";  // root context object

  private final String name;

  public VariableReference(String variableName, int startPos, int endPos) {
    super(startPos, endPos);
    this.name = variableName;
  }

  @Override
  public ValueRef getValueRef(ExpressionState state) throws SpelEvaluationException {
    if (this.name.equals(THIS)) {
      return new ValueRef.TypedValueHolderValueRef(state.getActiveContextObject(), this);
    }
    if (this.name.equals(ROOT)) {
      return new ValueRef.TypedValueHolderValueRef(state.getRootContextObject(), this);
    }
    TypedValue result = state.lookupVariable(this.name);
    // a null value will mean either the value was null or the variable was not found
    return new VariableRef(this.name, result, state.getEvaluationContext());
  }

  @Override
  public TypedValue getValueInternal(ExpressionState state) throws SpelEvaluationException {
    if (this.name.equals(THIS)) {
      return state.getActiveContextObject();
    }
    if (this.name.equals(ROOT)) {
      TypedValue result = state.getRootContextObject();
      this.exitTypeDescriptor = CodeFlow.toDescriptorFromObject(result.getValue());
      return result;
    }
    TypedValue result = state.lookupVariable(this.name);
    Object value = result.getValue();
    if (value == null || !Modifier.isPublic(value.getClass().getModifiers())) {
      // If the type is not public then when generateCode produces a checkcast to it
      // then an IllegalAccessError will occur.
      // If resorting to Object isn't sufficient, the hierarchy could be traversed for
      // the first public type.
      this.exitTypeDescriptor = "Ljava/lang/Object";
    }
    else {
      this.exitTypeDescriptor = CodeFlow.toDescriptorFromObject(value);
    }
    // a null value will mean either the value was null or the variable was not found
    return result;
  }

  @Override
  public void setValue(ExpressionState state, @Nullable Object value) throws SpelEvaluationException {
    state.setVariable(this.name, value);
  }

  @Override
  public String toStringAST() {
    return "#" + this.name;
  }

  @Override
  public boolean isWritable(ExpressionState expressionState) throws SpelEvaluationException {
    return !(this.name.equals(THIS) || this.name.equals(ROOT));
  }

  @Override
  public boolean isCompilable() {
    return (this.exitTypeDescriptor != null);
  }

  @Override
  public void generateCode(MethodVisitor mv, CodeFlow cf) {
    if (this.name.equals(ROOT)) {
      mv.visitVarInsn(ALOAD, 1);
    }
    else {
      mv.visitVarInsn(ALOAD, 2);
      mv.visitLdcInsn(this.name);
      mv.visitMethodInsn(INVOKEINTERFACE, "cn/taketoday/expression/EvaluationContext", "lookupVariable", "(Ljava/lang/String;)Ljava/lang/Object;", true);
    }
    CodeFlow.insertCheckCast(mv, this.exitTypeDescriptor);
    cf.pushDescriptor(this.exitTypeDescriptor);
  }

  private static class VariableRef implements ValueRef {

    private final String name;

    private final TypedValue value;

    private final EvaluationContext evaluationContext;

    public VariableRef(String name, TypedValue value, EvaluationContext evaluationContext) {
      this.name = name;
      this.value = value;
      this.evaluationContext = evaluationContext;
    }

    @Override
    public TypedValue getValue() {
      return this.value;
    }

    @Override
    public void setValue(@Nullable Object newValue) {
      this.evaluationContext.setVariable(this.name, newValue);
    }

    @Override
    public boolean isWritable() {
      return true;
    }
  }

}
