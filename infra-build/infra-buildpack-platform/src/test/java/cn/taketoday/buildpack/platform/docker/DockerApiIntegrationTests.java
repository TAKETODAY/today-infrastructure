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

import org.junit.jupiter.api.Test;

import java.io.IOException;

import cn.taketoday.buildpack.platform.docker.type.ImageReference;
import cn.taketoday.test.testcontainers.DisabledIfDockerUnavailable;

/**
 * Integration tests for {@link DockerApi}.
 *
 * @author Phillip Webb
 */
@DisabledIfDockerUnavailable
class DockerApiIntegrationTests {

  private final DockerApi docker = new DockerApi();

  @Test
  void pullImage() throws IOException {
    this.docker.image()
            .pull(ImageReference.of("gcr.io/paketo-buildpacks/builder:base"),
                    new TotalProgressPullListener(new TotalProgressBar("Pulling: ")));
  }

}
