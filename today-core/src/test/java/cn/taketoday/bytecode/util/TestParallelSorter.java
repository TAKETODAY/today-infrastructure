/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
package cn.taketoday.bytecode.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.util.ResourceUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Chris Nokleberg
 * <a href="mailto:chris@nokleberg.com">chris@nokleberg.com</a>
 * @version $Id: TestParallelSorter.java,v 1.4 2004/06/24 21:15:13 herbyderby
 * Exp $
 */
public class TestParallelSorter {
  public void testSorts() throws Throwable {
    Object[] data1 = getTestData();
    Object[] data2 = copy(data1);
    Object[] data3 = copy(data1);
    int[] idx1 = getIndexes(data1.length);
    int[] idx2 = getIndexes(data1.length);
    int[] idx3 = getIndexes(data1.length);
    ParallelSorter p1 = ParallelSorter.create(new Object[] { data1, idx1 });
    ParallelSorter p2 = ParallelSorter.create(new Object[] { data2, idx2 });
    p1.quickSort(0);
    p2.mergeSort(0);
    compare(data1, data2);
    compare(idx1, idx2);
    p1.quickSort(1);
    compare(idx1, idx3);
    compare(data1, data3);
  }

  private void compare(Object[] data1, Object[] data2) {
    assertTrue(data1.length == data2.length);
    for (int i = 0; i < data1.length; i++) {
      assertTrue(data1[i].equals(data2[i]));
    }
  }

  private void compare(int[] data1, int[] data2) {
    assertTrue(data1.length == data2.length);
    for (int i = 0; i < data1.length; i++) {
      assertTrue(data1[i] == data2[i]);
    }
  }

  private int[] getIndexes(int len) {
    int[] idx = new int[len];
    for (int i = 0; i < len; i++) {
      idx[i] = i;
    }
    return idx;
  }

  private Object[] getTestData() throws IOException {

    InputStream in = ResourceUtils.getResource("classpath:words.txt").getInputStream();
    BufferedReader r = new BufferedReader(new InputStreamReader(in));
    List list = new ArrayList();
    String line;
    int c = 0;
    while ((line = r.readLine()) != null) {
      list.add(line);
      if (c++ == 20)
        break;
    }
    return list.toArray();
  }

  private Object[] copy(Object[] data) {
    Object[] copy = new Object[data.length];
    System.arraycopy(data, 0, copy, 0, data.length);
    return copy;
  }

}
