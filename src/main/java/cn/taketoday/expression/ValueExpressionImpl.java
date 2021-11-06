/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package cn.taketoday.expression;

import java.util.Objects;

import cn.taketoday.expression.lang.EvaluationContext;
import cn.taketoday.expression.parser.AstLiteralExpression;
import cn.taketoday.expression.parser.Node;

/**
 * An <code>Expression</code> that can get or set a value.
 *
 * <p>
 * In previous incarnations of this API, expressions could only be read.
 * <code>ValueExpression</code> objects can now be used both to retrieve a value
 * and to set a value. Expressions that can have a value set on them are
 * referred to as l-value expressions. Those that cannot are referred to as
 * r-value expressions. Not all r-value expressions can be used as l-value
 * expressions (e.g. <code>"${1+1}"</code> or
 * <code>"${firstName} ${lastName}"</code>). See the EL Specification for
 * details. Expressions that cannot be used as l-values must always return
 * <code>true</code> from <code>isReadOnly()</code>.
 * </p>
 *
 * <p>
 * <code>The {@link ExpressionFactory#createValueExpression} method
 * can be used to parse an expression string and return a concrete instance
 * of <code>ValueExpression</code> that encapsulates the parsed expression. The
 * {@link FunctionMapper} is used at parse time, not evaluation time, so one is
 * not needed to evaluate an expression using this class. However, the
 * {@link ExpressionContext} is needed at evaluation time.
 * </p>
 *
 * <p>
 * The {@link #getValue}, {@link #setValue}, {@link #isReadOnly} and
 * {@link #getType} methods will evaluate the expression each time they are
 * called. The {@link ExpressionResolver} in the <code>ELContext</code> is used to
 * resolve the top-level variables and to determine the behavior of the
 * <code>.</code> and <code>[]</code> operators. For any of the four methods,
 * the {@link ExpressionResolver#getValue} method is used to resolve all properties up
 * to but excluding the last one. This provides the <code>base</code> object. At
 * the last resolution, the <code>ValueExpression</code> will call the
 * corresponding {@link ExpressionResolver#getValue}, {@link ExpressionResolver#setValue},
 * {@link ExpressionResolver#isReadOnly} or {@link ExpressionResolver#getType} method, depending
 * on which was called on the <code>ValueExpression</code>.
 * </p>
 *
 * <p>
 * See the notes about comparison, serialization and immutability in the
 * {@link Expression} javadocs.
 *
 * @author Jacob Hookom [jacob@hookom.net]
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: dochez $
 * @see cn.taketoday.expression.ExpressionResolver
 * @see cn.taketoday.expression.Expression
 * @see cn.taketoday.expression.ExpressionFactory
 * @see cn.taketoday.expression.ValueExpression
 */
@SuppressWarnings("serial")
public final class ValueExpressionImpl extends ValueExpression {
  private static final long serialVersionUID = 1L;

  private final String expr;
  private transient Node node;
  private final Class<?> expectedType;

  public ValueExpressionImpl(String expr, Node node, Class<?> expectedType) {
    this.expr = expr;
    this.node = node;
    this.expectedType = expectedType;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ValueExpressionImpl) {
      return getNode().equals(((ValueExpressionImpl) obj).getNode());
    }
    return false;
  }

  @Override
  public Class<?> getExpectedType() {
    return this.expectedType;
  }

  /**
   * Returns the type the result of the expression will be coerced to after
   * evaluation.
   *
   * @return the <code>expectedType</code> passed to the
   * <code>ExpressionFactory.createValueExpression</code> method that
   * created this <code>ValueExpression</code>.
   * @see cn.taketoday.expression.Expression#getExpressionString()
   */
  @Override
  public String getExpressionString() {
    return this.expr;
  }

  /**
   * @return The Node for the expression
   */
  private Node getNode() throws ExpressionException {
    if (this.node == null) {
      this.node = ExpressionFactory.createNode(this.expr);
    }
    return this.node;
  }

  @Override
  public Class<?> getType(ExpressionContext context) throws ExpressionException {
    return getNode().getType(new EvaluationContext(context));
  }

  @Override
  public ValueReference getValueReference(ExpressionContext context) throws ExpressionException {
    return getNode().getValueReference(new EvaluationContext(context));
  }

  @Override
  public Object getValue(final ExpressionContext context) throws ExpressionException {
    Object value = getNode().getValue(new EvaluationContext(context));

    if (value != null && expectedType != null) {
      try {
        if (!expectedType.isInstance(value)) {
          value = context.convertToType(value, expectedType);
        }
      }
      catch (IllegalArgumentException ex) {
        throw new ExpressionException(ex);
      }
    }
    return value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(expr, expectedType);
  }

  @Override
  public boolean isLiteralText() {
    try {
      return this.getNode() instanceof AstLiteralExpression;
    }
    catch (ExpressionException ele) {
      return false;
    }
  }

  @Override
  public boolean isReadOnly(ExpressionContext context) throws ExpressionException {
    return getNode().isReadOnly(new EvaluationContext(context));
  }

  @Override
  public void setValue(ExpressionContext context, Object value) throws ExpressionException {
    getNode().setValue(new EvaluationContext(context), value);
  }

  public String toString() {
    return "ValueExpression[" + this.expr + "]";
  }
}
