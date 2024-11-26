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

package infra.jmx.export.metadata;

import infra.jmx.JmxException;
import infra.jmx.export.assembler.MetadataMBeanInfoAssembler;

/**
 * Thrown by the {@code JmxAttributeSource} when it encounters
 * incorrect metadata on a managed resource or one of its methods.
 *
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JmxAttributeSource
 * @see MetadataMBeanInfoAssembler
 * @since 4.0
 */
public class InvalidMetadataException extends JmxException {

  /**
   * Create a new {@code InvalidMetadataException} with the supplied
   * error message.
   *
   * @param msg the detail message
   */
  public InvalidMetadataException(String msg) {
    super(msg);
  }

}
