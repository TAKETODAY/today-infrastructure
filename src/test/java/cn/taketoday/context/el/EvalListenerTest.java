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

import java.util.ArrayList;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import cn.taketoday.expression.ELContext;
import cn.taketoday.expression.EvaluationListener;
import cn.taketoday.expression.ExpressionManager;
import cn.taketoday.expression.ExpressionProcessor;

/**
 *
 * @author kichung
 */
public class EvalListenerTest {

    public EvalListenerTest() {}

    @BeforeClass
    public static void setUpClass() throws Exception {}

    @AfterClass
    public static void tearDownClass() throws Exception {}

    @Before
    public void setUp() {}

    @After
    public void tearDown() {}

    @Test
    public void testEvalListener() {
        ExpressionProcessor elp = new ExpressionProcessor();
        ExpressionManager elm = elp.getELManager();
        final ArrayList<String> msgs = new ArrayList<String>();
        elm.addEvaluationListener(new EvaluationListener() {
            @Override
            public void beforeEvaluation(ELContext ctxt, String expr) {
                System.out.println("Before: " + expr);
                msgs.add("Before: " + expr);
            }

            @Override
            public void afterEvaluation(ELContext ctxt, String expr) {
                System.out.println("After: " + expr);
                msgs.add("After: " + expr);
            }
        });
        elp.eval("100 + 10");
        elp.eval("x = 5; x*101");
        String[] expected = { "Before: ${100 + 10}", "After: ${100 + 10}", "Before: ${x = 5; x*101}", "After: ${x = 5; x*101}" };
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], msgs.get(i));
        }
    }

    @Test
    public void testResListener() {
        ExpressionProcessor elp = new ExpressionProcessor();
        ExpressionManager elm = elp.getELManager();
        final ArrayList<String> msgs = new ArrayList<String>();
        elm.addEvaluationListener(new EvaluationListener() {
            @Override
            public void propertyResolved(ELContext ctxt, Object b, Object p) {
                System.out.println("Resolved: " + b + " " + p);
                msgs.add("Resolved: " + b + " " + p);
            }
        });
        elp.eval("x = 10");
        elp.eval("[1,2,3][2]");
        elp.eval("'abcd'.length()");
        String[] expected = { "Resolved: null x", "Resolved: [1, 2, 3] 2", "Resolved: abcd length" };
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], msgs.get(i));
        }
    }
}
