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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import infra.http.MediaType;
import infra.http.converter.json.JacksonJsonHttpMessageConverter;
import infra.jdbc.RepositoryManager;
import infra.persistence.support.DefaultEntityManager;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.ResultActions;
import infra.test.web.mock.setup.MockMvcBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static infra.test.web.mock.request.MockMvcRequestBuilders.post;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

class UserScrollExampleTests {

  private final RepositoryManager repositoryManager = new RepositoryManager(
          "jdbc:h2:mem:userScrollExample;DB_CLOSE_DELAY=-1", "sa", "");

  private final JsonMapper json = JsonMapper.builder().build();

  private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
                  new UserScrollController(new DefaultEntityManager(repositoryManager)))
          .setMessageConverters(new JacksonJsonHttpMessageConverter(json)).build();

  @BeforeEach
  void createUsers() {
    repositoryManager.createNamedQuery("""
            create table scroll_user (
              id bigint primary key,
              created_at timestamp(9) with time zone not null,
              name varchar(100) not null
            )
            """).executeUpdate();
    repositoryManager.createNamedQuery("""
            insert into scroll_user (id, created_at, name) values
              (9007199254740991, '2026-01-01T00:00:00.123456788Z', 'Jo'),
              (9007199254740992, '2026-01-01T00:00:00.123456789Z', 'Jo'),
              (9007199254740993, '2026-01-01T00:00:00.123456789Z', 'Jo'),
              (9007199254740994, '2026-01-01T00:00:00.123456789Z', 'Jo'),
              (9007199254740995, '2026-01-01T00:00:00.123456789Z', 'Li')
            """).executeUpdate();
  }

  @AfterEach
  void dropUsers() {
    repositoryManager.createNamedQuery("drop table if exists scroll_user").executeUpdate();
  }

  @Test
  void resumesUsingTheReturnedCursorAcrossTiedTimestamps() throws Exception {
    JsonNode first = scroll("""
            {"name":"Jo","limit":2}
            """);
    assertThat(first.path("items")).isEqualTo(json.readTree("""
            ["9007199254740994","9007199254740993"]
            """));
    JsonNode cursor = first.path("nextCursor");
    assertThat(cursor.path("createdAt").asString()).isEqualTo("2026-01-01T00:00:00.123456789Z");
    assertThat(cursor.path("id").isString()).isTrue();
    assertThat(cursor.path("id").asString()).isEqualTo("9007199254740993");

    JsonNode last = scroll("""
            {"name":"Jo","limit":2,"cursor":%s}
            """.formatted(cursor));
    assertThat(last.path("items")).isEqualTo(json.readTree("""
            ["9007199254740992","9007199254740991"]
            """));
    assertThat(last.has("nextCursor")).isFalse();
  }

  @Test
  void changingTheFilterRequiresStartingWithoutACursor() throws Exception {
    JsonNode first = scroll("""
            {"name":"Jo","limit":2}
            """);
    performScroll("""
            {"name":"Li","limit":2,"cursor":%s}
            """.formatted(first.path("nextCursor"))).andExpect(status().isBadRequest());

    JsonNode restarted = scroll("""
            {"name":"Li","limit":2}
            """);
    assertThat(restarted.path("items")).isEqualTo(json.readTree("""
            ["9007199254740995"]
            """));
    assertThat(restarted.has("nextCursor")).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {
          """
                  {"name":"Jo","createdAt":"2026-01-01T00:00:00.123456789Z"}
                  """,
          """
                  {"name":"Jo","createdAt":"not-an-instant","id":"9007199254740993"}
                  """,
          """
                  {"name":"Jo","createdAt":"2026-01-01T00:00:00.123456789Z","id":"9223372036854775808"}
                  """
  })
  void rejectsInvalidCursors(String cursor) throws Exception {
    performScroll("""
            {"name":"Jo","cursor":%s}
            """.formatted(cursor)).andExpect(status().isBadRequest());
  }

  @Test
  void emptyResultsDoNotReturnACursor() throws Exception {
    JsonNode response = scroll("""
            {"name":"No matches"}
            """);
    assertThat(response.path("items")).isEqualTo(json.createArrayNode());
    assertThat(response.has("nextCursor")).isFalse();
  }

  private JsonNode scroll(String request) throws Exception {
    return json.readTree(performScroll(request).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());
  }

  private ResultActions performScroll(String request) throws Exception {
    return mvc.perform(post("/users/scroll").contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).content(request));
  }

}
