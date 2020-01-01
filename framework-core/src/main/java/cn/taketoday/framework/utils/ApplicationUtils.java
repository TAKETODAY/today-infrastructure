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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.framework.utils;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.framework.ConfigurableWebServerApplicationContext;
import cn.taketoday.framework.Constant;
import cn.taketoday.framework.server.AbstractWebServer;
import cn.taketoday.framework.server.ConfigurableWebServer;
import cn.taketoday.framework.server.WebServer;

/**
 * @author TODAY <br>
 *         2019-06-19 20:05
 */
public abstract class ApplicationUtils {

    public static WebServer obtainWebServer(ConfigurableWebServerApplicationContext beanFactory) throws Throwable {

        ConfigurableEnvironment environment = beanFactory.getEnvironment();
        // disable web mvc xml
        environment.setProperty(Constant.ENABLE_WEB_MVC_XML, "false");

        final Logger logger = LoggerFactory.getLogger(ApplicationUtils.class);
        logger.info("Looking For: [{}] Bean.", WebServer.class.getName());

        // Get WebServer instance
        WebServer webServer = beanFactory.getBean(WebServer.class);
        if (webServer == null) {
            throw new ConfigurationException("The context: ["
                    + beanFactory + "] doesn't exist a [cn.taketoday.framework.server.WebServer] bean");
        }

        if (webServer instanceof ConfigurableWebServer) {
            if (webServer instanceof AbstractWebServer) {
                ((AbstractWebServer) webServer).getWebApplicationConfiguration()
                        .configureWebServer((AbstractWebServer) webServer);
            }
            logger.info("Initializing Web Server: [{}]", webServer);
            ((ConfigurableWebServer) webServer).initialize();
        }
        return webServer;
    }

    /**
     * @param startupClass
     * @param subdir
     * @return
     */
    public static File getTemporalDirectory(Class<?> startupClass, String subdir) {
        if (StringUtils.isEmpty(subdir)) {
            return getBaseTemporalDirectory(startupClass);
        }
        final File dir = new File(getBaseTemporalDirectory(startupClass), subdir);
        dir.mkdirs();
        return dir;
    }

    /**
     * Return the directory to be used for application specific temp files.
     * 
     * @return the application temp directory
     */
    public static File getBaseTemporalDirectory(Class<?> startupClass) {

        final String property = System.getProperty("java.io.tmpdir");

        if (StringUtils.isEmpty(property)) {
            throw new IllegalStateException("There is no 'java.io.tmpdir' property set");
        }

        final File baseTempDir = new File(property);
        if (!baseTempDir.exists()) {
            throw new IllegalStateException("Temp directory " + baseTempDir + " does not exist");
        }

        if (!baseTempDir.isDirectory()) {
            throw new IllegalStateException("Temp location " + baseTempDir + " is not a directory");
        }

        final File directory = new File(baseTempDir, startupClass.getName());

        if (!directory.exists()) {

            directory.mkdirs();

            if (!directory.exists()) {
                throw new IllegalStateException("Unable to create temp directory " + directory);
            }
        }

        return directory;
    }

    /**
     * Returns the application home directory.
     * 
     * @return the home directory (never {@code null})
     */
    public File getApplicationDirectory(Class<?> startupClass) {
        return findHomeDir(getApplicationSource(startupClass));
    }

    /**
     * Returns the underlying source used to find the home directory. This is
     * usually the jar file or a directory. Can return {@code null} if the source
     * cannot be determined.
     * 
     * @return the underlying source or {@code null}
     */
    public File getApplicationSource(Class<?> startupClass) {
        return findSource(startupClass);
    }

    public static File findSource(Class<?> startupClass) {
        try {
            if (startupClass == null) {
                return null;
            }
            final ProtectionDomain domain = startupClass.getProtectionDomain();
            if (domain == null) {
                return null;
            }

            final CodeSource codeSource = domain.getCodeSource();
            if (codeSource == null) {
                return null;
            }
            final File source = findSource(codeSource.getLocation());

            if (source != null && source.exists()) {
                return source.getAbsoluteFile();
            }
            return null;
        }
        catch (Exception ex) {
            return null;
        }
    }

    private static File findSource(URL location) throws IOException {
        if (location == null) {
            return null;
        }
        URLConnection connection = location.openConnection();
        if (connection instanceof JarURLConnection) {

            String name = ((JarURLConnection) connection).getJarFile().getName();
            int separator = name.indexOf("!/");
            if (separator > 0) {
                name = name.substring(0, separator);
            }
            return new File(name);
        }
        return new File(location.getPath());
    }

    /**
     * @param homeDir
     *            source dir
     * @return
     */
    private static File findHomeDir(File homeDir) {

        File ret = homeDir;
        if (ret == null) {
            final String userDir = System.getProperty("user.dir");
            ret = new File(StringUtils.isNotEmpty(userDir) ? userDir : ".");
        }
        if (ret.isFile()) {
            ret = homeDir.getParentFile();
        }
        if (!ret.exists()) {
            ret = new File(".");
        }
        return ret.getAbsoluteFile();
    }

    // -------------------args

    /**
     * Parse command arguments
     * 
     * @param args
     *            arguments
     * @return key-value
     */
    public static Map<String, String> parseCommandArguments(final String... args) {

        if (StringUtils.isArrayEmpty(args)) {
            return Collections.emptyMap();
        }

        final Map<String, String> argsMap = new LinkedHashMap<>();
        for (final String arg : args) {
            if (arg.startsWith("--") && arg.indexOf('=') > -1) {
                final String[] param = arg.substring(2).split("=");
                if (param.length >= 2) {
                    argsMap.put(param[0], param[1]);
                }
            }
        }
        return argsMap;
    }

}
