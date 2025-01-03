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


package infra.core.type.filter;

import java.io.IOException;

import infra.core.type.classreading.MetadataReader;
import infra.core.type.classreading.MetadataReaderFactory;

/**
 * Base interface for type filters using a
 * {@link MetadataReader}.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 4.0
 */
@FunctionalInterface
public interface TypeFilter {

  /**
   * Determine whether this filter matches for the class described by
   * the given metadata.
   *
   * @param metadataReader the metadata reader for the target class
   * @param factory a factory for obtaining metadata readers
   * for other classes (such as superclasses and interfaces)
   * @return whether this filter matches
   * @throws IOException in case of I/O failure when reading metadata
   */
  boolean match(MetadataReader metadataReader, MetadataReaderFactory factory)
          throws IOException;

}
