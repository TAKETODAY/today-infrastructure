/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.session;

import org.redisson.api.BatchOptions;
import org.redisson.api.BatchResult;
import org.redisson.api.RBatch;
import org.redisson.api.RMap;
import org.redisson.api.RPatternTopic;
import org.redisson.api.RSet;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.PatternMessageListener;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.CompositeCodec;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import cn.taketoday.core.AttributeAccessor;
import cn.taketoday.core.Conventions;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY
 * @since 2019-09-28 10:31
 */
public class RedissonSessionRepository implements SessionRepository, PatternMessageListener<String> {

  /**
   * A session index that contains the current principal name (i.e. username).
   * <p>
   * It is the responsibility of the developer to ensure the index is populated since
   * Spring Session is not aware of the authentication mechanism being used.
   */
  private static final String PRINCIPAL_NAME_INDEX_NAME = Conventions.getQualifiedAttributeName(
          RedissonSessionRepository.class, "PRINCIPAL_NAME_INDEX_NAME");

  static final String SESSION_ATTR_PREFIX = "session-attr:";

  private static final String SECURITY_CONTEXT = "SECURITY_CONTEXT";

  private final RedissonClient redisson;
  private final RPatternTopic deletedTopic;
  private final RPatternTopic expiredTopic;
  private final RPatternTopic createdTopic;

  private String keyPrefix = "today:session:";

  @Nullable
  private Duration defaultMaxInactiveInterval;

  private final SessionEventDispatcher sessionEventDispatcher;
  private final SessionIdGenerator idGenerator;

  public RedissonSessionRepository(RedissonClient redissonClient) {
    this(redissonClient, null, null, null);
  }

  public RedissonSessionRepository(RedissonClient redisson,
          @Nullable String keyPrefix,
          @Nullable SessionIdGenerator idGenerator,
          @Nullable SessionEventDispatcher eventDispatcher) {

    if (StringUtils.hasText(keyPrefix)) {
      this.keyPrefix = keyPrefix;
    }
    this.redisson = redisson;
    this.deletedTopic = redisson.getPatternTopic("__keyevent@*:del", StringCodec.INSTANCE);
    this.expiredTopic = redisson.getPatternTopic("__keyevent@*:expired", StringCodec.INSTANCE);
    this.createdTopic = redisson.getPatternTopic(getEventsChannelPrefix() + "*", StringCodec.INSTANCE);

    // add listeners after all topics are created to avoid race and potential NPE if we get messages right away
    deletedTopic.addListener(String.class, this);
    expiredTopic.addListener(String.class, this);
    createdTopic.addListener(String.class, this);

    if (idGenerator == null) {
      idGenerator = new SecureRandomSessionIdGenerator();
    }

    if (eventDispatcher == null) {
      eventDispatcher = new SessionEventDispatcher();
    }

    this.idGenerator = idGenerator;
    this.sessionEventDispatcher = eventDispatcher;
  }

  @Nullable
  private MapSession loadSession(String sessionId) {
    RMap<String, Object> map = redisson.getMap(keyPrefix + sessionId,
            new CompositeCodec(StringCodec.INSTANCE, redisson.getConfig().getCodec()));

    Set<Map.Entry<String, Object>> entrySet = map.readAllEntrySet();
    if (entrySet.isEmpty()) {
      return null;
    }

    MapSession delegate = new MapSession(sessionId);
    for (Map.Entry<String, Object> entry : entrySet) {
      String key = entry.getKey();
      if ("session:creationTime".equals(key)) {
        delegate.setCreationTime(Instant.ofEpochMilli((Long) entry.getValue()));
      }
      else if ("session:lastAccessedTime".equals(key)) {
        delegate.setLastAccessTime(Instant.ofEpochMilli((Long) entry.getValue()));
      }
      else if ("session:setMaxIdleTime".equals(key)) {
        delegate.setMaxIdleTime(Duration.ofSeconds((Long) entry.getValue()));
      }
      else if (key.startsWith(SESSION_ATTR_PREFIX)) {
        delegate.setAttribute(key.substring(SESSION_ATTR_PREFIX.length()), entry.getValue());
      }
    }
    return delegate;
  }

  @Override
  public void onMessage(CharSequence pattern, CharSequence channel, String body) {
    String patternString = pattern.toString();
    if (createdTopic.getPatternNames().contains(patternString)) {
      RedissonSession session = retrieveSession(body);
      if (session != null) {
        sessionEventDispatcher.onSessionCreated(session);
      }
    }
    else if (expiredTopic.getPatternNames().contains(patternString)
            || deletedTopic.getPatternNames().contains(patternString)) {
      String expiredKeyPrefix = getExpiredKeyPrefix();
      if (body.startsWith(expiredKeyPrefix)) {
        String id = body.substring(expiredKeyPrefix.length());
        MapSession mapSession = loadSession(id);
        if (mapSession != null) {
          RedissonSession session = new RedissonSession(mapSession);
          session.clearPrincipal();
          sessionEventDispatcher.onSessionDestroyed(session);
        }
      }
    }
  }

  public void setDefaultMaxInactiveInterval(@Nullable Duration defaultMaxInactiveInterval) {
    this.defaultMaxInactiveInterval = defaultMaxInactiveInterval;
  }

  @Override
  public RedissonSession createSession() {
    RedissonSession session = new RedissonSession();
    if (defaultMaxInactiveInterval != null) {
      session.setMaxIdleTime(defaultMaxInactiveInterval);
    }
    return session;
  }

  @Nullable
  @Override
  public RedissonSession retrieveSession(String sessionId) {
    MapSession mapSession = loadSession(sessionId);
    if (mapSession == null || mapSession.isExpired()) {
      return null;
    }
    return new RedissonSession(mapSession);
  }

  @Nullable
  @Override
  public WebSession removeSession(String sessionId) {
    RedissonSession session = retrieveSession(sessionId);
    if (session == null) {
      return null;
    }

    redisson.getBucket(getExpiredKey(sessionId)).delete();

    session.clearPrincipal();
    session.setMaxIdleTime(Duration.ZERO);
    return session;
  }

  @Override
  public void updateLastAccessTime(WebSession webSession) {
    webSession.setLastAccessTime(Instant.now());
  }

  @Override
  public boolean contains(String sessionId) {
    return retrieveSession(sessionId) != null;
  }

  @Override
  public int getSessionCount() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String[] getIdentifiers() {
    throw new UnsupportedOperationException();
  }

  public void setKeyPrefix(String keyPrefix) {
    Assert.notNull(keyPrefix, "keyPrefix is required");
    this.keyPrefix = keyPrefix;
  }

  @Nullable
  String resolvePrincipal(WebSession session) {
    Object attribute = session.getAttribute(PRINCIPAL_NAME_INDEX_NAME);
    if (attribute instanceof String principalName) {
      return principalName;
    }
    return null;
  }

  String getEventsChannelName(String sessionId) {
    return getEventsChannelPrefix() + sessionId;
  }

  String getExpiredKey(String sessionId) {
    return getExpiredKeyPrefix() + sessionId;
  }

  String getExpiredKeyPrefix() {
    return keyPrefix + "sessions:expires:";
  }

  String getEventsChannelPrefix() {
    return keyPrefix + "created:event:";
  }

  String getPrincipalKey(String principalName) {
    return keyPrefix + "index:" + PRINCIPAL_NAME_INDEX_NAME + ":" + principalName;
  }

  String getSessionAttrNameKey(String name) {
    return SESSION_ATTR_PREFIX + name;
  }

  private RSet<String> getPrincipalSet(String indexValue) {
    String principalKey = getPrincipalKey(indexValue);
    return redisson.getSet(principalKey, StringCodec.INSTANCE);
  }

  final class RedissonSession implements WebSession {

    @Nullable
    private String principalName;

    private final MapSession delegate;

    @Nullable
    private RMap<String, Object> map;

    RedissonSession() {
      String id = idGenerator.generateId();
      this.delegate = new MapSession(id);
      map = redisson.getMap(keyPrefix + delegate.getId(),
              new CompositeCodec(StringCodec.INSTANCE, redisson.getConfig().getCodec()));

      Map<String, Object> newMap = new HashMap<>(3);
      newMap.put("session:creationTime", delegate.getCreationTime().toEpochMilli());
      newMap.put("session:lastAccessedTime", delegate.getLastAccessTime().toEpochMilli());
      newMap.put("session:setMaxIdleTime", delegate.getMaxIdleTime().getSeconds());
      map.putAll(newMap);

      updateExpiration();

      String channelName = getEventsChannelName(delegate.getId());
      RTopic topic = redisson.getTopic(channelName, StringCodec.INSTANCE);
      topic.publish(delegate.getId());
    }

    private void updateExpiration() {
      if (map != null && delegate.getMaxIdleTime().getSeconds() > 0) {
        redisson.getBucket(getExpiredKey(delegate.getId()))
                .set("", delegate.getMaxIdleTime().getSeconds(), TimeUnit.SECONDS);
        map.expire(delegate.getMaxIdleTime().getSeconds() + 60, TimeUnit.SECONDS);
      }
    }

    RedissonSession(MapSession session) {
      this.delegate = session;
      map = redisson.getMap(keyPrefix + session.getId(), new CompositeCodec(StringCodec.INSTANCE, redisson.getConfig().getCodec()));
      principalName = resolvePrincipal(this);
    }

    @Override
    public String getId() {
      return delegate.getId();
    }

    @Override
    public void setAttribute(String attributeName, @Nullable Object attributeValue) {
      if (attributeValue == null) {
        removeAttribute(attributeName);
      }
      else {
        delegate.setAttribute(attributeName, attributeValue);
        if (map != null) {
          map.fastPut(getSessionAttrNameKey(attributeName), attributeValue);

          if (attributeName.equals(PRINCIPAL_NAME_INDEX_NAME)
                  || attributeName.equals(SECURITY_CONTEXT)) {
            // remove old
            if (principalName != null) {
              RSet<String> set = getPrincipalSet(principalName);
              set.remove(getId());
            }

            principalName = resolvePrincipal(this);
            if (principalName != null) {
              RSet<String> set = getPrincipalSet(principalName);
              set.add(getId());
            }
          }
        }
      }
    }

    public void clearPrincipal() {
      principalName = resolvePrincipal(this);
      if (principalName != null) {
        RSet<String> set = getPrincipalSet(principalName);
        set.remove(getId());
      }
    }

    @Override
    public Object removeAttribute(String attributeName) {
      Object old = delegate.removeAttribute(attributeName);

      if (map != null) {
        map.fastRemove(getSessionAttrNameKey(attributeName));
      }

      return old;
    }

    @Override
    public Instant getCreationTime() {
      return delegate.getCreationTime();
    }

    public void setLastAccessTime(Instant lastAccessTime) {
      delegate.setLastAccessTime(lastAccessTime);

      if (map != null) {
        map.fastPut("session:lastAccessedTime", lastAccessTime.toEpochMilli());
        updateExpiration();
      }
    }

    @Override
    public Instant getLastAccessTime() {
      return delegate.getLastAccessTime();
    }

    @Override
    public void setMaxIdleTime(Duration interval) {
      delegate.setMaxIdleTime(interval);

      if (map != null) {
        map.fastPut("session:setMaxIdleTime", interval.getSeconds());
        updateExpiration();
      }
    }

    @Override
    public Duration getMaxIdleTime() {
      return delegate.getMaxIdleTime();
    }

    @Override
    public boolean isExpired() {
      return delegate.isExpired();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void changeSessionId() {
      String oldId = delegate.getId();
      delegate.changeSessionId();

      String id = delegate.getId();

      RBatch batch = redisson.createBatch(BatchOptions.defaults());
      batch.getBucket(getExpiredKey(oldId)).remainTimeToLiveAsync();
      batch.getBucket(getExpiredKey(oldId)).deleteAsync();

      Assert.state(map != null, "map is null");
      batch.getMap(map.getName(), map.getCodec()).readAllMapAsync();
      batch.getMap(map.getName()).deleteAsync();

      BatchResult<?> res = batch.execute();
      List<?> list = res.getResponses();

      Long remainTTL = (Long) list.get(0);
      Map<String, Object> oldState = (Map<String, Object>) list.get(2);

      if (remainTTL == -2) {
        // Either:
        // - a parallel request also invoked changeSessionId() on this session, and the
        //   expiredKey for oldId had been deleted
        // - sessions do not expire
        remainTTL = delegate.getMaxIdleTime().toMillis();
      }

      RBatch batchNew = redisson.createBatch();
      batchNew.getMap(keyPrefix + id, map.getCodec()).putAllAsync(oldState);
      if (remainTTL > 0) {
        batchNew.getBucket(getExpiredKey(id)).setAsync("", remainTTL, TimeUnit.MILLISECONDS);
      }
      batchNew.execute();

      map = redisson.getMap(keyPrefix + id, map.getCodec());
    }

    @Nullable
    @Override
    public Object getAttribute(String name) {
      return delegate.getAttribute(name);
    }

    @Override
    public Iterator<String> attributeNames() {
      return delegate.attributeNames();
    }

    @Override
    public boolean hasAttribute(String name) {
      return false;
    }

    @Override
    public String[] getAttributeNames() {
      return delegate.getAttributeNames();
    }

    @Override
    public boolean hasAttributes() {
      return delegate.hasAttributes();
    }

    @Override
    public Map<String, Object> getAttributes() {
      return delegate.getAttributes();
    }

    @Override
    public void copyAttributesFrom(AttributeAccessor source) {
      delegate.copyAttributesFrom(source);
    }

    @Override
    public void clearAttributes() {
      delegate.clearAttributes();
    }

    @Override
    public void save() {
      delegate.save();
    }

    @Override
    public void invalidate() {
      delegate.invalidate();
    }

    @Override
    public void start() {
      delegate.start();
    }

    @Override
    public boolean isStarted() {
      return delegate.isStarted();
    }

  }

}
