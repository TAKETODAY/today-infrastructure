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

package infra.beans.factory.support;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import infra.beans.factory.config.BeanDefinition;
import infra.core.io.AbstractResource;
import infra.core.io.DescriptiveResource;
import infra.core.io.Resource;
import infra.lang.Assert;
import infra.lang.Nullable;

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
