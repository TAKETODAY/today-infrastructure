/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import static cn.taketoday.context.utils.ClassUtils.loadClass;

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
import cn.taketoday.framework.WebApplication;
import cn.taketoday.framework.WebServerApplicationContext;

/**
 * @author TODAY <br>
 *         2019-06-12 10:03
 */
@Props(prefix = "devtools.hotswap.")
public class DefaultWatcherThread extends Thread implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(DefaultWatcherThread.class);

    private volatile WatchKey watchKey;
    private static final List<Path> watchingDirs;

    private volatile boolean running = true;

    private ClassLoader parent;
    private WebServerApplicationContext applicationContext;

    private static final DefaultClassResolver HOT_SWAP_RESOLVER = new DefaultClassResolver();

    private static int reloadCount = 0;

    private String[] hotSwapClassPrefix = { //

    };

    private URL[] urLs;
    private boolean scanAllDir = true;

    static {
        watchingDirs = buildWatchingPaths();
    }

    public DefaultWatcherThread(WebServerApplicationContext applicationContext) {

        this.applicationContext = applicationContext;

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

        List<String> dirList = new ArrayList<String>(watchingDirSet);
        Collections.sort(dirList);

        List<Path> pathList = new ArrayList<Path>(dirList.size());
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
            for (File f : fileList) {
                buildDirs(f, watchingDirSet);
            }
        }
    }

    public void run() {
        try {
            doRun();
        }
        catch (Throwable e) {
            log.error("Error occurred", e);
            //			throw new ContextException(e);
        }
    }

    protected void doRun() throws Throwable {

        log.info("HotSwapWatcher Is Running");

        WatchService watcher = FileSystems.getDefault().newWatchService();
        addShutdownHook(watcher);

        for (Path path : watchingDirs) {
            path.register(
                          watcher,
                          StandardWatchEventKinds.ENTRY_DELETE,
                          StandardWatchEventKinds.ENTRY_MODIFY,
                          StandardWatchEventKinds.ENTRY_CREATE);
        }

        while (running) {
            try {
                watchKey = watcher.take();

                if (watchKey == null) {
                    continue;
                }
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
            List<WatchEvent<?>> watchEvents = watchKey.pollEvents();
            for (WatchEvent<?> ev : watchEvents) {

                String fileName = ev.context().toString();

                final Kind<?> kind = ev.kind();

                log.info("File: [{}] changed, type: [{}]", fileName, kind);
                if (StandardWatchEventKinds.ENTRY_MODIFY == kind && fileName.endsWith(".class")) {
                    if (!applicationContext.hasStarted()) {
                        continue;
                    }
                    applicationContext.close();

                    replaceClassLoader();

                    Class<?> startupClass = applicationContext.getStartupClass();
                    if (startupClass != null) {
                        startupClass = loadClass(startupClass.getName());
                    }
                    WebApplication.run(startupClass);

                    resetWatchKey();
                    while ((watchKey = watcher.poll()) != null) {
                        // System.out.println("---> poll() ");
                        watchKey.pollEvents();
                        resetWatchKey();
                    }
                    exit();
                    break;
                }
            }
            resetWatchKey();
        }
    }

    protected void replaceClassLoader() {
        //		if (!enableJrebel) {
        ClassUtils.setClassLoader(new DefaultReloadClassLoader(urLs, parent, HOT_SWAP_RESOLVER));
        //		}
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
            //            interrupt();
            join();
        }
        catch (Exception e) {
            throw new ContextException(e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        this.parent = ClassUtils.getClassLoader();
        //        System.err.println(parent);
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
