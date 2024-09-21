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

package cn.taketoday.context.annotation.config;

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.function.UnaryOperator;

import cn.taketoday.core.type.classreading.MetadataReaderFactory;

/**
 * Public version of {@link AutoConfigurationSorter} for use in tests.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
public class TestAutoConfigurationSorter extends AutoConfigurationSorter {

  public TestAutoConfigurationSorter(MetadataReaderFactory metadataReaderFactory,
          UnaryOperator<String> replacementMapper) {
    super(metadataReaderFactory, AutoConfigurationMetadata.valueOf(new Properties()), replacementMapper);
  }

  @Override
  public List<String> getInPriorityOrder(Collection<String> classNames) {
    return super.getInPriorityOrder(classNames);
  }

}
