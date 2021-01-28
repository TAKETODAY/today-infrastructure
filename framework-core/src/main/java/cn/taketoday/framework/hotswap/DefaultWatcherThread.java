/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.framework.hotswap;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.exception.ContextException;
import cn.taketoday.context.factory.InitializingBean;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.framework.WebApplication;
import cn.taketoday.framework.WebServerApplicationContext;

import static cn.taketoday.context.utils.ClassUtils.loadClass;

/**
 * @author TODAY <br>
 * 2019-06-12 10:03
 */
@Props(prefix = "devtools.hotswap.")
public class DefaultWatcherThread
        extends Thread implements InitializingBean {

  private static final Logger log = LoggerFactory.getLogger(DefaultWatcherThread.class);

  private WatchKey watchKey;
  private static final List<Path> watchingDirs;

  private volatile boolean running = true;

  private ClassLoader parent;
  private WebServerApplicationContext applicationContext;

  private static final DefaultClassResolver HOT_SWAP_RESOLVER = new DefaultClassResolver();

  private static int reloadCount = 0;
  private static int sleepTime = 50;

  private String[] hotSwapClassPrefix = { //

  };

  private URL[] urLs;
  private boolean scanAllDir = true;

  static {
    watchingDirs = buildWatchingPaths();
  }

  public DefaultWatcherThread(WebServerApplicationContext context) {
    this.applicationContext = context;

    setName("Watcher-" + reloadCount++);

    setDaemon(false);
    setPriority(Thread.MAX_PRIORITY);
  }

  private static List<Path> buildWatchingPaths() {
    Set<String> watchingDirSet = new HashSet<>();
    String[] classPathArray = System.getProperty("java.class.path").split(File.pathSeparator);

    for (String classPath : classPathArray) {
      buildDirs(new File(classPath.trim()), watchingDirSet);
    }

    List<String> dirList = new ArrayList<>(watchingDirSet);
    Collections.sort(dirList);

    List<Path> pathList = new ArrayList<>(dirList.size());
    for (String dir : dirList) {
      if (!dir.contains("META-INF")) {
        if (log.isTraceEnabled()) {
          log.trace("Watching dir: [{}]", dir);
        }
        pathList.add(Paths.get(dir));
      }
    }
    return pathList;
  }

  private static void buildDirs(File file, Set<String> watchingDirSet) {
    if (file.isDirectory()) {
      watchingDirSet.add(file.getPath());

      File[] fileList = file.listFiles();
      if (ObjectUtils.isNotEmpty(fileList)) {
        for (File f : fileList) {
          buildDirs(f, watchingDirSet);
        }
      }
    }
  }

  @Override
  public void run() {
    try {
      doRun();
    }
    catch (Throwable e) {
      log.error("Error occurred", e);
      //throw new ContextException(e);
    }
  }

  protected void doRun() throws Throwable {

    log.info("HotSwapWatcher Is Running");

    WatchService watcher = FileSystems.getDefault().newWatchService();
    addShutdownHook(watcher);

    WatchEvent.Kind<?>[] kinds = new WatchEvent.Kind<?>[] {
            StandardWatchEventKinds.ENTRY_MODIFY
    };
    for (Path path : watchingDirs) {
      path.register(watcher, kinds);
    }

    while (running) {
      try {
        watchKey = watcher.take();

        if (watchKey == null) {
          continue;
        }
        sleep(sleepTime);
      }
      catch (InterruptedException e) {
        running = false;
        Thread.currentThread().interrupt();
        break;
      }
      catch (Exception e) {
        running = false;
        break;
      }
      process(watcher);
    }
  }

  protected void process(final WatchService watcher) {
    List<WatchEvent<?>> watchEvents = watchKey.pollEvents();

    if (reloadable(watchEvents)) {
      applicationContext.close();
      replaceClassLoader();

      Class<?> startupClass = applicationContext.getStartupClass();
      if (startupClass != null) {
        final String startupClassName = startupClass.getName();
        log.info("restart application: [{}]", startupClassName);
        startupClass = loadClass(startupClassName);
      }
      applicationContext = WebApplication.run(startupClass);

      resetWatchKey();
      exit();
    }
  }

  protected boolean reloadable(List<WatchEvent<?>> watchEvents) {
    if (!applicationContext.hasStarted()) {
      return false;
    }
    for (WatchEvent<?> ev : watchEvents) {
      String fileName = ev.context().toString();
      final Kind<?> kind = ev.kind();
      log.info("File: [{}] changed, type: [{}]", fileName, kind);
      if (StandardWatchEventKinds.ENTRY_MODIFY == kind && fileName.endsWith(".class")) {
        return true;
      }
    }
    return false;
  }

  protected void replaceClassLoader() {
    //if (!enableJrebel) {
    ClassUtils.setClassLoader(new DefaultReloadClassLoader(urLs, parent, HOT_SWAP_RESOLVER));
    //}
  }

  protected void resetWatchKey() {
    if (watchKey != null) {
      watchKey.reset();
      watchKey = null;
    }
  }

  protected void addShutdownHook(WatchService watcher) {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        watcher.close();
      }
      catch (IOException e) {
        throw new ContextException(e);
      }
    }));
  }

  public void exit() {
    log.info("Exit Previous Watcher");
    running = false;
    try {
      // interrupt();
      join();
    }
    catch (Exception e) {
      throw new ContextException(e);
    }
  }

  @Override
  public void afterPropertiesSet() {

    this.parent = ClassUtils.getClassLoader();
    // System.err.println(parent);
    if (parent instanceof URLClassLoader) {
      urLs = ((URLClassLoader) parent).getURLs();
    }
    else {
      urLs = new URL[0];
    }

    HOT_SWAP_RESOLVER.addHotSwapClassPrefix(hotSwapClassPrefix);
    HOT_SWAP_RESOLVER.setScanAllDir(scanAllDir);

    start();
  }
}
