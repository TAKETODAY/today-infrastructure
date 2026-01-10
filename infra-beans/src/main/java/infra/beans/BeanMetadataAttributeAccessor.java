/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.beans;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

import infra.core.AttributeAccessorSupport;

/**
 * Extension of {@link AttributeAccessorSupport},
 * holding attributes as {@link BeanMetadataAttribute} objects in order
 * to keep track of the definition source.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 12:30
 */
public class BeanMetadataAttributeAccessor extends AttributeAccessorSupport implements BeanMetadataElement, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  @Nullable
  private Object source;

  /**
   * Set the configuration source {@code Object} for this metadata element.
   * <p>The exact type of the object will depend on the configuration mechanism used.
   */
  public void setSource(@Nullable Object source) {
    this.source = source;
  }

  @Override
  @Nullable
  public Object getSource() {
    return this.source;
  }

  /**
   * Add the given BeanMetadataAttribute to this accessor's set of attributes.
   *
   * @param attribute the BeanMetadataAttribute object to register
   */
  public void addMetadataAttribute(BeanMetadataAttribute attribute) {
    super.setAttribute(attribute.getName(), attribute);
  }

  /**
   * Look up the given BeanMetadataAttribute in this accessor's set of attributes.
   *
   * @param name the name of the attribute
   * @return the corresponding BeanMetadataAttribute object,
   * or {@code null} if no such attribute defined
   */
  @Nullable
  public BeanMetadataAttribute getMetadataAttribute(String name) {
    return (BeanMetadataAttribute) super.getAttribute(name);
  }

  @Override
  public void setAttribute(String name, @Nullable Object value) {
    super.setAttribute(name, new BeanMetadataAttribute(name, value));
  }

  @Override
  @Nullable
  public Object getAttribute(String name) {
    if (super.getAttribute(name) instanceof BeanMetadataAttribute attribute) {
      return attribute.getValue();
    }
    return null;
  }

  @Override
  @Nullable
  public Object removeAttribute(String name) {
    if (super.removeAttribute(name) instanceof BeanMetadataAttribute attribute) {
      return attribute.getValue();
    }
    return null;
  }

}
