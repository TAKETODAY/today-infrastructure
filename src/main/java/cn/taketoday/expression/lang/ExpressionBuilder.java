/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.expression.lang;

import java.lang.reflect.Method;

import cn.taketoday.expression.ExpressionContext;
import cn.taketoday.expression.ExpressionException;
import cn.taketoday.expression.FunctionMapper;
import cn.taketoday.expression.MethodExpression;
import cn.taketoday.expression.MethodExpressionImpl;
import cn.taketoday.expression.MethodExpressionLiteral;
import cn.taketoday.expression.ValueExpression;
import cn.taketoday.expression.ValueExpressionImpl;
import cn.taketoday.expression.VariableMapper;
import cn.taketoday.expression.parser.AstDeferredExpression;
import cn.taketoday.expression.parser.AstDynamicExpression;
import cn.taketoday.expression.parser.AstFunction;
import cn.taketoday.expression.parser.AstIdentifier;
import cn.taketoday.expression.parser.AstLiteralExpression;
import cn.taketoday.expression.parser.AstMethodArguments;
import cn.taketoday.expression.parser.AstValue;
import cn.taketoday.expression.parser.ExpressionParser;
import cn.taketoday.expression.parser.Node;
import cn.taketoday.expression.parser.NodeVisitor;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/25 16:21
 */
public class ExpressionBuilder implements NodeVisitor {

  private FunctionMapper fnMapper;
  private VariableMapper varMapper;
  private final String expression;

  public ExpressionBuilder(String expression, ExpressionContext ctx) throws ExpressionException {
    this(expression, ctx.getFunctionMapper(), ctx.getVariableMapper());
  }

  public ExpressionBuilder(
          String expression, @Nullable FunctionMapper ctxFn, @Nullable VariableMapper ctxVar) throws ExpressionException {
    this.expression = expression;
    if (ctxFn != null) {
      this.fnMapper = new FunctionMapperFactory(ctxFn);
    }
    if (ctxVar != null) {
      this.varMapper = new VariableMapperFactory(ctxVar);
    }
  }

  public static Node parse(final String expr) throws ExpressionException {
    Node node = ExpressionParser.parse(expr);
    // validate composite expression
    int numChildren = node.jjtGetNumChildren();
    if (numChildren == 1) {
      node = node.jjtGetChild(0);
    }
    else {
      Class<?> type = null;
      for (int i = 0; i < numChildren; i++) {
        final Node child = node.jjtGetChild(i);
        if (child instanceof AstLiteralExpression) {
          continue;
        }
        if (type == null) {
          type = child.getClass();
        }
        else {
          if (!type.equals(child.getClass())) {
            throw new ExpressionException("Expression cannot contain both '#{..}' and '${..}' : " + expr);
          }
        }
      }
    }

    if (node instanceof AstDynamicExpression || node instanceof AstDeferredExpression) {
      node = node.jjtGetChild(0);
    }
    return node;
  }

  /**
   * Scan the expression nodes and captures the functions and variables used in
   * this expression. This ensures that any changes to the functions or variables
   * mappings during the expression will not affect the evaluation of this
   * expression, as the functions and variables are bound and resolved at parse
   * time, as specified in the spec.
   */
  private void prepare(Node node) throws ExpressionException {
    node.accept(this);
    if (this.fnMapper instanceof FunctionMapperFactory) {
      this.fnMapper = ((FunctionMapperFactory) this.fnMapper).create();
    }
    if (this.varMapper instanceof VariableMapperFactory) {
      this.varMapper = ((VariableMapperFactory) this.varMapper).create();
    }
  }

  private Node build() throws ExpressionException {
    Node node = createNode(expression);
    this.prepare(node);
    if (node instanceof AstDeferredExpression || node instanceof AstDynamicExpression) {
      node = node.jjtGetChild(0);
    }
    return node;
  }

  protected Node createNode(String expression) {
    return parse(expression);
  }

  public ValueExpression build(Class<?> expectedType) throws ExpressionException {
    Node n = build();
    return new ValueExpressionImpl(this.expression, n, this.fnMapper, this.varMapper, expectedType);
  }

  public MethodExpression build(Class<?> expectedReturnType, Class<?>[] expectedParamTypes) throws ExpressionException {
    Node n = build();
    if (n instanceof AstValue || n instanceof AstIdentifier) {
      return new MethodExpressionImpl(
              expression, n, this.fnMapper, this.varMapper, expectedParamTypes, expectedReturnType);
    }
    else if (n instanceof AstLiteralExpression) {
      return new MethodExpressionLiteral(expression, expectedReturnType, expectedParamTypes);
    }
    throw new ExpressionException("Not a Valid Method Expression: " + expression);
  }

  @Override
  public void visit(Node node) {
    if (node instanceof AstFunction funcNode) {
      final String prefix = funcNode.getPrefix();
      if ((prefix.isEmpty()) && //
              (fnMapper == null || fnMapper.resolveFunction(prefix, funcNode.getLocalName()) == null)) //
      {
        // This can be a call to a LambdaExpression. The target
        // of the call is a bean or an EL variable. Capture
        // the variable name in the variable mapper if it is an
        // variable. The decision to invoke the static method or
        // the LambdaExpression will be made at runtime.
        if (varMapper != null) {
          varMapper.resolveVariable(funcNode.getLocalName());
        }
        return;
      }

      if (fnMapper == null) {
        throw new ExpressionException("Expression uses functions, but no FunctionMapper was provided");
      }
      Method m = fnMapper.resolveFunction(prefix, funcNode.getLocalName());
      if (m == null) {
        throw new ExpressionException("Function ''" + funcNode.getOutputName() + "'' not found");
      }
      int pcnt = m.getParameterTypes().length;
      int acnt = ((AstMethodArguments) node.jjtGetChild(0)).getParameterCount();
      if (acnt != pcnt) {
        throw new ExpressionException(
                "Function ''" + funcNode.getOutputName() + "'' specifies " + pcnt + " params, but " + acnt + " were supplied");
      }
    }
    else if (node instanceof AstIdentifier && varMapper != null) {
      // simply capture it
      varMapper.resolveVariable(node.getImage());
    }
  }

}
