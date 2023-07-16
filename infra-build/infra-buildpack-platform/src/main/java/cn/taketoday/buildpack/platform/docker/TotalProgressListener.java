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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * {@link UpdateListener} that calculates the total progress of the entire image operation
 * and publishes {@link TotalProgressEvent}.
 *
 * @param <E> the type of {@link ImageProgressUpdateEvent}
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 4.0
 */
public abstract class TotalProgressListener<E extends ImageProgressUpdateEvent> implements UpdateListener<E> {

  private final Map<String, Layer> layers = new ConcurrentHashMap<>();

  private final Consumer<TotalProgressEvent> consumer;

  private final String[] trackedStatusKeys;

  private boolean progressStarted;

  /**
   * Create a new {@link TotalProgressListener} that sends {@link TotalProgressEvent
   * events} to the given consumer.
   *
   * @param consumer the consumer that receives {@link TotalProgressEvent progress
   * events}
   * @param trackedStatusKeys a list of status event keys to track the progress of
   */
  protected TotalProgressListener(Consumer<TotalProgressEvent> consumer, String[] trackedStatusKeys) {
    this.consumer = consumer;
    this.trackedStatusKeys = trackedStatusKeys;
  }

  @Override
  public void onStart() {
  }

  @Override
  public void onUpdate(E event) {
    if (event.getId() != null) {
      this.layers.computeIfAbsent(event.getId(), (value) -> new Layer(this.trackedStatusKeys)).update(event);
    }
    this.progressStarted = this.progressStarted || event.getProgress() != null;
    if (this.progressStarted) {
      publish(0);
    }
  }

  @Override
  public void onFinish() {
    this.layers.values().forEach(Layer::finish);
    publish(100);
  }

  private void publish(int fallback) {
    int count = 0;
    int total = 0;
    for (Layer layer : this.layers.values()) {
      count++;
      total += layer.getProgress();
    }
    TotalProgressEvent event = new TotalProgressEvent(
            (count != 0) ? withinPercentageBounds(total / count) : fallback);
    this.consumer.accept(event);
  }

  private static int withinPercentageBounds(int value) {
    if (value < 0) {
      return 0;
    }
    return Math.min(value, 100);
  }

  /**
   * Progress for an individual layer.
   */
  private static class Layer {

    private final Map<String, Integer> progressByStatus = new HashMap<>();

    Layer(String[] trackedStatusKeys) {
      Arrays.stream(trackedStatusKeys).forEach((status) -> this.progressByStatus.put(status, 0));
    }

    void update(ImageProgressUpdateEvent event) {
      String status = event.getStatus();
      if (event.getProgressDetail() != null && this.progressByStatus.containsKey(status)) {
        int current = this.progressByStatus.get(status);
        this.progressByStatus.put(status, updateProgress(current, event.getProgressDetail()));
      }
    }

    private int updateProgress(int current, ProgressUpdateEvent.ProgressDetail detail) {
      int result = withinPercentageBounds((int) ((100.0 / detail.getTotal()) * detail.getCurrent()));
      return Math.max(result, current);
    }

    void finish() {
      this.progressByStatus.keySet().forEach((key) -> this.progressByStatus.put(key, 100));
    }

    int getProgress() {
      return withinPercentageBounds((this.progressByStatus.values().stream().mapToInt(Integer::valueOf).sum())
              / this.progressByStatus.size());
    }

  }

}
