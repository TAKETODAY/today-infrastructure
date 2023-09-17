/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.app.loader.jar;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.Map;

import cn.taketoday.test.util.ReflectionTestUtils;

/**
 * JUnit 5 {@link Extension} for tests that interact with Infra {@link Handler}
 * for {@code jar:} URLs. Ensures that the handler is registered prior to test execution
 * and cleans up the handler's root file cache afterwards.
 *
 * @author Andy Wilkinson
 */
class JarUrlProtocolHandler implements BeforeEachCallback, AfterEachCallback {

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    JarFile.registerUrlProtocolHandler();
  }

  @Override
  @SuppressWarnings("unchecked")
  public void afterEach(ExtensionContext context) throws Exception {
    Map<File, JarFile> rootFileCache = ((SoftReference<Map<File, JarFile>>) ReflectionTestUtils
            .getField(Handler.class, "rootFileCache")).get();
    if (rootFileCache != null) {
      for (JarFile rootJarFile : rootFileCache.values()) {
        rootJarFile.close();
      }
      rootFileCache.clear();
    }
  }

}
