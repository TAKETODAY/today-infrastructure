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

package cn.taketoday.retry.stats;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cn.taketoday.retry.RetryStatistics;

/**
 * @author Dave Syer
 */
public class DefaultStatisticsRepository implements StatisticsRepository {

  private ConcurrentMap<String, MutableRetryStatistics> map = new ConcurrentHashMap<String, MutableRetryStatistics>();

  private RetryStatisticsFactory factory = new DefaultRetryStatisticsFactory();

  public void setRetryStatisticsFactory(RetryStatisticsFactory factory) {
    this.factory = factory;
  }

  @Override
  public RetryStatistics findOne(String name) {
    return map.get(name);
  }

  @Override
  public Iterable<RetryStatistics> findAll() {
    return new ArrayList<RetryStatistics>(map.values());
  }

  @Override
  public void addStarted(String name) {
    getStatistics(name).incrementStartedCount();
  }

  @Override
  public void addError(String name) {
    getStatistics(name).incrementErrorCount();
  }

  @Override
  public void addRecovery(String name) {
    getStatistics(name).incrementRecoveryCount();
  }

  @Override
  public void addComplete(String name) {
    getStatistics(name).incrementCompleteCount();
  }

  @Override
  public void addAbort(String name) {
    getStatistics(name).incrementAbortCount();
  }

  private MutableRetryStatistics getStatistics(String name) {
    MutableRetryStatistics stats;
    if (!map.containsKey(name)) {
      map.putIfAbsent(name, factory.create(name));
    }
    stats = map.get(name);
    return stats;
  }

}
