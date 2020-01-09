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

import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.expression.ELContext;
import cn.taketoday.expression.ELException;
import cn.taketoday.expression.MethodInfo;
import cn.taketoday.expression.PropertyNotWritableException;
import cn.taketoday.expression.ValueReference;
import cn.taketoday.expression.lang.ExpressionSupport;
import cn.taketoday.expression.lang.EvaluationContext;
import cn.taketoday.expression.util.MessageFactory;

/**
 * @author Jacob Hookom [jacob@hookom.net]
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 */
public abstract class SimpleNode extends ExpressionSupport implements Node {

    protected Node parent;

    protected Node[] children;

    protected final int id;

    protected String image;

    public SimpleNode(int i) {
        id = i;
    }

    public void jjtOpen() {}

    public void jjtClose() {}

    public void jjtSetParent(Node n) {
        parent = n;
    }

    public Node jjtGetParent() {
        return parent;
    }

    public void jjtAddChild(Node n, int i) {
        if (children == null) {
            children = new Node[i + 1];
        }
        else if (i >= children.length) {
            Node c[] = new Node[i + 1];
            System.arraycopy(children, 0, c, 0, children.length);
            children = c;
        }
        children[i] = n;
    }

    public Node jjtGetChild(int i) {
        return children[i];
    }

    public int jjtGetNumChildren() {
        return (children == null) ? 0 : children.length;
    }

    /*
     * You can override these two methods in subclasses of SimpleNode to customize
     * the way the node appears when the tree is dumped. If your output uses more
     * than one line you should override toString(String), otherwise overriding
     * toString() is probably all you need to do.
     */
    @Override
    public String toString() {
        if (this.image != null) {
            return ELParserTreeConstants.jjtNodeName[id] + "[" + this.image + "]";
        }
        return ELParserTreeConstants.jjtNodeName[id];
    }

    public String toString(String prefix) {
        return prefix + toString();
    }

    /**
     * Override this method if you want to customize how the node dumps out its
     * children.
     */
    public void dump(String prefix) {
        System.out.println(toString(prefix));
        if (children != null) {
            for (int i = 0; i < children.length; ++i) {
                SimpleNode n = (SimpleNode) children[i];
                if (n != null) {
                    n.dump(prefix + " ");
                }
            }
        }
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    @Override
    public Class<?> getType(EvaluationContext ctx) throws ELException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getValue(EvaluationContext ctx) throws ELException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ValueReference getValueReference(EvaluationContext ctx) throws ELException {
        return null;
    }

    @Override
    public boolean isReadOnly(EvaluationContext ctx) throws ELException {
        return true;
    }

    @Override
    public void setValue(EvaluationContext ctx, Object value) throws ELException {
        throw new PropertyNotWritableException(MessageFactory.get("error.syntax.set"));
    }

    @Override
    public void accept(NodeVisitor visitor, ELContext context) throws ELException {
        visitor.visit(this, context);
        final Node[] children = this.children;
        if (ObjectUtils.isNotEmpty(children)) {
            for (final Node node : children) {
                node.accept(visitor, context);
            }
        }
    }

    @Override
    public Object invoke(EvaluationContext ctx, Class<?>[] paramTypes, Object[] paramValues) throws ELException {
        throw new UnsupportedOperationException();
    }

    @Override
    public MethodInfo getMethodInfo(EvaluationContext ctx, Class<?>[] paramTypes) throws ELException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object node) {

        if (!(node instanceof SimpleNode)) {
            return false;
        }
        SimpleNode n = (SimpleNode) node;
        if (id != n.id) {
            return false;
        }
        final Node[] children = this.children;
        if (children == null && n.children == null) {
            if (image == null) {
                return n.image == null;
            }
            return image.equals(n.image);
        }
        if (children == null || n.children == null) {
            // One is null and the other is non-null
            return false;
        }
        if (children.length != n.children.length) {
            return false;
        }
        if (children.length == 0) {
            if (image == null) {
                return n.image == null;
            }
            return image.equals(n.image);
        }
        for (int i = 0; i < children.length; i++) {
            if (!children[i].equals(n.children[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isParametersProvided() {
        return false;
    }

    @Override
    public int hashCode() {
        final Node[] children = this.children;
        if (children == null || children.length == 0) {
            if (image != null) {
                return image.hashCode();
            }
            return id;
        }
        int h = 0;
        for (int i = children.length - 1; i >= 0; i--) {
            h = h + h + h + children[i].hashCode();
        }
        h = h + h + h + id;
        return h;
    }
}
