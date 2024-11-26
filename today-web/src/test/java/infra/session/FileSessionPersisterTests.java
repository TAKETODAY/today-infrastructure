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

package infra.session;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.core.env.MapPropertySource;
import infra.session.config.EnableWebSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/28 22:22
 */
class FileSessionPersisterTests {

  @TempDir
  File tempDir;

  @Test
  void illegalArgument() {
    assertThatThrownBy(() ->
            new FileSessionPersister(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("SessionRepository is required");
  }

  @Test
  void enableWebSession() {
    var context = new AnnotationConfigApplicationContext();
    context.getEnvironment().getPropertySources().addFirst(
            new MapPropertySource("server.session", Map.of("server.session.persistent", true))
    );
    context.register(Config.class);
    context.refresh();

    assertThat(context.containsBeanDefinition(PersistenceSessionRepository.class)).isTrue();
    context.close();
  }

  @Test
  void keys() throws IOException {
    var idGenerator = new SecureRandomSessionIdGenerator();
    var repository = new InMemorySessionRepository(new SessionEventDispatcher(), idGenerator);
    FileSessionPersister persister = new FileSessionPersister(repository);

    String id = idGenerator.generateId();
    File idSession = new File(tempDir, id + ".session");
    idSession.createNewFile();

    persister.setDirectory(tempDir);
    assertThat(persister.keys()).hasSize(1).containsExactly(id);

    persister.remove(id);
    assertThat(persister.keys()).hasSize(0);
  }

  @Test
  void persistAndFindById() throws IOException, ClassNotFoundException {
    var idGenerator = new SecureRandomSessionIdGenerator();
    var repository = new InMemorySessionRepository(new SessionEventDispatcher(), idGenerator);
    FileSessionPersister persister = new FileSessionPersister(repository);
    persister.setDirectory(tempDir);

    String id = idGenerator.generateId();

    WebSession session = repository.createSession(id);
    session.setAttribute("name", "value");
    persister.persist(session);

    assertThat(persister.keys()).hasSize(1).containsExactly(id);

    WebSession sessionFromPersister = persister.findById(id);
    assertThat(sessionFromPersister).isNotNull();
    assertThat(sessionFromPersister.getAttributes()).isNotNull().containsEntry("name", "value");

    persister.clear();

    assertThat(persister.keys()).hasSize(0);

    sessionFromPersister = persister.findById(id);
    assertThat(sessionFromPersister).isNull();
  }

  @Configuration
  @EnableWebSession
  static class Config {

  }

}