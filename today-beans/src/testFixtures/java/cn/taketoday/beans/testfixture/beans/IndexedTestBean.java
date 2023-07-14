/*
 * Copyright 2017 - 2023 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.beans.testfixture.beans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Juergen Hoeller
 * @since 11.11.2003
 */
@SuppressWarnings("rawtypes")
public class IndexedTestBean {

  private TestBean[] array;

  private Collection<?> collection;

  private List list;

  private Set<? super Object> set;

  private SortedSet<? super Object> sortedSet;

  private Map map;

  private SortedMap sortedMap;

  private MyTestBeans myTestBeans;

  public IndexedTestBean() {
    this(true);
  }

  public IndexedTestBean(boolean populate) {
    if (populate) {
      populate();
    }
  }

  @SuppressWarnings("unchecked")
  public void populate() {
    TestBean tb0 = new TestBean("name0", 0);
    TestBean tb1 = new TestBean("name1", 0);
    TestBean tb2 = new TestBean("name2", 0);
    TestBean tb3 = new TestBean("name3", 0);
    TestBean tb4 = new TestBean("name4", 0);
    TestBean tb5 = new TestBean("name5", 0);
    TestBean tb6 = new TestBean("name6", 0);
    TestBean tb7 = new TestBean("name7", 0);
    TestBean tb8 = new TestBean("name8", 0);
    TestBean tbX = new TestBean("nameX", 0);
    TestBean tbY = new TestBean("nameY", 0);
    TestBean tbZ = new TestBean("nameZ", 0);
    this.array = new TestBean[] { tb0, tb1 };
    this.list = new ArrayList<>();
    this.list.add(tb2);
    this.list.add(tb3);
    this.set = new TreeSet<>();
    this.set.add(tb6);
    this.set.add(tb7);
    this.map = new HashMap<>();
    this.map.put("key1", tb4);
    this.map.put("key2", tb5);
    this.map.put("key.3", tb5);
    List list = new ArrayList();
    list.add(tbX);
    list.add(tbY);
    this.map.put("key4", list);
    this.map.put("key5[foo]", tb8);
    this.myTestBeans = new MyTestBeans(tbZ);
  }

  public TestBean[] getArray() {
    return array;
  }

  public void setArray(TestBean[] array) {
    this.array = array;
  }

  public Collection<?> getCollection() {
    return collection;
  }

  public void setCollection(Collection<?> collection) {
    this.collection = collection;
  }

  public List getList() {
    return list;
  }

  public void setList(List list) {
    this.list = list;
  }

  public Set<?> getSet() {
    return set;
  }

  public void setSet(Set<? super Object> set) {
    this.set = set;
  }

  public SortedSet<? super Object> getSortedSet() {
    return sortedSet;
  }

  public void setSortedSet(SortedSet<? super Object> sortedSet) {
    this.sortedSet = sortedSet;
  }

  public Map getMap() {
    return map;
  }

  public void setMap(Map map) {
    this.map = map;
  }

  public SortedMap getSortedMap() {
    return sortedMap;
  }

  public void setSortedMap(SortedMap sortedMap) {
    this.sortedMap = sortedMap;
  }

  public MyTestBeans getMyTestBeans() {
    return myTestBeans;
  }

  public void setMyTestBeans(MyTestBeans myTestBeans) {
    this.myTestBeans = myTestBeans;
  }

  public static class MyTestBeans implements Iterable<TestBean> {

    private final Collection<TestBean> testBeans;

    public MyTestBeans(TestBean... testBeans) {
      this.testBeans = Arrays.asList(testBeans);
    }

    @Override
    public Iterator<TestBean> iterator() {
      return this.testBeans.iterator();
    }
  }

}
