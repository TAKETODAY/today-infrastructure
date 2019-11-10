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

package com.sun.el;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

import com.sun.el.lang.ELSupport;
import com.sun.el.parser.AstDeferredExpression;
import com.sun.el.parser.AstDynamicExpression;
import com.sun.el.parser.AstFunction;
import com.sun.el.parser.AstIdentifier;
import com.sun.el.parser.AstLiteralExpression;
import com.sun.el.parser.AstMethodArguments;
import com.sun.el.parser.AstValue;
import com.sun.el.parser.ELParser;
import com.sun.el.parser.Node;
import com.sun.el.parser.NodeVisitor;
import com.sun.el.stream.StreamELResolver;
import com.sun.el.util.ConcurrentCache;
import com.sun.el.util.MessageFactory;

/**
 * @see javax.el.ExpressionFactory
 * 
 * @author Jacob Hookom [jacob@hookom.net]
 * @author Kin-man Chung
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 */
public class ExpressionFactoryImpl extends ExpressionFactory implements NodeVisitor {

    private final Properties properties;
    private final boolean isBackwardCompatible22;
    private final Map<String, Method> functionMap = new HashMap<String, Method>(16, 1.0f);

    public ExpressionFactoryImpl() {
        this(null);
    }

    public ExpressionFactoryImpl(Properties properties) {
        this.properties = properties;
        this.isBackwardCompatible22 = "true".equals(getProperty("javax.el.bc2.2"));
    }

    @Override
    public Object coerceToType(Object obj, Class<?> type) {
        try {

            return ELSupport.coerceToType(obj, type, isBackwardCompatible22);
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

    @Override
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

    @Override
    public ValueExpression createValueExpression(ELContext context, String expression, Class<?> expectedType) {
        // if expectedType == null will not convert object
        return new ValueExpressionImpl(expression, build(expression, context), expectedType);
    }

    @Override
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

    @Override
    public ELResolver getStreamELResolver() {
        return StreamELResolver.getInstance();
    }

    @Override
    public Map<String, Method> getInitFunctionMap() {
        return this.functionMap;
    }

    // -----------------------build

    private static final String CACHE_SIZE_PROP = "javax.el.expression.cache.size";

    private static final ConcurrentCache<String, Node> EXPRESSION_CACHE;
    static {

        String cacheSizeStr;
        if (System.getSecurityManager() == null) {
            cacheSizeStr = System.getProperty(CACHE_SIZE_PROP, "2048");
        }
        else {
            cacheSizeStr = AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(CACHE_SIZE_PROP, "2048");
                }
            });
        }

        EXPRESSION_CACHE = new ConcurrentCache<>(Integer.parseInt(cacheSizeStr));
    }

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

}
