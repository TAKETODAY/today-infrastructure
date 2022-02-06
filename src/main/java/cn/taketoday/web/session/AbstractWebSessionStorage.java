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

/**
 * @author TODAY <br>
 * 2020-04-12 10:27
 */
public abstract class AbstractWebSessionStorage implements WebSessionStorage {
  private final long expire;

  public AbstractWebSessionStorage(long expire) {
    this.expire = expire;
  }

  @Override
  public WebSession get(String id) {
    String computeId = computeId(id);
    WebSession ret = getInternal(computeId);
    if (ret != null && System.currentTimeMillis() - ret.getCreationTime() > expire) {
      removeInternal(computeId);
      return null;
    }
    return ret;
  }

  protected String computeId(String id) {
    return id;
  }

  protected WebSession getInternal(String id) {
    return null;
  }

  @Override
  public final WebSession remove(String id) {
    return removeInternal(computeId(id));
  }

  protected WebSession removeInternal(String id) {
    return null;
  }

  @Override
  public boolean contains(String id) {
    return getInternal(computeId(id)) != null;
  }

  @Override
  public final void store(String id, WebSession session) {
    storeInternal(computeId(id), session);
  }

  protected abstract void storeInternal(String id, WebSession session);

  public long getExpire() {
    return expire;
  }
}
