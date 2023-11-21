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

package cn.taketoday.buildpack.platform.docker.type;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import cn.taketoday.buildpack.platform.io.Content;
import cn.taketoday.buildpack.platform.io.IOConsumer;
import cn.taketoday.buildpack.platform.io.InspectedContent;
import cn.taketoday.buildpack.platform.io.Layout;
import cn.taketoday.buildpack.platform.io.TarArchive;

import cn.taketoday.lang.Assert;

/**
 * A layer that can be written to an {@link ImageArchive}.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public class Layer implements Content {

  private final Content content;

  private final LayerId id;

  Layer(TarArchive tarArchive) throws NoSuchAlgorithmException, IOException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    this.content = InspectedContent.of(tarArchive::writeTo, digest::update);
    this.id = LayerId.ofSha256Digest(digest.digest());
  }

  /**
   * Return the ID of the layer.
   *
   * @return the layer ID
   */
  public LayerId getId() {
    return this.id;
  }

  @Override
  public int size() {
    return this.content.size();
  }

  @Override
  public void writeTo(OutputStream outputStream) throws IOException {
    this.content.writeTo(outputStream);
  }

  /**
   * Factory method to create a new {@link Layer} with a specific {@link Layout}.
   *
   * @param layout the layer layout
   * @return a new layer instance
   * @throws IOException on IO error
   */
  public static Layer of(IOConsumer<Layout> layout) throws IOException {
    Assert.notNull(layout, "Layout is required");
    return fromTarArchive(TarArchive.of(layout));
  }

  /**
   * Factory method to create a new {@link Layer} from a {@link TarArchive}.
   *
   * @param tarArchive the contents of the layer
   * @return a new layer instance
   * @throws IOException on error
   */
  public static Layer fromTarArchive(TarArchive tarArchive) throws IOException {
    Assert.notNull(tarArchive, "TarArchive is required");
    try {
      return new Layer(tarArchive);
    }
    catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

}
