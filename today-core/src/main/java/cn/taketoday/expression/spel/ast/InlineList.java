/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.core.CodeFlow;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.expression.spel.SpelNode;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Represent a list in an expression, e.g. '{1,2,3}'
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class InlineList extends SpelNodeImpl {

  @Nullable
  private final TypedValue constant;

  public InlineList(int startPos, int endPos, SpelNodeImpl... args) {
    super(startPos, endPos, args);
    this.constant = computeConstant();
  }

  /**
   * If all the components of the list are constants, or lists
   * that themselves contain constants, then a constant list
   * can be built to represent this node. This will speed up
   * later getValue calls and reduce the amount of garbage
   * created.
   */
  @Nullable
  private TypedValue computeConstant() {
    for (int c = 0, max = getChildCount(); c < max; c++) {
      SpelNode child = getChild(c);
      if (!(child instanceof Literal)) {
        if (child instanceof InlineList inlineList) {
          if (!inlineList.isConstant()) {
            return null;
          }
        }
        else if (!(child instanceof OpMinus opMinus) || !opMinus.isNegativeNumberLiteral()) {
          return null;
        }
      }
    }

    ArrayList<Object> constantList = new ArrayList<>();
    int childcount = getChildCount();
    ExpressionState expressionState = new ExpressionState(new StandardEvaluationContext());
    for (int c = 0; c < childcount; c++) {
      SpelNode child = getChild(c);
      if (child instanceof Literal literal) {
        constantList.add(literal.getLiteralValue().getValue());
      }
      else if (child instanceof InlineList inlineList) {
        constantList.add(inlineList.getConstantValue());
      }
      else if (child instanceof OpMinus) {
        constantList.add(child.getValue(expressionState));
      }
    }
    return new TypedValue(Collections.unmodifiableList(constantList));
  }

  @Override
  public TypedValue getValueInternal(ExpressionState expressionState) throws EvaluationException {
    if (this.constant != null) {
      return this.constant;
    }
    else {
      ArrayList<Object> returnValue = new ArrayList<>(getChildCount());
      for (SpelNodeImpl child : children) {
        returnValue.add(child.getValue(expressionState));
      }
      return new TypedValue(returnValue);
    }
  }

  @Override
  public String toStringAST() {
    StringJoiner sj = new StringJoiner(",", "{", "}");
    // String ast matches input string, not the 'toString()' of the resultant collection, which would use []
    for (SpelNodeImpl child : children) {
      sj.add(child.toStringAST());
    }
    return sj.toString();
  }

  /**
   * Return whether this list is a constant value.
   */
  public boolean isConstant() {
    return constant != null;
  }

  @SuppressWarnings("unchecked")
  @Nullable
  public List<Object> getConstantValue() {
    Assert.state(constant != null, "No constant");
    return (List<Object>) constant.getValue();
  }

  @Override
  public boolean isCompilable() {
    return isConstant();
  }

  @Override
  public void generateCode(MethodVisitor mv, CodeFlow codeflow) {
    final String constantFieldName = "inlineList$" + codeflow.nextFieldId();
    final String className = codeflow.getClassName();

    codeflow.registerNewField((cw, cflow) ->
            cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
                    constantFieldName, "Ljava/util/List;", null, null));

    codeflow.registerNewClinit((mVisitor, cflow) ->
            generateClinitCode(className, constantFieldName, mVisitor, cflow, false));

    mv.visitFieldInsn(GETSTATIC, className, constantFieldName, "Ljava/util/List;");
    codeflow.pushDescriptor("Ljava/util/List");
  }

  void generateClinitCode(String clazzname, String constantFieldName, MethodVisitor mv, CodeFlow codeflow, boolean nested) {
    mv.visitTypeInsn(NEW, "java/util/ArrayList");
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
    if (!nested) {
      mv.visitFieldInsn(PUTSTATIC, clazzname, constantFieldName, "Ljava/util/List;");
    }
    for (SpelNodeImpl child : children) {
      if (!nested) {
        mv.visitFieldInsn(GETSTATIC, clazzname, constantFieldName, "Ljava/util/List;");
      }
      else {
        mv.visitInsn(DUP);
      }
      // The children might be further lists if they are not constants. In this
      // situation do not call back into generateCode() because it will register another clinit adder.
      // Instead, directly build the list here:
      if (child instanceof InlineList inlineList) {
        inlineList.generateClinitCode(clazzname, constantFieldName, mv, codeflow, true);
      }
      else {
        child.generateCode(mv, codeflow);
        String lastDesc = codeflow.lastDescriptor();
        if (CodeFlow.isPrimitive(lastDesc)) {
          CodeFlow.insertBoxIfNecessary(mv, lastDesc.charAt(0));
        }
      }
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
      mv.visitInsn(POP);
    }
  }

}
