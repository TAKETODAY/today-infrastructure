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
