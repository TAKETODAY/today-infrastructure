package cn.taketoday.context.el;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

import cn.taketoday.expression.ExpressionProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
public class LinqTest {

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
  public void setup() { }

  void p(String msg) {
    System.out.println(msg);
  }

  /**
   * Test a Ling query that returns a Map
   *
   * @param name of the test
   * @param query The EL query string
   * @param expected The expected result of the Map. The element of the array should
   * equals a entry in the Map.
   */
  void testMap(String name, String query, String[] expected) {
    p("=== Test " + name + "===");
    p(query);
    Map map = (Map) elp.eval(query);
    p(" = returns =");
    int indx = 0;
    while (indx < expected.length) {
      boolean found = false;
      for (Object item : map.entrySet()) {
        if (item.toString().equals(expected[indx])) {
          p(" " + item);
          found = true;
          break;
        }
      }
      assertTrue(found);
      indx++;
    }
    assertTrue(indx == expected.length);
  }

  /**
   * Test a Linq query that returns an Iterable.
   *
   * @param name of the test
   * @param query The EL query string
   * @param expected The expected result of the Iterable. The array element should
   * equal the Iterable element, when enumerated.
   */
  void testIterable(String name, String query, String[] expected) {
    p("=== Testing " + name + " ===");
    p(query);
    Object ret = elp.eval(query);
    int indx = 0;
    p(" = returns =");
    for (Object item : (Iterable) ret) {
      p(" " + item.toString());
      assertEquals(item.toString(), expected[indx++]);
    }
    assertTrue(indx == expected.length);
  }

  static String exp0[] = { "Coming Home", "Greatest Hits", "History of Golf", "Toy Story", "iSee" };

  @Test
  public void testSkip() {
    testIterable("skip", "products.skip(1).select(p->p.name)", exp0);
  }

  static String[] exp1 = { "Product: 200, Eagle, book, 12.5, 100", "Product: 203, History of Golf, book, 11.0, 30", "Product: 204, Toy Story, dvd, 10.0, 1000", "Product: 205, iSee, book, 12.5, 150" };

  @Test
  public void testWhere() {
    testIterable("where", "products.where(p->p.unitPrice >= 10)", exp1);
  }

  static String exp2[] = { "Eagle", "Coming Home", "Greatest Hits", "History of Golf", "Toy Story", "iSee" };

  @Test
  public void testSelect() {
    testIterable("select", "products.select(p->p.name)", exp2);
  }

  static String[] exp3 = { "[Eagle, 12.5]", "[History of Golf, 11.0]", "[Toy Story, 10.0]", "[iSee, 12.5]" };

  @Test
  public void testSelect2() {
    testIterable("select 2",
            " products.where(p->p.unitPrice >= 10).\n" + "          select(p->[p.name,p.unitPrice])",
            exp3);
  }

  static String[] exp4 = { "0", "3", "4", "5" };

  @Test
  public void testSelect3() {
    testIterable("select 3",
            " products.select((p,i)->{'product':p, 'index':i}).\n" + "          where(p->p.product.unitPrice >= 10).\n"
                    + "          select(p->p.index)",
            exp4);
  }

  static String[] exp5 = { "Order: 10, 100, 2/18/2010, 20.8", "Order: 11, 100, 5/3/2011, 34.5", "Order: 12, 100, 8/2/2011, 210.75", "Order: 13, 101, 1/15/2011, 50.23", "Order: 14, 101, 1/3/2012, 126.77" };

  @Test
  public void testSelectMany() {
    testIterable("selectMany",
            " customers.where(c->c.country == 'USA').\n" + "           selectMany(c->c.orders)",
            exp5);
  }

  static String[] exp6 = { "[John Doe, 11]", "[John Doe, 12]", "[Mary Lane, 13]" };

  @Test
  public void testSelectMany2() {
    testIterable("selectMany 2",
            " customers.where(c->c.country == 'USA').\n" + "           selectMany(c->c.orders, (c,o)->{'o':o,'c':c}).\n"
                    + "           where(co->co.o.orderDate.year == 2011).\n" + "           select(co->[co.c.name, co.o.orderID])",
            exp6);
  }

  @Test
  public void testSelectMany2a() {
    testIterable("selectMany 2a",
            " customers.where(c->c.country == 'USA').\n" + "           selectMany(c->c.orders).\n"
                    + "           where(o->o.orderDate.year == 2011).\n"
                    + "           select(o-> [customers.where(c->c.customerID==o.customerID).\n"
                    + "                                 select(c->c.name).\n" + "                                 single(),\n"
                    + "                       o.orderID])",
            exp6);
  }

  static String[] exp7 = { "Product: 200, Eagle, book, 12.5, 100", "Product: 205, iSee, book, 12.5, 150", "Product: 203, History of Golf, book, 11.0, 30" };

  @Test
  public void testTake() {
    testIterable("take",
            " products.orderByDescending(p->p.unitPrice).\n" + "          take(3)",
            exp7);
  }

  static String[] exp8 = { "[John Doe, 2/18/2010, 20.8]", "[John Doe, 5/3/2011, 34.5]", "[John Doe, 8/2/2011, 210.75]", "[Mary Lane, 1/15/2011, 50.23]", "[Mary Lane, 1/3/2012, 126.77]", "[Charlie Yeh, 4/15/2011, 101.2]" };

  @Test
  public void testJoin() {
    testIterable("join",
            " customers.join(orders, c->c.customerID, o->o.customerID,\n"
                    + "                (c,o)->[c.name, o.orderDate, o.total])",
            exp8);
  }

  static String[] exp9 = { "[John Doe, 266.05]", "[Mary Lane, 177.0]", "[Charlie Yeh, 101.2]" };

  @Test
  public void testGroupJoin() {
    testIterable("groupJoin",
            " customers.groupJoin(orders, c->c.customerID, o->o.customerID,\n"
                    + "                     (c,os)->[c.name, os.sum(o->o.total)])",
            exp9);
  }

  @Test
  public void testGroupJoin2() {
    testIterable("groupJoinNot",
            " customers.select(c->[c.name, c.orders.sum(o->o.total)])",
            exp9);
  }

  static String[] exp10 = { "Product: 200, Eagle, book, 12.5, 100", "Product: 205, iSee, book, 12.5, 150", "Product: 203, History of Golf, book, 11.0, 30", "Product: 202, Greatest Hits, cd, 6.5, 200", "Product: 204, Toy Story, dvd, 10.0, 1000", "Product: 201, Coming Home, dvd, 8.0, 50" };

  @Test
  public void testOrderBy() {
    testIterable("orderBy",
            " products.orderBy(p->p.category).\n" + "          thenByDescending(p->p.unitPrice).\n"
                    + "          thenBy(p->p.name)",
            exp10);
  }

  static String[] exp11 = { "Product: 201, Coming Home, dvd, 8.0, 50", "Product: 200, Eagle, book, 12.5, 100", "Product: 202, Greatest Hits, cd, 6.5, 200", "Product: 203, History of Golf, book, 11.0, 30", "Product: 205, iSee, book, 12.5, 150", "Product: 204, Toy Story, dvd, 10.0, 1000" };

  @Test
  public void testOrderBy2() {
    testIterable("orderBy 2",
            " products.orderBy(p->p.name,\n" + "                  T(java.lang.String).CASE_INSENSITIVE_ORDER)",
            exp11);
  }

  static String[] exp12 = { "11=Order: 11, 100, 5/3/2011, 34.5", "12=Order: 12, 100, 8/2/2011, 210.75", "13=Order: 13, 101, 1/15/2011, 50.23", "15=Order: 15, 102, 4/15/2011, 101.2" };

  @Test
  public void testToMap() {
    testMap("toMap",
            " customers.selectMany(c->c.orders).\n" + "           where(o->o.orderDate.year == 2011).\n"
                    + "           toMap(o->o.orderID)",
            exp12);
  }

  @Test
  public void testToMap2() {
    testMap("toMap2",
            "orders.where(o->o.orderDate.year == 2011).\n" + "       toMap(o->o.orderID)",
            exp12);
  }

  static String[] exp13 = { "book: [Eagle, History of Golf, iSee]", "dvd: [Coming Home, Toy Story]", "cd: [Greatest Hits]" };

  @Test
  public void testGroupBy() {
    testIterable("groupBy",
            " products.groupBy(p->p.category, p->p.name) ",
            exp13);
  }

  static String[] exp14 = { "book=book: [Eagle, History of Golf, iSee]", "dvd=dvd: [Coming Home, Toy Story]", "cd=cd: [Greatest Hits]" };

  @Test
  public void testToLookup() {
    testMap("toLookup",
            "products.toLookup(p->p.category, p->p.name)",
            exp14);
  }

  static String[] exp15 = { "[book, 11.0]", "[dvd, 8.0]", "[cd, 6.5]" };

  @Test
  public void testMin() {
    testIterable("min",
            "products.groupBy(p->p.category).\n" + "         select(g->[g.key, g.min(p->p.unitPrice)])",
            exp15);
  }

  static String[] exp16 = { "[book, History of Golf]", "[dvd, Coming Home]", "[cd, Greatest Hits]" };

  @Test
  public void testAggregate() {
    testIterable("aggregate",
            "products.groupBy(p->p.category).\n" + "         select(g->[g.key,\n" + "                    g.select(p->p.name).\n"
                    + "                      aggregate((s,t)->t.length()>s.length()?t:s)])",
            exp16);
  }

  @Test
  public void testDistinct() {
    testIterable("distinct",
            " ['a', 'b', 'b', 'c'].distinct()",
            new String[]
                    { "a", "b", "c" });
  }

  @Test
  public void testUnion() {
    testIterable("union",
            "['a', 'b', 'b', 'c'].union(['b', 'c', 'd'])",
            new String[]
                    { "a", "b", "c", "d" });
  }

  @Test
  public void testIntersect() {
    testIterable("intersect",
            "['a', 'b', 'b', 'c'].intersect(['b', 'c', 'd'])",
            new String[]
                    { "b", "c" });
  }

  @Test
  public void testExcept() {
    testIterable("except",
            "['x', 'b', 'a', 'b', 'c'].except(['b', 'c', 'd'])",
            new String[]
                    { "x", "a" });
  }

  @Test
  public void testForEach() {
    testIterable("forEach",
            "lst = []; products.forEach(p->lst.add(p.name)); lst", exp2);
    /*
     * Since println is overloaded, this can be problem.
     * elp.eval("products.forEach((p,idx)->System.out.println(" +
     * "idx + \": \" + p.name + \", \" + p.unitPrice))");
     */
  }

  @Test
  public void testGen() {
    testIterable("collections:range", "collections:range(10, 5)()",
            new String[]
                    { "10", "11", "12", "13", "14" });
    System.out.println(elp.eval("collections:range(0,5).select(x->x*x).toList()"));
    testIterable("collections:repeat", "collections:repeat(\"xyz\", 3)",
            new String[]
                    { "xyz", "xyz", "xyz" });
  }

  @Test
  public void testMisc() {
    testIterable("concat", "['a','b'].concat(['x','y','z'])",
            new String[]
                    { "a", "b", "x", "y", "z" });
    testIterable("defaultIfEmpty", "['a', 'b'].defaultIfEmpty()",
            new String[]
                    { "a", "b" });
    testIterable("defaultIfEmpty", "[].defaultIfEmpty('zz')",
            new String[]
                    { "zz" });
    Object ret = elp.eval("[].defaultIfEmpty()");
    int i = 0;
    for (Object o : (Iterable) ret) {
      if (i == 0) {
        assertEquals(o, null);
      }
      i++;
    }
    assertEquals(i, 1);
    ret = elp.eval("[].average()");
    assertEquals(ret, null);
  }

}
