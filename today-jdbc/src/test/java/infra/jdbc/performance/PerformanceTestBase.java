/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.jdbc.performance;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;

import java.sql.SQLException;

/**
 * Basically a {@link Runnable} with an Integer input.
 */
public abstract class PerformanceTestBase implements Function<Integer, Void>, AutoCloseable {
  private Stopwatch watch = Stopwatch.createUnstarted();

  public Void apply(Integer input) {
    try {
      run(input);
    }
    catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  public void initialize() {
    watch.reset();
    init();
  }

  public abstract void init();

  public abstract void run(int input) throws SQLException;

  public abstract void close();

  String getName() {
    return getClass().getSimpleName();
  }

  Stopwatch getWatch() {
    return watch;
  }
}
