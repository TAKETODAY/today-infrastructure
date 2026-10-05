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

package infra.persistence.auditing;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import infra.beans.BeanWrapper;
import infra.persistence.EntityMetadata;
import infra.persistence.EntityProperty;
import infra.persistence.IllegalEntityException;
import infra.persistence.PropertyUpdateStrategy;
import infra.persistence.annotation.UpdateBy;
import infra.persistence.annotation.Version;
import infra.persistence.event.PersistEventListener;
import infra.persistence.event.UpdateEventListener;
import infra.persistence.support.DefaultEntityManager;
import infra.util.Assert;

/**
 * Populates annotated audit properties before insert and update. Register with
 * {@link DefaultEntityManager#getEntityEventRegistry()} to enable auditing.
 * Creation properties are overwritten on insert and excluded from updates;
 * modification properties are populated on both operations. Non-null audit
 * properties participate in SQL regardless of the supplied property strategy.
 * Subsequent listeners may replace the returned strategy and override these rules.
 *
 * <p>Time is sampled once per entity operation. Local date-times use the clock's
 * zone. A missing auditor preserves existing values. Invalid annotations or
 * incompatible auditor values fail before SQL execution. Callback mutations are
 * not restored when an operation fails or its transaction rolls back.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/10/05
 */
public class AuditingEntityListener implements PersistEventListener<Object>, UpdateEventListener<Object> {

  private final Clock clock;

  private final AuditorAware<?> auditorAware;

  private final Map<EntityMetadata, AuditMetadata> metadataCache =
          Collections.synchronizedMap(new IdentityHashMap<>());

  /**
   * Create a timestamp-only listener using the UTC system clock.
   */
  public AuditingEntityListener() {
    this(Clock.systemUTC(), () -> null);
  }

  /**
   * Create a listener using the given clock and auditor provider.
   *
   * @param clock the clock and zone to use
   * @param auditorAware the provider, which may return {@code null}
   */
  public AuditingEntityListener(Clock clock, AuditorAware<?> auditorAware) {
    Assert.notNull(clock, "clock is required");
    Assert.notNull(auditorAware, "auditorAware is required");
    this.clock = clock;
    this.auditorAware = auditorAware;
  }

  @Override
  public PropertyUpdateStrategy onPrePersist(Object entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
    AuditMetadata auditing = metadataCache.computeIfAbsent(metadata, this::inspect);
    if (auditing.properties().isEmpty()) {
      return strategy;
    }
    audit(entity, auditing.properties(), true);
    return (target, property) -> (auditing.roles().containsKey(property) && property.hasValue(target))
            || strategy.shouldUpdate(target, property);
  }

  @Override
  public PropertyUpdateStrategy onPreUpdate(Object entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
    AuditMetadata auditing = metadataCache.computeIfAbsent(metadata, this::inspect);
    if (auditing.properties().isEmpty()) {
      return strategy;
    }
    audit(entity, auditing.properties(), false);
    return (target, property) -> {
      Boolean created = auditing.roles().get(property);
      if (Boolean.TRUE.equals(created)) {
        return false;
      }
      return created != null ? property.hasValue(target) : strategy.shouldUpdate(target, property);
    };
  }

  private void audit(Object entity, List<AuditProperty> properties, boolean insert) {
    Instant now = null;
    Object auditor = null;
    BeanWrapper auditorProperties = null;
    boolean auditorResolved = false;
    for (AuditProperty audit : properties) {
      if (!insert && audit.created()) {
        continue;
      }
      EntityProperty property = audit.property();
      if (audit.date()) {
        if (now == null) {
          now = clock.instant();
        }
        property.setValue(entity, dateValue(property.getType(), now));
      }
      else {
        if (!auditorResolved) {
          auditor = auditorAware.getCurrentAuditor();
          auditorResolved = true;
        }
        if (auditor != null) {
          Object value = auditor;
          if (!audit.auditorPath().isEmpty()) {
            if (auditorProperties == null) {
              auditorProperties = BeanWrapper.forBeanPropertyAccess(auditor);
            }
            value = auditorProperties.getPropertyValue(audit.auditorPath());
          }
          if (value != null) {
            property.setValue(entity, value);
          }
        }
      }
    }
  }

  private AuditMetadata inspect(EntityMetadata metadata) {
    List<AuditProperty> result = new ArrayList<>();
    Map<EntityProperty, Boolean> roles = new IdentityHashMap<>();
    for (EntityProperty property : metadata.getEntityProperties(true)) {
      var createdByAnnotation = property.getAnnotation(CreatedBy.class);
      var modifiedByAnnotation = property.getAnnotation(LastModifiedBy.class);
      boolean createdBy = createdByAnnotation.isPresent();
      boolean modifiedBy = modifiedByAnnotation.isPresent();
      boolean createdDate = property.isPresent(CreatedDate.class);
      boolean modifiedDate = property.isPresent(LastModifiedDate.class);
      int count = (createdDate ? 1 : 0) + (createdBy ? 1 : 0)
              + (modifiedDate ? 1 : 0) + (modifiedBy ? 1 : 0);
      if (count == 0) {
        continue;
      }
      if (count != 1
              || property.isIdProperty()
              || property.isPresent(Version.class)
              || property.isPresent(UpdateBy.class)) {
        throw new IllegalEntityException("Conflicting audit annotations on property: " + property.getName());
      }
      boolean date = createdDate || modifiedDate;
      Class<?> type = property.getType();
      if (date && type != Instant.class && type != LocalDateTime.class
              && type != OffsetDateTime.class && type != ZonedDateTime.class) {
        throw new IllegalEntityException("Unsupported audit date type: " + type.getName());
      }
      String auditorPath = createdBy ? createdByAnnotation.getStringValue()
              : modifiedBy ? modifiedByAnnotation.getStringValue() : "";
      result.add(new AuditProperty(property, createdDate || createdBy, date, auditorPath));
      roles.put(property, createdDate || createdBy);
    }
    return new AuditMetadata(List.copyOf(result), Collections.unmodifiableMap(roles));
  }

  private Object dateValue(Class<?> type, Instant now) {
    if (type == Instant.class) {
      return now;
    }
    if (type == LocalDateTime.class) {
      return LocalDateTime.ofInstant(now, clock.getZone());
    }
    if (type == OffsetDateTime.class) {
      return OffsetDateTime.ofInstant(now, clock.getZone());
    }
    return ZonedDateTime.ofInstant(now, clock.getZone());
  }

  private record AuditMetadata(List<AuditProperty> properties, Map<EntityProperty, Boolean> roles) {
  }

  private record AuditProperty(EntityProperty property, boolean created, boolean date, String auditorPath) {
  }

}
