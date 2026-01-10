/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.session;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import infra.core.AttributeAccessor;
import infra.lang.Assert;
import infra.lang.Constant;
import infra.util.CollectionUtils;
import infra.util.StringUtils;

/**
 * Session events supported Session
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SessionEventDispatcher
 * @see AttributeBindingListener
 * @see SessionAttributeListener
 * @since 4.0 2022/10/30 15:43
 */
public abstract class AbstractSession implements Session {

  /** Map with String keys and Object values. */
  @Nullable
  protected Map<String, Object> attributes;

  protected final transient SessionEventDispatcher eventDispatcher;

  protected AbstractSession(SessionEventDispatcher eventDispatcher) {
    this.eventDispatcher = eventDispatcher;
  }

  @Override
  public void setAttributes(@Nullable Map<String, Object> attributes) {
    if (CollectionUtils.isNotEmpty(attributes)) {
      for (Map.Entry<String, Object> entry : attributes.entrySet()) {
        setAttribute(entry.getKey(), entry.getValue());
      }
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

      // SessionAttributeListener

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

  @Nullable
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

  protected void doInvalidate() {
  }

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
  public Iterable<String> attributeNames() {
    var attributes = this.attributes;
    if (attributes != null) {
      return attributes.keySet();
    }
    return Collections.emptyList();
  }

  /**
   * Copy the attributes from the supplied AttributeAccessor to this accessor.
   *
   * @param source the AttributeAccessor to copy from
   */
  protected final void copyAttributesFrom(Session source) {
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
  public void copyFrom(AttributeAccessor source) {
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
