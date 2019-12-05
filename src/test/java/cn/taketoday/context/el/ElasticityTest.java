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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.ELProcessor;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 *
 * @author kichung
 */
public class ElasticityTest {

    ELProcessor elp;

    public ElasticityTest() {}

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

    static public class Data {
        int s;
        int d;

        public Data(int s, int d) {
            this.s = s;
            this.d = d;
        }

        public int getS() {
            return this.s;
        }

        public int getD() {
            return this.d;
        }
    }

    static public class Metric {
        int limit;
        List<Data> list = new ArrayList<Data>();

        public Metric(int limit) {
            this.limit = limit;
        }

        public int getLimit() {
            return limit;
        }

        public List<Data> getList() {
            return list;
        }
    }

    Map<String, Metric> clusters = new HashMap<String, Metric>();

    private void init() {
        Metric m1 = new Metric(10);
        m1.getList().add(new Data(1, 80));
        m1.getList().add(new Data(3, 90));
        m1.getList().add(new Data(4, 100));
        m1.getList().add(new Data(5, 50));
        m1.getList().add(new Data(6, 60));

        Metric m2 = new Metric(10);
        m2.getList().add(new Data(1, 80));
        m2.getList().add(new Data(3, 82));
        m2.getList().add(new Data(7, 90));
        m2.getList().add(new Data(9, 140));
        m2.getList().add(new Data(15, 80));

        Metric m3 = new Metric(10);
        m3.getList().add(new Data(4, 100));
        m3.getList().add(new Data(5, 81));
        m3.getList().add(new Data(6, 200));
        m3.getList().add(new Data(20, 80));

        clusters.put("c1", m1);
        clusters.put("c2", m2);
        clusters.put("c3", m3);

        elp.defineBean("c", clusters);
    }

    public void testElaticity() {
        init();
        Object obj = elp.eval("c.values().select(" + "v->v.list.where(d->d.s>1 && d.s<10)." + "average(d->d.d)).toList()");

        System.out.println(obj);
        obj = elp.eval("c.values().select(v->v.list." + "where(d->d.s>1 && d.s<10)." + "average(d->d.d) > 100).toList()");
        System.out.println(obj);
        obj = elp.eval(
                       "c.values().select(v->v.list." + "where(d->d.s>1 && d.s<10)." + "average(d->d.d) > 100).any()");
        System.out.println(obj);
        obj = elp.eval(
                       "c.entrySet().select(s->[s.key, s.value.list." + "where(d->d.s>1 && d.s<10)." + "average(d->d.d)]).toList()");
        System.out.println(obj);
    }
}
