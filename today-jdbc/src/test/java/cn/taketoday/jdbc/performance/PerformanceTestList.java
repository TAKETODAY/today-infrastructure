/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.jdbc.performance;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Ordering;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author aldenquimby@gmail.com
 */
@SuppressWarnings("serial")
public class PerformanceTestList extends ArrayList<PerformanceTestBase> {
  public void run(int iterations) throws SQLException {
    // initialize
    for (PerformanceTestBase test : this) {
      test.initialize();
    }

    final Random rand = new Random();

    for (int i = 1; i <= iterations; i++) {
      Iterable<PerformanceTestBase> sortedByRandom = orderBy(this, input -> rand.nextInt());

      for (PerformanceTestBase test : sortedByRandom) {
        final Stopwatch watch = test.getWatch();
        watch.start();
        test.run(i);
        watch.stop();
      }
    }

    // close up
    for (PerformanceTestBase test : this) {
      test.close();
    }
  }

  public void printResults(String heading) {
    Iterable<PerformanceTestBase> sortedByTime = orderBy(this, input -> input.getWatch().elapsed(TimeUnit.MILLISECONDS));

    System.out.println(heading + " Results");
    System.out.println("-------------------------");

    PerformanceTestBase fastest = null;

    for (PerformanceTestBase test : sortedByTime) {
      long millis = test.getWatch().elapsed(TimeUnit.MILLISECONDS);
      String testName = test.getName().replaceAll(heading + "$", "");
      if (fastest == null) {
        fastest = test;
        System.out.println(String.format("%s took %dms", testName, millis));
      }
      else {
        long fastestMillis = fastest.getWatch().elapsed(TimeUnit.MILLISECONDS);
        double percentSlower = (double) (millis - fastestMillis) / fastestMillis * 100;
        System.out.println(String.format("%s took %dms (%.2f%% slower)", testName, millis, percentSlower));
      }
    }
  }

  private static <T> Iterable<T> orderBy(Iterable<T> iterable, Function<T, ? extends Comparable> selector) {
    return Ordering.natural().onResultOf(selector).sortedCopy(iterable);
  }
}
