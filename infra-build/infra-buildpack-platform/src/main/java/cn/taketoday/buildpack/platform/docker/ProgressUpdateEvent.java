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
 * An {@link UpdateEvent} that includes progress information.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public abstract class ProgressUpdateEvent extends UpdateEvent {

  private final String status;

  private final ProgressDetail progressDetail;

  private final String progress;

  protected ProgressUpdateEvent(String status, ProgressDetail progressDetail, String progress) {
    this.status = status;
    this.progressDetail = (ProgressDetail.isEmpty(progressDetail)) ? null : progressDetail;
    this.progress = progress;
  }

  /**
   * Return the status for the update. For example, "Extracting" or "Downloading".
   *
   * @return the status of the update.
   */
  public String getStatus() {
    return this.status;
  }

  /**
   * Return progress details if available.
   *
   * @return progress details or {@code null}
   */
  public ProgressDetail getProgressDetail() {
    return this.progressDetail;
  }

  /**
   * Return a text based progress bar if progress information is available.
   *
   * @return the progress bar or {@code null}
   */
  public String getProgress() {
    return this.progress;
  }

  /**
   * Provide details about the progress of a task.
   */
  public static class ProgressDetail {

    private final Integer current;

    private final Integer total;

    @JsonCreator
    public ProgressDetail(Integer current, Integer total) {
      this.current = current;
      this.total = total;
    }

    /**
     * Return the current progress value.
     *
     * @return the current progress
     */
    public int getCurrent() {
      return this.current;
    }

    /**
     * Return the total progress possible value.
     *
     * @return the total progress possible
     */
    public int getTotal() {
      return this.total;
    }

    public static boolean isEmpty(ProgressDetail progressDetail) {
      return progressDetail == null || progressDetail.current == null || progressDetail.total == null;
    }

  }

}
