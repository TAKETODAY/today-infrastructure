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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import infra.beans.factory.config.BeanDefinition;
import infra.core.io.AbstractResource;
import infra.core.io.DescriptiveResource;
import infra.core.io.Resource;
import infra.lang.Assert;

/**
 * Descriptive {@link Resource} wrapper for
 * a {@link BeanDefinition}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DescriptiveResource
 * @since 4.0 2022/3/7 21:20
 */
class BeanDefinitionResource extends AbstractResource {

  private final BeanDefinition beanDefinition;

  /**
   * Create a new BeanDefinitionResource.
   *
   * @param beanDefinition the BeanDefinition object to wrap
   */
  public BeanDefinitionResource(BeanDefinition beanDefinition) {
    Assert.notNull(beanDefinition, "BeanDefinition is required");
    this.beanDefinition = beanDefinition;
  }

  /**
   * Return the wrapped BeanDefinition object.
   */
  public final BeanDefinition getBeanDefinition() {
    return this.beanDefinition;
  }

  @Override
  public boolean exists() {
    return false;
  }

  @Override
  public boolean isReadable() {
    return false;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    throw new FileNotFoundException(
            "Resource cannot be opened because it points to " + this);
  }

  @Override
  public String toString() {
    return "BeanDefinition defined in " + this.beanDefinition.getResourceDescription();
  }

  /**
   * This implementation compares the underlying BeanDefinition.
   */
  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof BeanDefinitionResource &&
            ((BeanDefinitionResource) other).beanDefinition.equals(this.beanDefinition)));
  }

  /**
   * This implementation returns the hash code of the underlying BeanDefinition.
   */
  @Override
  public int hashCode() {
    return this.beanDefinition.hashCode();
  }

}
