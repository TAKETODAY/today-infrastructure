/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.persistence.event;

import java.util.ArrayList;

import infra.persistence.EntityMetadata;
import infra.persistence.PropertyUpdateStrategy;

/**
 * Holds the context of a single batch persistence operation.
 *
 * <p>It exposes the SQL statement, the entity metadata, the property update
 * strategy and whether auto-generated IDs are handled, together with the
 * {@link #entities} collected for the batch. Instances are created by the
 * persistence infrastructure; the constructor is {@code protected} and is not
 * intended for application use.
 *
 * <p>The metadata fields are read-only, while {@link #entities} is mutable and
 * accumulates the entities to be processed. A {@code BatchExecution} is passed to
 * {@link BatchPersistListener} callbacks so that listeners can inspect the batch
 * before and after it is executed.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Encapsulates the SQL statement and entity metadata for batch operations.</li>
 *   <li>Reports whether auto-generated IDs are handled for the batch.</li>
 *   <li>Exposes the {@link PropertyUpdateStrategy} used for property updates.</li>
 *   <li>Collects the entities to be processed via the {@link #entities} list.</li>
 * </ul>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BatchPersistListener
 * @see EntityMetadata
 * @see PropertyUpdateStrategy
 * @since 4.0 2024/2/20 23:25
 */
public class BatchExecution {

  public final String statement;

  public final boolean autoGenerateId;

  public final EntityMetadata entityMetadata;

  public final PropertyUpdateStrategy strategy;

  public final ArrayList<Object> entities = new ArrayList<>();

  protected BatchExecution(String statement, PropertyUpdateStrategy strategy,
          EntityMetadata entityMetadata, boolean autoGenerateId) {
    this.statement = statement;
    this.strategy = strategy;
    this.entityMetadata = entityMetadata;
    this.autoGenerateId = autoGenerateId;
  }

}
