/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.resource;

import java.io.IOException;

import cn.taketoday.core.io.Resource;
import cn.taketoday.util.DigestUtils;
import cn.taketoday.util.FileCopyUtils;

/**
 * A {@code VersionStrategy} that calculates an Hex MD5 hashes from the content
 * of the resource and appends it to the file name, e.g.
 * {@code "styles/main-e36d2e05253c6c7085a91522ce43a0b4.css"}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see VersionResourceResolver
 * @since 4.0
 */
public class ContentVersionStrategy extends AbstractVersionStrategy {

  public ContentVersionStrategy() {
    super(new FileNameVersionPathStrategy());
  }

  @Override
  public String getResourceVersion(Resource resource) {
    try {
      byte[] content = FileCopyUtils.copyToByteArray(resource.getInputStream());
      return DigestUtils.md5DigestAsHex(content);
    }
    catch (IOException ex) {
      throw new IllegalStateException("Failed to calculate hash for " + resource, ex);
    }
  }

}
