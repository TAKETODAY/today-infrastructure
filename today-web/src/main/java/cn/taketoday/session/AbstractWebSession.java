/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.session;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cn.taketoday.core.AttributeAccessor;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Session events supported WebSession
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SessionEventDispatcher
 * @see AttributeBindingListener
 * @see WebSessionAttributeListener
 * @since 4.0 2022/10/30 15:43
 */
public abstract class AbstractWebSession implements WebSession {

  /** Map with String keys and Object values. */
  @Nullable
  protected Map<String, Object> attributes;

  protected transient final SessionEventDispatcher eventDispatcher;

  protected AbstractWebSession(SessionEventDispatcher eventDispatcher) {
    this.eventDispatcher = eventDispatcher;
  }

  @Override
  public void addAttributes(Map<String, Object> attributes) {
    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
      setAttribute(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void setAttribute(String name, @Nullable Object value) {
    if (value != null) {
      // Replace or add this attribute
      Object oldValue = getAttributes().put(name, value);
      if (attributeBinding(value, oldValue)) {
        // Don't call any notification if replacing with the same value
        // unless configured to do so
        if (oldValue instanceof AttributeBindingListener listener) {
          listener.valueUnbound(this, name);
        }

        if (value instanceof AttributeBindingListener listener) {
          listener.valueBound(this, name);
        }
      }

      // WebSessionAttributeListener

      if (oldValue != null) {
        if (allowAttributeReplaced(value, oldValue)) {
          attributeReplaced(name, oldValue, value);
        }
      }
      else {
        attributeAdded(name, value);
      }
    }
    else {
      removeAttribute(name);
    }

  }

  protected boolean attributeBinding(Object value, @Nullable Object oldValue) {
    return oldValue != value;
  }

  protected boolean allowAttributeReplaced(Object value, @Nullable Object oldValue) {
    return oldValue != value;
  }

  protected void attributeAdded(String name, Object value) {
    eventDispatcher.attributeAdded(this, name, value);
  }

  protected void attributeReplaced(String name, Object oldValue, Object value) {
    eventDispatcher.attributeReplaced(this, name, oldValue, value);
  }

  protected void attributeRemoved(String name, Object attribute) {
    eventDispatcher.attributeRemoved(this, name, attribute);
  }

  @Nullable
  @Override
  public Object getAttribute(final String name) {
    var attributes = this.attributes;
    if (attributes == null) {
      return null;
    }
    return attributes.get(name);
  }

  @Override
  public Object removeAttribute(String name) {
    var attributes = this.attributes;
    if (attributes != null) {
      Object attribute = attributes.remove(name);
      if (attribute instanceof AttributeBindingListener listener) {
        listener.valueUnbound(this, name);
      }
      if (attribute != null) {
        attributeRemoved(name, attribute);
      }
      return attribute;
    }
    return null;
  }

  @Override
  public void invalidate() {
    eventDispatcher.onSessionDestroyed(this);

    for (String attributeName : getAttributeNames()) {
      removeAttribute(attributeName);
    }
    doInvalidate();
  }

  protected void doInvalidate() { }

  @Override
  public boolean hasAttribute(String name) {
    var attributes = this.attributes;
    if (attributes != null) {
      return attributes.containsKey(name);
    }
    return false;
  }

  @Override
  public String[] getAttributeNames() {
    var attributes = this.attributes;
    if (attributes != null) {
      return StringUtils.toStringArray(attributes.keySet());
    }
    return Constant.EMPTY_STRING_ARRAY;
  }

  @Override
  public Iterator<String> attributeNames() {
    var attributes = this.attributes;
    if (attributes != null) {
      return attributes.keySet().iterator();
    }
    return Collections.emptyIterator();
  }

  /**
   * Copy the attributes from the supplied AttributeAccessor to this accessor.
   *
   * @param source the AttributeAccessor to copy from
   */
  protected final void copyAttributesFrom(WebSession source) {
    Assert.notNull(source, "Source is required");
    for (String attributeName : source.getAttributeNames()) {
      setAttribute(attributeName, source.getAttribute(attributeName));
    }
  }

  /**
   * Returns {@code true} if this map contains no key-value mappings.
   *
   * @return {@code true} if this map contains no key-value mappings
   */
  @Override
  public boolean hasAttributes() {
    var attributes = this.attributes;
    return attributes != null && !attributes.isEmpty();
  }

  @Override
  public int hashCode() {
    return ObjectUtils.nullSafeHashCode(attributes);
  }

  @Override
  public Map<String, Object> getAttributes() {
    var attributes = this.attributes;
    if (attributes == null) {
      attributes = createAttributes();
      this.attributes = attributes;
    }
    return attributes;
  }

  protected Map<String, Object> createAttributes() {
    return new HashMap<>();
  }

  @Override
  public void copyAttributesFrom(AttributeAccessor source) {
    Assert.notNull(source, "Source is required");
    for (String attributeName : source.getAttributeNames()) {
      setAttribute(attributeName, source.getAttribute(attributeName));
    }
  }

  @Override
  public void clearAttributes() {
    if (attributes != null) {
      attributes.clear();
    }
  }
}
