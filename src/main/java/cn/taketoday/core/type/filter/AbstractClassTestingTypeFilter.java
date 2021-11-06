/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.core.type.filter;

import java.io.IOException;

import cn.taketoday.core.type.ClassMetadata;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;

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
          MetadataReader metadataReader, MetadataReaderFactory factory) throws IOException {
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
