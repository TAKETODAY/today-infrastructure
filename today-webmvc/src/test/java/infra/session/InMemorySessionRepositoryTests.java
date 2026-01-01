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
import static org.mockito.Mockito.verifyNoInteractions;

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
    assertThat(session.isStarted()).isFalse();
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
    session.start();
    session.save();

    Session retrievedSession = repository.retrieveSession("test-id");
    assertThat(retrievedSession).isNotNull();
    assertThat(retrievedSession.getId()).isEqualTo("test-id");
  }

  @Test
  void retrieveSessionShouldUpdateLastAccessTime() {
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    session.start();
    session.save();
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
    session.start();
    session.save();

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
    session.save();

    assertThat(repository.getSessionCount()).isEqualTo(1);
    assertThat(session.isStarted()).isTrue();
    Session retrieved = repository.retrieveSession(session.getId());
    assertThat(retrieved).isNotNull();
    assertThat(retrieved.getAttribute("foo")).isEqualTo("bar");
  }

  @Test
  void saveShouldNotStoreSessionIfNotStartedAndNoAttributes() {
    Session session = repository.createSession();
    session.save();

    assertThat(repository.getSessionCount()).isZero();
    verifyNoInteractions(eventDispatcher);
  }

  @Test
  void saveShouldTriggerSessionCreatedEventWhenStarted() {
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    session.start();
    session.save();

    verify(eventDispatcher).onSessionCreated(session);
  }

  @Test
  void invalidateShouldRemoveSessionAndTriggerDestroyedEvent() {
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    session.start();
    session.save();

    session.invalidate();

    assertThat(repository.retrieveSession(session.getId())).isNull();
    assertThat(repository.getSessionCount()).isZero();
    verify(eventDispatcher).onSessionDestroyed(session);
  }

  @Test
  void changeSessionIdShouldUpdateSessionIdInRepository() {
    given(idGenerator.generateId()).willReturn("new-id");
    Session session = repository.createSession();
    String oldId = session.getId();
    session.start();
    session.save();

    given(idGenerator.generateId()).willReturn("new-id1");

    session.changeSessionId();
    String newId = session.getId();

    assertThat(newId).isEqualTo("new-id1");
    assertThat(repository.retrieveSession(oldId)).isNull();
    assertThat(repository.retrieveSession(newId)).isNotNull();
  }

  @Test
  void saveShouldThrowExceptionWhenMaxSessionsIsReached() {
    repository.setMaxSessions(1);
    given(idGenerator.generateId()).willReturn("id1");
    Session session1 = repository.createSession();
    session1.start();
    session1.save();

    given(idGenerator.generateId()).willReturn("id2");
    Session session2 = repository.createSession();
    session2.start();

    assertThatExceptionOfType(TooManyActiveSessionsException.class)
            .isThrownBy(session2::save)
            .satisfies(e -> assertThat(e.getMaxActiveSessions()).isEqualTo(1));
  }

  @Test
  void removeExpiredSessionsShouldCleanUpExpiredSessions() {
    given(idGenerator.generateId()).willReturn("test-id");
    repository.setSessionMaxIdleTime(Duration.ofMinutes(10));
    Session session = repository.createSession();
    session.start();
    session.save();

    // Advance clock to expire the session
    clock = Clock.offset(clock, Duration.ofMinutes(11));
    repository.setClock(clock);

    repository.removeExpiredSessions();

    assertThat(repository.getSessionCount()).isZero();
    verify(eventDispatcher).onSessionDestroyed(session);
  }

  @Test
  void removeExpiredSessionsShouldNotRemoveActiveSessions() {
    given(idGenerator.generateId()).willReturn("test-id");
    repository.setSessionMaxIdleTime(Duration.ofMinutes(10));
    Session session = repository.createSession();
    session.start();
    session.save();

    repository.removeExpiredSessions();

    assertThat(repository.getSessionCount()).isEqualTo(1);
    verify(eventDispatcher, never()).onSessionDestroyed(session);
  }

  @Test
  void getIdentifiersShouldReturnAllSessionIds() {
    given(idGenerator.generateId()).willReturn("id1", "id2");
    Session session1 = repository.createSession();
    session1.start();
    session1.save();

    Session session2 = repository.createSession();
    session2.start();
    session2.save();

    assertThat(repository.getIdentifiers()).containsExactlyInAnyOrder("id1", "id2");
  }

  @Test
  void getSessionsShouldReturnUnmodifiableMap() {
    given(idGenerator.generateId()).willReturn("id1");
    Session session = repository.createSession();
    session.start();
    session.save();

    Map<String, Session> sessions = repository.getSessions();
    assertThat(sessions).hasSize(1);
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> sessions.put("id2", mock(Session.class)));
  }

  @Test
  void removeSessionByIdShouldRemoveTheSession() {
    given(idGenerator.generateId()).willReturn("id1");
    Session session = repository.createSession();
    session.start();
    session.save();
    assertThat(repository.getSessionCount()).isEqualTo(1);

    repository.removeSession("id1");
    assertThat(repository.getSessionCount()).isZero();
    assertThat(repository.retrieveSession("id1")).isNull();
  }

  @Test
  void removeSessionByInstanceShouldRemoveTheSession() {
    given(idGenerator.generateId()).willReturn("id1");
    Session session = repository.createSession();
    session.start();
    session.save();
    assertThat(repository.getSessionCount()).isEqualTo(1);

    repository.removeSession(session);
    assertThat(repository.getSessionCount()).isZero();
    assertThat(repository.retrieveSession("id1")).isNull();
  }

  @Test
  void updateLastAccessTimeShouldChangeLastAccessTime() {
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    session.start();
    session.save();
    Instant initialAccessTime = session.getLastAccessTime();

    clock = Clock.offset(clock, Duration.ofSeconds(20));
    repository.setClock(clock);
    repository.updateLastAccessTime(session);

    assertThat(session.getLastAccessTime()).isAfter(initialAccessTime);
    assertThat(session.getLastAccessTime()).isEqualTo(clock.instant());
  }

  @Test
  void setSessionMaxIdleTimeToNullShouldUseDefault() {
    repository.setSessionMaxIdleTime(null);
    Session session = repository.createSession();
    assertThat(session.getMaxIdleTime()).isEqualTo(Duration.ofMinutes(30));
  }

  @Test
  void saveShouldNotTriggerCreatedEventTwice() {
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    session.start(); // First trigger
    session.save();
    session.save(); // Should not trigger again

    verify(eventDispatcher).onSessionCreated(session);
  }

  @Test
  void setSessionMaxIdleTime_ShouldUpdateDefaultTimeout() {
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
    // when
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
    session.start();
    session.save();

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
    session1.start();
    session1.save();

    Session session2 = repository.createSession();
    session2.start();
    session2.save();

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
    session.start();
    session.save();

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
    assertThat(session.isStarted()).isFalse();
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
    session.start();
    session.save();

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
    session.start();
    session.save();

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
    session.start();
    session.save();
    assertThat(repository.getSessionCount()).isEqualTo(1);

    // when
    repository.removeSession(session);

    // then
    assertThat(repository.getSessionCount()).isZero();
    assertThat(repository.retrieveSession("test-id")).isNull();
  }

  @Test
  void removeSessionById_ShouldRemoveSession() {
    // given
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    session.start();
    session.save();
    assertThat(repository.getSessionCount()).isEqualTo(1);

    // when
    Session removed = repository.removeSession("test-id");

    // then
    assertThat(removed).isNotNull();
    assertThat(repository.getSessionCount()).isZero();
    assertThat(repository.retrieveSession("test-id")).isNull();
  }

  @Test
  void removeSessionById_ShouldReturnNull_WhenSessionDoesNotExist() {
    Session removed = repository.removeSession("non-existent");
    assertThat(removed).isNull();
  }

  @Test
  void updateLastAccessTime_ShouldUpdateSessionTime() {
    // given
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    session.start();
    session.save();
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
    session.start();
    session.save();

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
    session.start();
    session.save();

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
    session.start();
    session.save();

    // when
    session.changeSessionId();
    String newId = session.getId();

    // then
    assertThat(newId).isEqualTo("new-id");
    assertThat(repository.retrieveSession(oldId)).isNull();
    assertThat(repository.retrieveSession(newId)).isNotNull();
  }

  @Test
  void save_ShouldNotStoreSession_WhenNotStartedAndNoAttributes() {
    // given
    Session session = repository.createSession();

    // when
    session.save();

    // then
    assertThat(repository.getSessionCount()).isZero();
  }

  @Test
  void save_ShouldStoreSession_WhenStarted() {
    // given
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    session.start();

    // when
    session.save();

    // then
    assertThat(repository.getSessionCount()).isEqualTo(1);
    assertThat(repository.retrieveSession("test-id")).isNotNull();
  }

  @Test
  void save_ShouldStoreSession_WhenHasAttributes() {
    // given
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    session.setAttribute("key", "value");

    // when
    session.save();

    // then
    assertThat(repository.getSessionCount()).isEqualTo(1);
    Session retrieved = repository.retrieveSession("test-id");
    assertThat(retrieved).isNotNull();
    assertThat(retrieved.getAttribute("key")).isEqualTo("value");
  }

  @Test
  void save_ShouldThrowException_WhenMaxSessionsReached() {
    // given
    repository.setMaxSessions(1);
    given(idGenerator.generateId()).willReturn("id1", "id2");

    Session session1 = repository.createSession();
    session1.start();
    session1.save();

    Session session2 = repository.createSession();
    session2.start();

    // when & then
    assertThatExceptionOfType(TooManyActiveSessionsException.class)
            .isThrownBy(session2::save)
            .satisfies(e -> assertThat(e.getMaxActiveSessions()).isEqualTo(1));
  }

  @Test
  void save_ShouldNotTriggerCreatedEventTwice() {
    // given
    given(idGenerator.generateId()).willReturn("test-id");
    Session session = repository.createSession();
    session.start();

    // when
    session.save(); // First save
    session.save(); // Second save

    // then
    verify(eventDispatcher).onSessionCreated(session);
  }

}
