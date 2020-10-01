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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.expression.lang.LocalBeanNameResolver;

/**
 * A standard ELContext suitable for use in a stand alone environment. This
 * class provides a default implementation of an ELResolver that contains a
 * number of useful ELResolvers. It also provides local repositories for the
 * FunctionMapper, VariableMapper, and BeanNameResolver.
 *
 * @since EL 3.0
 */
public class StandardExpressionContext extends ExpressionContext {

  private final ExpressionResolver elResolver;
  private final CompositeExpressionResolver customResolvers;

  private final FunctionMapper functionMapper;
  private final VariableMapper variableMapper;

  private final Map<String, Object> beans = new HashMap<>(8);

  private final ExpressionContext delegate;

  /**
   * Construct a default ELContext for a stand-alone environment.
   * <p>
   * Retrieves the <code>ELResolver</code> associated with this context. This is a
   * <code>CompositeELResover</code> consists of an ordered list of
   * <code>ELResolver</code>s.
   * <ol>
   * <li>A {@link BeanNameExpressionResolver} for beans defined locally</li>
   * <li>Any custom <code>ELResolver</code>s</li>
   * <li>An <code>ELResolver</code> supporting the collection operations</li>
   * <li>A {@link StaticFieldExpressionResolver} for resolving static fields</li>
   * <li>A {@link MapExpressionResolver} for resolving Map properties</li>
   * <li>A {@link ResourceBundleExpressionResolver} for resolving ResourceBundle
   * properties</li>
   * <li>A {@link ListExpressionResolver} for resolving List properties</li>
   * <li>An {@link ArrayExpressionResolver} for resolving array properties</li>
   * <li>A {@link BeanExpressionResolver} for resolving bean properties</li>
   * </ol>
   * </p>
   *
   * @param factory
   *         The ExpressionFactory
   */
  public StandardExpressionContext(ExpressionFactory factory) {

    delegate = null;
    variableMapper = new DefaultVariableMapper();
    functionMapper = new DefaultFunctionMapper(factory.getInitFunctionMap());

    customResolvers = new CompositeExpressionResolver(2);

    CompositeExpressionResolver resolver = new CompositeExpressionResolver(
            customResolvers,
            new BeanNameExpressionResolver(new LocalBeanNameResolver(beans)),

            new StaticFieldExpressionResolver(),
            new MapExpressionResolver(),
            new ResourceBundleExpressionResolver(),
            new ListExpressionResolver(),
            new ArrayExpressionResolver()
    );

    ExpressionResolver streamELResolver = factory.getStreamELResolver();
    if (streamELResolver != null) {
      resolver.add(streamELResolver);
    }
    resolver.add(new BeanExpressionResolver());

    this.elResolver = resolver;
  }

  /**
   * Construct a StandardELContext from another ELContext.
   *
   * @param context
   *         The ELContext that acts as a delegate in most cases
   */
  public StandardExpressionContext(ExpressionContext context) {
    this.delegate = context;
    this.customResolvers = new CompositeExpressionResolver();
    // Copy all attributes except map and resolved
    this.elResolver = new CompositeExpressionResolver(
            customResolvers,
            new BeanNameExpressionResolver(new LocalBeanNameResolver(beans)),
            context.getResolver()
    );

    this.functionMapper = context.getFunctionMapper();
    this.variableMapper = context.getVariableMapper();
    setLocale(context.getLocale());
  }

  @Override
  public void putContext(Class<?> key, Object contextObject) {
    if (delegate != null) {
      delegate.putContext(key, contextObject);
    }
    else {
      super.putContext(key, contextObject);
    }
  }

  @Override
  public Object getContext(Class<?> key) {
    if (delegate == null) {
      return super.getContext(key);
    }
    return delegate.getContext(key);
  }

  /**
   * @return The ELResolver for this context.
   */
  @Override
  public ExpressionResolver getResolver() {
    return elResolver;
  }

  /**
   * Add a custom ELResolver to the context. The list of the custom ELResolvers
   * will be accessed in the order they are added. A custom ELResolver added to
   * the context cannot be removed.
   *
   * @param cELResolver
   *         The new ELResolver to be added to the context
   */
  public void addResolver(ExpressionResolver cELResolver) {
    customResolvers.add(cELResolver);
  }

  /**
   * Get the local bean repository
   *
   * @return the bean repository
   */
  public Map<String, Object> getBeans() {
    return beans;
  }

  /**
   * Define a bean in the local bean repository
   *
   * @param name
   *         The name of the bean
   * @param bean
   *         The bean instance to be defined. If null, the definition of the
   *         bean is removed.
   */
  public Object defineBean(String name, Object bean) {
    return beans.put(name, bean);
  }

  @Override
  public FunctionMapper getFunctionMapper() {
    return functionMapper;
  }

  @Override
  public VariableMapper getVariableMapper() {
    return variableMapper;
  }

  private final static class DefaultFunctionMapper extends FunctionMapper {

    private final Map<String, Method> functions;

    DefaultFunctionMapper(Map<String, Method> initMap) {
      functions = (initMap == null) ? new HashMap<String, Method>(16, 1.0f) : initMap;
    }

    @Override
    public Method resolveFunction(String prefix, String localName) {
      return functions.get(prefix + ":" + localName);
    }

    @Override
    public void mapFunction(String prefix, String localName, Method meth) {
      functions.put(prefix + ":" + localName, meth);
    }
  }

  private final static class DefaultVariableMapper extends VariableMapper {

    private final Map<String, ValueExpression> variables = new HashMap<>();

    @Override
    public ValueExpression resolveVariable(String variable) {
      return variables.get(variable);
    }

    @Override
    public ValueExpression setVariable(String variable, ValueExpression expression) {
      if (expression == null) {
        return variables.remove(variable);
      }
      return variables.put(variable, expression);
    }
  }

}
