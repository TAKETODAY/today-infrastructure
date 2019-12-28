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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import cn.taketoday.expression.ELContext;
import cn.taketoday.expression.ELManager;
import cn.taketoday.expression.ELProcessor;
import cn.taketoday.expression.ExpressionFactory;
import cn.taketoday.expression.MethodExpression;

public class ELProcessorTest {

    static ELProcessor elp;
    static ELManager elm;
    static ExpressionFactory factory;

    @BeforeClass
    public static void setUpClass() throws Exception {
        elp = new ELProcessor();
        elm = elp.getELManager();
        factory = elm.getExpressionFactory();
    }

    @Before
    public void setUp() {}

    @Test
    public void testMethExpr() {
        MethodExpression meth = null;
        ELContext ctxt = elm.getELContext();
        try {
            meth = factory.createMethodExpression(ctxt, "#{str.length}", Object.class, null);
        }
        catch (NullPointerException ex) {
            // Do nothing
        }
        assertTrue(meth == null);
        meth = factory.createMethodExpression(
                                              ctxt, "#{'abc'.length()}", Object.class, null);
        Object result = meth.invoke(ctxt, new Object[] { "abcde" });
        System.out.println("'abc'.length() called, equals " + result);
        assertEquals(result, new Integer(3));
    }

    @Test
    public void testGetValue() {
        Object result = elp.eval("10 + 1");
        assertEquals(result.toString(), "11");
        result = elp.getValue("10 + 2", String.class);
        assertEquals(result, "12");
    }

    @Test
    public void testSetVariable() {
        elp.setVariable("xx", "100");
        Object result = elp.getValue("xx + 11", String.class);
        assertEquals(result, "111");
        elp.setVariable("xx", null);
        assertEquals(elp.eval("xx"), null);
        elp.setVariable("yy", "abc");
        assertEquals(elp.eval("yy = 123; abc"), 123L);
        assertEquals(elp.eval("abc = 456; yy"), 456L);
    }

    @Test
    public void testConcat() {
        Object result = elp.eval("'10' + 1");
        assertEquals(result, 11L);
        result = elp.eval("10 += '1'");
        assertEquals(result.toString(), "101");
    }

    @Test
    public void defineFuncTest() {
        Class c = MyBean.class;
        Method meth = null;
        Method meth2 = null;
        try {
            meth = c.getMethod("getBar", new Class<?>[] {});
            meth2 = c.getMethod("getFoo", new Class<?>[] {});
        }
        catch (Exception e) {
            System.out.printf("Exception: ", e);
        }
        try {
            elp.defineFunction("xx", "", meth);
            Object ret = elp.eval("xx:getBar() == 64");
            assertTrue((Boolean) ret);
        }
        catch (NoSuchMethodException ex) {

        }

        boolean caught = false;
        try {
            elp.defineFunction("", "", meth2);
            Object ret = elp.eval("getFoo() == 100");
            assertTrue((Boolean) ret);
        }
        catch (NoSuchMethodException ex) {
            caught = true;
        }
        assertTrue(caught);

        try {
            elp.defineFunction("yy", "", "cn.taketoday.context.el.test.ELProcessorTest$MyBean", "getBar");
            Object ret = elp.eval("yy:getBar() == 64");
            assertTrue((Boolean) ret);
        }
        catch (ClassNotFoundException | NoSuchMethodException ex) {

        }

        caught = false;
        try {
            elp.defineFunction("yy", "", "cn.taketoday.context.el.test.ELProcessorTest$MyBean", "getFooBar");
            Object ret = elp.eval("yy:getBar() == 100");
            assertTrue((Boolean) ret);
        }
        catch (ClassNotFoundException | NoSuchMethodException ex) {
            caught = true;
        }
        assertTrue(caught);

        caught = false;
        try {
            elp.defineFunction("yy", "", "testBean", "getFoo");
            Object ret = elp.eval("yy:getBar() == 100");
            assertTrue((Boolean) ret);
        }
        catch (ClassNotFoundException | NoSuchMethodException ex) {
            caught = true;
        }
        assertTrue(caught);
    }
    /*
     * @Test public void testBean() { elp.defineBean("xyz", new MyBean()); Object
     * result = elp.eval("xyz.foo"); assertEquals(result.toString(), "100"); }
     */

    @Test
    public void testImport() {
        elm.importClass("cn.taketoday.context.el.ELProcessorTest$MyBean");
        assertTrue((Boolean) elp.eval("ELProcessorTest$MyBean.aaaa == 101"));
        assertTrue((Boolean) elp.eval("ELProcessorTest$MyBean.getBar() == 64"));
        elm.importStatic("cn.taketoday.context.el.ELProcessorTest$MyBean.aaaa");
        assertEquals(new Integer(101), elp.eval("aaaa"));
        elm.importStatic("cn.taketoday.context.el.ELProcessorTest$MyBean.getBar");
        assertEquals(new Integer(64), elp.eval("getBar()"));
        /*
         * elm.importStatic("a.b.NonExisting.foobar"); elp.eval("foobar");
         * elp.eval("ELProcessorTest$MyBean.getFoo()");
         */
    }

    static public class MyBean {
        public static int aaaa = 101;

        public int getFoo() {
            return 100;
        }

        public int getFoo(int i) {
            return 200;
        }

        public static int getBar() {
            return 64;
        }
    }
}
