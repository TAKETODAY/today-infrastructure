/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

// Based on http://jira.qos.ch/browse/SLF4J-30

/**
 * <p>Bridge/route all JUL log records to the SLF4J API.
 * <p>Essentially, the idea is to install on the root logger an instance of
 * <code>SLF4JBridgeHandler</code> as the sole JUL handler in the system. Subsequently, the
 * SLF4JBridgeHandler instance will redirect all JUL log records are redirected
 * to the SLF4J API based on the following mapping of levels:
 *
 * <pre>
 * FINEST  -&gt; TRACE
 * FINER   -&gt; DEBUG
 * FINE    -&gt; DEBUG
 * INFO    -&gt; INFO
 * WARNING -&gt; WARN
 * SEVERE  -&gt; ERROR</pre>
 * <p><b>Programmatic installation:</b>
 * <pre>
 * // Optionally remove existing handlers attached to j.u.l root logger
 * SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)
 * // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
 * // the initialization phase of your application
 * SLF4JBridgeHandler.install();</pre>
 * <p><b>Installation via <em>logging.properties</em> configuration file:</b>
 * <pre>
 * // register SLF4JBridgeHandler as handler for the j.u.l. root logger
 * handlers = org.slf4j.bridge.SLF4JBridgeHandler</pre>
 * <p>Once SLF4JBridgeHandler is installed, logging by j.u.l. loggers will be directed to
 * SLF4J. Example:
 * <pre>
 * import  java.util.logging.Logger;
 * ...
 * // usual pattern: get a Logger and then log a message
 * Logger julLogger = Logger.getLogger(&quot;org.wombat&quot;);
 * julLogger.fine(&quot;hello world&quot;); // this will get redirected to SLF4J</pre>
 *
 * <p>Please note that translating a java.util.logging event into SLF4J incurs the
 * cost of constructing {@link LogRecord} instance regardless of whether the
 * SLF4J logger is disabled for the given level. <b>Consequently, j.u.l. to
 * SLF4J translation can seriously increase the cost of disabled logging
 * statements (60 fold or 6000% increase) and measurably impact the performance of enabled log
 * statements (20% overall increase).</b> Please note that as of logback-version 0.9.25,
 * it is possible to completely eliminate the 60 fold translation overhead for disabled
 * log statements with the help of <a href="http://logback.qos.ch/manual/configuration.html#LevelChangePropagator">LevelChangePropagator</a>.
 *
 *
 * <p>If you are concerned about application performance, then use of <code>SLF4JBridgeHandler</code>
 * is appropriate only if any one the following two conditions is true:
 * <ol>
 * <li>few j.u.l. logging statements are in play</li>
 * <li>LevelChangePropagator has been installed</li>
 * </ol>
 *
 * <h2>As a Java 9/Jigsaw module</h2>
 *
 * <p>Given that <b>to</b> is a reserved keyword under Java 9 within module productions,
 * the MAFIFEST.MF file in <em>jul-to-slf4j.jar</em> declares <b>jul_to_slf4j</b> as
 * its Automatic Module Name. Thus, if your application is Jigsaw modularized, the requires
 * statement in your <em>module-info.java</em> needs to be <b>jul_to_slf4j</b>
 * (note the two underscores).
 *
 * @author Christian Stein
 * @author Joern Huxhorn
 * @author Ceki G&uuml;lc&uuml;
 * @author Darryl Smith
 * @author TODAY 2021/10/31 21:31
 * @since 4.0
 */
public class SLF4JBridgeHandler extends Handler {

  // The caller is java.util.logging.Logger
  private static final String FQCN = java.util.logging.Logger.class.getName();
  private static final String UNKNOWN_LOGGER_NAME = "unknown.jul.logger";

  private static final int TRACE_LEVEL_THRESHOLD = Level.FINEST.intValue();
  private static final int DEBUG_LEVEL_THRESHOLD = Level.FINE.intValue();
  private static final int INFO_LEVEL_THRESHOLD = Level.INFO.intValue();
  private static final int WARN_LEVEL_THRESHOLD = Level.WARNING.intValue();

  /**
   * No-op implementation.
   */
  @Override
  public void close() {
    // empty
  }

  /**
   * No-op implementation.
   */
  @Override
  public void flush() {
    // empty
  }

  /**
   * Return the Logger instance that will be used for logging.
   *
   * @param record a LogRecord
   * @return an SLF4J logger corresponding to the record parameter's logger name
   */
  protected Logger getSLF4JLogger(LogRecord record) {
    String name = record.getLoggerName();
    if (name == null) {
      name = UNKNOWN_LOGGER_NAME;
    }
    return LoggerFactory.getLogger(name);
  }

  protected void callLocationAwareLogger(LocationAwareLogger lal, LogRecord record) {
    int julLevelValue = record.getLevel().intValue();
    int slf4jLevel;

    if (julLevelValue <= TRACE_LEVEL_THRESHOLD) {
      slf4jLevel = LocationAwareLogger.TRACE_INT;
    }
    else if (julLevelValue <= DEBUG_LEVEL_THRESHOLD) {
      slf4jLevel = LocationAwareLogger.DEBUG_INT;
    }
    else if (julLevelValue <= INFO_LEVEL_THRESHOLD) {
      slf4jLevel = LocationAwareLogger.INFO_INT;
    }
    else if (julLevelValue <= WARN_LEVEL_THRESHOLD) {
      slf4jLevel = LocationAwareLogger.WARN_INT;
    }
    else {
      slf4jLevel = LocationAwareLogger.ERROR_INT;
    }
    String i18nMessage = getMessageI18N(record);
    lal.log(null, FQCN, slf4jLevel, i18nMessage, null, record.getThrown());
  }

  protected void callPlainSLF4JLogger(Logger slf4jLogger, LogRecord record) {
    String i18nMessage = getMessageI18N(record);
    int julLevelValue = record.getLevel().intValue();
    if (julLevelValue <= TRACE_LEVEL_THRESHOLD) {
      slf4jLogger.trace(i18nMessage, record.getThrown());
    }
    else if (julLevelValue <= DEBUG_LEVEL_THRESHOLD) {
      slf4jLogger.debug(i18nMessage, record.getThrown());
    }
    else if (julLevelValue <= INFO_LEVEL_THRESHOLD) {
      slf4jLogger.info(i18nMessage, record.getThrown());
    }
    else if (julLevelValue <= WARN_LEVEL_THRESHOLD) {
      slf4jLogger.warn(i18nMessage, record.getThrown());
    }
    else {
      slf4jLogger.error(i18nMessage, record.getThrown());
    }
  }

  /**
   * Get the record's message, possibly via a resource bundle.
   */
  private String getMessageI18N(LogRecord record) {
    String message = record.getMessage();

    if (message == null) {
      return null;
    }

    ResourceBundle bundle = record.getResourceBundle();
    if (bundle != null) {
      try {
        message = bundle.getString(message);
      }
      catch (MissingResourceException ignored) {
      }
    }
    Object[] params = record.getParameters();
    // avoid formatting when there are no or 0 parameters. see also
    // http://jira.qos.ch/browse/SLF4J-203
    if (params != null && params.length > 0) {
      try {
        message = MessageFormat.format(message, params);
      }
      catch (IllegalArgumentException e) {
        // default to the same behavior as in java.util.logging.Formatter.formatMessage(LogRecord)
        // see also http://jira.qos.ch/browse/SLF4J-337
        return message;
      }
    }
    return message;
  }

  /**
   * Publish a LogRecord.
   * <p>
   * The logging request was made initially to a Logger object, which
   * initialized the LogRecord and forwarded it here.
   * <p>
   * This handler ignores the Level attached to the LogRecord, as SLF4J cares
   * about discarding log statements.
   *
   * @param record Description of the log event. A null record is silently ignored
   * and is not published.
   */
  @Override
  public void publish(LogRecord record) {
    // Silently ignore null records.
    if (record == null) {
      return;
    }

    Logger slf4jLogger = getSLF4JLogger(record);
    // this is a check to avoid calling the underlying logging system
    // with a null message. While it is legitimate to invoke j.u.l. with
    // a null message, other logging frameworks do not support this.
    // see also http://jira.qos.ch/browse/SLF4J-99
    if (record.getMessage() == null) {
      record.setMessage("");
    }
    if (slf4jLogger instanceof LocationAwareLogger) {
      callLocationAwareLogger((LocationAwareLogger) slf4jLogger, record);
    }
    else {
      callPlainSLF4JLogger(slf4jLogger, record);
    }
  }

  // static

  /**
   * Adds a SLF4JBridgeHandler instance to jul's root logger.
   *
   * <p>This handler will redirect j.u.l. logging to SLF4J. However, only logs enabled
   * in j.u.l. will be redirected. For example, if a log statement invoking a
   * j.u.l. logger is disabled, then the corresponding non-event will <em>not</em>
   * reach SLF4JBridgeHandler and cannot be redirected.
   */
  public static void install() {
    java.util.logging.Logger rootLogger = getRootLogger();
    Handler[] handlers = rootLogger.getHandlers();
    for (Handler handler : handlers) {
      rootLogger.removeHandler(handler);
    }
    rootLogger.addHandler(new SLF4JBridgeHandler());
  }

  private static java.util.logging.Logger getRootLogger() {
    return LogManager.getLogManager().getLogger("");
  }

  /**
   * Removes previously installed SLF4JBridgeHandler instances. See also
   * {@link #install()}.
   *
   * @throws SecurityException A <code>SecurityException</code> is thrown, if a security manager
   * exists and if the caller does not have
   * LoggingPermission("control").
   */
  public static void uninstall() throws SecurityException {
    java.util.logging.Logger rootLogger = getRootLogger();
    Handler[] handlers = rootLogger.getHandlers();
    for (Handler handler : handlers) {
      if (handler instanceof SLF4JBridgeHandler) {
        rootLogger.removeHandler(handler);
      }
    }
  }

  /**
   * Returns true if SLF4JBridgeHandler has been previously installed, returns false otherwise.
   *
   * @return true if SLF4JBridgeHandler is already installed, false otherwise
   */
  public static boolean isInstalled() {
    java.util.logging.Logger rootLogger = getRootLogger();
    Handler[] handlers = rootLogger.getHandlers();
    for (Handler handler : handlers) {
      if (handler instanceof SLF4JBridgeHandler) {
        return true;
      }
    }
    return false;
  }

}
