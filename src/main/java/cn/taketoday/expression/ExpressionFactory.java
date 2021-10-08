/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.expression;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.expression.lang.ExpressionUtils;
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
import cn.taketoday.expression.stream.StreamExpressionResolver;
import cn.taketoday.util.ConcurrentCache;

/**
 * Provides an implementation for creating and evaluating EL expressions.
 *
 * <p>
 * Classes that implement the EL expression language expose their functionality
 * via this abstract class. An implementation supports the following
 * functionalities.
 * <ul>
 * <li>Parses a <code>String</code> into a {@link ValueExpression} or
 * {@link MethodExpression} instance for later evaluation.</li>
 * <li>Implements an <code>ELResolver</code> for query operators</li>
 * <li>Provides a default type coercion</li>
 * </ul>
 * </p>
 *
 * <p>
 * The {@link #createValueExpression} method is used to parse expressions that
 * evaluate to values (both l-values and r-values are supported). The
 * {@link #createMethodExpression} method is used to parse expressions that
 * evaluate to a reference to a method on an object.
 * </p>
 *
 * <p>
 * Resolution of model objects is performed at evaluation time, via the
 * {@link ExpressionResolver} associated with the {@link ExpressionContext} passed to the
 * <code>ValueExpression</code> or <code>MethodExpression</code>.
 * </p>
 *
 * <p>
 * The ELContext object also provides access to the {@link FunctionMapper} and
 * {@link VariableMapper} to be used when parsing the expression. EL function
 * and variable mapping is performed at parse-time, and the results are bound to
 * the expression. Therefore, the {@link ExpressionContext}, {@link FunctionMapper}, and
 * {@link VariableMapper} are not stored for future use and do not have to be
 * <code>Serializable</code>.
 * </p>
 *
 * <p>
 * The <code>createValueExpression</code> and
 * <code>createMethodExpression</code> methods must be thread-safe. That is,
 * multiple threads may call these methods on the same
 * <code>ExpressionFactory</code> object simultaneously. Implementations should
 * synchronize access if they depend on transient state. Implementations should
 * not, however, assume that only one object of each
 * <code>ExpressionFactory</code> type will be instantiated; global caching
 * should therefore be static.
 * </p>
 *
 * <p>
 * The <code>ExpressionFactory</code> must be able to handle the following types
 * of input for the <code>expression</code> parameter:
 * <ul>
 * <li>Single expressions using the <code>${}</code> delimiter (e.g.
 * <code>"${employee.lastName}"</code>).</li>
 * <li>Single expressions using the <code>#{}</code> delimiter (e.g.
 * <code>"#{employee.lastName}"</code>).</li>
 * <li>Literal text containing no <code>${}</code> or <code>#{}</code>
 * delimiters (e.g. <code>"John Doe"</code>).</li>
 * <li>Multiple expressions using the same delimiter (e.g.
 * <code>"${employee.firstName}${employee.lastName}"</code> or
 * <code>"#{employee.firstName}#{employee.lastName}"</code>).</li>
 * <li>Mixed literal text and expressions using the same delimiter (e.g.
 * <code>"Name: ${employee.firstName} ${employee.lastName}"</code>).</li>
 * </ul>
 * </p>
 *
 * <p>
 * The following types of input are illegal and must cause an
 * {@link ExpressionException} to be thrown:
 * <ul>
 * <li>Multiple expressions using different delimiters (e.g.
 * <code>"${employee.firstName}#{employee.lastName}"</code>).</li>
 * <li>Mixed literal text and expressions using different delimiters(e.g.
 * <code>"Name: ${employee.firstName} #{employee.lastName}"</code>).</li>
 * </ul>
 * </p>
 *
 * @since JSP 2.1
 */
public class ExpressionFactory implements NodeVisitor {

  private final Properties properties;
  private final HashMap<String, Method> functionMap = new HashMap<>();

  private static final String CACHE_SIZE_PROP = "expression.cache.size";
  private static final ExpressionFactory sharedExpressionFactory;
  private static final ConcurrentCache<String, Node> EXPRESSION_CACHE;

  static {
    String cacheSizeStr =
            System.getSecurityManager() == null
            ? System.getProperty(CACHE_SIZE_PROP, "2048")
            : AccessController.doPrivileged((PrivilegedAction<String>) () -> {
              return System.getProperty(CACHE_SIZE_PROP, "2048");
            });

    EXPRESSION_CACHE = new ConcurrentCache<>(Integer.parseInt(cacheSizeStr));
    sharedExpressionFactory = new ExpressionFactory();
  }

  public ExpressionFactory() {
    this(null);
  }

  public ExpressionFactory(Properties properties) {
    this.properties = properties;
  }

  /**
   * Coerces an object to a specific type according to the EL type conversion
   * rules. The custom type conversions in the <code>ELResolver</code>s are not
   * considered.
   *
   * <p>
   * An <code>ELException</code> is thrown if an error results from applying the
   * conversion rules.
   * </p>
   *
   * @param obj
   *         The object to coerce.
   * @param type
   *         The target type for the coercion.
   *
   * @throws ExpressionException
   *         thrown if an error results from applying the conversion rules.
   */
  public Object coerceToType(Object obj, Class<?> type) {
    try {
      return ExpressionUtils.coerceToType(obj, type);
    }
    catch (IllegalArgumentException ex) {
      throw new ExpressionException(ex);
    }
  }

  protected Node build(final String expression, ExpressionContext context) {
    final Node n = createNode(expression);
    this.prepare(n, context);
    return n;
  }

  /**
   * Parses an expression into a {@link MethodExpression} for later evaluation.
   * Use this method for expressions that refer to methods.
   *
   * <p>
   * If the expression is a String literal, a <code>MethodExpression
   * </code> is created, which when invoked, returns the String literal, coerced
   * to expectedReturnType. An ELException is thrown if expectedReturnType is void
   * or if the coercion of the String literal to the expectedReturnType yields an
   * error (see Section "1.16 Type Conversion").
   * </p>
   * <p>
   * This method should perform syntactic validation of the expression. If in
   * doing so it detects errors, it should raise an <code>ELException</code>.
   * </p>
   *
   * @param context
   *         The EL context used to parse the expression. The
   *         <code>FunctionMapper</code> and <code>VariableMapper</code> stored
   *         in the ELContext are used to resolve functions and variables found
   *         in the expression. They can be <code>null</code>, in which case
   *         functions or variables are not supported for this expression. The
   *         object returned must invoke the same functions and access the same
   *         variable mappings regardless of whether the mappings in the
   *         provided <code>FunctionMapper</code> and
   *         <code>VariableMapper</code> instances change between calling
   *         <code>ExpressionFactory.createMethodExpression()</code> and any
   *         method on <code>MethodExpression</code>.
   *         <p>
   *         Note that within the EL, the ${} and #{} syntaxes are treated
   *         identically. This includes the use of VariableMapper and
   *         FunctionMapper at expression creation time. Each is invoked if not
   *         null, independent of whether the #{} or ${} syntax is used for the
   *         expression.
   *         </p>
   * @param expression
   *         The expression to parse
   * @param expectedReturnType
   *         The expected return type for the method to be found. After
   *         evaluating the expression, the <code>MethodExpression</code> must
   *         check that the return type of the actual method matches this type.
   *         Passing in a value of <code>null</code> indicates the caller does
   *         not care what the return type is, and the check is disabled.
   * @param expectedParamTypes
   *         The expected parameter types for the method to be found. Must be
   *         an array with no elements if there are no parameters expected. It
   *         is illegal to pass <code>null</code>, unless the method is
   *         specified with arugments in the EL expression, in which case these
   *         arguments are used for method selection, and this parameter is
   *         ignored.
   *
   * @return The parsed expression
   *
   * @throws ExpressionException
   *         Thrown if there are syntactical errors in the provided
   *         expression.
   * @throws NullPointerException
   *         if paramTypes is <code>null</code>.
   */
  public MethodExpression createMethodExpression(
          ExpressionContext context, String expression, Class<?> expectedReturnType, Class<?>[] expectedParamTypes)//
  {
    MethodExpression methodExpression;

    final Node node = this.build(expression, context);

    if (node instanceof AstValue || node instanceof AstIdentifier) {
      methodExpression = new MethodExpressionImpl(expression, node, expectedParamTypes, expectedReturnType);
    }
    else if (node instanceof AstLiteralExpression) {
      methodExpression = new MethodExpressionLiteral(expression, expectedReturnType, expectedParamTypes);
    }
    else {
      throw new ExpressionException("Not a Valid Method Expression: " + expression);
    }

    if (expectedParamTypes == null && !methodExpression.isParametersProvided()) {
      throw new NullPointerException("Parameter types cannot be null");
    }
    return methodExpression;
  }

  /**
   * Parses an expression into a {@link ValueExpression} for later evaluation. Use
   * this method for expressions that refer to values.
   *
   * <p>
   * This method should perform syntactic validation of the expression. If in
   * doing so it detects errors, it should raise an <code>ELException</code>.
   * </p>
   *
   * @param context
   *         The EL context used to parse the expression. The
   *         <code>FunctionMapper</code> and <code>VariableMapper</code> stored
   *         in the ELContext are used to resolve functions and variables found
   *         in the expression. They can be <code>null</code>, in which case
   *         functions or variables are not supported for this expression. The
   *         object returned must invoke the same functions and access the same
   *         variable mappings regardless of whether the mappings in the
   *         provided <code>FunctionMapper</code> and
   *         <code>VariableMapper</code> instances change between calling
   *         <code>ExpressionFactory.createValueExpression()</code> and any
   *         method on <code>ValueExpression</code>.
   *         <p>
   *         Note that within the EL, the ${} and #{} syntaxes are treated
   *         identically. This includes the use of VariableMapper and
   *         FunctionMapper at expression creation time. Each is invoked if not
   *         null, independent of whether the #{} or ${} syntax is used for the
   *         expression.
   *         </p>
   * @param expression
   *         The expression to parse
   * @param expectedType
   *         The type the result of the expression will be coerced to after
   *         evaluation.
   *
   * @return The parsed expression
   *
   * @throws NullPointerException
   *         Thrown if expectedType is null.
   * @throws ExpressionException
   *         Thrown if there are syntactical errors in the provided
   *         expression.
   */
  public ValueExpression createValueExpression(ExpressionContext context, String expression, Class<?> expectedType) {
    // if expectedType == null will not convert object
    return new ValueExpressionImpl(expression, build(expression, context), expectedType);
  }

  /**
   * Creates a ValueExpression that wraps an object instance. This method can be
   * used to pass any object as a ValueExpression. The wrapper ValueExpression is
   * read only, and returns the wrapped object via its <code>getValue()</code>
   * method, optionally coerced.
   *
   * @param instance
   *         The object instance to be wrapped.
   * @param expectedType
   *         The type the result of the expression will be coerced to after
   *         evaluation. There will be no coercion if it is Object.class,
   *
   * @throws NullPointerException
   *         Thrown if expectedType is null.
   */
  public ValueExpression createValueExpression(Object instance, Class<?> expectedType) {
    // if expectedType == null will not convert object
    return new ValueExpressionLiteral(instance, expectedType);
  }

  public String getProperty(String key) {
    if (properties == null) {
      return null;
    }
    return properties.getProperty(key);
  }

  /**
   * Retrieves an ELResolver that implements the operations in collections.
   *
   * <p>
   * This ELResolver resolves the method invocation on the pair
   * (<code>base</code>, <code>property</code>) when <code>base</code> is a
   * <code>Collection</code> or a <code>Map</code>, and <code>property</code> is
   * the name of the operation.
   * <p>
   * See EL.2 for detailed descriptions of these operators, their arguments, and
   * return values.
   * </p>
   *
   * @return The <code>ELResolver</code> that implements the Query Operators.
   *
   * @since EL 3.0
   */
  public ExpressionResolver getStreamELResolver() {
    return StreamExpressionResolver.getInstance();
  }

  /**
   * Retrieve a function map containing a pre-configured function mapping.
   *
   * @return A initial map for functions, null if there is none.
   *
   * @since EL 3.0
   */
  public Map<String, Method> getInitFunctionMap() {
    return this.functionMap;
  }

  // -----------------------build

  public static Node createNode(final String expr) throws ExpressionException {
    if (expr == null) {
      throw new ExpressionException("Expression cannot be null");
    }

    Node node = EXPRESSION_CACHE.get(expr);
    if (node == null) {
      node = ExpressionParser.parse(expr);
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
      EXPRESSION_CACHE.put(expr, node);
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
  protected void prepare(Node node, ExpressionContext context) {
    node.accept(this, context);
  }

  // ------------------------NodeVisitor

  @Override
  public void visit(Node node, ExpressionContext context) {
    if (node instanceof AstFunction) {

      FunctionMapper fnMapper = context.getFunctionMapper();
      VariableMapper varMapper = context.getVariableMapper();

      AstFunction funcNode = (AstFunction) node;
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
    else if (node instanceof AstIdentifier) {

      final VariableMapper varMapper = context.getVariableMapper();
      if (varMapper != null) {
        // simply capture it
        varMapper.resolveVariable(node.getImage());
      }
    }
  }

  public static ExpressionFactory getSharedInstance() {
    return sharedExpressionFactory;
  }
}
