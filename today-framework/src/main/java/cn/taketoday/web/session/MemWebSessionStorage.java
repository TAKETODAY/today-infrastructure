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

import java.util.HashMap;
import java.util.Map;

/**
 * @author TODAY <br>
 * 2019-09-28 10:31
 */
public class MemWebSessionStorage
        extends AbstractWebSessionStorage implements WebSessionStorage {

  private final Map<String, WebSession> sessions;

  public MemWebSessionStorage() {
    this(3600_000);
  }

  public MemWebSessionStorage(long expire) {
    this(expire, new HashMap<>(1024));
  }

  public MemWebSessionStorage(long expire, Map<String, WebSession> sessions) {
    super(expire);
    this.sessions = sessions;
  }

  @Override
  protected WebSession getInternal(String id) {
    return sessions.get(id);
  }

  @Override
  public WebSession removeInternal(String id) {
    return sessions.remove(id);
  }

  @Override
  protected void storeInternal(String id, WebSession session) {
    sessions.put(id, session);
  }
}
