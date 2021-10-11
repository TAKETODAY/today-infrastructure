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
package cn.taketoday.web.session;

import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

/**
 * @author TODAY <br>
 * 2019-09-28 10:31
 */
public class RedissonSessionStorage
        extends AbstractWebSessionStorage implements WebSessionStorage {

  private final String prefix;
  private final RMapCache<String, WebSession> sessions;

//    private static final String DEFAULT_NAME = "sessions:";

  public RedissonSessionStorage() {
    this(3600_000);
  }

  public RedissonSessionStorage(RedissonClient redisson) {
    this(3600_000, null, redisson.getMapCache("sessions"));
  }

  public RedissonSessionStorage(long expire) {
    this(expire, "sessions:", null);
  }

  public RedissonSessionStorage(long expire, String prefix, RMapCache<String, WebSession> sessions) {
    super(expire);
    this.prefix = prefix;
    this.sessions = sessions;
  }

  @Override
  protected String computeId(String id) {
    final String prefix = this.prefix;
    if (prefix == null) {
      return id;
    }
    return prefix.concat(id);
  }

  @Override
  protected WebSession getInternal(String id) {
    return sessions.get(id);
  }

  @Override
  protected WebSession removeInternal(String id) {
    return sessions.remove(id);
  }

  @Override
  protected void storeInternal(String id, WebSession session) {
    sessions.put(id, session);
  }

}
