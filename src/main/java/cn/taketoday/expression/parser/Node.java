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
import cn.taketoday.expression.MethodInfo;
import cn.taketoday.expression.ValueReference;
import cn.taketoday.expression.lang.EvaluationContext;

/* All AST nodes must implement this interface.  It provides basic
   machinery for constructing the parent and child relationships
   between nodes. */

/**
 * @author Jacob Hookom [jacob@hookom.net]
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 */
public interface Node {

  /**
   * This method is called after the node has been made the current node. It
   * indicates that child nodes can now be added to it.
   */
  void jjtOpen();

  /**
   * This method is called after all the child nodes have been added.
   */
  void jjtClose();

  /**
   * This pair of methods are used to inform the node of its parent.
   */
  void jjtSetParent(Node n);

  Node jjtGetParent();

  /**
   * This method tells the node to add its argument to the node's list of
   * children.
   */
  void jjtAddChild(Node n, int i);

  /**
   * This method returns a child node. The children are numbered from zero, left
   * to right.
   */
  Node jjtGetChild(int i);

  /** Return the number of children the node has. */
  int jjtGetNumChildren();

  String getImage();

  Object getValue(EvaluationContext ctx) throws ExpressionException;

  void setValue(EvaluationContext ctx, Object value) throws ExpressionException;

  Class<?> getType(EvaluationContext ctx) throws ExpressionException;

  ValueReference getValueReference(EvaluationContext ctx) throws ExpressionException;

  boolean isReadOnly(EvaluationContext ctx) throws ExpressionException;

  void accept(NodeVisitor visitor) throws ExpressionException;

  MethodInfo getMethodInfo(EvaluationContext ctx, Class<?>[] paramTypes) throws ExpressionException;

  Object invoke(EvaluationContext ctx, Class<?>[] paramTypes, Object[] paramValues) throws ExpressionException;

  boolean equals(Object n);

  int hashCode();

  boolean isParametersProvided();
}
