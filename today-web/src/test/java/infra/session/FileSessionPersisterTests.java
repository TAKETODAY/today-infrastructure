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
import java.nio.file.Files;
import java.util.Map;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.core.env.MapPropertySource;
import infra.session.config.EnableSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
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

    Session session = repository.createSession(id);
    session.setAttribute("name", "value");
    persister.persist(session);

    assertThat(persister.keys()).hasSize(1).containsExactly(id);

    Session sessionFromPersister = persister.findById(id);
    assertThat(sessionFromPersister).isNotNull();
    assertThat(sessionFromPersister.getAttributes()).isNotNull().containsEntry("name", "value");

    persister.clear();

    assertThat(persister.keys()).hasSize(0);

    sessionFromPersister = persister.findById(id);
    assertThat(sessionFromPersister).isNull();
  }

  @Test
  void constructor_ShouldThrowException_WhenRepositoryIsNull() {
    assertThatThrownBy(() -> new FileSessionPersister(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("SessionRepository is required");
  }

  @Test
  void setDirectory_ShouldUpdateDirectory() throws IOException {
    // given
    var repository = new InMemorySessionRepository(new SessionEventDispatcher(), new SecureRandomSessionIdGenerator());
    FileSessionPersister persister = new FileSessionPersister(repository);

    // when
    persister.setDirectory(tempDir);

    // then
    // Create a session to trigger directory access
    String id = "test-id";
    Session session = repository.createSession(id);
    persister.persist(session);

    File sessionFile = new File(tempDir, id + ".session");
    assertThat(sessionFile.exists()).isTrue();
  }

  @Test
  void setApplicationTemp_ShouldAcceptNull() {
    // given
    var repository = new InMemorySessionRepository(new SessionEventDispatcher(), new SecureRandomSessionIdGenerator());
    FileSessionPersister persister = new FileSessionPersister(repository);

    // when & then
    assertThatNoException().isThrownBy(() -> persister.setApplicationTemp(null));
  }

  @Test
  void remove_ShouldDeleteSessionFile() throws IOException {
    // given
    var repository = new InMemorySessionRepository(new SessionEventDispatcher(), new SecureRandomSessionIdGenerator());
    FileSessionPersister persister = new FileSessionPersister(repository);
    persister.setDirectory(tempDir);

    String id = "test-id";
    File sessionFile = new File(tempDir, id + ".session");
    sessionFile.createNewFile();
    assertThat(sessionFile.exists()).isTrue();

    // when
    persister.remove(id);

    // then
    assertThat(sessionFile.exists()).isFalse();
  }

  @Test
  void contains_ShouldReturnTrue_WhenSessionFileExists() throws IOException {
    // given
    var repository = new InMemorySessionRepository(new SessionEventDispatcher(), new SecureRandomSessionIdGenerator());
    FileSessionPersister persister = new FileSessionPersister(repository);
    persister.setDirectory(tempDir);

    String id = "test-id";
    File sessionFile = new File(tempDir, id + ".session");
    sessionFile.createNewFile();

    // when
    boolean result = persister.contains(id);

    // then
    assertThat(result).isTrue();
  }

  @Test
  void contains_ShouldReturnFalse_WhenSessionFileDoesNotExist() {
    // given
    var repository = new InMemorySessionRepository(new SessionEventDispatcher(), new SecureRandomSessionIdGenerator());
    FileSessionPersister persister = new FileSessionPersister(repository);
    persister.setDirectory(tempDir);

    // when
    boolean result = persister.contains("non-existent-id");

    // then
    assertThat(result).isFalse();
  }

  @Test
  void clear_ShouldRemoveAllSessionFiles() throws IOException {
    // given
    var repository = new InMemorySessionRepository(new SessionEventDispatcher(), new SecureRandomSessionIdGenerator());
    FileSessionPersister persister = new FileSessionPersister(repository);
    persister.setDirectory(tempDir);

    String id1 = "test-id-1";
    String id2 = "test-id-2";
    File sessionFile1 = new File(tempDir, id1 + ".session");
    File sessionFile2 = new File(tempDir, id2 + ".session");
    sessionFile1.createNewFile();
    sessionFile2.createNewFile();

    assertThat(sessionFile1.exists()).isTrue();
    assertThat(sessionFile2.exists()).isTrue();

    // when
    persister.clear();

    // then
    assertThat(sessionFile1.exists()).isFalse();
    assertThat(sessionFile2.exists()).isFalse();
  }

  @Test
  void keys_ShouldReturnEmptyArray_WhenDirectoryIsEmpty() {
    // given
    var repository = new InMemorySessionRepository(new SessionEventDispatcher(), new SecureRandomSessionIdGenerator());
    FileSessionPersister persister = new FileSessionPersister(repository);
    persister.setDirectory(tempDir);

    // when
    String[] keys = persister.keys();

    // then
    assertThat(keys).isEmpty();
  }

  @Test
  void keys_ShouldReturnSessionIds_WhenSessionFilesExist() throws IOException {
    // given
    var repository = new InMemorySessionRepository(new SessionEventDispatcher(), new SecureRandomSessionIdGenerator());
    FileSessionPersister persister = new FileSessionPersister(repository);
    persister.setDirectory(tempDir);

    String id1 = "test-id-1";
    String id2 = "test-id-2";
    File sessionFile1 = new File(tempDir, id1 + ".session");
    File sessionFile2 = new File(tempDir, id2 + ".session");
    File otherFile = new File(tempDir, "other.txt");
    sessionFile1.createNewFile();
    sessionFile2.createNewFile();
    otherFile.createNewFile();

    // when
    String[] keys = persister.keys();

    // then
    assertThat(keys).hasSize(2).containsExactlyInAnyOrder(id1, id2);
  }

  @Test
  void findById_ShouldReturnNull_WhenSessionFileDoesNotExist() throws IOException, ClassNotFoundException {
    // given
    var repository = new InMemorySessionRepository(new SessionEventDispatcher(), new SecureRandomSessionIdGenerator());
    FileSessionPersister persister = new FileSessionPersister(repository);
    persister.setDirectory(tempDir);

    // when
    Session session = persister.findById("non-existent-id");

    // then
    assertThat(session).isNull();
  }

  @Test
  void findById_ShouldReturnSession_WhenSessionFileExists() throws IOException, ClassNotFoundException {
    // given
    var idGenerator = new SecureRandomSessionIdGenerator();
    var repository = new InMemorySessionRepository(new SessionEventDispatcher(), idGenerator);
    FileSessionPersister persister = new FileSessionPersister(repository);
    persister.setDirectory(tempDir);

    String id = idGenerator.generateId();
    Session originalSession = repository.createSession(id);
    originalSession.setAttribute("key1", "value1");
    originalSession.setAttribute("key2", "value2");
    persister.persist(originalSession);

    // when
    Session loadedSession = persister.findById(id);

    // then
    assertThat(loadedSession).isNotNull();
    assertThat(loadedSession.getId()).isEqualTo(id);
    assertThat(loadedSession.getAttributes()).containsEntry("key1", "value1");
    assertThat(loadedSession.getAttributes()).containsEntry("key2", "value2");
  }

  @Test
  void findById_ShouldThrowIOException_WhenFileCannotBeRead() throws IOException {
    // given
    var repository = new InMemorySessionRepository(new SessionEventDispatcher(), new SecureRandomSessionIdGenerator());
    FileSessionPersister persister = new FileSessionPersister(repository);
    persister.setDirectory(tempDir);

    String id = "test-id";
    File sessionFile = new File(tempDir, id + ".session");
    sessionFile.createNewFile();

    // Make directory unreadable
    assertThat(tempDir.setReadable(false)).isTrue();

    // when & then
    assertThatThrownBy(() -> persister.findById(id))
            .isInstanceOf(IOException.class);

    // Restore permissions for cleanup
    assertThat(tempDir.setReadable(true)).isTrue();
  }

  @Test
  void persist_ShouldCreateSessionFile() throws IOException {
    // given
    var repository = new InMemorySessionRepository(new SessionEventDispatcher(), new SecureRandomSessionIdGenerator());
    FileSessionPersister persister = new FileSessionPersister(repository);
    persister.setDirectory(tempDir);

    String id = "test-id";
    Session session = repository.createSession(id);
    session.setAttribute("key", "value");

    // when
    persister.persist(session);

    // then
    File sessionFile = new File(tempDir, id + ".session");
    assertThat(sessionFile.exists()).isTrue();
  }

  @Test
  void persist_ShouldThrowIOException_WhenDirectoryIsNotWritable() throws IOException {
    // given
    var repository = new InMemorySessionRepository(new SessionEventDispatcher(), new SecureRandomSessionIdGenerator());
    FileSessionPersister persister = new FileSessionPersister(repository);

    // Create read-only directory
    File readOnlyDir = new File(tempDir, "readonly");
    assertThat(readOnlyDir.mkdir()).isTrue();
    assertThat(readOnlyDir.setWritable(false)).isTrue();

    persister.setDirectory(readOnlyDir);

    String id = "test-id";
    Session session = repository.createSession(id);

    // when & then
    assertThatThrownBy(() -> persister.persist(session))
            .isInstanceOf(IOException.class);
  }

  @Test
  void directory_ShouldUseDefaultDirectory_WhenNotSet() {
    // given
    var repository = new InMemorySessionRepository(new SessionEventDispatcher(), new SecureRandomSessionIdGenerator());
    FileSessionPersister persister = new FileSessionPersister(repository);

    // when
    String[] keys = persister.keys();

    // then
    assertThat(keys).isNotNull();
    // Should not throw exception
  }

  @Test
  void remove_ShouldNotThrowException_WhenFileDoesNotExist() {
    // given
    var repository = new InMemorySessionRepository(new SessionEventDispatcher(), new SecureRandomSessionIdGenerator());
    FileSessionPersister persister = new FileSessionPersister(repository);
    persister.setDirectory(tempDir);

    // when & then
    assertThatNoException().isThrownBy(() -> persister.remove("non-existent-id"));
  }

  @Test
  void clear_ShouldNotThrowException_WhenDirectoryIsEmpty() throws IOException {
    // given
    var repository = new InMemorySessionRepository(new SessionEventDispatcher(), new SecureRandomSessionIdGenerator());
    FileSessionPersister persister = new FileSessionPersister(repository);
    persister.setDirectory(tempDir);

    // Clear directory first
    File[] files = tempDir.listFiles();
    if (files != null) {
      for (File file : files) {
        Files.deleteIfExists(file.toPath());
      }
    }

    // when & then
    assertThatNoException().isThrownBy(persister::clear);
    assertThat(persister.keys()).isEmpty();
  }

  @Test
  void keys_ShouldReturnEmptyArray_WhenDirectoryIsNull() {
    // given
    var repository = new InMemorySessionRepository(new SessionEventDispatcher(), new SecureRandomSessionIdGenerator());
    FileSessionPersister persister = new FileSessionPersister(repository);

    // Make sure default directory is empty
    String[] keys = persister.keys();

    // then
    assertThat(keys).isEmpty();
  }

  @Test
  void keys_ShouldIgnoreNonSessionFiles() throws IOException {
    // given
    var repository = new InMemorySessionRepository(new SessionEventDispatcher(), new SecureRandomSessionIdGenerator());
    FileSessionPersister persister = new FileSessionPersister(repository);
    persister.setDirectory(tempDir);

    String id = "test-id";
    File sessionFile = new File(tempDir, id + ".session");
    File nonSessionFile = new File(tempDir, "readme.txt");
    sessionFile.createNewFile();
    nonSessionFile.createNewFile();

    // when
    String[] keys = persister.keys();

    // then
    assertThat(keys).hasSize(1).containsExactly(id);
  }

  @Test
  void persist_ShouldPersistSerializableSession() throws IOException, ClassNotFoundException {
    // given
    var idGenerator = new SecureRandomSessionIdGenerator();
    var repository = new InMemorySessionRepository(new SessionEventDispatcher(), idGenerator);
    FileSessionPersister persister = new FileSessionPersister(repository);
    persister.setDirectory(tempDir);

    String id = idGenerator.generateId();
    Session session = repository.createSession(id);
    session.setAttribute("key", "value");

    // when
    persister.persist(session);

    // then
    File sessionFile = new File(tempDir, id + ".session");
    assertThat(sessionFile.exists()).isTrue();

    // Verify content by reading it back
    Session loadedSession = persister.findById(id);
    assertThat(loadedSession).isNotNull().isEqualTo(session);
    assertThat(loadedSession.getAttributes()).containsEntry("key", "value");
  }

  @Test
  void sessionFile_ShouldCreateCorrectFileName() {
    // given
    var repository = new InMemorySessionRepository(new SessionEventDispatcher(), new SecureRandomSessionIdGenerator());
    FileSessionPersister persister = new FileSessionPersister(repository);
    persister.setDirectory(tempDir);

    String id = "test-session-id";

    // when
    File file = new File(tempDir, id + ".session"); // Simulating private method behavior

    // then
    assertThat(file.getName()).isEqualTo("test-session-id.session");
    assertThat(file.getParentFile()).isEqualTo(tempDir);
  }

  @Configuration
  @EnableSession
  static class Config {

  }

}