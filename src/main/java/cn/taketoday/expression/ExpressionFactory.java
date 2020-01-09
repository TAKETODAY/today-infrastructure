/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.context.utils.ConcurrentCache;
import cn.taketoday.expression.lang.ExpressionSupport;
import cn.taketoday.expression.parser.AstDeferredExpression;
import cn.taketoday.expression.parser.AstDynamicExpression;
import cn.taketoday.expression.parser.AstFunction;
import cn.taketoday.expression.parser.AstIdentifier;
import cn.taketoday.expression.parser.AstLiteralExpression;
import cn.taketoday.expression.parser.AstMethodArguments;
import cn.taketoday.expression.parser.AstValue;
import cn.taketoday.expression.parser.ELParser;
import cn.taketoday.expression.parser.Node;
import cn.taketoday.expression.parser.NodeVisitor;
import cn.taketoday.expression.stream.StreamELResolver;
import cn.taketoday.expression.util.MessageFactory;

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
 * <p>
 * The {@link #newInstance} method can be used to obtain an instance of the
 * implementation. Technologies such as JavaServer Pages and JavaServer Faces
 * provide access to an implementation via factory methods.
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
 * {@link ELResolver} associated with the {@link ELContext} passed to the
 * <code>ValueExpression</code> or <code>MethodExpression</code>.
 * </p>
 *
 * <p>
 * The ELContext object also provides access to the {@link FunctionMapper} and
 * {@link VariableMapper} to be used when parsing the expression. EL function
 * and variable mapping is performed at parse-time, and the results are bound to
 * the expression. Therefore, the {@link ELContext}, {@link FunctionMapper}, and
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
 * {@link ELException} to be thrown:
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
    private final Map<String, Method> functionMap = new HashMap<>();

    private static final String CACHE_SIZE_PROP = "expression.cache.size";
    private static final ExpressionFactory sharedExpressionFactory;
    private static final ConcurrentCache<String, Node> EXPRESSION_CACHE;

    static {
        String cacheSizeStr = System.getSecurityManager() == null
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
     *            The object to coerce.
     * @param targetType
     *            The target type for the coercion.
     * @throws ELException
     *             thrown if an error results from applying the conversion rules.
     */
    public Object coerceToType(Object obj, Class<?> type) {
        try {
            return ExpressionSupport.coerceToType(obj, type);
        }
        catch (IllegalArgumentException ex) {
            throw new ELException(ex);
        }
    }

    protected Node build(final String expression, ELContext context) throws ELException {
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
     *            The EL context used to parse the expression. The
     *            <code>FunctionMapper</code> and <code>VariableMapper</code> stored
     *            in the ELContext are used to resolve functions and variables found
     *            in the expression. They can be <code>null</code>, in which case
     *            functions or variables are not supported for this expression. The
     *            object returned must invoke the same functions and access the same
     *            variable mappings regardless of whether the mappings in the
     *            provided <code>FunctionMapper</code> and
     *            <code>VariableMapper</code> instances change between calling
     *            <code>ExpressionFactory.createMethodExpression()</code> and any
     *            method on <code>MethodExpression</code>.
     *            <p>
     *            Note that within the EL, the ${} and #{} syntaxes are treated
     *            identically. This includes the use of VariableMapper and
     *            FunctionMapper at expression creation time. Each is invoked if not
     *            null, independent of whether the #{} or ${} syntax is used for the
     *            expression.
     *            </p>
     *
     * @param expression
     *            The expression to parse
     * @param expectedReturnType
     *            The expected return type for the method to be found. After
     *            evaluating the expression, the <code>MethodExpression</code> must
     *            check that the return type of the actual method matches this type.
     *            Passing in a value of <code>null</code> indicates the caller does
     *            not care what the return type is, and the check is disabled.
     * @param expectedParamTypes
     *            The expected parameter types for the method to be found. Must be
     *            an array with no elements if there are no parameters expected. It
     *            is illegal to pass <code>null</code>, unless the method is
     *            specified with arugments in the EL expression, in which case these
     *            arguments are used for method selection, and this parameter is
     *            ignored.
     * @return The parsed expression
     * @throws ELException
     *             Thrown if there are syntactical errors in the provided
     *             expression.
     * @throws NullPointerException
     *             if paramTypes is <code>null</code>.
     */

    public MethodExpression createMethodExpression(ELContext context, String expression, Class<?> expectedReturnType, //
                                                   Class<?>[] expectedParamTypes)//
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
            throw new ELException("Not a Valid Method Expression: " + expression);
        }

        if (expectedParamTypes == null && !methodExpression.isParametersProvided()) {
            throw new NullPointerException(MessageFactory.get("error.method.nullParms"));
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
     *            The EL context used to parse the expression. The
     *            <code>FunctionMapper</code> and <code>VariableMapper</code> stored
     *            in the ELContext are used to resolve functions and variables found
     *            in the expression. They can be <code>null</code>, in which case
     *            functions or variables are not supported for this expression. The
     *            object returned must invoke the same functions and access the same
     *            variable mappings regardless of whether the mappings in the
     *            provided <code>FunctionMapper</code> and
     *            <code>VariableMapper</code> instances change between calling
     *            <code>ExpressionFactory.createValueExpression()</code> and any
     *            method on <code>ValueExpression</code>.
     *            <p>
     *            Note that within the EL, the ${} and #{} syntaxes are treated
     *            identically. This includes the use of VariableMapper and
     *            FunctionMapper at expression creation time. Each is invoked if not
     *            null, independent of whether the #{} or ${} syntax is used for the
     *            expression.
     *            </p>
     * @param expression
     *            The expression to parse
     * @param expectedType
     *            The type the result of the expression will be coerced to after
     *            evaluation.
     * @return The parsed expression
     * @throws NullPointerException
     *             Thrown if expectedType is null.
     * @throws ELException
     *             Thrown if there are syntactical errors in the provided
     *             expression.
     */

    public ValueExpression createValueExpression(ELContext context, String expression, Class<?> expectedType) {
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
     *            The object instance to be wrapped.
     * @param expectedType
     *            The type the result of the expression will be coerced to after
     *            evaluation. There will be no coercion if it is Object.class,
     * @throws NullPointerException
     *             Thrown if expectedType is null.
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

    public ELResolver getStreamELResolver() {
        return StreamELResolver.getInstance();
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

    public static Node createNode(final String expr) throws ELException {

        if (expr == null) {
            throw new ELException(MessageFactory.get("error.null"));
        }

        Node node = EXPRESSION_CACHE.get(expr);

        if (node == null) {
            node = ELParser.parse(expr);
            // validate composite expression
            int numChildren = node.jjtGetNumChildren();
            if (numChildren == 1) {
                node = node.jjtGetChild(0);
            }
            else {
                Class<?> type = null;
                Node child = null;
                for (int i = 0; i < numChildren; i++) {
                    child = node.jjtGetChild(i);
                    if (child instanceof AstLiteralExpression) {
                        continue;
                    }
                    if (type == null) {
                        type = child.getClass();
                    }
                    else {
                        if (!type.equals(child.getClass())) {
                            throw new ELException(MessageFactory.get("error.mixed", expr));
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
    protected void prepare(Node node, ELContext context) throws ELException {
        node.accept(this, context);
    }

    // ------------------------NodeVisitor

    @Override
    public void visit(Node node, ELContext context) throws ELException {

        if (node instanceof AstFunction) {

            FunctionMapper fnMapper = context.getFunctionMapper();
            VariableMapper varMapper = context.getVariableMapper();

            AstFunction funcNode = (AstFunction) node;
            if ((funcNode.getPrefix().length() == 0) && //
                (fnMapper == null || fnMapper.resolveFunction(funcNode.getPrefix(), funcNode.getLocalName()) == null)) //
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
                throw new ELException(MessageFactory.get("error.fnMapper.null"));
            }
            Method m = fnMapper.resolveFunction(funcNode.getPrefix(), funcNode.getLocalName());
            if (m == null) {
                throw new ELException(MessageFactory.get("error.fnMapper.method", funcNode.getOutputName()));
            }
            int pcnt = m.getParameterTypes().length;
            int acnt = ((AstMethodArguments) node.jjtGetChild(0)).getParameterCount();
            if (acnt != pcnt) {
                throw new ELException(MessageFactory.get("error.fnMapper.paramcount", funcNode.getOutputName(), "" + pcnt, "" + acnt));
            }
        }
        else if (node instanceof AstIdentifier) {

            final VariableMapper varMapper = context.getVariableMapper();
            if (varMapper != null) {
                // simply capture it
                varMapper.resolveVariable(((AstIdentifier) node).getImage());
            }
        }
    }

    public static ExpressionFactory getSharedInstance() {
        return sharedExpressionFactory;
    }
}
