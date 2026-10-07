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

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.persistence.EntityMetadata;
import infra.persistence.EntityProperty;
import infra.util.Assert;

/**
 * Read-only failure information for an entity operation.
 *
 * <p>The selected properties are snapshotted, while metadata, ID and exception
 * retain their original references. This context does not imply rollback or
 * absence of database changes. For batch notifications, the phase and exception
 * describe the shared batch failure, not necessarily a failure of each entity.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public final class EntityFailureContext {

  private final EntityMetadata metadata;

  private final EntityOperationPhase phase;

  private final Throwable exception;

  private final @Nullable List<EntityProperty> properties;

  private final @Nullable Object id;

  /**
   * Create a failure snapshot.
   *
   * @param metadata the entity metadata
   * @param phase the phase in which the failure occurred
   * @param properties complete selected write properties, or {@code null} when
   * selection has not completed or for deletion
   * @param id the operation ID when known, otherwise {@code null}
   * @param exception the original failure
   */
  public EntityFailureContext(EntityMetadata metadata, EntityOperationPhase phase,
          @Nullable List<EntityProperty> properties, @Nullable Object id, Throwable exception) {
    Assert.notNull(metadata, "Entity metadata is required");
    Assert.notNull(phase, "Operation phase is required");
    Assert.notNull(exception, "Exception is required");
    this.metadata = metadata;
    this.phase = phase;
    this.properties = properties == null ? null : List.copyOf(properties);
    this.id = id;
    this.exception = exception;
  }

  /**
   * Return the entity metadata.
   */
  public EntityMetadata getMetadata() {
    return metadata;
  }

  /**
   * Return the failure phase, independently of transaction outcome.
   */
  public EntityOperationPhase getPhase() {
    return phase;
  }

  /**
   * Return the original failure; failure listener exceptions may be suppressed on it.
   */
  public Throwable getException() {
    return exception;
  }

  /**
   * Return immutable write properties in SQL order, or {@code null} when unavailable.
   */
  public @Nullable List<EntityProperty> getProperties() {
    return properties;
  }

  /**
   * Return the operation ID when known, otherwise {@code null}.
   */
  public @Nullable Object getId() {
    return id;
  }

}
