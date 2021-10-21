/*
 * Copyright 2002-2014 the original author or authors.
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

package cn.taketoday.core.type.filter;

import cn.taketoday.core.type.ClassMetadata;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;

import java.io.IOException;

/**
 * Type filter that exposes a
 * {@link cn.taketoday.core.type.ClassMetadata} object
 * to subclasses, for class testing purposes.
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @author Juergen Hoeller
 * @see #match(cn.taketoday.core.type.ClassMetadata)
 * @since 4.0
 */
public abstract class AbstractClassTestingTypeFilter implements TypeFilter {

  @Override
  public final boolean match(
          MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
    return match(metadataReader.getClassMetadata());
  }

  /**
   * Determine a match based on the given ClassMetadata object.
   *
   * @param metadata the ClassMetadata object
   * @return whether this filter matches on the specified type
   */
  protected abstract boolean match(ClassMetadata metadata);

}
