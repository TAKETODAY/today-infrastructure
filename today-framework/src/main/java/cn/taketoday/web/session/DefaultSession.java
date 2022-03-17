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

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import cn.taketoday.core.AttributeAccessorSupport;

/**
 * @author TODAY <br>
 * 2019-09-27 19:40
 */
public class DefaultSession extends AttributeAccessorSupport implements WebSession, Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private final String id;
  private final long creationTime;
  private final WebSessionStorage storage;

  public DefaultSession(String id, WebSessionStorage storage) {
    this.id = id;
    this.storage = storage;
    this.creationTime = System.currentTimeMillis();
  }

  @Override
  public long getCreationTime() {
    return creationTime;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void invalidate() {
    clear();
    storage.remove(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof DefaultSession))
      return false;
    if (!super.equals(o))
      return false;
    final DefaultSession that = (DefaultSession) o;
    return creationTime == that.creationTime && Objects.equals(id, that.id) && Objects.equals(storage, that.storage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, creationTime, storage);
  }

  @Override
  public String toString() {
    return "DefaultSession{" +
            "id='" + id + '\'' +
            ", creationTime=" + creationTime +
            ", storage=" + storage +
            '}';
  }
}
