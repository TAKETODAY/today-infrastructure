/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author TODAY <br>
 *         2019-09-27 19:40
 */
public class DefaultSession implements WebSession, Serializable {
  private static final long serialVersionUID = 1L;

  private final String id;
  private final long creationTime;
  private final Map<String, Object> attributes = new HashMap<>();

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
  public Object getAttribute(String name) {
    return attributes.get(name);
  }

  @Override
  public String[] getNames() {
    final Map<String, Object> attributes = this.attributes;
    return attributes.keySet().toArray(new String[attributes.size()]);
  }

  @Override
  public Set<String> getKeys() {
    return attributes.keySet();
  }

  @Override
  public void setAttribute(String name, Object value) {
    attributes.put(name, value);
  }

  @Override
  public void removeAttribute(String name) {
    attributes.remove(name);
  }

  @Override
  public void invalidate() {
    attributes.clear();
    storage.remove(this);
  }

}
