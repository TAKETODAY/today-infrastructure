/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.beans.factory.parsing;

import org.jspecify.annotations.Nullable;

import infra.core.io.Resource;

/**
 * Simple {@link SourceExtractor} implementation that just passes
 * the candidate source metadata object through for attachment.
 *
 * <p>Using this implementation means that tools will get raw access to the
 * underlying configuration source metadata provided by the tool.
 *
 * <p>This implementation <strong>should not</strong> be used in a production
 * application since it is likely to keep too much metadata in memory
 * (unnecessarily).
 *
 * @author Rob Harrop
 * @since 4.0
 */
public class PassThroughSourceExtractor implements SourceExtractor {

  /**
   * Simply returns the supplied {@code sourceCandidate} as-is.
   *
   * @param sourceCandidate the source metadata
   * @return the supplied {@code sourceCandidate}
   */
  @Override
  public Object extractSource(Object sourceCandidate, @Nullable Resource definingResource) {
    return sourceCandidate;
  }

}
