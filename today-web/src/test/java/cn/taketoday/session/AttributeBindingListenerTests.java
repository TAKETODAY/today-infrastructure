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

package cn.taketoday.session;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/7/30 13:58
 */
class AttributeBindingListenerTests {

  @Test
  void normal() {
    InMemorySessionRepository repository = new InMemorySessionRepository(
            new SessionEventDispatcher(), new SecureRandomSessionIdGenerator());

    WebSession session = repository.createSession();

    SerializableAttribute attribute = new SerializableAttribute();
    session.setAttribute("valueBound", attribute);

    assertThat(attribute.bindCount).isEqualTo(1);
    session.removeAttribute("valueBound");
    assertThat(attribute.bindCount).isEqualTo(0);
  }

  @Test
  void persistence(@TempDir File tempDir) throws IOException, ClassNotFoundException {
    InMemorySessionRepository repository = new InMemorySessionRepository(
            new SessionEventDispatcher(), new SecureRandomSessionIdGenerator());

    var sessionPersister = new FileSessionPersister(repository);
    sessionPersister.setDirectory(tempDir);
    var persistenceRepository = new PersistenceSessionRepository(sessionPersister, repository);

    WebSession session = repository.createSession();
    session.start();
    session.save();

    SerializableAttribute attribute = new SerializableAttribute();
    session.setAttribute("serializable", attribute);
    UnSerializableAttribute unSerializableAttribute = new UnSerializableAttribute();
    session.setAttribute("unserializable", unSerializableAttribute);

    assertThat(attribute.bindCount).isEqualTo(1);
    assertThat(unSerializableAttribute.bindCount).isEqualTo(1);

    persistenceRepository.persistSessions();

    WebSession webSession = sessionPersister.findById(session.getId());
    assertThat(webSession).isNotNull();
    assertThat(webSession.hasAttribute("serializable")).isTrue();
    assertThat(webSession.hasAttribute("unserializable")).isFalse();
    assertThat(attribute.bindCount).isEqualTo(1);

    var serializableAttribute = (SerializableAttribute) webSession.getAttribute("serializable");
    assertThat(serializableAttribute).isNotNull();
    assertThat(serializableAttribute.bindCount).isEqualTo(2);
  }

  static class UnSerializableAttribute implements AttributeBindingListener {
    int bindCount = 0;

    @Override
    public void valueBound(WebSession session, String attributeName) {
      bindCount++;
    }

    @Override
    public void valueUnbound(WebSession session, String attributeName) {
      bindCount--;
    }
  }

  static class SerializableAttribute implements AttributeBindingListener, Serializable {

    int bindCount = 0;

    @Override
    public void valueBound(WebSession session, String attributeName) {
      bindCount++;
    }

    @Override
    public void valueUnbound(WebSession session, String attributeName) {
      bindCount--;
    }
  }

}