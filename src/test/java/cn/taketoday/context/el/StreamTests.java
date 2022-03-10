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

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;

import cn.taketoday.expression.ExpressionException;
import cn.taketoday.expression.ExpressionProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamTests {

  static ExpressionProcessor elp;
  static DataBase database = null;

  @BeforeAll
  public static void setUpClass() throws Exception {
    elp = new ExpressionProcessor();
    database = new DataBase();
    database.init();
    elp.defineBean("customers", database.getCustomers());
    elp.defineBean("products", database.getProducts());
    elp.defineBean("orders", database.getOrders());
  }

  @BeforeEach
  void setup() { }

  void p(String msg) {
    System.out.println(msg);
  }

  /**
   * Test a collection query that returns an array, list or Iterable.
   *
   * @param name of the test
   * @param query The EL query string
   * @param expected The expected result of the array, list or Iterable. The array
   * element should equal the elements in the array, list or Iterable,
   * when enumerated.
   */

  void testStream(String name, String query, String[] expected) {
    p("=== Testing " + name + " ===");
    p(query);
    Object ret = elp.eval(query);
    p(" = returns =");

    if (ret.getClass().isArray()) {
      int size = Array.getLength(ret);
      assertTrue(size == expected.length);
      for (int i = 0; i < size; i++) {
        Object item = Array.get(ret, i);
        p(" " + item.toString());
        assertEquals(item.toString(), expected[i]);
      }
      return;
    }

    if (ret instanceof List) {
      List<Object> list = (List<Object>) ret;
      int i = 0;
      for (Object item : list) {
        p(" " + item.toString());
        assertEquals(item.toString(), expected[i++]);
      }
      assertTrue(i == expected.length);
      return;
    }

    if (ret instanceof Iterator) {
      int i = 0;
      Iterator<Object> iter = (Iterator<Object>) ret;
      while (iter.hasNext()) {
        Object item = iter.next();
        p(" " + item.toString());
        assertEquals(item.toString(), expected[i++]);
      }
      assertTrue(i == expected.length);
      return;
    }

    // unexpected return type
    assertTrue(false);
  }

  void testStream(String name, String query, Object expected) {
    p("=== Testing " + name + " ===");
    p(query);
    Object ret = elp.eval(query);
    p(" = returns " + ret + "(" + ret.getClass() + ")");
    assertEquals(ret, expected);
    p("");
  }

  static String[] exp0 = { "1", "2", "3", "4", "5", "6" };
  static String[] exp1 = { "6", "5", "4", "3", "2", "1" };
  static String[] exp2 = { "q", "z", "yz", "aaa", "abc", "xyz" };
  static String[] exp3 = { "2", "3", "4" };
  static String[] exp4 = { "20", "30", "40" };

  @Test
  void testFilterMap() {
    testStream("filter", "[1,2,3,4].stream().filter(i->i > 1).toList()", exp3);
    testStream("map", "[2,3,4].stream().map(i->i*10).iterator()", exp4);
    testStream("filtermap", "[1,2,3,4].stream().filter(i->i > 1)\n" + "                  .map(i->i*10).toArray()", exp4);
  }

  static String[] exp5 = { "Product: 201, Coming Home, dvd, 8.0, 50", "Product: 200, Eagle, book, 12.5, 100", "Product: 202, Greatest Hits, cd, 6.5, 200", "Product: 203, History of Golf, book, 11.0, 30", "Product: 204, Toy Story, dvd, 10.0, 1000", "Product: 205, iSee, book, 12.5, 150" };

  static String[] exp6 = { "Product: 203, History of Golf, book, 11.0, 30", "Product: 200, Eagle, book, 12.5, 100", "Product: 205, iSee, book, 12.5, 150", "Product: 202, Greatest Hits, cd, 6.5, 200", "Product: 201, Coming Home, dvd, 8.0, 50", "Product: 204, Toy Story, dvd, 10.0, 1000" };

  @Test
  void testSorted() {
    testStream("distinct", "[2, 3, 2, 4, 4].stream().distinct().toList()", exp3);
    testStream("sorted", "[1, 3, 5, 2, 4, 6].stream().sorted().toList()", exp0);
    testStream("sorted", "[1, 3, 5, 2, 4, 6].stream().sorted((i,j)->i-j).toList()", exp0);
    testStream("sorted", "[1, 3, 5, 2, 4, 6].stream().sorted((i,j)->i.compareTo(j)).toList()", exp0);
    testStream("sorted", "['2', '4', '6', '5', '3', '1'].stream().sorted((s, t)->s.compareTo(t)).toList()", exp0);
    testStream("sorted", "[1, 3, 5, 2, 4, 6].stream().sorted((i,j)->j.compareTo(i)).toList()", exp1);
    testStream("sorted",
            "['xyz', 'yz', 'z', 'abc', 'aaa', 'q'].stream().sorted"
                    + "((s,t)->(s.length()== t.length()? s.compareTo(t): s.length() - t.length())).toList()",
            exp2);
    elp.eval("comparing = map->(x,y)->map(x).compareTo(map(y))");
    testStream("sorted", "products.stream().sorted(" + "(x,y)->x.name.compareTo(y.name)).toList()", exp5);
    testStream("sorted", "products.stream().sorted(" + "comparing(p->p.name)).toList()", exp5);
    elp.eval("compose = (m1,m2)->(x,y)->(tx = m1(x).compareTo(m1(y)); " + "tx!=0? tx: (m2(x).compareTo(m2(y))))");
    testStream("sorted", "products.stream().sorted(" + "compose(p->p.category, p->p.unitPrice)).toList()", exp6);
  }

  static String exp8[] = { "Eagle", "Coming Home", "Greatest Hits", "History of Golf", "Toy Story", "iSee" };

  String exp11[] = { "1", "2", "3", "4" };

  @Test
  void testForEach() {
    testStream("forEach",
            "lst = []; products.stream().forEach(p->lst.add(p.name)); lst", exp8);
    testStream("peek",
            "lst = []; [1,2,3,4].stream().peek(i->lst.add(i)).toList()", exp11);
    testStream("peek2", "lst", exp11);
  }

  static String[] exp7 = { "Order: 10, 100, 2/18/2010, 20.8", "Order: 11, 100, 5/3/2011, 34.5", "Order: 12, 100, 8/2/2011, 210.75", "Order: 13, 101, 1/15/2011, 50.23", "Order: 14, 101, 1/3/2012, 126.77" };

  static String[] exp9 = { "t", "h", "e", "q", "u", "i", "c", "k", "b", "r", "o", "w", "n", "f", "o", "x" };

  @Test
  void testFlapMap() {
    testStream("flatMap",
            "customers.stream().filter(c->c.country=='USA')\n" + "                  .flatMap(c->c.orders.stream()).toList()",
            exp7);
    testStream("flatMap String",
            "['the', 'quick', 'brown', 'fox']" + ".stream().flatMap(s->s.toCharArray().stream()).toList()",
            exp9);
  }

  static String exp10[] = { "0", "1", "2" };

  @Test
  void testSubstream() {
    testStream("limit", "[0,1,2,3,4,5].stream().limit(3).toList()", exp10);
    testStream("substream", "[0,1,2,3,4].stream().substream(2).toList()", exp3);
    testStream("substream", "[0,1,2,3,4,5,6].stream().substream(2,5).toList()", exp3);
  }

  @Test
  void testReduce() {
    testStream("reduce", "[1,2,3,4,5].stream().reduce(0, (l,r)->l+r)", Long.valueOf(15));
    testStream("reduce", "[1,2,3,4,5].stream().reduce((l,r)->l+r).get()", Long.valueOf(15));
    testStream("reduce", "[].stream().reduce((l,r)->l+r).orElse(101)", Long.valueOf(101));
    testStream("reduce", "c = 0; [1,2,3,4,5,6].stream().reduce(0, (l,r)->(c = c+1; c % 2 == 0? l+r: l-r))", Long.valueOf(3));
  }

  @Test
  void testMatch() {
    testStream("anyMatch", "[1,2,3,4].stream().anyMatch(e->e == 3)", Boolean.TRUE);
    testStream("anyMatch", "[1,2,3,4].stream().anyMatch(e->e > 10)", Boolean.FALSE);
    testStream("allMatch", "[1,2,3,4].stream().allMatch(e->e > 0)", Boolean.TRUE);
    testStream("allMatch", "[1,2,3,4].stream().allMatch(e->e > 1)", Boolean.FALSE);
    testStream("noneMatch", "[1,2,3,4].stream().noneMatch(e->e > 1)", Boolean.FALSE);
    testStream("noneMatch", "[1,2,3,4].stream().noneMatch(e->e > 10)", Boolean.TRUE);
  }

  @Test
  void testToType() {
    testStream("toArray", "[2,3,4].stream().map(i->i*10).toArray()", exp4);
    testStream("toList", "[2,3,4].stream().map(i->i*10).toList()", exp4);
    testStream("Iterator", "[2,3,4].stream().map(i->i*10).iterator()", exp4);
  }

  @Test
  void testFind() {
    testStream("findFirst", "[101, 100].stream().findFirst().get()", Long.valueOf(101));
    boolean caught = false;
    try {
      elp.eval("[].stream().findFirst().get()");
    }
    catch (ExpressionException ex) {
      caught = true;
    }
    assertTrue(caught);
    testStream("findFirst", "[101, 100].stream().findFirst().isPresent()", Boolean.TRUE);
    testStream("findFirst", "[].stream().findFirst().isPresent()", Boolean.FALSE);
  }

  @Test
  void testArith() {
    testStream("sum", "[1,2,3,4,5].stream().sum()", Long.valueOf(15));
    testStream("sum", "[1.4,2,3,4,5.1].stream().sum()", Double.valueOf(15.5));
    testStream("average", "[1,2,3,4,5].stream().average().get()", Double.valueOf(3.0));
    testStream("average", "[1.4,2,3,4,5.1].stream().average().get()", Double.valueOf(3.1));
    testStream("count", "[1,2,3,4,5].stream().count()", Long.valueOf(5));
  }

  @Test
  void testMinMax() {
    testStream("min", "[2,3,1,5].stream().min().get()", Long.valueOf(1));
    testStream("max", "[2,3,1,5].stream().max().get()", Long.valueOf(5));
    testStream("max", "['xy', 'xyz', 'abc'].stream().max().get()", "xyz");
    testStream("max", "[2].stream().max((i,j)->i-j).get()", Long.valueOf(2));
    elp.eval("comparing = map->(x,y)->map(x).compareTo(map(y))");
    testStream("max", "customers.stream().max((x,y)->x.orders.size()-y.orders.size()).get().name", "John Doe");
    testStream("max", "customers.stream().max(comparing(c->c.orders.size())).get().name", "John Doe");
    testStream("min", "[3,2,1].stream().min((i,j)->i-j).get()", Long.valueOf(1));
    testStream("min", "customers.stream().min((x,y)->x.orders.size()-y.orders.size()).get().name", "Charlie Yeh");
    elp.eval("comparing = map->(x,y)->map(x).compareTo(map(y))");
    testStream("min", "customers.stream().min(comparing(c->c.orders.size())).get().name", "Charlie Yeh");
  }

  @Test
  void testMap() {
    Object r = elp.eval("v = {'one':1, 'two':2}");
    System.out.println(" " + r);
    r = elp.eval("{1,2,3}");
    System.out.println(" " + r);
  }
}
