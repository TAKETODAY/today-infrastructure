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

package cn.taketoday.framework.web.embedded.undertow;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import io.undertow.servlet.api.SessionPersistenceManager.PersistentSession;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FileSessionPersistence}.
 *
 * @author Phillip Webb
 */
class FileSessionPersistenceTests {

  private File dir;

  private FileSessionPersistence persistence;

  private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

  private Date expiration = new Date(System.currentTimeMillis() + 10000);

  @BeforeEach
  void setup(@TempDir File tempDir) {
    this.dir = tempDir;
    this.dir.mkdir();
    this.persistence = new FileSessionPersistence(this.dir);
  }

  @Test
  void loadsNullForMissingFile() {
    Map<String, PersistentSession> attributes = this.persistence.loadSessionAttributes("test", this.classLoader);
    Assertions.assertThat(attributes).isNull();
  }

  @Test
  void persistAndLoad() {
    Map<String, PersistentSession> sessionData = new LinkedHashMap<>();
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("spring", "boot");
    PersistentSession session = new PersistentSession(this.expiration, data);
    sessionData.put("abc", session);
    this.persistence.persistSessions("test", sessionData);
    Map<String, PersistentSession> restored = this.persistence.loadSessionAttributes("test", this.classLoader);
    Assertions.assertThat(restored).isNotNull();
    Assertions.assertThat(restored.get("abc").getExpiration()).isEqualTo(this.expiration);
    Assertions.assertThat(restored.get("abc").getSessionData().get("spring")).isEqualTo("boot");
  }

  @Test
  void dontRestoreExpired() {
    Date expired = new Date(System.currentTimeMillis() - 1000);
    Map<String, PersistentSession> sessionData = new LinkedHashMap<>();
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("spring", "boot");
    PersistentSession session = new PersistentSession(expired, data);
    sessionData.put("abc", session);
    this.persistence.persistSessions("test", sessionData);
    Map<String, PersistentSession> restored = this.persistence.loadSessionAttributes("test", this.classLoader);
    Assertions.assertThat(restored).isNotNull();
    Assertions.assertThat(restored).doesNotContainKey("abc");
  }

  @Test
  void deleteFileOnClear() {
    File sessionFile = new File(this.dir, "test.session");
    Map<String, PersistentSession> sessionData = new LinkedHashMap<>();
    this.persistence.persistSessions("test", sessionData);
    assertThat(sessionFile.exists()).isTrue();
    this.persistence.clear("test");
    assertThat(sessionFile.exists()).isFalse();
  }

}
