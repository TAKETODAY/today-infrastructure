/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import cn.taketoday.web.RequestContext;
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

  private List<String> keys;
  private final RequestContext request;

  public RequestContextParametersHashModel(RequestContext request) {
    this.request = request;
  }

  @Override
  public TemplateModel get(String key) {
    return SimpleScalar.newInstanceOrNull(request.parameter(key));
  }

  @Override
  public boolean isEmpty() {
    return !request.parameterNames().hasMoreElements();
  }

  @Override
  public int size() {
    return getKeys().size();
  }

  @Override
  public TemplateCollectionModel keys() {
    return new SimpleCollection(getKeys().iterator());
  }

  @Override
  public TemplateCollectionModel values() {

    final Iterator<String> iter = getKeys().iterator();
    return new SimpleCollection(new Iterator<Object>() {
      public boolean hasNext() {
        return iter.hasNext();
      }

      public Object next() {
        return request.parameter(iter.next());
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }
    });
  }

  private synchronized List<String> getKeys() {
    List<String> keys = this.keys;
    if (keys == null) {
      keys = new ArrayList<>();
      for (final Enumeration<String> enumeration = request.parameterNames(); enumeration.hasMoreElements(); ) {
        keys.add(enumeration.nextElement());
      }
    }
    return keys;
  }
}
