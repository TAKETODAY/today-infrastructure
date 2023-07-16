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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.buildpack.platform.json.AbstractJsonTests;
import cn.taketoday.buildpack.platform.json.JsonStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TotalProgressPullListener}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class TotalProgressListenerTests extends AbstractJsonTests {

  @Test
  void totalProgress() throws Exception {
    List<Integer> progress = new ArrayList<>();
    TestTotalProgressListener listener = new TestTotalProgressListener((event) -> progress.add(event.getPercent()));
    run(listener);
    int last = 0;
    for (Integer update : progress) {
      assertThat(update).isGreaterThanOrEqualTo(last);
      last = update;
    }
    assertThat(last).isEqualTo(100);
  }

  @Test
  @Disabled("For visual inspection")
  void totalProgressUpdatesSmoothly() throws Exception {
    TestTotalProgressListener listener = new TestTotalProgressListener(new TotalProgressBar("Pulling layers:"));
    run(listener);
  }

  private void run(TestTotalProgressListener listener) throws IOException {
    JsonStream jsonStream = new JsonStream(getObjectMapper());
    listener.onStart();
    jsonStream.get(getContent("pull-stream.json"), TestImageUpdateEvent.class, listener::onUpdate);
    listener.onFinish();
  }

  private static class TestTotalProgressListener extends TotalProgressListener<TestImageUpdateEvent> {

    TestTotalProgressListener(Consumer<TotalProgressEvent> consumer) {
      super(consumer, new String[] { "Pulling", "Downloading", "Extracting" });
    }

    @Override
    public void onUpdate(TestImageUpdateEvent event) {
      super.onUpdate(event);
      try {
        Thread.sleep(10);
      }
      catch (InterruptedException ex) {
      }
    }

  }

  private static class TestImageUpdateEvent extends ImageProgressUpdateEvent {

    @JsonCreator
    TestImageUpdateEvent(String id, String status, ProgressDetail progressDetail, String progress) {
      super(id, status, progressDetail, progress);
    }

  }

}
