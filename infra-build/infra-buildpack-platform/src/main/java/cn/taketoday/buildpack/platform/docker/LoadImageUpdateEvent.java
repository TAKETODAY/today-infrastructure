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

package cn.taketoday.buildpack.platform.docker;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * A {@link ProgressUpdateEvent} fired as an image is loaded.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public class LoadImageUpdateEvent extends ProgressUpdateEvent {

  private final String stream;

  @JsonCreator
  public LoadImageUpdateEvent(String stream, String status, ProgressDetail progressDetail, String progress) {
    super(status, progressDetail, progress);
    this.stream = stream;
  }

  /**
   * Return the stream response or {@code null} if no response is available.
   *
   * @return the stream response.
   */
  public String getStream() {
    return this.stream;
  }

}
