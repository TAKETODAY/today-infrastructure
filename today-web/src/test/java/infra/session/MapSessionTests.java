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

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/5 23:16
 */
@ExtendWith(MockitoExtension.class)
class MapSessionTests {

  private MapSession session;

  @Mock
  private SessionEventDispatcher eventDispatcher;

  @BeforeEach
  void setUp() {
    session = new MapSession();
  }

  // 构造函数测试
  @Test
  void constructor_withoutId_shouldGenerateRandomId() {
    MapSession session = new MapSession();
    assertThat(session.getId()).isNotNull().isNotEmpty();
    assertThat(session.getOriginalId()).isEqualTo(session.getId());
  }

  @Test
  void constructor_withId_shouldUseProvidedId() {
    String customId = "custom-session-id";
    MapSession session = new MapSession(customId);
    assertThat(session.getId()).isEqualTo(customId);
    assertThat(session.getOriginalId()).isEqualTo(customId);
  }

  @Test
  void constructor_withIdAndEventDispatcher_shouldInitializeCorrectly() {
    String customId = "custom-session-id";
    MapSession session = new MapSession(customId, eventDispatcher);
    assertThat(session.getId()).isEqualTo(customId);
    assertThat(session.getOriginalId()).isEqualTo(customId);
  }

  @Test
  void constructor_withWebSession_shouldCopyProperties() {
    String sessionId = "test-session-id";
    MapSession original = new MapSession(sessionId);
    original.setAttribute("key1", "value1");
    original.setMaxIdleTime(Duration.ofHours(1));

    Instant creationTime = Instant.now().minusSeconds(100);
    Instant lastAccessTime = Instant.now().minusSeconds(50);
    original.setCreationTime(creationTime);
    original.setLastAccessTime(lastAccessTime);

    MapSession copy = new MapSession(original);

    assertThat(copy.getId()).isEqualTo(sessionId);
    assertThat(copy.getOriginalId()).isEqualTo(sessionId);
    assertThat(copy.getAttribute("key1")).isEqualTo("value1");
    assertThat(copy.getMaxIdleTime()).isEqualTo(Duration.ofHours(1));
    assertThat(copy.getCreationTime()).isEqualTo(creationTime);
    assertThat(copy.getLastAccessTime()).isEqualTo(lastAccessTime);
  }

  // ID相关测试
  @Test
  void getId_shouldReturnSessionId() {
    String sessionId = session.getId();
    assertThat(sessionId).isNotNull().isNotEmpty();
  }

  @Test
  void getOriginalId_shouldReturnOriginalSessionId() {
    String originalId = session.getOriginalId();
    String currentId = session.getId();
    assertThat(originalId).isEqualTo(currentId);
  }

  @Test
  void changeSessionId_shouldGenerateNewId() {
    String originalId = session.getId();
    session.changeSessionId();
    String newId = session.getId();

    assertThat(newId).isNotNull().isNotEmpty();
    assertThat(newId).isNotEqualTo(originalId);
    assertThat(session.getOriginalId()).isEqualTo(originalId);
  }

  // 时间相关测试
  @Test
  void getCreationTime_shouldReturnInstant() {
    Instant creationTime = session.getCreationTime();
    assertThat(creationTime).isNotNull();
  }

  @Test
  void getLastAccessTime_shouldReturnInstant() {
    Instant lastAccessTime = session.getLastAccessTime();
    assertThat(lastAccessTime).isNotNull();
  }

  @Test
  void setLastAccessTime_shouldUpdateTime() {
    Instant newTime = Instant.now().plusSeconds(100);
    session.setLastAccessTime(newTime);
    assertThat(session.getLastAccessTime()).isEqualTo(newTime);
  }

  @Test
  void setCreationTime_shouldUpdateTime() {
    Instant newTime = Instant.now().plusSeconds(100);
    session.setCreationTime(newTime);
    assertThat(session.getCreationTime()).isEqualTo(newTime);
  }

  // 过期时间测试
  @Test
  void getMaxIdleTime_shouldReturnDefault() {
    Duration maxIdleTime = session.getMaxIdleTime();
    assertThat(maxIdleTime).isEqualTo(Duration.ofSeconds(MapSession.DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS));
  }

  @Test
  void setMaxIdleTime_shouldUpdateTime() {
    Duration newMaxIdleTime = Duration.ofHours(2);
    session.setMaxIdleTime(newMaxIdleTime);
    assertThat(session.getMaxIdleTime()).isEqualTo(newMaxIdleTime);
  }

  @Test
  void isExpired_withNegativeMaxIdleTime_shouldReturnFalse() {
    session.setMaxIdleTime(Duration.ofSeconds(-1)); // Negative means never expire
    assertThat(session.isExpired()).isFalse();
  }

  @Test
  void isExpired_withNotExpiredSession_shouldReturnFalse() {
    session.setLastAccessTime(Instant.now());
    session.setMaxIdleTime(Duration.ofMinutes(30));
    assertThat(session.isExpired()).isFalse();
  }

  @Test
  void isExpired_withExpiredSession_shouldReturnTrue() {
    Instant pastTime = Instant.now().minus(Duration.ofMinutes(31));
    session.setLastAccessTime(pastTime);
    session.setMaxIdleTime(Duration.ofMinutes(30));
    assertThat(session.isExpired()).isTrue();
  }

  @Test
  void isExpired_withExpiredSession_usingSpecificTime_shouldReturnTrue() {
    Instant pastTime = Instant.now().minus(Duration.ofMinutes(31));
    session.setLastAccessTime(pastTime);
    session.setMaxIdleTime(Duration.ofMinutes(30));
    assertThat(session.isExpired(Instant.now())).isTrue();
  }

  // 属性操作测试
  @Test
  void setAttribute_shouldStoreValue() {
    session.setAttribute("key", "value");
    assertThat(session.getAttribute("key")).isEqualTo("value");
  }

  @Test
  void getAttribute_withNonExistentKey_shouldReturnNull() {
    assertThat(session.getAttribute("non-existent")).isNull();
  }

  @Test
  void getAttributeNames_shouldReturnAllKeys() {
    session.setAttribute("key1", "value1");
    session.setAttribute("key2", "value2");

    assertThat(session.getAttributeNames()).containsExactlyInAnyOrder("key1", "key2");
  }

  @Test
  void removeAttribute_shouldRemoveValue() {
    session.setAttribute("key", "value");
    Object removed = session.removeAttribute("key");

    assertThat(removed).isEqualTo("value");
    assertThat(session.getAttribute("key")).isNull();
  }

  @Test
  void removeAttribute_withNonExistentKey_shouldReturnNull() {
    Object removed = session.removeAttribute("non-existent");
    assertThat(removed).isNull();
  }

  @Test
  void hasAttribute_withExistingKey_shouldReturnTrue() {
    session.setAttribute("key", "value");
    assertThat(session.hasAttribute("key")).isTrue();
  }

  @Test
  void hasAttribute_withNonExistentKey_shouldReturnFalse() {
    assertThat(session.hasAttribute("non-existent")).isFalse();
  }

  // 生命周期测试
  @Test
  void start_shouldTriggerSessionCreatedEvent() {
    MapSession sessionWithDispatcher = new MapSession("test-id", eventDispatcher);
    sessionWithDispatcher.start();
    verify(eventDispatcher).onSessionCreated(sessionWithDispatcher);
  }

  @Test
  void isStarted_shouldReturnTrue() {
    assertThat(session.isStarted()).isTrue();
  }

  @Test
  void save_shouldNotThrowException() {
    assertThatCode(() -> session.save()).doesNotThrowAnyException();
  }

  @Test
  void invalidate_shouldClearAttributes() {
    session.setAttribute("key1", "value1");
    session.setAttribute("key2", "value2");

    session.invalidate();

    assertThat(session.getAttribute("key1")).isNull();
    assertThat(session.getAttribute("key2")).isNull();
    assertThat(session.getAttributeNames()).isEmpty();
  }

  // equals 和 hashCode 测试
  @Test
  void equals_withSameSession_shouldReturnTrue() {
    assertThat(session.equals(session)).isTrue();
  }

  @Test
  void equals_withSameId_shouldReturnTrue() {
    String sessionId = session.getId();
    MapSession otherSession = new MapSession(sessionId);
    assertThat(session.equals(otherSession)).isTrue();
  }

  @Test
  void equals_withDifferentId_shouldReturnFalse() {
    MapSession otherSession = new MapSession();
    assertThat(session.equals(otherSession)).isFalse();
  }

  @Test
  void equals_withNull_shouldReturnFalse() {
    assertThat(session.equals(null)).isFalse();
  }

  @Test
  void equals_withDifferentClass_shouldReturnFalse() {
    assertThat(session.equals(new Object())).isFalse();
  }

  @Test
  void hashCode_shouldBeConsistentWithId() {
    String sessionId = session.getId();
    MapSession otherSession = new MapSession(sessionId);
    assertThat(session.hashCode()).isEqualTo(otherSession.hashCode());
  }

  // 静态方法测试
  @Test
  void getEventDispatcher_withAbstractWebSession_shouldReturnItsDispatcher() {
    SessionEventDispatcher dispatcher = new SessionEventDispatcher();
    MapSession sessionWithDispatcher = new MapSession("test-id", dispatcher);

    SessionEventDispatcher returnedDispatcher = MapSession.getEventDispatcher(sessionWithDispatcher);
    assertThat(returnedDispatcher).isSameAs(dispatcher);
  }

  @Test
  void getEventDispatcher_withNull_shouldThrowException() {
    assertThatThrownBy(() -> MapSession.getEventDispatcher(null))
            .isInstanceOf(IllegalArgumentException.class);
  }

  // 构造函数边界测试
  @Test
  void constructor_withNullId_shouldThrowException() {
    assertThatThrownBy(() -> new MapSession((String) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("sessionId is required");
  }

  @Test
  void constructor_withNullIdAndEventDispatcher_shouldThrowException() {
    assertThatThrownBy(() -> new MapSession(null, eventDispatcher))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("sessionId is required");
  }

  // ID相关边界测试
  @Test
  void setId_shouldUpdateId() {
    String newId = "new-session-id";
    String oldId = session.getId();
    session.setId(newId);

    assertThat(session.getId()).isEqualTo(newId);
    assertThat(session.getOriginalId()).isEqualTo(oldId); // originalId should not change
  }

  // 过期时间边界测试
  @Test
  void setMaxIdleTime_withNull_shouldAllow() {
    assertThatCode(() -> session.setMaxIdleTime(null))
            .doesNotThrowAnyException();
    assertThat(session.getMaxIdleTime()).isNull();
  }

  @Test
  void setMaxIdleTime_withZero_shouldExpireImmediately() {
    session.setMaxIdleTime(Duration.ZERO);
    session.setLastAccessTime(Instant.now().minusSeconds(1));
    assertThat(session.isExpired()).isTrue();
  }

  @Test
  void isExpired_withPastTime_shouldReturnCorrectly() {
    Instant now = Instant.now();
    Instant pastAccessTime = now.minus(Duration.ofMinutes(10));
    session.setLastAccessTime(pastAccessTime);
    session.setMaxIdleTime(Duration.ofMinutes(5));

    assertThat(session.isExpired(now)).isTrue();
    assertThat(session.isExpired(now.minus(Duration.ofMinutes(15)))).isFalse();
  }

  @Test
  void largeNumberOfAttributes_shouldHandleCorrectly() {
    int attributeCount = 1000;
    for (int i = 0; i < attributeCount; i++) {
      session.setAttribute("key" + i, "value" + i);
    }

    assertThat(session.getAttributeNames()).hasSize(attributeCount);

    for (int i = 0; i < attributeCount; i += 2) {
      session.removeAttribute("key" + i);
    }

    assertThat(session.getAttributeNames()).hasSize(attributeCount / 2);

    for (int i = 1; i < attributeCount; i += 2) {
      assertThat(session.getAttribute("key" + i)).isEqualTo("value" + i);
    }
  }

  // 属性监听器测试
  @Test
  void attributeBindingListener_shouldBeCalled() {
    WebSessionAttributeListener listener = mock(WebSessionAttributeListener.class);
    session.eventDispatcher.addAttributeListeners(listener);

    String key = "test-key";
    String value = "test-value";
    session.setAttribute(key, value);

    verify(listener).attributeAdded(session, key, value);
  }

  @Test
  void attributeReplaceListener_shouldBeCalled() {
    WebSessionAttributeListener listener = mock(WebSessionAttributeListener.class);
    session.eventDispatcher.addAttributeListeners(listener);

    String key = "test-key";
    String oldValue = "old-value";
    String newValue = "new-value";

    session.setAttribute(key, oldValue);
    session.setAttribute(key, newValue);

    verify(listener).attributeAdded(session, key, oldValue);
    verify(listener).attributeReplaced(session, key, oldValue, newValue);
  }

  @Test
  void attributeRemoveListener_shouldBeCalled() {
    WebSessionAttributeListener listener = mock(WebSessionAttributeListener.class);
    session.eventDispatcher.addAttributeListeners(listener);

    String key = "test-key";
    String value = "test-value";
    session.setAttribute(key, value);
    session.removeAttribute(key);

    verify(listener).attributeAdded(session, key, value);
    verify(listener).attributeRemoved(session, key, value);
  }

  // 复制构造函数边界测试
  @Test
  void copyConstructor_withNullSession_shouldThrowException() {
    assertThatThrownBy(() -> new MapSession((WebSession) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("session is required");
  }

  // 时间操作边界测试
  @Test
  void setTime_withVeryLargeValues_shouldHandle() {
    Instant veryPast = Instant.EPOCH;
    Instant veryFuture = Instant.MAX;

    session.setCreationTime(veryPast);
    session.setLastAccessTime(veryFuture);

    assertThat(session.getCreationTime()).isEqualTo(veryPast);
    assertThat(session.getLastAccessTime()).isEqualTo(veryFuture);
  }

  @Test
  void setSameCreationAndAccessTime_shouldHandle() {
    Instant sameTime = Instant.now();
    session.setCreationTime(sameTime);
    session.setLastAccessTime(sameTime);

    assertThat(session.getCreationTime()).isEqualTo(sameTime);
    assertThat(session.getLastAccessTime()).isEqualTo(sameTime);
  }

  // invalidate方法测试
  @Test
  void invalidate_calledMultipleTimes_shouldHandle() {
    session.setAttribute("key1", "value1");
    session.setAttribute("key2", "value2");

    session.invalidate();
    session.invalidate(); // 第二次调用

    assertThat(session.getAttributeNames()).isEmpty();
  }

  // 空属性集合测试
  @Test
  void emptySession_attributeOperations_shouldWork() {
    assertThat(session.getAttributeNames()).isEmpty();
    assertThat(session.getAttribute("non-existent")).isNull();
    assertThat(session.removeAttribute("non-existent")).isNull();
    assertThat(session.hasAttribute("non-existent")).isFalse();
  }

  // changeSessionId边界测试
  @Test
  void changeSessionId_multipleTimes_shouldGenerateDifferentIds() {
    String originalId = session.getOriginalId();
    String id1 = session.getId();

    session.changeSessionId();
    String id2 = session.getId();

    session.changeSessionId();
    String id3 = session.getId();

    assertThat(id1).isEqualTo(originalId);
    assertThat(id2).isNotEqualTo(id1);
    assertThat(id3).isNotEqualTo(id1).isNotEqualTo(id2);
    assertThat(session.getOriginalId()).isEqualTo(originalId);
  }

  // 默认值测试
  @Test
  void defaultMaxIdleTime_shouldBe30Minutes() {
    assertThat(MapSession.DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS).isEqualTo(1800);
    MapSession newSession = new MapSession();
    assertThat(newSession.getMaxIdleTime()).isEqualTo(Duration.ofMinutes(30));
  }

  // 并发安全相关测试
  @Test
  void constructor_shouldSetOriginalIdCorrectly() {
    String customId = "custom-id";
    MapSession session1 = new MapSession(customId);
    session1.changeSessionId();

    assertThat(session1.getOriginalId()).isEqualTo(customId);
    assertThat(session1.getId()).isNotEqualTo(customId);
  }
}
