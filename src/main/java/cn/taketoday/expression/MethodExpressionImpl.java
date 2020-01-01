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

import cn.taketoday.expression.lang.EvaluationContext;
import cn.taketoday.expression.parser.Node;

/**
 * An <code>Expression</code> that refers to a method on an object.
 * 
 * <p>
 * <code>The {@link ExpressionFactory#createMethodExpression} method
 * can be used to parse an expression string and return a concrete instance
 * of <code>MethodExpression</code> that encapsulates the parsed expression. The
 * {@link FunctionMapper} is used at parse time, not evaluation time, so one is
 * not needed to evaluate an expression using this class. However, the
 * {@link ELContext} is needed at evaluation time.
 * </p>
 *
 * <p>
 * The {@link #getMethodInfo} and {@link #invoke} methods will evaluate the
 * expression each time they are called. The {@link ELResolver} in the
 * <code>ELContext</code> is used to resolve the top-level variables and to
 * determine the behavior of the <code>.</code> and <code>[]</code> operators.
 * For any of the two methods, the {@link ELResolver#getValue} method is used to
 * resolve all properties up to but excluding the last one. This provides the
 * <code>base</code> object on which the method appears. If the
 * <code>base</code> object is null, a <code>NullPointerException</code> must be
 * thrown. At the last resolution, the final <code>property</code> is then
 * coerced to a <code>String</code>, which provides the name of the method to be
 * found. A method matching the name and expected parameters provided at parse
 * time is found and it is either queried or invoked (depending on the method
 * called on this <code>MethodExpression</code>).
 * </p>
 *
 * <p>
 * See the notes about comparison, serialization and immutability in the
 * {@link Expression} javadocs.
 *
 * @see cn.taketoday.expression.ELResolver
 * @see cn.taketoday.expression.Expression
 * @see cn.taketoday.expression.ExpressionFactory
 * @see cn.taketoday.expression.MethodExpression
 * 
 * @author Jacob Hookom [jacob@hookom.net]
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 */
@SuppressWarnings("serial")
public final class MethodExpressionImpl extends MethodExpression {

    private Node node;
    private final String expr;
    private final Class<?>[] paramTypes;
    private final Class<?> expectedType;

    public MethodExpressionImpl() {
        this(null, null, null, null);
    }

    /**
     * 
     * @param expr
     * @param node
     * @param paramTypes
     */
    public MethodExpressionImpl(String expr, Node node, Class<?>[] paramTypes, Class<?> expectedType) {
        super();
        this.expr = expr;
        this.node = node;
        this.paramTypes = paramTypes;
        this.expectedType = expectedType;
    }

    /**
     * Returns the original String used to create this <code>Expression</code>,
     * unmodified.
     * 
     * <p>
     * This is used for debugging purposes but also for the purposes of comparison
     * (e.g. to ensure the expression in a configuration file has not changed).
     * </p>
     * 
     * <p>
     * This method does not provide sufficient information to re-create an
     * expression. Two different expressions can have exactly the same expression
     * string but different function mappings. Serialization should be used to save
     * and restore the state of an <code>Expression</code>.
     * </p>
     * 
     * @return The original expression String.
     * 
     * @see cn.taketoday.expression.Expression#getExpressionString()
     */
    public String getExpressionString() {
        return this.expr;
    }

    /**
     * Evaluates the expression relative to the provided context, and returns
     * information about the actual referenced method.
     * 
     * @param context
     *            The context of this evaluation
     * @return an instance of <code>MethodInfo</code> containing information about
     *         the method the expression evaluated to.
     * @throws NullPointerException
     *             if context is <code>null</code> or the base object is
     *             <code>null</code> on the last resolution.
     * @throws PropertyNotFoundException
     *             if one of the property resolutions failed because a specified
     *             variable or property does not exist or is not readable.
     * @throws MethodNotFoundException
     *             if no suitable method can be found.
     * @throws ELException
     *             if an exception was thrown while performing property or variable
     *             resolution. The thrown exception must be included as the cause
     *             property of this exception, if available.
     * @see cn.taketoday.expression.MethodExpression#getMethodInfo(cn.taketoday.expression.ELContext)
     */
    public MethodInfo getMethodInfo(ELContext context)
            throws PropertyNotFoundException, MethodNotFoundException, ELException//
    {
        return getNode().getMethodInfo(new EvaluationContext(context), this.paramTypes);
    }

    private final Node getNode() throws ELException {
        final Node node = this.node;
        if (node == null) {
            return this.node = ExpressionFactory.createNode(this.expr);
        }
        return node;
    }

    /**
     * Returns the hash code for this <code>Expression</code>.
     * 
     * <p>
     * See the note in the {@link #equals} method on how two expressions can be
     * equal if their expression Strings are different. Recall that if two objects
     * are equal according to the <code>equals(Object)</code> method, then calling
     * the <code>hashCode</code> method on each of the two objects must produce the
     * same integer result. Implementations must take special note and implement
     * <code>hashCode</code> correctly.
     * </p>
     * 
     * @return The hash code for this <code>Expression</code>.
     * @see #equals
     * @see java.util.Hashtable
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return getNode().hashCode();
    }

    /**
     * Evaluates the expression relative to the provided context, invokes the method
     * that was found using the supplied parameters, and returns the result of the
     * method invocation.
     * 
     * @param context
     *            The context of this evaluation.
     * @param params
     *            The parameters to pass to the method, or <code>null</code> if no
     *            parameters.
     * @return the result of the method invocation (<code>null</code> if the method
     *         has a <code>void</code> return type).
     * @throws NullPointerException
     *             if context is <code>null</code> or the base object is
     *             <code>null</code> on the last resolution.
     * @throws PropertyNotFoundException
     *             if one of the property resolutions failed because a specified
     *             variable or property does not exist or is not readable.
     * @throws MethodNotFoundException
     *             if no suitable method can be found.
     * @throws ELException
     *             if an exception was thrown while performing property or variable
     *             resolution. The thrown exception must be included as the cause
     *             property of this exception, if available. If the exception thrown
     *             is an <code>InvocationTargetException</code>, extract its
     *             <code>cause</code> and pass it to the <code>ELException</code>
     *             constructor.
     * @see cn.taketoday.expression.MethodExpression#invoke(cn.taketoday.expression.ELContext,
     *      java.lang.Object[])
     */
    public Object invoke(final ELContext context, Object[] params) throws PropertyNotFoundException, //
            MethodNotFoundException, ELException //
    {
        Object value = this.getNode().invoke(new EvaluationContext(context), this.paramTypes, params);

        final Class<?> expectedType = this.expectedType;
        if (value != null && expectedType != null && !expectedType.isInstance(value)) {
            try {
                value = context.convertToType(value, expectedType);
            }
            catch (IllegalArgumentException ex) {
                throw new ELException(ex);
            }
        }

        return value;
    }

    public boolean isLiteralText() {
        return false;
    }

    @Override
    public boolean isParametersProvided() {
        return this.getNode().isParametersProvided();
    }

    /**
     * Determines whether the specified object is equal to this
     * <code>Expression</code>.
     * 
     * <p>
     * The result is <code>true</code> if and only if the argument is not
     * <code>null</code>, is an <code>Expression</code> object that is the of the
     * same type (<code>ValueExpression</code> or <code>MethodExpression</code>),
     * and has an identical parsed representation.
     * </p>
     * 
     * <p>
     * Note that two expressions can be equal if their expression Strings are
     * different. For example, <code>${fn1:foo()}</code> and
     * <code>${fn2:foo()}</code> are equal if their corresponding
     * <code>FunctionMapper</code>s mapped <code>fn1:foo</code> and
     * <code>fn2:foo</code> to the same method.
     * </p>
     * 
     * @param obj
     *            the <code>Object</code> to test for equality.
     * @return <code>true</code> if <code>obj</code> equals this
     *         <code>Expression</code>; <code>false</code> otherwise.
     * @see java.util.Hashtable
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj instanceof MethodExpressionImpl) {
            MethodExpressionImpl me = (MethodExpressionImpl) obj;
            return getNode().equals(me.getNode());
        }
        return false;
    }

}
