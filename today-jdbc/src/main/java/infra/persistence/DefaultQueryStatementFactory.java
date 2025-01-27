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

package infra.persistence;

import java.util.List;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/10 16:53
 */
final class DefaultQueryStatementFactory implements QueryStatementFactory {

  private final EntityMetadataFactory factory;

  private final List<ConditionPropertyExtractor> extractors;

  public DefaultQueryStatementFactory(EntityMetadataFactory factory, List<ConditionPropertyExtractor> extractors) {
    this.factory = factory;
    this.extractors = extractors;
  }

  @Override
  public QueryStatement createQuery(Object example) {
    return new ExampleQuery(factory, example, extractors);
  }

  @Override
  public ConditionStatement createCondition(Object example) {
    return new ExampleQuery(factory, example, extractors);
  }

}
