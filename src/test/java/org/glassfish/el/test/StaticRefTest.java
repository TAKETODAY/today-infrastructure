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

package org.glassfish.el.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.el.ELProcessor;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author kichung
 */
public class StaticRefTest {

    ELProcessor elp;

    public StaticRefTest() {}

    @BeforeClass
    public static void setUpClass() throws Exception {}

    @AfterClass
    public static void tearDownClass() throws Exception {}

    @Before
    public void setUp() {
        elp = new ELProcessor();
    }

    @After
    public void tearDown() {}

    @Test
    public void testStaticRef() {
        // Pre imported java.lang classes
//        assertTrue((Boolean)elp.eval("T(java.lang.Boolean).TRUE"));
//        assertTrue((Boolean)elp.eval("T(Boolean).TRUE"));
        assertTrue((Boolean) elp.eval("Boolean.TRUE"));
        assertTrue((Boolean) elp.eval("Boolean.TRUE")); // test caching Boolean
    }

    /*
     * @Test public void testClass() { assertEquals(String.class,
     * elp.eval("String.class")); }
     */

    @Test
    public void testConstructor() {
//        assertEquals(new Integer(1001), elp.eval("T(Integer)(1001)"));
        assertEquals(new Integer(1001), elp.eval("Integer(1001)"));
    }

    @Test
    public void testStaticMethod() {
        assertEquals(4, elp.eval("Integer.numberOfTrailingZeros(16)"));
    }
}
