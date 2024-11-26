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
package infra.retry.backoff;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple {@link Sleeper} implementation that just waits on a local Object.
 *
 * @author Dave Syer
 */
@SuppressWarnings("serial")
public class DummySleeper implements Sleeper {

  private final List<Long> backOffs = new ArrayList<>();

  /**
   * Public getter for the long.
   *
   * @return the lastBackOff
   */
  public long getLastBackOff() {
    return backOffs.get(backOffs.size() - 1);
  }

  public long[] getBackOffs() {
    long[] result = new long[backOffs.size()];
    int i = 0;
    for (Long value : backOffs) {
      result[i++] = value;
    }
    return result;
  }

  /*
   * (non-Javadoc)
   *
   * @see infra.batch.retry.backoff.Sleeper#sleep(long)
   */
  public void sleep(long backOffPeriod) throws InterruptedException {
    this.backOffs.add(backOffPeriod);
  }

}
