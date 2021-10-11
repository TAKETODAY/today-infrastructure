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

package cn.taketoday.expression.parser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import cn.taketoday.lang.Constant;
import cn.taketoday.core.bytecode.commons.MethodSignature;
import cn.taketoday.expression.ExpressionException;
import cn.taketoday.expression.FunctionMapper;
import cn.taketoday.expression.LambdaExpression;
import cn.taketoday.expression.ValueExpression;
import cn.taketoday.expression.VariableMapper;
import cn.taketoday.expression.lang.EvaluationContext;
import cn.taketoday.util.StringUtils;

/**
 * @author Jacob Hookom [jacob@hookom.net]
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 */
public final class AstFunction extends SimpleNode {

  protected String prefix = Constant.BLANK;
  protected String localName = Constant.BLANK;

  public AstFunction(int id) {
    super(id);
  }

  public String getLocalName() {
    return localName;
  }

  public String getOutputName() {
    if (StringUtils.isEmpty(prefix)) {
      return this.localName;
    }
    return prefix + ':' + localName;
  }

  public String getPrefix() {
    return prefix;
  }

  @Override
  public Class<?> getType(EvaluationContext ctx) throws ExpressionException {

    FunctionMapper fnMapper = ctx.getFunctionMapper();

    // quickly validate again for this request
    if (fnMapper == null) {
      throw new ExpressionException("Expression uses functions, but no FunctionMapper was provided");
    }
    Method m = fnMapper.resolveFunction(prefix, localName);
    if (m == null) {
      throw new ExpressionException("Function ''" + this.getOutputName() + "'' not found");
    }
    return m.getReturnType();
  }

  /*
   * Find the object associated with the given name. Return null if the there is
   * no such object.
   */
  private Object findValue(EvaluationContext ctx, String name) {

    // First check if this is a Lambda argument
    if (ctx.isLambdaArgument(name)) {
      return ctx.getLambdaArgument(name);
    }

    // Next check if this an EL variable
    final VariableMapper varMapper = ctx.getVariableMapper();
    if (varMapper != null) {
      final ValueExpression expr = varMapper.resolveVariable(name);
      if (expr != null) {
        return expr.getValue(ctx);
      }
    }
    // Check if this is resolvable by an ELResolver
    ctx.setPropertyResolved(false);
    final Object ret = ctx.getResolver().getValue(ctx, null, name);
    if (ctx.isPropertyResolved()) {
      return ret;
    }
    return null;
  }

  @Override
  public Object getValue(EvaluationContext ctx) throws ExpressionException {

    final Node[] children = this.children;
    final String localName = this.localName;
    // Check to see if a function is a bean that is a Lambdaexpression.
    // If so, invoke it. Also allow for the case that a Lambda expression
    // can return another Lambda expression.
    if (prefix.isEmpty()) {
      Object val = findValue(ctx, localName);
      // Check the case of repeated lambda invocation, such as f()()()
      if (val instanceof LambdaExpression) {
        for (final Node child : children) {
          Object[] params = ((AstMethodArguments) child).getParameters(ctx);
          if (!(val instanceof LambdaExpression)) {
            throw new ExpressionException("Syntax error in calling function ''" + getOutputName() + "''");
          }
          val = ((LambdaExpression) val).invoke(ctx, params);
        }
        return val;
      }
    }

    final FunctionMapper fnMapper = ctx.getFunctionMapper();

    Method m = fnMapper.resolveFunction(prefix, localName);
    if (m == null) {
      if (prefix.isEmpty() && ctx.getImportHandler() != null) {
        // Check if this is a constructor call for an imported class
        Class<?> c = ctx.getImportHandler().resolveClass(localName);
        String methodName = null;
        if (c != null) {
          methodName = MethodSignature.CONSTRUCTOR_NAME;
        }
        else {
          // Check if this is a imported static method
          c = ctx.getImportHandler().resolveStatic(localName);
          methodName = localName;
        }
        if (c != null) {
          // Use StaticFieldELResolver to invoke the constructor or the static method.
          final Object[] params = ((AstMethodArguments) children[0]).getParameters(ctx);
          return ctx.getResolver().invoke(ctx, c, methodName, null, params);
        }
      }
      // quickly validate for this request
      throw new ExpressionException("Function ''" + this.getOutputName() + "'' not found");
    }

    final Class<?>[] paramTypes = m.getParameterTypes();
    final Object[] params = ((AstMethodArguments) children[0]).getParameters(ctx);
    try {
      for (int i = 0; i < params.length; i++) {
        params[i] = ctx.convertToType(params[i], paramTypes[i]);
      }
    }
    catch (ExpressionException ele) {
      throw new ExpressionException("Problems calling function '" + this.getOutputName() + "'", ele);
    }

    try {
      return m.invoke(null, params); // static method
    }
    catch (IllegalAccessException iae) {
      throw new ExpressionException("Problems calling function '" + this.getOutputName() + "'", iae);
    }
    catch (InvocationTargetException ite) {
      throw new ExpressionException("Problems calling function '" + this.getOutputName() + "'", ite.getCause());
    }
  }

  public void setLocalName(String localName) {
    this.localName = localName;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  @Override
  public String toString() {
    return ELParserTreeConstants.NODE_NAME[id] + "[" + this.getOutputName() + "]";
  }
}
