/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.jdbc.datasource;

import java.sql.SQLException;
import java.sql.ShardingKey;

import cn.taketoday.lang.Nullable;

/**
 * Strategy interface for determining sharding keys which are used to establish direct
 * shard connections in the context of sharded databases. This is used as a callback
 * for providing the current sharding key (plus optionally a super sharding key) in
 * {@link cn.taketoday.jdbc.datasource.ShardingKeyDataSourceAdapter}.
 *
 * <p>Can be used as a functional interface (e.g. with a lambda expression) for a simple
 * sharding key, or as a two-method interface when including a super sharding key as well.
 *
 * @author Mohamed Lahyane (Anir)
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ShardingKeyDataSourceAdapter#setShardingKeyProvider
 * @since 4.0
 */
public interface ShardingKeyProvider {

  /**
   * Determine the sharding key. This method returns the sharding key relevant to the current
   * context which will be used to obtain a direct shard connection.
   *
   * @return the sharding key, or {@code null} if it is not available or cannot be determined
   * @throws SQLException if an error occurs while obtaining the sharding key
   */
  @Nullable
  ShardingKey getShardingKey() throws SQLException;

  /**
   * Determine the super sharding key, if any. This method returns the super sharding key
   * relevant to the current context which will be used to obtain a direct shard connection.
   *
   * @return the super sharding key, or {@code null} if it is not available or cannot be
   * determined (the default)
   * @throws SQLException if an error occurs while obtaining the super sharding key
   */
  @Nullable
  default ShardingKey getSuperShardingKey() throws SQLException {
    return null;
  }

}
