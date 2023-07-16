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

/**
 * A {@link ProgressUpdateEvent} fired for image events.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 4.0
 */
public class ImageProgressUpdateEvent extends ProgressUpdateEvent {

  private final String id;

  protected ImageProgressUpdateEvent(String id, String status, ProgressDetail progressDetail, String progress) {
    super(status, progressDetail, progress);
    this.id = id;
  }

  /**
   * Returns the ID of the image layer being updated if available.
   *
   * @return the ID of the updated layer or {@code null}
   */
  public String getId() {
    return this.id;
  }

}
