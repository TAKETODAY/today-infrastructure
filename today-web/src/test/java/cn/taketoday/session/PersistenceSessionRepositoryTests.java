/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.session;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import lombok.Data;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/3/3 16:46
 */
class PersistenceSessionRepositoryTests {

  @TempDir
  File tempDir;

//  static class

  @Data
  static class User implements Serializable {
    public int age;
    public String name;
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

    WebSession retrieveSession = repository.retrieveSession(id);
    assertThat(retrieveSession).isNull();

    WebSession session = repository.createSession(id);
    User loginUser = new User();
    loginUser.age = 20;
    loginUser.name = "TODAY";
    session.setAttribute("name", "value");
    session.setAttribute("loginUser", loginUser);
    persister.save(session);

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

}