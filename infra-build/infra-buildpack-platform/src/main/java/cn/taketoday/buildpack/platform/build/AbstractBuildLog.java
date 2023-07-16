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

import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.buildpack.platform.docker.LogUpdateEvent;
import cn.taketoday.buildpack.platform.docker.TotalProgressEvent;
import cn.taketoday.buildpack.platform.docker.type.Image;
import cn.taketoday.buildpack.platform.docker.type.ImageReference;
import cn.taketoday.buildpack.platform.docker.type.VolumeName;

/**
 * Base class for {@link BuildLog} implementations.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Andrey Shlykov
 * @author Rafael Ceccone
 * @since 4.0
 */
public abstract class AbstractBuildLog implements BuildLog {

  @Override
  public void start(BuildRequest request) {
    log("Building image '" + request.getName() + "'");
    log();
  }

  @Override
  public Consumer<TotalProgressEvent> pullingImage(ImageReference imageReference, ImageType imageType) {
    return getProgressConsumer(String.format(" > Pulling %s '%s'", imageType.getDescription(), imageReference));
  }

  @Override
  public void pulledImage(Image image, ImageType imageType) {
    log(String.format(" > Pulled %s '%s'", imageType.getDescription(), getDigest(image)));
  }

  @Override
  public Consumer<TotalProgressEvent> pushingImage(ImageReference imageReference) {
    return getProgressConsumer(String.format(" > Pushing image '%s'", imageReference));
  }

  @Override
  public void pushedImage(ImageReference imageReference) {
    log(String.format(" > Pushed image '%s'", imageReference));
  }

  @Override
  public void executingLifecycle(BuildRequest request, LifecycleVersion version, VolumeName buildCacheVolume) {
    log(" > Executing lifecycle version " + version);
    log(" > Using build cache volume '" + buildCacheVolume + "'");
  }

  @Override
  public Consumer<LogUpdateEvent> runningPhase(BuildRequest request, String name) {
    log();
    log(" > Running " + name);
    String prefix = String.format("    %-14s", "[" + name + "] ");
    return (event) -> log(prefix + event);
  }

  @Override
  public void skippingPhase(String name, String reason) {
    log();
    log(" > Skipping " + name + " " + reason);
    log();
  }

  @Override
  public void executedLifecycle(BuildRequest request) {
    log();
    log("Successfully built image '" + request.getName() + "'");
    log();
  }

  @Override
  public void taggedImage(ImageReference tag) {
    log("Successfully created image tag '" + tag + "'");
    log();
  }

  private String getDigest(Image image) {
    List<String> digests = image.getDigests();
    return (digests.isEmpty() ? "" : digests.get(0));
  }

  protected void log() {
    log("");
  }

  protected abstract void log(String message);

  protected abstract Consumer<TotalProgressEvent> getProgressConsumer(String message);

}
