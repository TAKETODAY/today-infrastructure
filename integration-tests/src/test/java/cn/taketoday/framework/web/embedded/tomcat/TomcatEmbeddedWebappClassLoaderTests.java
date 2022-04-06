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

package cn.taketoday.framework.web.embedded.tomcat;

import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.ParallelWebappClassLoader;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.catalina.webresources.WarResourceSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import cn.taketoday.framework.web.embedded.tomcat.TomcatEmbeddedWebappClassLoader;
import cn.taketoday.util.CollectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TomcatEmbeddedWebappClassLoader}.
 *
 * @author Andy Wilkinson
 */
class TomcatEmbeddedWebappClassLoaderTests {

  @TempDir
  File tempDir;

  @Test
  void getResourceFindsResourceFromParentClassLoader() throws Exception {
    File war = createWar();
    withWebappClassLoader(war, (classLoader) -> assertThat(classLoader.getResource("test.txt"))
            .isEqualTo(new URL(webInfClassesUrlString(war) + "test.txt")));
  }

  @Test
  void getResourcesOnlyFindsResourcesFromParentClassLoader() throws Exception {
    File warFile = createWar();
    withWebappClassLoader(warFile, (classLoader) -> {
      List<URL> urls = new ArrayList<>();
      CollectionUtils.toIterator(classLoader.getResources("test.txt")).forEachRemaining(urls::add);
      assertThat(urls).containsExactly(new URL(webInfClassesUrlString(warFile) + "test.txt"));
    });
  }

  private void withWebappClassLoader(File war, ClassLoaderConsumer consumer) throws Exception {
    URLClassLoader parent = new URLClassLoader(new URL[] { new URL(webInfClassesUrlString(war)) }, null);
    try (ParallelWebappClassLoader classLoader = new TomcatEmbeddedWebappClassLoader(parent)) {
      StandardContext context = new StandardContext();
      context.setName("test");
      StandardRoot resources = new StandardRoot();
      resources.setContext(context);
      resources.addJarResources(new WarResourceSet(resources, "/", war.getAbsolutePath()));
      resources.start();
      classLoader.setResources(resources);
      classLoader.start();
      try {
        consumer.accept(classLoader);
      }
      finally {
        classLoader.stop();
        classLoader.close();
        resources.stop();
      }
    }
    parent.close();
  }

  private String webInfClassesUrlString(File war) {
    return "jar:file:" + war.getAbsolutePath() + "!/WEB-INF/classes/";
  }

  private File createWar() throws IOException {
    File warFile = new File(this.tempDir, "test.war");
    try (JarOutputStream warOut = new JarOutputStream(new FileOutputStream(warFile))) {
      createEntries(warOut, "WEB-INF/", "WEB-INF/classes/", "WEB-INF/classes/test.txt");
    }
    return warFile;
  }

  private void createEntries(JarOutputStream out, String... names) throws IOException {
    for (String name : names) {
      out.putNextEntry(new ZipEntry(name));
      out.closeEntry();
    }
  }

  interface ClassLoaderConsumer {

    void accept(ClassLoader classLoader) throws Exception;

  }

}
