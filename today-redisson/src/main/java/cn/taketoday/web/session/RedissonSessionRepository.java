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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationEventPublisher;
import cn.taketoday.core.AttributeAccessorSupport;
import cn.taketoday.core.Conventions;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY
 * @since 2019-09-28 10:31
 */
public class RedissonSessionRepository implements SessionRepository, PatternMessageListener<String> {
  private static final Logger log = LoggerFactory.getLogger(RedissonSessionRepository.class);

  /**
   * A session index that contains the current principal name (i.e. username).
   * <p>
   * It is the responsibility of the developer to ensure the index is populated since
   * Spring Session is not aware of the authentication mechanism being used.
   */
  String PRINCIPAL_NAME_INDEX_NAME = Conventions.getQualifiedAttributeName(
          RedissonSessionRepository.class, "PRINCIPAL_NAME_INDEX_NAME");

  static final String SESSION_ATTR_PREFIX = "session-attr:";

  private static final String SECURITY_CONTEXT = "SECURITY_CONTEXT";

  private final RedissonClient redisson;
  private final ApplicationEventPublisher eventPublisher;
  private final RPatternTopic deletedTopic;
  private final RPatternTopic expiredTopic;
  private final RPatternTopic createdTopic;

  private String keyPrefix = "today:session:";
  private Integer defaultMaxInactiveInterval;

  public RedissonSessionRepository(RedissonClient redisson, ApplicationEventPublisher eventPublisher, String keyPrefix) {
    this.redisson = redisson;
    this.eventPublisher = eventPublisher;
    if (StringUtils.hasText(keyPrefix)) {
      this.keyPrefix = keyPrefix;
    }

    deletedTopic = redisson.getPatternTopic("__keyevent@*:del", StringCodec.INSTANCE);
    expiredTopic = redisson.getPatternTopic("__keyevent@*:expired", StringCodec.INSTANCE);
    createdTopic = redisson.getPatternTopic(getEventsChannelPrefix() + "*", StringCodec.INSTANCE);

    // add listeners after all topics are created to avoid race and potential NPE if we get messages right away
    deletedTopic.addListener(String.class, this);
    expiredTopic.addListener(String.class, this);
    createdTopic.addListener(String.class, this);
  }

  public RedissonSessionRepository(RedissonClient redissonClient, ApplicationEventPublisher eventPublisher) {
    this(redissonClient, eventPublisher, null);
  }

  private MapSession loadSession(String sessionId) {
    RMap<String, Object> map = redisson.getMap(keyPrefix + sessionId, new CompositeCodec(StringCodec.INSTANCE, redisson.getConfig().getCodec()));
    Set<Map.Entry<String, Object>> entrySet = map.readAllEntrySet();
    if (entrySet.isEmpty()) {
      return null;
    }

    MapSession delegate = new MapSession(sessionId);
    for (Map.Entry<String, Object> entry : entrySet) {
      if ("session:creationTime".equals(entry.getKey())) {
        delegate.setCreationTime(Instant.ofEpochMilli((Long) entry.getValue()));
      }
      else if ("session:lastAccessedTime".equals(entry.getKey())) {
        delegate.setLastAccessedTime(Instant.ofEpochMilli((Long) entry.getValue()));
      }
      else if ("session:setMaxIdleTime".equals(entry.getKey())) {
        delegate.setMaxInactiveInterval(Duration.ofSeconds((Long) entry.getValue()));
      }
      else if (entry.getKey().startsWith(SESSION_ATTR_PREFIX)) {
        delegate.setAttribute(entry.getKey().substring(SESSION_ATTR_PREFIX.length()), entry.getValue());
      }
    }
    return delegate;
  }

  @Override
  public void onMessage(CharSequence pattern, CharSequence channel, String body) {
    if (createdTopic.getPatternNames().contains(pattern.toString())) {
      RedissonSession session = findById(body);
      if (session != null) {
        publishEvent(new SessionCreatedEvent(this, session));
      }
    }
    else if (deletedTopic.getPatternNames().contains(pattern.toString())) {
      if (!body.startsWith(getExpiredKeyPrefix())) {
        return;
      }

      String id = body.split(getExpiredKeyPrefix())[1];
      MapSession mapSession = loadSession(id);
      if (mapSession != null) {
        RedissonSession session = new RedissonSession(mapSession);
        session.clearPrincipal();
        publishEvent(new SessionDeletedEvent(this, session));
      }
    }
    else if (expiredTopic.getPatternNames().contains(pattern.toString())) {
      if (!body.startsWith(getExpiredKeyPrefix())) {
        return;
      }

      String id = body.split(getExpiredKeyPrefix())[1];
      MapSession mapSession = loadSession(id);
      if (mapSession != null) {
        RedissonSession session = new RedissonSession(mapSession);
        session.clearPrincipal();
        publishEvent(new SessionExpiredEvent(this, session));
      }
    }
  }

  private void publishEvent(ApplicationEvent event) {
    try {
      eventPublisher.publishEvent(event);
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  public void setDefaultMaxInactiveInterval(Integer defaultMaxInactiveInterval) {
    this.defaultMaxInactiveInterval = defaultMaxInactiveInterval;
  }

  @Override
  public RedissonSession createSession() {
    RedissonSession session = new RedissonSession();
    if (defaultMaxInactiveInterval != null) {
      session.setMaxInactiveInterval(Duration.ofSeconds(defaultMaxInactiveInterval));
    }
    return session;
  }

  @Override
  public void save(RedissonSession session) {
    // session changes are stored in real-time
  }

  @Override
  public RedissonSession findById(String id) {
    MapSession mapSession = loadSession(id);
    if (mapSession == null || mapSession.isExpired()) {
      return null;
    }
    return new RedissonSession(mapSession);
  }

  @Override
  public void deleteById(String id) {
    RedissonSession session = findById(id);
    if (session == null) {
      return;
    }

    redisson.getBucket(getExpiredKey(id)).delete();

    session.clearPrincipal();
    session.setMaxInactiveInterval(Duration.ZERO);
  }

  public void setKeyPrefix(String keyPrefix) {
    this.keyPrefix = keyPrefix;
  }

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

  public Map<String, RedissonSession> findByIndexNameAndIndexValue(String indexName, String indexValue) {
    if (!PRINCIPAL_NAME_INDEX_NAME.equals(indexName)) {
      return Collections.emptyMap();
    }

    RSet<String> set = getPrincipalSet(indexValue);

    Set<String> sessionIds = set.readAll();
    Map<String, RedissonSession> result = new HashMap<>();
    for (String id : sessionIds) {
      RedissonSession session = findById(id);
      if (session != null) {
        result.put(id, session);
      }
    }
    return result;
  }

  private RSet<String> getPrincipalSet(String indexValue) {
    String principalKey = getPrincipalKey(indexValue);
    return redisson.getSet(principalKey, StringCodec.INSTANCE);
  }

  final class RedissonSession extends AttributeAccessorSupport implements WebSession {

    private String principalName;
    private final WebSession delegate;
    private RMap<String, Object> map;

    RedissonSession() {
      this.delegate = new MemSessionRepository();
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
      if (delegate.getMaxIdleTime().getSeconds() > 0) {
        redisson.getBucket(getExpiredKey(delegate.getId()))
                .set("", delegate.getMaxIdleTime().getSeconds(), TimeUnit.SECONDS);
        map.expire(delegate.getMaxIdleTime().getSeconds() + 60, TimeUnit.SECONDS);
      }
    }

    RedissonSession(WebSession session) {
      this.delegate = session;
      map = redisson.getMap(keyPrefix + session.getId(), new CompositeCodec(StringCodec.INSTANCE, redisson.getConfig().getCodec()));
      principalName = resolvePrincipal(this);
    }

    @Override
    public String getId() {
      return delegate.getId();
    }

    @Override
    public void setAttribute(String attributeName, Object attributeValue) {
      if (attributeValue == null) {
        removeAttribute(attributeName);
        return;
      }

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

    public void clearPrincipal() {
      principalName = resolvePrincipal(this);
      if (principalName != null) {
        RSet<String> set = getPrincipalSet(principalName);
        set.remove(getId());
      }
    }

    @Override
    public WebSession removeAttribute(String attributeName) {
      delegate.removeAttribute(attributeName);

      if (map != null) {
        map.fastRemove(getSessionAttrNameKey(attributeName));
      }

    }

    @Override
    public Instant getCreationTime() {
      return delegate.getCreationTime();
    }

    public void setLastAccessedTime(Instant lastAccessedTime) {
      delegate.setLastAccessedTime(lastAccessedTime);

      if (map != null) {
        map.fastPut("session:lastAccessedTime", lastAccessedTime.toEpochMilli());
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
    public void changeSessionId() {
      String oldId = delegate.getId();
      delegate.changeSessionId();

      String id = delegate.getId();

      RBatch batch = redisson.createBatch(BatchOptions.defaults());
      batch.getBucket(getExpiredKey(oldId)).remainTimeToLiveAsync();
      batch.getBucket(getExpiredKey(oldId)).deleteAsync();
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
  }

}
