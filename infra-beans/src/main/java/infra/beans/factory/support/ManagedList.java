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

package infra.beans.factory.support;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import infra.beans.BeanMetadataElement;
import infra.beans.Mergeable;
import infra.util.CollectionUtils;

/**
 * Tag collection class used to hold managed List elements, which may
 * include runtime bean references (to be resolved into bean objects).
 *
 * @param <E> the element type
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ManagedList<E> extends ArrayList<E> implements Mergeable, BeanMetadataElement {

  @Nullable
  private Object source;

  @Nullable
  private String elementTypeName;

  private boolean mergeEnabled;

  public ManagedList() { }

  public ManagedList(int initialCapacity) {
    super(initialCapacity);
  }

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
   * Set the default element type name (class name) to be used for this list.
   */
  public void setElementTypeName(@Nullable String elementTypeName) {
    this.elementTypeName = elementTypeName;
  }

  /**
   * Return the default element type name (class name) to be used for this list.
   */
  @Nullable
  public String getElementTypeName() {
    return this.elementTypeName;
  }

  /**
   * Set whether merging should be enabled for this collection,
   * in case of a 'parent' collection value being present.
   */
  public void setMergeEnabled(boolean mergeEnabled) {
    this.mergeEnabled = mergeEnabled;
  }

  @Override
  public boolean isMergeEnabled() {
    return this.mergeEnabled;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<E> merge(@Nullable Object parent) {
    if (!this.mergeEnabled) {
      throw new IllegalStateException("Not allowed to merge when the 'mergeEnabled' property is set to 'false'");
    }
    if (parent == null) {
      return this;
    }
    if (!(parent instanceof List)) {
      throw new IllegalArgumentException("Cannot merge with object of type [%s]".formatted(parent.getClass()));
    }
    List<E> merged = new ManagedList<>();
    merged.addAll((List<E>) parent);
    merged.addAll(this);
    return merged;
  }

  /**
   * Return a new instance containing an arbitrary number of elements.
   *
   * @param elements the elements to be contained in the list
   * @param <E> the {@code List}'s element type
   * @return a {@code List} containing the specified elements
   */
  @SuppressWarnings({ "unchecked", "varargs" })
  public static <E> ManagedList<E> of(E... elements) {
    ManagedList<E> list = new ManagedList<>();
    CollectionUtils.addAll(list, elements);
    return list;
  }
}
