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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import infra.http.MediaType;
import infra.http.converter.json.JacksonJsonHttpMessageConverter;
import infra.jdbc.RepositoryManager;
import infra.jdbc.model.UserModel;
import infra.persistence.EntityManager;
import infra.persistence.Scroll;
import infra.persistence.ScrollPageable;
import infra.persistence.ScrollPosition;
import infra.persistence.ScrollPositionSource;
import infra.persistence.annotation.EntityRef;
import infra.persistence.annotation.Id;
import infra.persistence.annotation.OrderBy;
import infra.persistence.support.DefaultEntityManager;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.setup.MockMvcBuilders;
import infra.web.annotation.GetMapping;
import infra.web.annotation.PostMapping;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestParam;
import infra.web.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.request.MockMvcRequestBuilders.post;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises keyset scrolling across real MVC requests and a JDBC repository.
 */
class ScrollPaginationIntegrationTests {

  @Test
  void postJsonScrollsAcrossWindows() throws Exception {
    try (TestStore store = new TestStore()) {
      JsonMapper json = JsonMapper.builder().build();
      MockMvc mvc = MockMvcBuilders.standaloneSetup(new PostScrollController(store.entityManager))
              .setMessageConverters(new JacksonJsonHttpMessageConverter(json)).build();

      ScrollResponse first = postScroll(mvc, json, "{\"name\":\"same\"}");
      assertThat(first.ids()).containsExactly(store.users.get(0).id, store.users.get(2).id);
      assertThat(first.last()).isFalse();
      assertThat(first.nextCursor()).isNotNull();

      ScrollResponse second = postScroll(mvc, json,
              json.writeValueAsString(Map.of("name", "same",
                      "age", first.nextCursor().age(), "id", first.nextCursor().id())));
      assertThat(second.ids()).containsExactly(store.users.get(3).id, store.users.get(4).id);
      assertThat(second.last()).isFalse();

      ScrollResponse third = postScroll(mvc, json,
              json.writeValueAsString(Map.of("name", "same",
                      "age", second.nextCursor().age(), "id", second.nextCursor().id())));
      assertThat(third.ids()).containsExactly(store.users.get(5).id);
      assertThat(third.last()).isTrue();
      assertThat(third.nextCursor()).isNull();
    }
  }

  @Test
  void getQueryParametersScrollAcrossWindowsAndControlSize() throws Exception {
    try (TestStore store = new TestStore()) {
      JsonMapper json = JsonMapper.builder().build();
      MockMvc mvc = MockMvcBuilders.standaloneSetup(new GetScrollController(store.entityManager))
              .setMessageConverters(new JacksonJsonHttpMessageConverter(json)).build();

      ScrollResponse first = getScroll(mvc, json, "same", 2, null);
      assertThat(first.ids()).containsExactly(store.users.get(0).id, store.users.get(2).id);
      assertThat(first.last()).isFalse();
      assertThat(first.nextCursor()).isNotNull();

      ScrollResponse second = getScroll(mvc, json, "same", 2, first.nextCursor());
      assertThat(second.ids()).containsExactly(store.users.get(3).id, store.users.get(4).id);
      assertThat(second.last()).isFalse();

      ScrollResponse third = getScroll(mvc, json, "same", 2, second.nextCursor());
      assertThat(third.ids()).containsExactly(store.users.get(5).id);
      assertThat(third.last()).isTrue();
      assertThat(third.nextCursor()).isNull();

      ScrollResponse largerWindow = getScroll(mvc, json, "same", 3, null);
      assertThat(largerWindow.ids()).containsExactly(store.users.get(0).id, store.users.get(2).id, store.users.get(3).id);
      assertThat(largerWindow.last()).isFalse();
    }
  }

  private static ScrollResponse postScroll(MockMvc mvc, JsonMapper json, String request) throws Exception {
    String response = mvc.perform(post("/users/scroll")
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    return json.readValue(response, ScrollResponse.class);
  }

  private static ScrollResponse getScroll(MockMvc mvc, JsonMapper json, String name, int pageSize,
          @Nullable ScrollCursor cursor) throws Exception {
    var request = get("/users/scroll").param("name", name).param("pageSize", String.valueOf(pageSize))
            .accept(MediaType.APPLICATION_JSON);
    if (cursor != null) {
      request.param("cursorAge", cursor.age().toString());
      request.param("cursorId", cursor.id().toString());
    }
    String response = mvc.perform(request)
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    return json.readValue(response, ScrollResponse.class);
  }

  private static ScrollResponse response(Scroll<UserView> scroll) {
    ScrollCursor next = null;
    if (scroll.hasNext() && !scroll.isEmpty()) {
      UserView row = scroll.lastRow();
      next = new ScrollCursor(row.age, row.id);
    }
    return new ScrollResponse(scroll.rows().stream().map(user -> user.id).toList(), scroll.isLast(), next);
  }

  private static final class TestStore implements AutoCloseable {

    final RepositoryManager repositoryManager = new RepositoryManager("jdbc:h2:mem:scrollWeb;DB_CLOSE_DELAY=-1", "sa", "");

    final EntityManager entityManager = new DefaultEntityManager(repositoryManager);

    final List<UserModel> users = List.of(
            UserModel.male("same", 10), UserModel.male("other", 15),
            UserModel.male("same", 10), UserModel.male("same", 20),
            UserModel.male("same", 30), UserModel.male("same", 40));

    TestStore() {
      repositoryManager.createNamedQuery("drop table if exists t_user").executeUpdate();
      repositoryManager.createNamedQuery("create table t_user (id int auto_increment primary key, age int, name varchar(255), gender int)")
              .executeUpdate();
      entityManager.persist(users);
    }

    @Override
    public void close() {
      repositoryManager.createNamedQuery("drop table t_user").executeUpdate();
    }
  }

  @RestController
  static class PostScrollController {

    private final EntityManager entityManager;

    PostScrollController(EntityManager entityManager) {
      this.entityManager = entityManager;
    }

    @PostMapping("/users/scroll")
    public ScrollResponse scroll(@RequestBody UserSearch request) {
      return response(entityManager.scroll(UserView.class, request, ScrollPageable.of(2)));
    }
  }

  @RestController
  static class GetScrollController {

    private final EntityManager entityManager;

    GetScrollController(EntityManager entityManager) {
      this.entityManager = entityManager;
    }

    @GetMapping("/users/scroll")
    public ScrollResponse scroll(@RequestParam String name, @RequestParam int pageSize,
            @RequestParam(required = false) @Nullable Integer cursorAge,
            @RequestParam(required = false) @Nullable Integer cursorId) {
      UserSearch request = new UserSearch();
      request.name = name;
      request.age = cursorAge;
      request.id = cursorId;
      return response(entityManager.scroll(UserView.class, request, ScrollPageable.of(pageSize)));
    }
  }

  @EntityRef(UserModel.class)
  public static class UserSearch implements ScrollPositionSource {

    public String name;

    public Integer age;

    public Integer id;

    @Override
    public @Nullable ScrollPosition scrollPosition() {
      return ScrollPosition.builder()
              .asc("age", age)
              .asc("id", id)
              .build();
    }
  }

  @EntityRef(UserModel.class)
  static class UserView {

    @Id
    public Integer id;

    @OrderBy
    public Integer age;

    public String name;
  }

  record ScrollResponse(List<Integer> ids, boolean last, @Nullable ScrollCursor nextCursor) {
  }

  record ScrollCursor(Integer age, Integer id) {
  }

  @EntityRef(UserModel.class)
  public static class UserSearchOrderBy {

    public String name;

    @OrderBy(order = 2)
    public Integer id;

    @OrderBy(order = 1)
    public Integer age;
  }
}
