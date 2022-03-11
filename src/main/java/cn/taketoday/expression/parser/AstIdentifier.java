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

import cn.taketoday.expression.ExpressionException;
import cn.taketoday.expression.ImportHandler;
import cn.taketoday.expression.MethodExpression;
import cn.taketoday.expression.MethodInfo;
import cn.taketoday.expression.MethodNotFoundException;
import cn.taketoday.expression.PropertyNotWritableException;
import cn.taketoday.expression.ValueExpression;
import cn.taketoday.expression.ValueReference;
import cn.taketoday.expression.VariableMapper;
import cn.taketoday.expression.lang.EvaluationContext;

/**
 * @author Jacob Hookom [jacob@hookom.net]
 * @author Kin-man Chung
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 */
public final class AstIdentifier extends SimpleNode {

  public AstIdentifier(int id) {
    super(id);
  }

  @Override
  public Class<?> getType(EvaluationContext ctx) {
    // First check if this is a lambda argument
    if (ctx.isLambdaArgument(image)) {
      return Object.class;
    }
    VariableMapper varMapper = ctx.getVariableMapper();
    if (varMapper != null) {
      ValueExpression expr = varMapper.resolveVariable(image);
      if (expr != null) {
        return expr.getType(ctx);
      }
    }
    ctx.setPropertyResolved(false);
    Class<?> ret = ctx.getResolver().getType(ctx, null, image);
    if (!ctx.isPropertyResolved()) {
      ctx.handlePropertyNotResolved(null, image, ctx);
    }
    return ret;
  }

  @Override
  public ValueReference getValueReference(EvaluationContext ctx) {
    VariableMapper varMapper = ctx.getVariableMapper();
    if (varMapper != null) {
      ValueExpression expr = varMapper.resolveVariable(image);
      if (expr != null) {
        return expr.getValueReference(ctx);
      }
    }
    return new ValueReference(null, image);
  }

  @Override
  public Object getValue(EvaluationContext ctx) {
    // First check if this is a lambda argument
    if (ctx.isLambdaArgument(image)) {
      return ctx.getLambdaArgument(image);
    }

    VariableMapper varMapper = ctx.getVariableMapper();
    if (varMapper != null) {
      ValueExpression expr = varMapper.resolveVariable(image);
      if (expr != null) {
        return expr.getValue(ctx);
      }
    }

    ctx.setPropertyResolved(false);
    Object ret = ctx.getResolver().getValue(ctx, null, image);
    if (!ctx.isPropertyResolved()) {
      // Check if this is an imported static field
      ImportHandler importHandler = ctx.getImportHandler();
      if (importHandler != null) {
        Class<?> c = importHandler.resolveStatic(image);
        if (c != null) {
          return ctx.getResolver().getValue(ctx, c, image);
        }
      }
      ret = ctx.handlePropertyNotResolved(null, image, ctx);
    }
    return ret;
  }

  @Override
  public boolean isReadOnly(EvaluationContext ctx) {
    // Lambda arguments are read only.
    if (ctx.isLambdaArgument(image)) {
      return true;
    }
    VariableMapper varMapper = ctx.getVariableMapper();
    if (varMapper != null) {
      ValueExpression expr = varMapper.resolveVariable(image);
      if (expr != null) {
        return expr.isReadOnly(ctx);
      }
    }
    ctx.setPropertyResolved(false);
    boolean ret = ctx.getResolver().isReadOnly(ctx, null, image);
    if (!ctx.isPropertyResolved()) {
      ctx.handlePropertyNotResolved(null, image, ctx);
    }
    return ret;
  }

  @Override
  public void setValue(EvaluationContext ctx, Object value) {
    // First check if this is a lambda argument
    if (ctx.isLambdaArgument(image)) {
      throw new PropertyNotWritableException("The Lambda parameter ''" + image + "'' is not writable");
    }
    VariableMapper varMapper = ctx.getVariableMapper();
    if (varMapper != null) {
      ValueExpression expr = varMapper.resolveVariable(image);
      if (expr != null) {
        expr.setValue(ctx, value);
        return;
      }
    }
    ctx.setPropertyResolved(false);
    ctx.getResolver().setValue(ctx, null, image, value);
    if (!ctx.isPropertyResolved()) {
      ctx.handlePropertyNotResolved(null, image, ctx);
    }
  }

  @Override
  public Object invoke(
          EvaluationContext ctx, Class<?>[] paramTypes, Object[] paramValues) {
    return getMethodExpression(ctx).invoke(ctx, paramValues);
  }

  @Override
  public MethodInfo getMethodInfo(EvaluationContext ctx, Class<?>[] paramTypes) {
    return getMethodExpression(ctx).getMethodInfo(ctx);
  }

  private MethodExpression getMethodExpression(EvaluationContext ctx) {
    // case A: ValueExpression exists, getValue which must  be a MethodExpression
    Object obj = null;
    VariableMapper varMapper = ctx.getVariableMapper();
    ValueExpression ve = null;
    if (varMapper != null) {
      ve = varMapper.resolveVariable(image);
      if (ve != null) {
        obj = ve.getValue(ctx);
      }
    }

    // case B: evaluate the identity against the ELResolver, again, must be a MethodExpression to be able to invoke
    if (ve == null) {
      ctx.setPropertyResolved(false);
      obj = ctx.getResolver().getValue(ctx, null, image);
    }

    // finally provide helpful hints
    if (obj instanceof MethodExpression) {
      return (MethodExpression) obj;
    }
    else if (obj == null) {
      throw new MethodNotFoundException("Identity '" + image + "' was null and was unable to invoke");
    }
    throw new ExpressionException(
            "Identity '" + image + "' does not reference a MethodExpression instance, returned type: "
                    + obj.getClass().getName());
  }
}
