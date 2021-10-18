/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.expression.lang.EvaluationContext;

/**
 * <p>
 * Encapsulates a parameterized {@link ValueExpression}.
 * </p>
 *
 * <p>
 * A <code>LambdaExpression</code> is a representation of the EL Lambda
 * expression syntax. It consists of a list of the formal parameters and a body,
 * represented by a {@link ValueExpression}. The body can be any valid
 * <code>Expression</code>, including another <code>LambdaExpression</code>.
 * </p>
 * A <code>LambdaExpression</code> is created when an EL expression containing a
 * Lambda expression is evaluated.
 * </p>
 * <p>
 * A <code>LambdaExpression</code> can be invoked by calling
 * {@link LambdaExpression#invoke}, with an {@link cn.taketoday.expression.ExpressionContext} and a
 * list of the actual arguments. Alternately, a <code>LambdaExpression</code>
 * can be invoked without passing a <code>ELContext</code>, in which case the
 * <code>ELContext</code> previously set by calling
 * {@link LambdaExpression#setELContext} will be used. The evaluation of the
 * <code>ValueExpression</code> in the body uses the {@link ExpressionContext} to
 * resolve references to the parameters, and to evaluate the lambda expression.
 * The result of the evaluation is returned.
 * </p>
 *
 * @see ExpressionContext#getLambdaArgument
 * @see ExpressionContext#enterLambdaScope
 * @see ExpressionContext#exitLambdaScope
 */
public class LambdaExpression {

  private final ExpressionContext context;
  private final ValueExpression expression;
  private final List<String> formalParameters;
  // Arguments from nesting lambdas, when the body is another lambda
  private final Map<String, Object> envirArgs;

  /**
   * Creates a new LambdaExpression.
   *
   * @param formalParameters The list of String representing the formal parameters.
   * @param expression The <code>ValueExpression</code> representing the body.
   * @param context {@link EvaluationContext}
   */
  public LambdaExpression(List<String> formalParameters, ValueExpression expression, ExpressionContext context) {
    this.formalParameters = formalParameters;
    this.expression = expression;
    this.context = context;
    this.envirArgs = new HashMap<>();
  }

  /**
   * Invoke the encapsulated Lambda expression.
   * <p>
   * The supplied arguments are matched, in the same order, to the formal
   * parameters. If there are more arguments than the formal parameters, the extra
   * arguments are ignored. If there are less arguments than the formal
   * parameters, an <code>ELException</code> is thrown.
   * </p>
   *
   * <p>
   * The actual Lambda arguments are added to the ELContext and are available
   * during the evaluation of the Lambda expression. They are removed after the
   * evaluation.
   * </p>
   *
   * @param elContext The ELContext used for the evaluation of the expression The
   * ELContext set by {@link #setELContext} is ignored.
   * @param args The arguments to invoke the Lambda expression. For calls with no
   * arguments, an empty array must be provided. A Lambda argument can
   * be <code>null</code>.
   * @return The result of invoking the Lambda expression
   * @throws ExpressionException if not enough arguments are provided
   * @throws NullPointerException is elContext is null
   */
  public Object invoke(ExpressionContext elContext, Object... args) throws ExpressionException {
    int i = 0;
    // First get arguments injected from the outter lambda, if any
    final HashMap<String, Object> lambdaArgs = new HashMap<>(envirArgs);

    for (String fParam : formalParameters) {
      if (i >= args.length) {
        throw new ExpressionException("Expected Argument " + fParam + " missing in Lambda Expression");
      }
      lambdaArgs.put(fParam, args[i++]);
    }

    elContext.enterLambdaScope(lambdaArgs);
    final Object ret = expression.getValue(elContext);

    // If the result of evaluating the body is another LambdaExpression,
    // whose body has not been evaluated yet. (A LambdaExpression is
    // evaluated iff when its invoke method is called.) The current lambda
    // arguments may be needed in that body when it is evaluated later,
    // after the current lambda exits. To make these arguments available
    // then, they are injected into it.
    if (ret instanceof LambdaExpression) {
      ((LambdaExpression) ret).envirArgs.putAll(lambdaArgs);
    }
    elContext.exitLambdaScope();
    return ret;
  }

  /**
   * Invoke the encapsulated Lambda expression.
   * <p>
   * The supplied arguments are matched, in the same order, to the formal
   * parameters. If there are more arguments than the formal parameters, the extra
   * arguments are ignored. If there are less arguments than the formal
   * parameters, an <code>ELException</code> is thrown.
   * </p>
   *
   * <p>
   * The actual Lambda arguments are added to the ELContext and are available
   * during the evaluation of the Lambda expression. They are removed after the
   * evaluation.
   * </p>
   *
   * The ELContext set by {@link LambdaExpression#setELContext} is used in the
   * evaluation of the lambda Expression.
   *
   * @param args The arguments to invoke the Lambda expression. For calls with no
   * arguments, an empty array must be provided. A Lambda argument can
   * be <code>null</code>.
   * @return The result of invoking the Lambda expression
   * @throws ExpressionException if not enough arguments are provided
   */
  public Object invoke(Object... args) {
    return invoke(this.context, args);
  }
}
