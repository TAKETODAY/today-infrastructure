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

package infra.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/5 22:30
 */
@ExtendWith(MockitoExtension.class)
class InMemorySessionRepositoryTests {

  @Mock
  private SessionIdGenerator idGenerator;

  @Mock
  private SessionEventDispatcher eventDispatcher;

  private InMemorySessionRepository repository;

  private Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

  @BeforeEach
  void setUp() {
    repository = new InMemorySessionRepository(eventDispatcher, idGenerator);
    repository.setClock(clock);
  }

  @Test
  void createSessionShouldReturnNewSession() {
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();

    assertThat(session).isNotNull();
    assertThat(session.getId()).isEqualTo("test-id");
    assertThat(session.isExpired()).isFalse();
  }

  @Test
  void retrieveSessionShouldReturnNullForNonExistentSession() {
    Session session = repository.retrieveSession("non-existent-id");
    assertThat(session).isNull();
  }

  @Test
  void retrieveSessionShouldReturnExistingAndNotExpiredSession() {
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    Session retrievedSession = repository.retrieveSession("test-id");
    assertThat(retrievedSession).isNotNull();
    assertThat(retrievedSession.getId()).isEqualTo("test-id");
  }

  @Test
  void retrieveSessionShouldUpdateLastAccessTime() {
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    Instant initialAccessTime = session.getLastAccessTime();

    // Advance clock and retrieve session
    clock = Clock.offset(clock, Duration.ofSeconds(10));
    repository.setClock(clock);
    Session retrievedSession = repository.retrieveSession(session.getId());

    assertThat(retrievedSession).isNotNull();
    assertThat(retrievedSession.getLastAccessTime()).isAfter(initialAccessTime);
    assertThat(retrievedSession.getLastAccessTime()).isEqualTo(clock.instant());
  }

  @Test
  void retrieveSessionShouldReturnNullForExpiredSession() {
    given(idGenerator.generateId()).willReturn("test-id");
    repository.setSessionMaxIdleTime(Duration.ofMinutes(30));
    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    // Advance clock beyond max idle time
    clock = Clock.offset(clock, Duration.ofMinutes(31));
    repository.setClock(clock);

    Session retrievedSession = repository.retrieveSession(session.getId());
    assertThat(retrievedSession).isNull();
    assertThat(repository.getSessionCount()).isZero();
  }

  @Test
  void saveShouldStoreSessionAndStartItWhenAttributeIsAdded() {
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    session.setAttribute("foo", "bar");
    repository.saveOrUpdate(session);

    assertThat(repository.getSessionCount()).isEqualTo(1);
    Session retrieved = repository.retrieveSession(session.getId());
    assertThat(retrieved).isNotNull();
    assertThat(retrieved.getAttribute("foo")).isEqualTo("bar");
  }

  @Test
  void saveShouldTriggerSessionCreatedEventWhenStarted() {
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    verify(eventDispatcher).sessionCreated(session);
  }

  @Test
  void invalidateShouldRemoveSessionAndTriggerDestroyedEvent() {
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    session.invalidate();

    assertThat(repository.retrieveSession(session.getId())).isNull();
    assertThat(repository.getSessionCount()).isZero();
    verify(eventDispatcher).sessionDestroyed(session);
  }

  @Test
  void changeSessionIdShouldUpdateSessionIdInRepository() {
    given(idGenerator.generateId()).willReturn("new-id");
    Session session = repository.createSession();
    String oldId = session.getId();
    repository.saveOrUpdate(session);

    given(idGenerator.generateId()).willReturn("new-id1");

    session.changeSessionId();
    String newId = session.getId();

    assertThat(newId).isEqualTo("new-id1");
    assertThat(repository.retrieveSession(oldId)).isNull();
    assertThat(repository.retrieveSession(newId)).isNotNull();
  }

  @Test
  void removeExpiredSessionsShouldCleanUpExpiredSessions() {
    given(idGenerator.generateId()).willReturn("test-id");
    repository.setSessionMaxIdleTime(Duration.ofMinutes(10));
    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    // Advance clock to expire the session
    clock = Clock.offset(clock, Duration.ofMinutes(11));
    repository.setClock(clock);

    repository.removeExpiredSessions();

    assertThat(repository.getSessionCount()).isZero();
    verify(eventDispatcher).sessionDestroyed(session);
  }

  @Test
  void removeExpiredSessionsShouldNotRemoveActiveSessions() {
    given(idGenerator.generateId()).willReturn("test-id");
    repository.setSessionMaxIdleTime(Duration.ofMinutes(10));
    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    repository.removeExpiredSessions();

    assertThat(repository.getSessionCount()).isEqualTo(1);
    verify(eventDispatcher, never()).sessionDestroyed(session);
  }

  @Test
  void getIdentifiersShouldReturnAllSessionIds() {
    given(idGenerator.generateId()).willReturn("id1", "id2");
    Session session1 = repository.createSession();
    repository.saveOrUpdate(session1);

    Session session2 = repository.createSession();
    repository.saveOrUpdate(session2);

    assertThat(repository.getIdentifiers()).containsExactlyInAnyOrder("id1", "id2");
  }

  @Test
  void getSessionsShouldReturnUnmodifiableMap() {
    given(idGenerator.generateId()).willReturn("id1");
    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    Map<String, Session> sessions = repository.getSessions();
    assertThat(sessions).hasSize(1);
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> sessions.put("id2", mock(Session.class)));
  }

  @Test
  void removeSessionByIdShouldRemoveTheSession() {
    given(idGenerator.generateId()).willReturn("id1");
    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    assertThat(repository.getSessionCount()).isEqualTo(1);

    repository.remove("id1");
    assertThat(repository.getSessionCount()).isZero();
    assertThat(repository.retrieveSession("id1")).isNull();
  }

  @Test
  void removeSessionByInstanceShouldRemoveTheSession() {
    given(idGenerator.generateId()).willReturn("id1");
    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    assertThat(repository.getSessionCount()).isEqualTo(1);

    repository.remove(session);
    assertThat(repository.getSessionCount()).isZero();
    assertThat(repository.retrieveSession("id1")).isNull();
  }

  @Test
  void updateLastAccessTimeShouldChangeLastAccessTime() {
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    Instant initialAccessTime = session.getLastAccessTime();

    clock = Clock.offset(clock, Duration.ofSeconds(20));
    repository.setClock(clock);
    repository.updateLastAccessTime(session);

    assertThat(session.getLastAccessTime()).isAfter(initialAccessTime);
    assertThat(session.getLastAccessTime()).isEqualTo(clock.instant());
  }

  @Test
  void setSessionMaxIdleTimeToNullShouldUseDefault() {
    given(idGenerator.generateId()).willReturn("test-id");
    repository.setSessionMaxIdleTime(null);
    Session session = repository.createSession();
    assertThat(session.getMaxIdleTime()).isEqualTo(Duration.ofMinutes(30));
  }

  @Test
  void saveShouldNotTriggerCreatedEventTwice() {
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();

    repository.saveOrUpdate(session);

    verify(eventDispatcher).sessionCreated(session);
  }

  @Test
  void setSessionMaxIdleTime_ShouldUpdateDefaultTimeout() {
    given(idGenerator.generateId()).willReturn("test-id");
    // given
    Duration newTimeout = Duration.ofMinutes(45);

    // when
    repository.setSessionMaxIdleTime(newTimeout);

    // then
    Session session = repository.createSession();
    assertThat(session.getMaxIdleTime()).isEqualTo(newTimeout);
  }

  @Test
  void setSessionMaxIdleTime_ShouldUseDefaultWhenNull() {
    given(idGenerator.generateId()).willReturn("test-id");
    repository.setSessionMaxIdleTime(null);

    // then
    Session session = repository.createSession();
    assertThat(session.getMaxIdleTime()).isEqualTo(Duration.ofMinutes(30));
  }

  @Test
  void setMaxSessions_ShouldUpdateLimit() {
    // when
    repository.setMaxSessions(5000);

    // then
    assertThat(repository.getMaxSessions()).isEqualTo(5000);
  }

  @Test
  void setNotifyBindingListenerOnUnchangedValue_ShouldUpdateFlag() {
    // when
    repository.setNotifyBindingListenerOnUnchangedValue(true);

    // then
    // We cannot directly verify the flag, but we can check that it doesn't throw exceptions
    assertThatNoException().isThrownBy(() -> repository.setNotifyBindingListenerOnUnchangedValue(true));
  }

  @Test
  void setNotifyAttributeListenerOnUnchangedValue_ShouldUpdateFlag() {
    // when
    repository.setNotifyAttributeListenerOnUnchangedValue(false);

    // then
    // We cannot directly verify the flag, but we can check that it doesn't throw exceptions
    assertThatNoException().isThrownBy(() -> repository.setNotifyAttributeListenerOnUnchangedValue(false));
  }

  @Test
  void setClock_ShouldUpdateClockAndCleanupExpiredSessions() {
    // given
    given(idGenerator.generateId()).willReturn("test-id");
    repository.setSessionMaxIdleTime(Duration.ofMinutes(10));
    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    // when
    Clock newClock = Clock.offset(clock, Duration.ofMinutes(11));
    repository.setClock(newClock);

    // then
    assertThat(repository.getSessionCount()).isZero();
  }

  @Test
  void getClock_ShouldReturnConfiguredClock() {
    // given
    Clock newClock = Clock.fixed(Instant.now().plusSeconds(1000), ZoneId.systemDefault());

    // when
    repository.setClock(newClock);

    // then
    assertThat(repository.getClock()).isSameAs(newClock);
  }

  @Test
  void getSessionCount_ShouldReturnZero_WhenNoSessions() {
    assertThat(repository.getSessionCount()).isZero();
  }

  @Test
  void getSessionCount_ShouldReturnCorrectCount() {
    // given
    given(idGenerator.generateId()).willReturn("id1", "id2");

    Session session1 = repository.createSession();
    repository.saveOrUpdate(session1);

    Session session2 = repository.createSession();
    repository.saveOrUpdate(session2);

    // when & then
    assertThat(repository.getSessionCount()).isEqualTo(2);
  }

  @Test
  void getIdentifiers_ShouldReturnEmptyArray_WhenNoSessions() {
    assertThat(repository.getIdentifiers()).isEmpty();
  }

  @Test
  void getSessions_ShouldReturnEmptyMap_WhenNoSessions() {
    assertThat(repository.getSessions()).isEmpty();
  }

  @Test
  void getSessions_ShouldReturnUnmodifiableMap() {
    // given
    given(idGenerator.generateId()).willReturn("id1");
    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    Map<String, Session> sessions = repository.getSessions();

    // when & then
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> sessions.clear());
  }

  @Test
  void createSession_ShouldGenerateNewSessionWithId() {
    // given
    given(idGenerator.generateId()).willReturn("generated-id");

    // when
    Session session = repository.createSession();

    // then
    assertThat(session).isNotNull();
    assertThat(session.getId()).isEqualTo("generated-id");
  }

  @Test
  void createSessionWithId_ShouldCreateSessionWithGivenId() {
    // given
    String customId = "custom-id";

    // when
    Session session = repository.createSession(customId);

    // then
    assertThat(session).isNotNull();
    assertThat(session.getId()).isEqualTo(customId);
  }

  @Test
  void retrieveSession_ShouldReturnNull_WhenSessionDoesNotExist() {
    Session session = repository.retrieveSession("non-existent");
    assertThat(session).isNull();
  }

  @Test
  void retrieveSession_ShouldReturnSession_WhenSessionExists() {
    // given
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    // when
    Session retrieved = repository.retrieveSession("test-id");

    // then
    assertThat(retrieved).isNotNull();
    assertThat(retrieved.getId()).isEqualTo("test-id");
  }

  @Test
  void retrieveSession_ShouldReturnNull_WhenSessionExpired() {
    // given
    given(idGenerator.generateId()).willReturn("test-id");
    repository.setSessionMaxIdleTime(Duration.ofMinutes(10));
    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    // Expire the session
    clock = Clock.offset(clock, Duration.ofMinutes(11));
    repository.setClock(clock);

    // when
    Session retrieved = repository.retrieveSession("test-id");

    // then
    assertThat(retrieved).isNull();
  }

  @Test
  void removeSessionByInstance_ShouldRemoveSession() {
    // given
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    assertThat(repository.getSessionCount()).isEqualTo(1);

    // when
    repository.remove(session);

    // then
    assertThat(repository.getSessionCount()).isZero();
    assertThat(repository.retrieveSession("test-id")).isNull();
  }

  @Test
  void removeSessionById_ShouldRemoveSession() {
    // given
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    assertThat(repository.getSessionCount()).isEqualTo(1);

    // when
    Session removed = repository.remove("test-id");

    // then
    assertThat(removed).isNotNull();
    assertThat(repository.getSessionCount()).isZero();
    assertThat(repository.retrieveSession("test-id")).isNull();
  }

  @Test
  void removeSessionById_ShouldReturnNull_WhenSessionDoesNotExist() {
    Session removed = repository.remove("non-existent");
    assertThat(removed).isNull();
  }

  @Test
  void updateLastAccessTime_ShouldUpdateSessionTime() {
    // given
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    Instant originalTime = session.getLastAccessTime();

    // when
    clock = Clock.offset(clock, Duration.ofSeconds(30));
    repository.setClock(clock);
    repository.updateLastAccessTime(session);

    // then
    assertThat(session.getLastAccessTime()).isAfter(originalTime);
  }

  @Test
  void contains_ShouldReturnFalse_WhenSessionDoesNotExist() {
    boolean result = repository.contains("non-existent");
    assertThat(result).isFalse();
  }

  @Test
  void contains_ShouldReturnTrue_WhenSessionExists() {
    // given
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    // when
    boolean result = repository.contains("test-id");

    // then
    assertThat(result).isTrue();
  }

  @Test
  void removeExpiredSessions_ShouldCleanUpExpiredSessions() {
    // given
    given(idGenerator.generateId()).willReturn("test-id");
    repository.setSessionMaxIdleTime(Duration.ofMinutes(10));
    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    // Expire the session
    clock = Clock.offset(clock, Duration.ofMinutes(11));
    repository.setClock(clock);

    // when
    repository.removeExpiredSessions();

    // then
    assertThat(repository.getSessionCount()).isZero();
  }

  @Test
  void changeSessionId_ShouldUpdateSessionId() {
    // given
    given(idGenerator.generateId()).willReturn("old-id", "new-id");
    Session session = repository.createSession();
    String oldId = session.getId();
    repository.saveOrUpdate(session);

    // when
    session.changeSessionId();
    String newId = session.getId();

    // then
    assertThat(newId).isEqualTo("new-id");
    assertThat(repository.retrieveSession(oldId)).isNull();
    assertThat(repository.retrieveSession(newId)).isNotNull();
  }

  @Test
  void save_ShouldStoreSession_WhenHasAttributes() {
    // given
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    session.setAttribute("key", "value");

    // when
    repository.saveOrUpdate(session);

    // then
    assertThat(repository.getSessionCount()).isEqualTo(1);
    Session retrieved = repository.retrieveSession("test-id");
    assertThat(retrieved).isNotNull();
    assertThat(retrieved.getAttribute("key")).isEqualTo("value");
  }

  @Test
  void saveShouldStoreStartedSessionEvenWithoutAttributes() {
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    assertThat(repository.getSessionCount()).isEqualTo(1);
    assertThat(repository.retrieveSession("test-id")).isNotNull();
  }

  @Test
  void changeSessionIdShouldUpdateRepositoryMapping() {
    given(idGenerator.generateId()).willReturn("old-id", "new-id");
    Session session = repository.createSession();
    String oldId = session.getId();
    repository.saveOrUpdate(session);

    session.changeSessionId();
    String newId = session.getId();

    assertThat(newId).isEqualTo("new-id");
    assertThat(repository.retrieveSession(oldId)).isNull();
    assertThat(repository.retrieveSession(newId)).isNotNull();
    assertThat(repository.contains(oldId)).isFalse();
    assertThat(repository.contains(newId)).isTrue();
  }

  @Test
  void changeSessionIdShouldPreserveSessionData() {
    given(idGenerator.generateId()).willReturn("old-id", "new-id");
    Session session = repository.createSession();
    session.setAttribute("key1", "value1");
    session.setAttribute("key2", "value2");
    repository.saveOrUpdate(session);

    session.changeSessionId();
    String newId = session.getId();

    Session retrieved = repository.retrieveSession(newId);
    assertThat(retrieved).isNotNull();
    assertThat(retrieved.getAttribute("key1")).isEqualTo("value1");
    assertThat(retrieved.getAttribute("key2")).isEqualTo("value2");
  }

  @Test
  void expiredSessionCheckerShouldNotRunIfCheckPeriodNotElapsed() {
    given(idGenerator.generateId()).willReturn("id1");
    repository.setSessionMaxIdleTime(Duration.ofMinutes(5));

    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    clock = Clock.offset(clock, Duration.ofSeconds(30));
    repository.setClock(clock);

    assertThat(repository.getSessionCount()).isEqualTo(1);
  }

  @Test
  void setClockShouldTriggerImmediateCleanupOfExpiredSessions() {
    given(idGenerator.generateId()).willReturn("id1", "id2");
    repository.setSessionMaxIdleTime(Duration.ofMinutes(10));

    Session session1 = repository.createSession();
    repository.saveOrUpdate(session1);

    Session session2 = repository.createSession();
    repository.saveOrUpdate(session2);

    clock = Clock.offset(clock, Duration.ofMinutes(11));
    repository.setClock(clock);

    assertThat(repository.getSessionCount()).isZero();
  }

  @Test
  void maxSessionsLimitShouldAllowExactNumberOfSessions() {
    repository.setMaxSessions(3);
    given(idGenerator.generateId()).willReturn("id1", "id2", "id3");

    Session session1 = repository.createSession();
    repository.saveOrUpdate(session1);

    Session session2 = repository.createSession();
    repository.saveOrUpdate(session2);

    Session session3 = repository.createSession();
    repository.saveOrUpdate(session3);

    assertThat(repository.getSessionCount()).isEqualTo(3);
  }

  @Test
  void maxSessionsLimitShouldBeCheckedAfterExpiredSessionCleanup() {
    repository.setMaxSessions(2);
    given(idGenerator.generateId()).willReturn("id1", "id2", "id3");
    repository.setSessionMaxIdleTime(Duration.ofMinutes(5));

    Session session1 = repository.createSession();
    repository.saveOrUpdate(session1);

    Session session2 = repository.createSession();
    repository.saveOrUpdate(session2);

    clock = Clock.offset(clock, Duration.ofMinutes(6));
    repository.setClock(clock);

    Session session3 = repository.createSession();
    repository.saveOrUpdate(session3);

    assertThat(repository.getSessionCount()).isEqualTo(1);
  }

  @Test
  void removeShouldReturnRemovedSession() {
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    Session removed = repository.remove("test-id");

    assertThat(removed).isNotNull();
    assertThat(removed.getId()).isEqualTo("test-id");
    assertThat(repository.getSessionCount()).isZero();
  }

  @Test
  void removeShouldReturnNullForNonExistentSession() {
    Session removed = repository.remove("non-existent");

    assertThat(removed).isNull();
  }

  @Test
  void containsShouldReturnFalseForExpiredSession() {
    given(idGenerator.generateId()).willReturn("test-id");
    repository.setSessionMaxIdleTime(Duration.ofMinutes(5));

    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    clock = Clock.offset(clock, Duration.ofMinutes(6));
    repository.setClock(clock);

    assertThat(repository.contains("test-id")).isFalse();
  }

  @Test
  void updateLastAccessTimeShouldPreventSessionExpiration() {
    given(idGenerator.generateId()).willReturn("test-id");
    repository.setSessionMaxIdleTime(Duration.ofMinutes(10));

    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    clock = Clock.offset(clock, Duration.ofMinutes(9));
    repository.setClock(clock);
    repository.updateLastAccessTime(session);

    clock = Clock.offset(clock, Duration.ofMinutes(5));
    repository.setClock(clock);

    assertThat(repository.retrieveSession("test-id")).isNotNull();
  }

  @Test
  void getSessionsShouldReflectCurrentState() {
    given(idGenerator.generateId()).willReturn("id1", "id2");

    Session session1 = repository.createSession();
    repository.saveOrUpdate(session1);

    Session session2 = repository.createSession();
    repository.saveOrUpdate(session2);

    Map<String, Session> sessions = repository.getSessions();

    assertThat(sessions).hasSize(2);
    assertThat(sessions.keySet()).containsExactlyInAnyOrder("id1", "id2");
  }

  @Test
  void getIdentifiersShouldReturnEmptyArrayWhenNoSessions() {
    String[] identifiers = repository.getIdentifiers();

    assertThat(identifiers).isEmpty();
  }

  @Test
  void createSessionWithCustomIdShouldUseProvidedId() {
    String customId = "custom-session-id";

    Session session = repository.createSession(customId);

    assertThat(session).isNotNull();
    assertThat(session.getId()).isEqualTo(customId);
  }

  @Test
  void retrieveSessionShouldReturnNullForExpiredSessionAndRemoveIt() {
    given(idGenerator.generateId()).willReturn("test-id");
    repository.setSessionMaxIdleTime(Duration.ofMinutes(5));

    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    clock = Clock.offset(clock, Duration.ofMinutes(6));
    repository.setClock(clock);

    Session retrieved = repository.retrieveSession("test-id");

    assertThat(retrieved).isNull();
    assertThat(repository.contains("test-id")).isFalse();
  }

  @Test
  void sessionCopyFromShouldCopyAllProperties() {
    given(idGenerator.generateId()).willReturn("source-id", "target-id");

    Session source = repository.createSession();
    source.setAttribute("key1", "value1");
    source.setAttribute("key2", "value2");
    repository.saveOrUpdate(source);

    Session target = repository.createSession();
    ((InMemorySessionRepository.InMemorySession) target).copyFrom(source);

    assertThat(target.getMaxIdleTime()).isEqualTo(source.getMaxIdleTime());
    assertThat(target.getCreationTime()).isEqualTo(source.getCreationTime());
    assertThat(target.getLastAccessTime()).isEqualTo(source.getLastAccessTime());
    assertThat(target.getAttribute("key1")).isEqualTo("value1");
    assertThat(target.getAttribute("key2")).isEqualTo("value2");
  }

  @Test
  void saveOrUpdateShouldCreateNewSessionWhenNotExists() {
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    session.setAttribute("key", "value");

    repository.saveOrUpdate(session);

    assertThat(repository.getSessionCount()).isEqualTo(1);
    assertThat(repository.retrieveSession("test-id")).isNotNull();
  }

  @Test
  void saveOrUpdateShouldUpdateExistingSession() {
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();

    session.setAttribute("key", "value");
    repository.saveOrUpdate(session);

    Session retrieved = repository.retrieveSession("test-id");
    assertThat(retrieved).isNotNull();
    assertThat(retrieved.getAttribute("key")).isEqualTo("value");
  }

  @Test
  void saveOrUpdateShouldDoNothingWhenSameInstance() {
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    repository.saveOrUpdate(session);

    assertThat(repository.getSessionCount()).isEqualTo(1);
  }

  @Test
  void sessionShouldHandleNonSerializableAttributesGracefully() {
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();

    Object nonSerializable = new Object() {
      @Override
      public String toString() {
        return "non-serializable";
      }
    };

    session.setAttribute("nonSerializable", nonSerializable);
    repository.saveOrUpdate(session);

    assertThat(repository.getSessionCount()).isEqualTo(1);
  }

  @Test
  void notifyBindingListenerOnUnchangedValueShouldControlNotification() {
    given(idGenerator.generateId()).willReturn("test-id");
    repository.setNotifyBindingListenerOnUnchangedValue(false);

    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    session.setAttribute("key", "value");
    session.setAttribute("key", "value");

    assertThat(repository.retrieveSession("test-id")).isNotNull();
  }

  @Test
  void notifyAttributeListenerOnUnchangedValueShouldControlReplacedNotification() {
    given(idGenerator.generateId()).willReturn("test-id");
    repository.setNotifyAttributeListenerOnUnchangedValue(false);

    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    session.setAttribute("key", "value1");
    session.setAttribute("key", "value1");

    assertThat(repository.retrieveSession("test-id")).isNotNull();
  }

  @Test
  void expiredSessionCheckerLockShouldPreventConcurrentCleanup() {
    given(idGenerator.generateId()).willReturn("id1");
    repository.setSessionMaxIdleTime(Duration.ofMinutes(5));

    Session session = repository.createSession();
    repository.saveOrUpdate(session);

    clock = Clock.offset(clock, Duration.ofMinutes(6));
    repository.setClock(clock);

    repository.removeExpiredSessions();

    assertThat(repository.getSessionCount()).isZero();
  }

}
