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

package cn.taketoday.framework.jdbc.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

/**
 * A {@link DataSourcePoolMetadataProvider} implementation that returns the first
 * {@link DataSourcePoolMetadata} that is found by one of its delegate.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class CompositeDataSourcePoolMetadataProvider implements DataSourcePoolMetadataProvider {

  private final List<DataSourcePoolMetadataProvider> providers;

  /**
   * Create a {@link CompositeDataSourcePoolMetadataProvider} instance with an initial
   * collection of delegates to use.
   *
   * @param providers the data source pool metadata providers
   */
  public CompositeDataSourcePoolMetadataProvider(Collection<? extends DataSourcePoolMetadataProvider> providers) {
    this.providers = (providers != null) ? Collections.unmodifiableList(new ArrayList<>(providers))
                                         : Collections.emptyList();
  }

  @Override
  public DataSourcePoolMetadata getDataSourcePoolMetadata(DataSource dataSource) {
    for (DataSourcePoolMetadataProvider provider : this.providers) {
      DataSourcePoolMetadata metadata = provider.getDataSourcePoolMetadata(dataSource);
      if (metadata != null) {
        return metadata;
      }
    }
    return null;
  }

}
