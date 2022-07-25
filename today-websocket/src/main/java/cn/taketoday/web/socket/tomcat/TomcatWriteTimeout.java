/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.socket.tomcat;

import org.apache.tomcat.websocket.BackgroundProcess;
import org.apache.tomcat.websocket.BackgroundProcessManager;

import java.util.Comparator;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author TODAY 2021/5/5 22:24
 * @since 3.0.1
 */
final class TomcatWriteTimeout implements BackgroundProcess {

  private final ConcurrentSkipListSet<TomcatRemoteEndpointImplServer>
          endpoints = new ConcurrentSkipListSet<>(new EndpointComparator());
  private final AtomicInteger count = new AtomicInteger(0);
  private int backgroundProcessCount = 0;
  private volatile int processPeriod = 1;

  @Override
  public void backgroundProcess() {
    // This method gets called once a second.
    backgroundProcessCount++;

    if (backgroundProcessCount >= processPeriod) {
      backgroundProcessCount = 0;

      long now = System.currentTimeMillis();
      for (TomcatRemoteEndpointImplServer endpoint : endpoints) {
        if (endpoint.getTimeoutExpiry() < now) {
          // Background thread, not the thread that triggered the
          // write so no need to use a dispatch
          endpoint.onTimeout(false);
        }
        else {
          // Endpoints are ordered by timeout expiry so if this point
          // is reached there is no need to check the remaining
          // endpoints
          break;
        }
      }
    }
  }

  @Override
  public void setProcessPeriod(int period) {
    this.processPeriod = period;
  }

  /**
   * {@inheritDoc}
   *
   * The default value is 1 which means asynchronous write timeouts are
   * processed every 1 second.
   */
  @Override
  public int getProcessPeriod() {
    return processPeriod;
  }

  public void register(TomcatRemoteEndpointImplServer endpoint) {
    if (endpoints.add(endpoint)) {
      int newCount = count.incrementAndGet();
      if (newCount == 1) {
        BackgroundProcessManager.getInstance().register(this);
      }
    }
  }

  public void unregister(TomcatRemoteEndpointImplServer endpoint) {
    if (endpoints.remove(endpoint)) {
      int newCount = count.decrementAndGet();
      if (newCount == 0) {
        BackgroundProcessManager.getInstance().unregister(this);
      }
    }
  }

  /**
   * Note: this comparator imposes orderings that are inconsistent with equals
   */
  static class EndpointComparator
          implements Comparator<TomcatRemoteEndpointImplServer> {

    @Override
    public int compare(TomcatRemoteEndpointImplServer o1,
                       TomcatRemoteEndpointImplServer o2) {

      long t1 = o1.getTimeoutExpiry();
      long t2 = o2.getTimeoutExpiry();
      return Long.compare(t1, t2);
    }
  }
}
