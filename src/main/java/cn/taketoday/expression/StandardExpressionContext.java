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
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.expression.stream.StreamExpressionResolver;
import cn.taketoday.lang.Nullable;

/**
 * A standard ELContext suitable for use in a stand alone environment. This
 * class provides a default implementation of an ELResolver that contains a
 * number of useful ELResolvers. It also provides local repositories for the
 * FunctionMapper, VariableMapper, and BeanNameResolver.
 *
 * @since EL 3.0
 */
public class StandardExpressionContext extends ExpressionContext implements BeanNameResolver {

  private FunctionMapper functionMapper;
  private VariableMapper variableMapper;

  @Nullable
  private final ExpressionContext delegate;

  /**
   * The pre-configured init function map;
   */
  private Map<String, Method> initFunctionMap;

  private ExpressionResolver streamResolver;

  private ExpressionResolver expressionResolver;

  private ExpressionResolverComposite customResolvers;

  private final HashMap<String, Object> variables = new LinkedHashMap<>();

  public StandardExpressionContext() {
    this.delegate = null;
  }

  /**
   * Construct a default ELContext for a stand-alone environment.
   *
   * @param factory The ExpressionFactory
   */
  public StandardExpressionContext(ExpressionFactory factory) {
    this.delegate = null;
    this.streamResolver = factory.getStreamResolver();
    this.initFunctionMap = factory.getInitFunctionMap();
  }

  /**
   * Construct a StandardContext from another ELContext.
   *
   * @param delegate The ELContext that acts as a delegate in most cases
   */
  public StandardExpressionContext(@Nullable ExpressionContext delegate) {
    this.delegate = delegate;
    this.customResolvers = new ExpressionResolverComposite();
    if (delegate != null) {
      // Copy all attributes except map and resolved
      this.expressionResolver = new ExpressionResolverComposite(
              customResolvers,
              new BeanNameExpressionResolver(this),
              delegate.getResolver()
      );
      this.functionMapper = delegate.getFunctionMapper();
      this.variableMapper = delegate.getVariableMapper();
      setLocale(delegate.getLocale());
    }
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
    if (expressionResolver == null) {
      ExpressionResolverComposite resolver = new ExpressionResolverComposite(9);
      this.customResolvers = new ExpressionResolverComposite();
      resolver.add(customResolvers);
      resolver.add(new BeanNameExpressionResolver(this));
      if (streamResolver != null) {
        resolver.add(streamResolver);
      }
      resolver.add(new StaticFieldExpressionResolver());
      resolver.add(new MapExpressionResolver());
      resolver.add(new ResourceBundleExpressionResolver());
      resolver.add(new ListExpressionResolver());
      resolver.add(new ArrayExpressionResolver());
      resolver.add(new BeanPropertyExpressionResolver());
      this.expressionResolver = resolver;
    }
    return expressionResolver;
  }

  /**
   * @param streamResolver streamResolver
   * @see StreamExpressionResolver
   * @since 4.0
   */
  public void setStreamResolver(ExpressionResolver streamResolver) {
    this.streamResolver = streamResolver;
  }

  /**
   * Add a custom ELResolver to the context. The list of the custom ELResolvers
   * will be accessed in the order they are added. A custom ELResolver added to
   * the context cannot be removed.
   *
   * @param expressionResolver The new ELResolver to be added to the context
   */
  public void addResolver(ExpressionResolver expressionResolver) {
    if (this.expressionResolver == null) {
      getResolver(); // make sure elResolver is constructed
    }
    customResolvers.add(expressionResolver);
  }

  /**
   * Get the local bean repository
   *
   * @return the bean repository
   */
  public Map<String, Object> getVariables() {
    return variables;
  }

  /**
   * Define a bean in the local bean repository
   *
   * @param name The name of the bean
   * @param bean The bean instance to be defined. If null, the definition of the
   * bean is removed.
   */
  public Object setVariable(String name, Object bean) {
    return variables.put(name, bean);
  }

  /**
   * add variables
   *
   * @param variables variables
   */
  public void addVariables(Map<String, ?> variables) {
    this.variables.putAll(variables);
  }

  @Override
  public FunctionMapper getFunctionMapper() {
    if (functionMapper == null) {
      functionMapper = new DefaultFunctionMapper(initFunctionMap);
    }
    return functionMapper;
  }

  @Override
  public VariableMapper getVariableMapper() {
    if (variableMapper == null) {
      variableMapper = new DefaultVariableMapper();
    }
    return variableMapper;
  }

  // BeanNameResolver

  @Override
  public void setBeanValue(String beanName, Object value) {
    variables.put(beanName, value);
  }

  @Override
  public boolean isNameResolved(String beanName) {
    return variables.containsKey(beanName);
  }

  @Override
  public Object getBean(String beanName) {
    return variables.get(beanName);
  }

  @Override
  public boolean isReadOnly(String beanName) {
    return false;
  }

  @Override
  public boolean canCreateBean(String beanName) {
    return true;
  }

  private final static class DefaultFunctionMapper extends FunctionMapper {
    private final HashMap<String, Method> functions;

    DefaultFunctionMapper(Map<String, Method> initMap) {
      functions = initMap == null ? new HashMap<>() : new HashMap<>(initMap);
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
    private HashMap<String, ValueExpression> variables;

    @Override
    public ValueExpression resolveVariable(String variable) {
      if (variables == null) {
        return null;
      }
      return variables.get(variable);
    }

    @Override
    public ValueExpression setVariable(String variable, ValueExpression expression) {
      if (variables == null) {
        variables = new HashMap<>();
      }
      ValueExpression prev;
      if (expression == null) {
        prev = variables.remove(variable);
      }
      else {
        prev = variables.put(variable, expression);
      }
      return prev;
    }
  }

}
