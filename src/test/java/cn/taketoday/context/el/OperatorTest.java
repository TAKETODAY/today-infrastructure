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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import cn.taketoday.expression.ExpressionManager;
import cn.taketoday.expression.ExpressionProcessor;
import cn.taketoday.expression.ValueExpression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Kin-man
 */
public class OperatorTest {

  static ExpressionProcessor elp;

  public OperatorTest() { }

  @BeforeAll
  public static void setUpClass() throws Exception {
    elp = new ExpressionProcessor();
  }

  @AfterAll
  public static void tearDownClass() throws Exception { }

  public void setUp() { }

  void testExpr(String testname, String expr, Long expected) {
    System.out.println("=== Test " + testname + " ===");
    System.out.println(" ** " + expr);
    Object result = elp.eval(expr);
    System.out.println("    returns " + result);
    assertEquals(expected, result);
  }

  void testExpr(String testname, String expr, String expected) {
    System.out.println("=== Test " + testname + " ===");
    System.out.println(" ** " + expr);
    Object result = elp.eval(expr);
    System.out.println("    returns " + result);
    assertEquals(expected, result);
  }

  @Test
  public void testConcat() {
    testExpr("concat", "a = null; b = null; a + b", 0L);
    testExpr("add", "10 + 11", 21L);
    testExpr("concat", "'10' + 11", 21L);
    testExpr("concat 2", "11 + '10'", 21L);
    testExpr("concat 3", "100 += 10 ", "10010");
    testExpr("concat 4", "'100' += 10", "10010");
    testExpr("concat 5", "'100' + 10 + 1", 111L);
    testExpr("concat 6", "'100' += 10 + 1", "10011");
  }

  @Test
  public void testAssign() {
    elp.eval("vv = 10");
    testExpr("assign", "vv+1", 11L);
    elp.eval("vv = 100");
    testExpr("assign 2", "vv", 100L);
    testExpr("assign 3", "x = vv = vv+1; x + vv", 202L);
    elp.eval("map = {'one':100, 'two':200}");
    testExpr("assign 4", "map.two = 201; map.two", 201L);
    testExpr("assign string", "x='string'; x += 1", "string1");
  }

  @Test
  public void testSemi() {
    testExpr("semi", "10; 20", 20L);
    testExpr("semi0", "10; 20; 30", 30L);
    elp.eval("x = 10; 20");
    testExpr("semi 2", "x", 10L);
    testExpr("semi 3", "(x = 10; 20) + (x ; x+1)", 31L);
    testExpr("semi 4", "(x = 10; y) = 11; x + y", 21L);
  }

  @Test
  public void testMisc() {
    testExpr("quote", "\"'\"", "'");
    testExpr("quote", "'\"'", "\"");
    ExpressionManager elm = elp.getManager();
    ValueExpression v = elm.getExpressionFactory().createValueExpression(
            elm.getContext(), "#${1+1}", Object.class);
    Object ret = v.getValue(elm.getContext());
    assertEquals(ret, "#2");

    elp.setVariable("debug", "true");
    ret = elp.eval("debug == true");
//        elp.eval("[1,2][true]"); // throws IllegalArgumentExpression
    /*
     * elp.defineBean("date", new Date(2013, 1,2)); elp.eval("date.getYear()");
     * elp.defineBean("href", null); testExpr("space", "(empty href)?'#':href",
     * "#"); MethodExpression m = elm.getExpressionFactory().createMethodExpression(
     * elm.getELContext(), "${name}", Object.class, new Class[] {});
     * m.invoke(elm.getELContext(), null);
     */
  }

  boolean booleanEval(String exp) {
    return (boolean) elp.eval(exp);
  }

  @Test
  public void testBoolean() {
//        elp.eval("1==2");

    assertTrue(null == null);
    assertFalse(booleanEval("1==2"));
    assertTrue(booleanEval("1>2||2>1"));
    assertFalse(booleanEval("1>2&&2<1"));
    assertFalse(booleanEval("1>2!=2<1"));
    assertFalse(booleanEval("1>=2||2<=1"));
    assertTrue(booleanEval("null<=null"));
    assertTrue(booleanEval("null>=null"));

  }

}
