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
package cn.taketoday.retry.backoff;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Simple {@link Sleeper} implementation that just waits on a local Object.
 *
 * @author Dave Syer
 */
@SuppressWarnings("serial")
public class DummySleeper implements Sleeper {

  private List<Long> backOffs = new ArrayList<Long>();

  /**
   * Public getter for the long.
   *
   * @return the lastBackOff
   */
  public long getLastBackOff() {
    return backOffs.get(backOffs.size() - 1).longValue();
  }

  public long[] getBackOffs() {
    long[] result = new long[backOffs.size()];
    int i = 0;
    for (Iterator<Long> iterator = backOffs.iterator(); iterator.hasNext(); ) {
      Long value = iterator.next();
      result[i++] = value.longValue();
    }
    return result;
  }

  /*
   * (non-Javadoc)
   *
   * @see cn.taketoday.batch.retry.backoff.Sleeper#sleep(long)
   */
  public void sleep(long backOffPeriod) throws InterruptedException {
    this.backOffs.add(Long.valueOf(backOffPeriod));
  }

}
