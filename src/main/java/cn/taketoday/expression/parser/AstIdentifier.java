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

import cn.taketoday.expression.ELException;
import cn.taketoday.expression.ImportHandler;
import cn.taketoday.expression.MethodExpression;
import cn.taketoday.expression.MethodInfo;
import cn.taketoday.expression.MethodNotFoundException;
import cn.taketoday.expression.PropertyNotWritableException;
import cn.taketoday.expression.ValueExpression;
import cn.taketoday.expression.ValueReference;
import cn.taketoday.expression.VariableMapper;
import cn.taketoday.expression.lang.ELSupport;
import cn.taketoday.expression.lang.EvaluationContext;
import cn.taketoday.expression.util.MessageFactory;

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
    public Class<?> getType(EvaluationContext ctx) throws ELException {
        final String image = this.image;
        // First check if this is a lambda argument
        if (ctx.isLambdaArgument(image)) {
            return Object.class;
        }
        final VariableMapper varMapper = ctx.getVariableMapper();
        if (varMapper != null) {
            ValueExpression expr = varMapper.resolveVariable(image);
            if (expr != null) {
                return expr.getType(ctx);
            }
        }
        ctx.setPropertyResolved(false);
        final Class<?> ret = ctx.getELResolver().getType(ctx, null, image);
        if (!ctx.isPropertyResolved()) {
            ELSupport.throwUnhandled(null, image);
        }
        return ret;
    }

    public ValueReference getValueReference(final EvaluationContext ctx) throws ELException {
        final String image = this.image;
        final VariableMapper varMapper = ctx.getVariableMapper();
        if (varMapper != null) {
            final ValueExpression expr = varMapper.resolveVariable(image);
            if (expr != null) {
                return expr.getValueReference(ctx);
            }
        }
        return new ValueReference(null, image);
    }

    @Override
    public Object getValue(final EvaluationContext ctx) throws ELException {
        final String image = this.image;
        // First check if this is a lambda argument
        if (ctx.isLambdaArgument(image)) {
            return ctx.getLambdaArgument(image);
        }

        final ValueExpression expr = ctx.getVariableMapper().resolveVariable(image);
        if (expr != null) {
            return expr.getValue(ctx);
        }
        ctx.setPropertyResolved(false);
        final Object ret = ctx.getELResolver().getValue(ctx, null, image);
        if (!ctx.isPropertyResolved()) {
            // Check if this is an imported static field
            final ImportHandler importHandler = ctx.getImportHandler();
            if (importHandler != null) {
                final Class<?> c = importHandler.resolveStatic(image);
                if (c != null) {
                    return ctx.getELResolver().getValue(ctx, c, image);
                }
            }
//            return ""; //TODO
            ELSupport.throwUnhandled(null, image);
        }
        return ret;
    }

    public boolean isReadOnly(final EvaluationContext ctx) throws ELException {
        final String image = this.image;
        // Lambda arguments are read only.
        if (ctx.isLambdaArgument(image)) {
            return true;
        }
        final VariableMapper varMapper = ctx.getVariableMapper();
        if (varMapper != null) {
            final ValueExpression expr = varMapper.resolveVariable(image);
            if (expr != null) {
                return expr.isReadOnly(ctx);
            }
        }
        ctx.setPropertyResolved(false);
        boolean ret = ctx.getELResolver().isReadOnly(ctx, null, image);
        if (!ctx.isPropertyResolved()) {
            ELSupport.throwUnhandled(null, image);
        }
        return ret;
    }

    public void setValue(final EvaluationContext ctx, final Object value) throws ELException {
        final String image = this.image;
        // First check if this is a lambda argument
        if (ctx.isLambdaArgument(image)) {
            throw new PropertyNotWritableException(MessageFactory.get("error.lambda.parameter.readonly",
                                                                      image));
        }
        final VariableMapper varMapper = ctx.getVariableMapper();
        if (varMapper != null) {
            ValueExpression expr = varMapper.resolveVariable(image);
            if (expr != null) {
                expr.setValue(ctx, value);
                return;
            }
        }
        ctx.setPropertyResolved(false);
        ctx.getELResolver().setValue(ctx, null, image, value);
        if (!ctx.isPropertyResolved()) {
            ELSupport.throwUnhandled(null, image);
        }
    }

    public Object invoke(final EvaluationContext ctx,
                         final Class<?>[] paramTypes, final Object[] paramValues) throws ELException {
        return getMethodExpression(ctx).invoke(ctx, paramValues);
    }

    public MethodInfo getMethodInfo(final EvaluationContext ctx, final Class<?>[] paramTypes) throws ELException {
        return getMethodExpression(ctx).getMethodInfo(ctx);
    }

    protected MethodExpression getMethodExpression(final EvaluationContext ctx) throws ELException {
        // case A: ValueExpression exists, getValue which must  be a MethodExpression
        Object obj = null;
        final String image = this.image;
        final VariableMapper varMapper = ctx.getVariableMapper();
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
            obj = ctx.getELResolver().getValue(ctx, null, image);
        }

        // finally provide helpful hints
        if (obj instanceof MethodExpression) {
            return (MethodExpression) obj;
        }
        else if (obj == null) {
            throw new MethodNotFoundException("Identity '" + image + "' was null and was unable to invoke");
        }
        throw new ELException("Identity '" + image + "' does not reference a MethodExpression instance, returned type: " + obj
                .getClass().getName());
    }
}
