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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import cn.taketoday.expression.ExpressionContext;
import cn.taketoday.expression.ExpressionProcessor;
import cn.taketoday.expression.TypeConverter;

/**
 *
 * @author kichung
 */
public class ConvertTest {
    ExpressionProcessor elp;

    public ConvertTest() {}

    @BeforeClass
    public static void setUpClass() throws Exception {}

    @AfterClass
    public static void tearDownClass() throws Exception {}

    @Before
    public void setUp() {
        elp = new ExpressionProcessor();
    }

    @After
    public void tearDown() {}

    static public class MyBean {
        String name;
        int pint;
        Integer integer;

        MyBean() {

        }

        MyBean(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public void setPint(int v) {
            this.pint = v;
        }

        public int getPint() {
            return this.pint;
        }

        public void setInteger(Integer i) {
            this.integer = i;
        }

        public Integer getInteger() {
            return this.integer;
        }
    }

    @Test
    public void testVoid() {

        MyBean bean = new MyBean();
        elp.defineBean("bean", bean);
        // Assig null to int is 0;
        Object obj = elp.eval("bean.pint = null");
        assertEquals(obj, null);
        assertEquals(bean.getPint(), 0);

        // Assig null to Integer is null
        elp.setValue("bean.integer", null);
        assertEquals(bean.getInteger(), null);
    }

    @Test
    public void testCustom() {
        elp.getELManager().addELResolver(new TypeConverter() {
            @Override
            public Object convertToType(ExpressionContext context, Object obj, Class<?> type) {
                if (obj instanceof String && type == MyBean.class) {
                    context.setPropertyResolved(true);
                    return new MyBean((String) obj);
                }
                return null;
            }
        });

        Object val = elp.getValue("'John Doe'", MyBean.class);
        assertTrue(val instanceof MyBean);
        assertEquals(((MyBean) val).getName(), "John Doe");
    }
}
