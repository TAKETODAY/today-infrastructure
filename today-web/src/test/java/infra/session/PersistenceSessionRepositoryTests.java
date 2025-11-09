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

package infra.session;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/3/3 16:46
 */
class PersistenceSessionRepositoryTests {

  @TempDir
  File tempDir;

//  static class

  static class User implements Serializable {
    public int age;
    public String name;

    public int getAge() {
      return age;
    }

    public void setAge(int age) {
      this.age = age;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof User user))
        return false;
      return age == user.age && Objects.equals(name, user.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(age, name);
    }

  }

  @Test
  void lifecycle() throws IOException {
    var idGenerator = new SecureRandomSessionIdGenerator();
    var delegate = new InMemorySessionRepository(new SessionEventDispatcher(), idGenerator);
    var persister = new FileSessionPersister(delegate);
    persister.setDirectory(tempDir);

    var repository = new PersistenceSessionRepository(persister, delegate);

    String id = idGenerator.generateId();

    assertThat(repository.contains(id)).isFalse();
    assertThat(repository.getSessionCount()).isZero();
    assertThat(repository.getIdentifiers()).isEmpty();

    Session retrieveSession = repository.retrieveSession(id);
    assertThat(retrieveSession).isNull();

    Session session = repository.createSession(id);
    User loginUser = new User();
    loginUser.age = 20;
    loginUser.name = "TODAY";
    session.setAttribute("name", "value");
    session.setAttribute("loginUser", loginUser);
    persister.persist(session);

    assertThat(repository.getSessionCount()).isEqualTo(1);
    assertThat(repository.getIdentifiers()).contains(id);

    retrieveSession = repository.retrieveSession(id);
    assertThat(retrieveSession).isNotNull();
    assertThat(retrieveSession.getId()).isEqualTo(id);
    assertThat(retrieveSession.hasAttributes()).isTrue();
    assertThat(retrieveSession.getAttributeNames()).contains("name", "loginUser");
    assertThat(retrieveSession.getAttributes()).isNotNull()
            .contains(Map.entry("loginUser", loginUser), Map.entry("name", "value"));

    persister.clear();

    assertThat(repository.getSessionCount()).isEqualTo(1);
    assertThat(repository.getIdentifiers()).contains(id);
    retrieveSession = repository.retrieveSession(id);

    assertThat(retrieveSession).isNotNull();
    assertThat(retrieveSession.hasAttribute("name")).isTrue();
    assertThat(retrieveSession.hasAttribute("loginUser")).isTrue();
    assertThat(retrieveSession.hasAttribute("loginUser1")).isFalse();
    assertThat(retrieveSession.getAttribute("name")).isEqualTo("value");
    assertThat(retrieveSession.getAttribute("loginUser")).isEqualTo(loginUser);
    assertThat(retrieveSession).isSameAs(repository.retrieveSession(id));
    assertThat(retrieveSession.getCreationTime()).isEqualTo(session.getCreationTime());
    assertThat(retrieveSession.isExpired()).isEqualTo(session.isExpired());
    assertThat(session).isNotEqualTo(retrieveSession);

    assertThat(persister.keys()).isEmpty();

    // destroy

    repository.destroy();
    assertThat(repository.contains(id)).isTrue();
    assertThat(persister.keys()).hasSize(1).contains(id);
    assertThat(repository.getSessionCount()).isEqualTo(1);
    assertThat(repository.getIdentifiers()).contains(id);

    // removeSession

    repository.removeSession(retrieveSession);
    assertThat(persister.keys()).isEmpty();
    assertThat(repository.contains(id)).isFalse();
    assertThat(repository.getSessionCount()).isZero();
    assertThat(repository.getIdentifiers()).isEmpty();

  }

  @Test
  void constructor_ShouldThrowException_WhenSessionPersisterIsNull() {
    var delegate = new InMemorySessionRepository(new SessionEventDispatcher(), new SecureRandomSessionIdGenerator());

    assertThatThrownBy(() -> new PersistenceSessionRepository(null, delegate))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("SessionPersister is required");
  }

  @Test
  void constructor_ShouldThrowException_WhenDelegateIsNull() {
    var persister = new FileSessionPersister(new InMemorySessionRepository(new SessionEventDispatcher(), new SecureRandomSessionIdGenerator()));

    assertThatThrownBy(() -> new PersistenceSessionRepository(persister, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("SessionRepository is required");
  }

  @Test
  void createSession_ShouldDelegateToUnderlyingRepository() {
    var idGenerator = new SecureRandomSessionIdGenerator();
    var delegate = new InMemorySessionRepository(new SessionEventDispatcher(), idGenerator);
    var persister = new FileSessionPersister(delegate);
    var repository = new PersistenceSessionRepository(persister, delegate);

    Session session = repository.createSession();

    assertThat(session).isNotNull();
    assertThat(session.getId()).isNotBlank();
  }

  @Test
  void createSessionWithId_ShouldDelegateToUnderlyingRepository() {
    var idGenerator = new SecureRandomSessionIdGenerator();
    var delegate = new InMemorySessionRepository(new SessionEventDispatcher(), idGenerator);
    var persister = new FileSessionPersister(delegate);
    var repository = new PersistenceSessionRepository(persister, delegate);

    String customId = "custom-session-id";
    Session session = repository.createSession(customId);

    assertThat(session).isNotNull();
    assertThat(session.getId()).isEqualTo(customId);
  }

  @Test
  void retrieveSession_ShouldReturnNull_WhenSessionNotFound() {
    var idGenerator = new SecureRandomSessionIdGenerator();
    var delegate = new InMemorySessionRepository(new SessionEventDispatcher(), idGenerator);
    var persister = new FileSessionPersister(delegate);
    var repository = new PersistenceSessionRepository(persister, delegate);

    Session session = repository.retrieveSession("non-existent-id");

    assertThat(session).isNull();
  }

  @Test
  void retrieveSession_ShouldReturnSessionFromDelegate_WhenAvailable() {
    var idGenerator = new SecureRandomSessionIdGenerator();
    var delegate = new InMemorySessionRepository(new SessionEventDispatcher(), idGenerator);
    var persister = new FileSessionPersister(delegate);
    var repository = new PersistenceSessionRepository(persister, delegate);

    String id = idGenerator.generateId();
    Session createdSession = repository.createSession(id);
    createdSession.setAttribute("key", "value");
    createdSession.save();

    Session retrievedSession = repository.retrieveSession(id);

    assertThat(retrievedSession).isNotNull();
    assertThat(retrievedSession.getId()).isEqualTo(id);
    assertThat(retrievedSession.getAttribute("key")).isEqualTo("value");
  }

  @Test
  void removeSessionByInstance_ShouldRemoveFromDelegateAndPersister() {
    var idGenerator = new SecureRandomSessionIdGenerator();
    var delegate = new InMemorySessionRepository(new SessionEventDispatcher(), idGenerator);
    var persister = new FileSessionPersister(delegate);
    var repository = new PersistenceSessionRepository(persister, delegate);

    String id = idGenerator.generateId();
    Session session = repository.createSession(id);
    session.setAttribute("key", "value");
    session.save();

    assertThat(repository.getSessionCount()).isEqualTo(1);

    repository.removeSession(session);

    assertThat(repository.getSessionCount()).isEqualTo(0);
    assertThat(repository.retrieveSession(id)).isNull();
  }

  @Test
  void removeSessionById_ShouldRemoveFromDelegateAndPersister() {
    var idGenerator = new SecureRandomSessionIdGenerator();
    var delegate = new InMemorySessionRepository(new SessionEventDispatcher(), idGenerator);
    var persister = new FileSessionPersister(delegate);
    var repository = new PersistenceSessionRepository(persister, delegate);

    String id = idGenerator.generateId();
    Session session = repository.createSession(id);
    session.setAttribute("key", "value");
    session.save();

    assertThat(repository.getSessionCount()).isEqualTo(1);

    Session removedSession = repository.removeSession(id);

    assertThat(removedSession).isNotNull();
    assertThat(repository.getSessionCount()).isEqualTo(0);
    assertThat(repository.retrieveSession(id)).isNull();
  }

  @Test
  void updateLastAccessTime_ShouldDelegateToUnderlyingRepository() {
    var idGenerator = new SecureRandomSessionIdGenerator();
    var delegate = new InMemorySessionRepository(new SessionEventDispatcher(), idGenerator);
    var persister = new FileSessionPersister(delegate);
    var repository = new PersistenceSessionRepository(persister, delegate);

    String id = idGenerator.generateId();
    Session session = repository.createSession(id);
    session.save();

    Instant beforeUpdate = session.getLastAccessTime();

    // Advance clock
    delegate.setClock(Clock.offset(delegate.getClock(), Duration.ofSeconds(10)));

    repository.updateLastAccessTime(session);

    assertThat(session.getLastAccessTime()).isAfter(beforeUpdate);
  }

  @Test
  void contains_ShouldReturnFalse_WhenSessionNotExists() {
    var idGenerator = new SecureRandomSessionIdGenerator();
    var delegate = new InMemorySessionRepository(new SessionEventDispatcher(), idGenerator);
    var persister = new FileSessionPersister(delegate);
    var repository = new PersistenceSessionRepository(persister, delegate);

    boolean result = repository.contains("non-existent-id");

    assertThat(result).isFalse();
  }

  @Test
  void getSessionCount_ShouldReturnZero_WhenNoSessions() {
    var idGenerator = new SecureRandomSessionIdGenerator();
    var delegate = new InMemorySessionRepository(new SessionEventDispatcher(), idGenerator);
    var persister = new FileSessionPersister(delegate);
    var repository = new PersistenceSessionRepository(persister, delegate);

    int count = repository.getSessionCount();

    assertThat(count).isEqualTo(0);
  }

  @Test
  void getIdentifiers_ShouldReturnEmptyArray_WhenNoSessions() {
    var idGenerator = new SecureRandomSessionIdGenerator();
    var delegate = new InMemorySessionRepository(new SessionEventDispatcher(), idGenerator);
    var persister = new FileSessionPersister(delegate);
    var repository = new PersistenceSessionRepository(persister, delegate);

    String[] identifiers = repository.getIdentifiers();

    assertThat(identifiers).isEmpty();
  }

  @Test
  void persistSessions_ShouldSaveAllDelegateSessions() throws IOException, ClassNotFoundException {
    var idGenerator = new SecureRandomSessionIdGenerator();
    var delegate = new InMemorySessionRepository(new SessionEventDispatcher(), idGenerator);
    var persister = new FileSessionPersister(delegate);
    persister.setDirectory(tempDir);
    var repository = new PersistenceSessionRepository(persister, delegate);

    String id1 = idGenerator.generateId();
    String id2 = idGenerator.generateId();

    Session session1 = repository.createSession(id1);
    session1.setAttribute("key1", "value1");
    session1.save();

    Session session2 = repository.createSession(id2);
    session2.setAttribute("key2", "value2");
    session2.save();

    repository.persistSessions();

    assertThat(persister.keys()).containsExactlyInAnyOrder(id1, id2);

    Session persistedSession1 = persister.findById(id1);
    Session persistedSession2 = persister.findById(id2);

    assertThat(persistedSession1).isNotNull();
    assertThat(persistedSession1.getAttribute("key1")).isEqualTo("value1");

    assertThat(persistedSession2).isNotNull();
    assertThat(persistedSession2.getAttribute("key2")).isEqualTo("value2");
  }

  @Test
  void destroy_ShouldPersistAllSessions() {
    var idGenerator = new SecureRandomSessionIdGenerator();
    var delegate = new InMemorySessionRepository(new SessionEventDispatcher(), idGenerator);
    var persister = new FileSessionPersister(delegate);
    persister.setDirectory(tempDir);
    var repository = new PersistenceSessionRepository(persister, delegate);

    String id = idGenerator.generateId();
    Session session = repository.createSession(id);
    session.setAttribute("key", "value");
    session.save();

    repository.destroy();

    assertThat(persister.keys()).containsExactly(id);
  }

  @Test
  void createDestructionCallback_ShouldCreateValidCallback() {
    var persister = new FileSessionPersister(new InMemorySessionRepository(new SessionEventDispatcher(), new SecureRandomSessionIdGenerator()));
    var callback = PersistenceSessionRepository.createDestructionCallback(persister);

    assertThat(callback).isNotNull();
    assertThat(callback).isInstanceOf(PersistenceSessionRepository.PersisterDestructionCallback.class);
  }

  @Test
  void createDestructionCallback_ShouldThrowException_WhenPersisterIsNull() {
    assertThatThrownBy(() -> PersistenceSessionRepository.createDestructionCallback(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No SessionPersister");
  }

  @Test
  void persisterDestructionCallback_SessionDestroyed_ShouldRemoveFromPersister() throws IOException {
    var idGenerator = new SecureRandomSessionIdGenerator();
    var delegate = new InMemorySessionRepository(new SessionEventDispatcher(), idGenerator);
    var persister = new FileSessionPersister(delegate);
    persister.setDirectory(tempDir);
    var callback = new PersistenceSessionRepository.PersisterDestructionCallback(persister);

    String id = idGenerator.generateId();
    Session session = delegate.createSession(id);
    session.setAttribute("key", "value");
    persister.persist(session);

    assertThat(persister.keys()).contains(id);

    callback.sessionDestroyed(session);

    assertThat(persister.keys()).doesNotContain(id);
  }

}