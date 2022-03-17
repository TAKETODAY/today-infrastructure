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

package cn.taketoday.beans.factory.parsing;

import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Nullable;

/**
 * Context that gets passed along a bean definition reading process,
 * encapsulating all relevant configuration as well as state.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 4.0
 */
public class ReaderContext {

  private final Resource resource;

  private final ProblemReporter problemReporter;

  private final ReaderEventListener eventListener;

  private final SourceExtractor sourceExtractor;

  /**
   * Construct a new {@code ReaderContext}.
   *
   * @param resource the XML bean definition resource
   * @param problemReporter the problem reporter in use
   * @param eventListener the event listener in use
   * @param sourceExtractor the source extractor in use
   */
  public ReaderContext(Resource resource, ProblemReporter problemReporter,
          ReaderEventListener eventListener, SourceExtractor sourceExtractor) {

    this.resource = resource;
    this.problemReporter = problemReporter;
    this.eventListener = eventListener;
    this.sourceExtractor = sourceExtractor;
  }

  public final Resource getResource() {
    return this.resource;
  }

  // Errors and warnings

  /**
   * Raise a fatal error.
   */
  public void fatal(String message, @Nullable Object source) {
    fatal(message, source, null, null);
  }

  /**
   * Raise a fatal error.
   */
  public void fatal(String message, @Nullable Object source, @Nullable Throwable cause) {
    fatal(message, source, null, cause);
  }

  /**
   * Raise a fatal error.
   */
  public void fatal(String message, @Nullable Object source, @Nullable ParseState parseState) {
    fatal(message, source, parseState, null);
  }

  /**
   * Raise a fatal error.
   */
  public void fatal(String message, @Nullable Object source, @Nullable ParseState parseState, @Nullable Throwable cause) {
    Location location = new Location(getResource(), source);
    this.problemReporter.fatal(new Problem(message, location, parseState, cause));
  }

  /**
   * Raise a regular error.
   */
  public void error(String message, @Nullable Object source) {
    error(message, source, null, null);
  }

  /**
   * Raise a regular error.
   */
  public void error(String message, @Nullable Object source, @Nullable Throwable cause) {
    error(message, source, null, cause);
  }

  /**
   * Raise a regular error.
   */
  public void error(String message, @Nullable Object source, @Nullable ParseState parseState) {
    error(message, source, parseState, null);
  }

  /**
   * Raise a regular error.
   */
  public void error(String message, @Nullable Object source, @Nullable ParseState parseState, @Nullable Throwable cause) {
    Location location = new Location(getResource(), source);
    this.problemReporter.error(new Problem(message, location, parseState, cause));
  }

  /**
   * Raise a non-critical warning.
   */
  public void warning(String message, @Nullable Object source) {
    warning(message, source, null, null);
  }

  /**
   * Raise a non-critical warning.
   */
  public void warning(String message, @Nullable Object source, @Nullable Throwable cause) {
    warning(message, source, null, cause);
  }

  /**
   * Raise a non-critical warning.
   */
  public void warning(String message, @Nullable Object source, @Nullable ParseState parseState) {
    warning(message, source, parseState, null);
  }

  /**
   * Raise a non-critical warning.
   */
  public void warning(String message, @Nullable Object source, @Nullable ParseState parseState, @Nullable Throwable cause) {
    Location location = new Location(getResource(), source);
    this.problemReporter.warning(new Problem(message, location, parseState, cause));
  }

  // Explicit parse events

  /**
   * Fire a defaults-registered event.
   */
  public void fireDefaultsRegistered(DefaultsDefinition defaultsDefinition) {
    this.eventListener.defaultsRegistered(defaultsDefinition);
  }

  /**
   * Fire a component-registered event.
   */
  public void fireComponentRegistered(ComponentDefinition componentDefinition) {
    this.eventListener.componentRegistered(componentDefinition);
  }

  /**
   * Fire an alias-registered event.
   */
  public void fireAliasRegistered(String beanName, String alias, @Nullable Object source) {
    this.eventListener.aliasRegistered(new AliasDefinition(beanName, alias, source));
  }

  /**
   * Fire an import-processed event.
   */
  public void fireImportProcessed(String importedResource, @Nullable Object source) {
    this.eventListener.importProcessed(new ImportDefinition(importedResource, source));
  }

  /**
   * Fire an import-processed event.
   */
  public void fireImportProcessed(String importedResource, Resource[] actualResources, @Nullable Object source) {
    this.eventListener.importProcessed(new ImportDefinition(importedResource, actualResources, source));
  }

  // Source extraction

  /**
   * Return the source extractor in use.
   */
  public SourceExtractor getSourceExtractor() {
    return this.sourceExtractor;
  }

  /**
   * Call the source extractor for the given source object.
   *
   * @param sourceCandidate the original source object
   * @return the source object to store, or {@code null} for none.
   * @see #getSourceExtractor()
   * @see SourceExtractor#extractSource
   */
  @Nullable
  public Object extractSource(Object sourceCandidate) {
    return this.sourceExtractor.extractSource(sourceCandidate, this.resource);
  }

}
