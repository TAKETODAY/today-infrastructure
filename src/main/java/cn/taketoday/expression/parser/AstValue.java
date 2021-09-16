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

import java.lang.reflect.Method;

import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.expression.ExpressionException;
import cn.taketoday.expression.ExpressionResolver;
import cn.taketoday.expression.ImportHandler;
import cn.taketoday.expression.MethodInfo;
import cn.taketoday.expression.PropertyNotFoundException;
import cn.taketoday.expression.PropertyNotWritableException;
import cn.taketoday.expression.ValueReference;
import cn.taketoday.expression.lang.EvaluationContext;
import cn.taketoday.expression.lang.ExpressionUtils;
import cn.taketoday.expression.util.ReflectionUtil;

/**
 * @author Jacob Hookom [jacob@hookom.net]
 * @author Kin-man Chung
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 */
public final class AstValue extends SimpleNode {

  public AstValue(int id) {
    super(id);
  }

  public Class<?> getType(EvaluationContext ctx) throws ExpressionException {
    Target t = getTarget(ctx);
    if (t.isMethodCall()) {
      return null;
    }
    Object property = t.suffixNode.getValue(ctx);
    ctx.setPropertyResolved(false);
    Class<?> ret = ctx.getResolver().getType(ctx, t.base, property);
    if (!ctx.isPropertyResolved()) {
      ctx.handlePropertyNotResolved(t.base, image, ctx);
    }
    return ret;
  }

  public ValueReference getValueReference(EvaluationContext ctx) throws ExpressionException {
    Target t = getTarget(ctx);
    if (t.isMethodCall()) {
      return null;
    }
    Object property = t.suffixNode.getValue(ctx);
    return new ValueReference(t.base, property);
  }

  private static AstMethodArguments getArguments(Node n) {

    if (n instanceof AstDotSuffix && n.jjtGetNumChildren() > 0) {
      return (AstMethodArguments) n.jjtGetChild(0);
    }
    if (n instanceof AstBracketSuffix && n.jjtGetNumChildren() > 1) {
      return (AstMethodArguments) n.jjtGetChild(1);
    }
    return null;
  }

  private Object getValue(Object base, Node child, EvaluationContext ctx) throws ExpressionException {

    final ExpressionResolver resolver = ctx.getResolver();
    final Object property = child.getValue(ctx);
    final AstMethodArguments args = getArguments(child);
    if (args != null) {
      // This is a method call
      if (property instanceof String) {
        ctx.setPropertyResolved(false);
        return resolver.invoke(ctx, base, property, args.getParamTypes(), args.getParameters(ctx));
      }
      throw new ExpressionException("An instance of " + property + " is specified as the static method name, it must be a String");
    }
    Object value = null;
    if (property != null) {
      ctx.setPropertyResolved(false);
      value = resolver.getValue(ctx, base, property);
      if (!ctx.isPropertyResolved()) {
        final Object resolved = ctx.handlePropertyNotResolved(base, image, ctx);
        if (resolved != null) {
          return resolved;
        }
      }
    }
    return value;
  }

  private Object getBase(EvaluationContext ctx) {
    final Node child = this.children[0];
    try {
      return child.getValue(ctx);
    }
    catch (PropertyNotFoundException ex) {
      // Next check if the base is an imported class
      if (child instanceof AstIdentifier) {
        String name = ((AstIdentifier) child).image;
        ImportHandler importHandler = ctx.getImportHandler();
        if (importHandler != null) {
          Class<?> c = importHandler.resolveClass(name);
          if (c != null) {
            return c;
          }
        }
      }
      throw ex;
    }
  }

  private Target getTarget(EvaluationContext ctx) throws ExpressionException {
    // evaluate expr-a to value-a
    Object base = getBase(ctx);

    // if our base is null (we know there are more properites to evaluate)
    if (base == null) {
      throw new PropertyNotFoundException("Target Unreachable, identifier ''" + children[0].getImage() + "'' resolved to null");
    }

    // set up our start/end
    int propCount = this.jjtGetNumChildren() - 1;
    int i = 1;

    // evaluate any properties before our target
    final Node[] children = this.children;
    if (propCount > 1) {
      while (base != null && i < propCount) {
        base = getValue(base, children[i], ctx);
        i++;
      }
      // if we are in this block, we have more properties to resolve,
      // but our base was null
      if (base == null) {
        throw new PropertyNotFoundException("Target Unreachable, returned null");
      }
    }
    return new Target(base, children[propCount]);
  }

  @Override
  public Object getValue(EvaluationContext ctx) throws ExpressionException {
    Object base = getBase(ctx);
    int propCount = this.jjtGetNumChildren();
    int i = 1;
    final Node[] children = this.children;
    while (base != null && i < propCount) {
      base = getValue(base, children[i], ctx);
      i++;
    }
    return base;
  }

  @Override
  public boolean isReadOnly(EvaluationContext ctx) throws ExpressionException {
    Target t = getTarget(ctx);
    if (t.isMethodCall()) {
      return true;
    }
    Object property = t.suffixNode.getValue(ctx);
    ctx.setPropertyResolved(false);
    boolean ret = ctx.getResolver().isReadOnly(ctx, t.base, property);
    if (!ctx.isPropertyResolved()) {
      ctx.handlePropertyNotResolved(t.base, image, ctx);
    }
    return ret;
  }

  @Override
  public void setValue(EvaluationContext ctx, Object value) throws ExpressionException {
    Target t = getTarget(ctx);
    if (t.isMethodCall()) {
      throw new PropertyNotWritableException("Illegal Syntax for Set Operation");
    }
    Object property = t.suffixNode.getValue(ctx);
    ExpressionResolver elResolver = ctx.getResolver();

    /*
     * Note by kchung 10/2013 The spec does not say if the value should be cocerced
     * to the target type before setting the value to the target. The conversion is
     * kept here to be backward compatible.
     */
    ctx.setPropertyResolved(false);
    Class<?> targetType = elResolver.getType(ctx, t.base, property);
    if (ctx.isPropertyResolved()) {
      ctx.setPropertyResolved(false);
      Object targetValue = elResolver.convertToType(ctx, value, targetType);

      if (ctx.isPropertyResolved()) {
        value = targetValue;
      }
      else {
        if (value != null || targetType.isPrimitive()) {
          value = ExpressionUtils.coerceToType(value, targetType);
        }
      }
    }

    ctx.setPropertyResolved(false);
    elResolver.setValue(ctx, t.base, property, value);
    if (!ctx.isPropertyResolved()) {
      ctx.handlePropertyNotResolved(t.base, image, ctx);
    }
  }

  @Override
  public MethodInfo getMethodInfo(EvaluationContext ctx, Class<?>[] paramTypes) throws ExpressionException {
    Target t = getTarget(ctx);
    if (t.isMethodCall()) {
      return null;
    }
    Object property = t.suffixNode.getValue(ctx);
    Method m = ReflectionUtil.findMethod(t.base.getClass(), property.toString(), paramTypes, null);
    return new MethodInfo(m.getName(), m.getReturnType(), m.getParameterTypes());
  }

  @Override
  public Object invoke(EvaluationContext ctx, Class<?>[] paramTypes, Object[] paramValues) throws ExpressionException {
    Target t = getTarget(ctx);
    if (t.isMethodCall()) {
      AstMethodArguments args = getArguments(t.suffixNode);
      // Always use the param types in the expression, and ignore those
      // specified elsewhere, such as TLD
      paramTypes = args.getParamTypes();
      Object[] params = args.getParameters(ctx);
      String method = (String) t.suffixNode.getValue(ctx);

      ctx.setPropertyResolved(false);
      ExpressionResolver resolver = ctx.getResolver();
      return resolver.invoke(ctx, t.base, method, paramTypes, params);
    }

    // @since 3.0.4
    MethodInvoker targetInvoker = this.targetInvoker;
    if (targetInvoker == null) {
      final Object property = t.suffixNode.getValue(ctx); // maybe this property can dynamic
      final Method method = ReflectionUtil.findMethod(t.base.getClass(), property.toString(), paramTypes, paramValues);
      targetInvoker = MethodInvoker.fromMethod(method);
      this.targetInvoker = targetInvoker;
    }
    return ReflectionUtil.invokeMethod(ctx, targetInvoker, t.base, paramValues);
  }

  /** @since 3.0.4 native invoke target method */
  private MethodInvoker targetInvoker;

  @Override
  public boolean isParametersProvided() {
    return getArguments(this.children[this.jjtGetNumChildren() - 1]) != null;
  }

  static class Target {
    public final Object base;
    public final Node suffixNode;

    Target(Object base, Node suffixNode) {
      this.base = base;
      this.suffixNode = suffixNode;
    }

    boolean isMethodCall() {
      return getArguments(suffixNode) != null;
    }
  }

}
