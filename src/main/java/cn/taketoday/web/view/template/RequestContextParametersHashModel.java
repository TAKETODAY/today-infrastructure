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
package cn.taketoday.web.view.template;

import java.util.Iterator;
import java.util.Set;

import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.web.RequestContext;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleCollection;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;

/**
 * @author TODAY <br>
 * 2019-11-22 22:19
 */
public class RequestContextParametersHashModel implements TemplateHashModelEx {

  private Set<String> keys;
  private final ObjectWrapper wrapper;
  private final RequestContext request;

  public RequestContextParametersHashModel(ObjectWrapper wrapper, RequestContext request) {
    this.wrapper = wrapper;
    this.request = request;
  }

  @Override
  public TemplateModel get(String key) {
    return SimpleScalar.newInstanceOrNull(request.parameter(key));
  }

  @Override
  public boolean isEmpty() {
    return CollectionUtils.isEmpty(request.parameters());
  }

  @Override
  public int size() {
    return request.parameters().size();
  }

  @Override
  public TemplateCollectionModel keys() {
    return new SimpleCollection(getKeys(), wrapper);
  }

  @Override
  public TemplateCollectionModel values() {
    final Iterator<String> iter = getKeys().iterator();
    final class KeysIterator implements Iterator<Object> {
      public boolean hasNext() {
        return iter.hasNext();
      }

      public Object next() {
        return request.parameter(iter.next());
      }
    }

    return new SimpleCollection(new KeysIterator(), wrapper);
  }

  private Set<String> getKeys() {
    Set<String> keys = this.keys;
    if (keys == null) {
      keys = request.parameters().keySet();
      this.keys = keys;
    }
    return keys;
  }
}
