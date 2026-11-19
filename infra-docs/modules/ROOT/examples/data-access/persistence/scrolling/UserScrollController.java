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

package infra.web;

// tag::http-scroll[]

import org.jspecify.annotations.Nullable;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import infra.http.HttpStatus;
import infra.persistence.EntityManager;
import infra.persistence.Scroll;
import infra.persistence.ScrollPageable;
import infra.persistence.ScrollPosition;
import infra.persistence.annotation.Column;
import infra.persistence.annotation.EntityRef;
import infra.persistence.annotation.Id;
import infra.persistence.annotation.Table;
import infra.persistence.sql.OrderSpec;
import infra.persistence.sql.OrderSpecSource;
import infra.web.annotation.PostMapping;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RestController;
import infra.web.server.ResponseStatusException;

@RestController
class UserScrollController {

  private final EntityManager entityManager;

  UserScrollController(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @PostMapping("/users/scroll")
  ScrollResponse scroll(@RequestBody ScrollRequest request) {
    String name = request.name();
    if (name == null || name.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
    }
    int limit = request.limit() != null ? request.limit() : 20;
    if (limit < 1 || limit > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 100");
    }

    Cursor cursor = request.cursor();
    ScrollPosition position = cursor == null ? null : cursor.toPosition(name);
    Scroll<User> window = entityManager.scroll(User.class, new UserFilter(name), new ScrollPageable(limit, position));
    ScrollPosition next = window.nextPosition();
    return new ScrollResponse(window.rows().stream().map(user -> Long.toString(user.id)).toList(),
            next == null ? null : Cursor.fromPosition(name, next));
  }

  record ScrollRequest(@Nullable String name, @Nullable Integer limit, @Nullable Cursor cursor) { }

  record ScrollResponse(List<String> items, @JsonInclude(JsonInclude.Include.NON_NULL) @Nullable Cursor nextCursor) { }

  record Cursor(@Nullable String name, @Nullable String createdAt, @Nullable String id) {

    ScrollPosition toPosition(String filterName) {
      if (!filterName.equals(name) || createdAt == null || id == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cursor is incomplete or does not match the filter");
      }
      try {
        return ScrollPosition.builder().desc("createdAt", Instant.parse(createdAt))
                .desc("id", Long.parseLong(id)).build();
      }
      catch (DateTimeException | NumberFormatException ex) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor", ex);
      }
    }

    static Cursor fromPosition(String filterName, ScrollPosition position) {
      // The entries follow the fixed sort in UserFilter.
      return new Cursor(filterName, ((Instant) position.cursor().get(0).value()).toString(),
              Long.toString((Long) position.cursor().get(1).value()));
    }

  }

  @EntityRef(User.class)
  static class UserFilter implements OrderSpecSource {

    public final String name;

    UserFilter(String name) {
      this.name = name;
    }

    @Override
    public OrderSpec orderSpec() {
      return OrderSpec.builder().desc("created_at").desc("id").build();
    }

  }

  @Table("scroll_user")
  static class User {

    @Id
    public long id;

    @Column("created_at")
    public Instant createdAt;

    public String name;

  }

}
// end::http-scroll[]
