/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.beans.factory.parsing;

import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Nullable;

/**
 * Simple strategy allowing tools to control how source metadata is attached
 * to the bean definition metadata.
 *
 * <p>Configuration parsers <strong>may</strong> provide the ability to attach
 * source metadata during the parse phase. They will offer this metadata in a
 * generic format which can be further modified by a {@link SourceExtractor}
 * before being attached to the bean definition metadata.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see cn.taketoday.beans.BeanMetadataElement#getSource()
 * @see cn.taketoday.beans.factory.support.BeanDefinition
 * @since 4.0
 */
@FunctionalInterface
public interface SourceExtractor {

  /**
   * Extract the source metadata from the candidate object supplied
   * by the configuration parser.
   *
   * @param sourceCandidate the original source metadata (never {@code null})
   * @param definingResource the resource that defines the given source object
   * (may be {@code null})
   * @return the source metadata object to store (may be {@code null})
   */
  @Nullable
  Object extractSource(Object sourceCandidate, @Nullable Resource definingResource);

}
