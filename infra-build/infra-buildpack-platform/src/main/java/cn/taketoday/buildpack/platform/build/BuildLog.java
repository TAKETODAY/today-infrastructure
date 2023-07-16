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

package cn.taketoday.buildpack.platform.build;

import java.io.PrintStream;
import java.util.function.Consumer;

import cn.taketoday.buildpack.platform.docker.LogUpdateEvent;
import cn.taketoday.buildpack.platform.docker.TotalProgressEvent;
import cn.taketoday.buildpack.platform.docker.type.Image;
import cn.taketoday.buildpack.platform.docker.type.ImageReference;
import cn.taketoday.buildpack.platform.docker.type.VolumeName;

/**
 * Callback interface used to provide {@link Builder} output logging.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Andrey Shlykov
 * @author Rafael Ceccone
 * @see #toSystemOut()
 * @since 4.0
 */
public interface BuildLog {

  /**
   * Log that a build is starting.
   *
   * @param request the build request
   */
  void start(BuildRequest request);

  /**
   * Log that an image is being pulled.
   *
   * @param imageReference the image reference
   * @param imageType the image type
   * @return a consumer for progress update events
   */
  Consumer<TotalProgressEvent> pullingImage(ImageReference imageReference, ImageType imageType);

  /**
   * Log that an image has been pulled.
   *
   * @param image the image that was pulled
   * @param imageType the image type that was pulled
   */
  void pulledImage(Image image, ImageType imageType);

  /**
   * Log that an image is being pushed.
   *
   * @param imageReference the image reference
   * @return a consumer for progress update events
   */
  Consumer<TotalProgressEvent> pushingImage(ImageReference imageReference);

  /**
   * Log that an image has been pushed.
   *
   * @param imageReference the image reference
   */
  void pushedImage(ImageReference imageReference);

  /**
   * Log that the lifecycle is executing.
   *
   * @param request the build request
   * @param version the lifecycle version
   * @param buildCacheVolume the name of the build cache volume in use
   */
  void executingLifecycle(BuildRequest request, LifecycleVersion version, VolumeName buildCacheVolume);

  /**
   * Log that a specific phase is running.
   *
   * @param request the build request
   * @param name the name of the phase
   * @return a consumer for log updates
   */
  Consumer<LogUpdateEvent> runningPhase(BuildRequest request, String name);

  /**
   * Log that a specific phase is being skipped.
   *
   * @param name the name of the phase
   * @param reason the reason the phase is skipped
   */
  void skippingPhase(String name, String reason);

  /**
   * Log that the lifecycle has executed.
   *
   * @param request the build request
   */
  void executedLifecycle(BuildRequest request);

  /**
   * Log that a tag has been created.
   *
   * @param tag the tag reference
   */
  void taggedImage(ImageReference tag);

  /**
   * Factory method that returns a {@link BuildLog} the outputs to {@link System#out}.
   *
   * @return a build log instance that logs to system out
   */
  static BuildLog toSystemOut() {
    return to(System.out);
  }

  /**
   * Factory method that returns a {@link BuildLog} the outputs to a given
   * {@link PrintStream}.
   *
   * @param out the print stream used to output the log
   * @return a build log instance that logs to the given print stream
   */
  static BuildLog to(PrintStream out) {
    return new PrintStreamBuildLog(out);
  }

}
