/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package cn.taketoday.context.el;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.expression.ExpressionProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LambdaTest {

  @BeforeAll
  static void setUpClass() throws Exception { }

  @BeforeEach
  void setUp() { }

  void testExpr(ExpressionProcessor elp, String testname, String expr, Long expected) {
    System.out.println("=== Test Lambda Expression:" + testname + " ===");
    System.out.println(" ** " + expr);
    Object result = elp.eval(expr);
    System.out.println("    returns " + result);
    assertEquals(expected, result);
  }

  @Test
  void testImmediate() {
    ExpressionProcessor elp = new ExpressionProcessor();
    testExpr(elp, "immediate", "(x->x+1)(10)", 11L);
    testExpr(elp, "immediate0", "(()->1001)()", 1001L);
    testExpr(elp, "immediate1", "((x,y)->x+y)(null, null)", 0L);
    testExpr(elp, "immediate 2", "(((x,y)->x+y)(3,4))", 7L);
    testExpr(elp, "immediate 3", "(x->(y=x)+1)(10) + y", 21L);
  }

  @Test
  void testAssignInvoke() {
    ExpressionProcessor elp = new ExpressionProcessor();
    testExpr(elp, "assign", "func = x->x+1; func(10)", 11L);
    testExpr(elp, "assign 2", "func = (x,y)->x+y; func(3,4)", 7L);
  }

  @Test
  void testConditional() {
    ExpressionProcessor elp = new ExpressionProcessor();
    elp.eval("cond = true");
    testExpr(elp, "conditional", "(x->cond? x+1: x+2)(10)", 11L);
    elp.eval("cond = false");
    testExpr(elp, "conditional 2",
            "func = cond? (x->x+1): (x->x+2); func(10)", 12L);
  }

  @Test
  void testFact() {
    ExpressionProcessor elp = new ExpressionProcessor();
    testExpr(elp, "factorial", "fact = n->n==0? 1: n*fact(n-1); fact(5)", 120L);
    testExpr(elp, "fibonacci", "f = n->n==0? 0: n==1? 1: f(n-1)+f(n-2); f(10)", 55L);
  }

  @Test
  void testVar() {
    ExpressionProcessor elp = new ExpressionProcessor();
    elp.setVariable("v", "x->x+1");
    testExpr(elp, "assignment to variable", "v(10)", 11L);
  }

  @Test
  void testLambda() {
    ExpressionProcessor elp = new ExpressionProcessor();
    testExpr(elp, "Lambda Lambda", "f = ()->y->y+1; f()(100)", 101L);
    testExpr(elp, "Lambda Lambda 2", "f = (x)->(tem=x; y->tem+y); f(1)(100)", 101L);
    testExpr(elp, "Lambda Lambda 3", "(()->y->y+1)()(100)", 101L);
    testExpr(elp, "Lambda Lambda 4", "(x->(y->x+y)(1))(100)", 101L);
    testExpr(elp, "Lambda Lambda 5", "(x->(y->x+y))(1)(100)", 101L);
    testExpr(elp, "Lambda Lambda 6", "(x->y->x(0)+y)(x->x+1)(100)", 101L);
  }
}
