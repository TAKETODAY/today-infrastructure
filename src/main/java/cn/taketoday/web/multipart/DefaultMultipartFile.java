/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.multipart;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import javax.servlet.http.Part;

import cn.taketoday.web.exception.BadRequestException;

/**
 * @author TODAY <br>
 *         2018-06-28 22:40:32
 */
@SuppressWarnings("serial")
public class DefaultMultipartFile implements MultipartFile {

    private final Part part;
    public static final int BUFFER_SIZE = 4096;

    public DefaultMultipartFile(Part part) throws BadRequestException {
        if (part == null) {
            throw new BadRequestException("There isn't a file item, bad request.");
        }
        this.part = part;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return part.getInputStream();
    }

    @Override
    public String getContentType() {
        return part.getContentType();
    }

    @Override
    public long getSize() {
        return part.getSize();
    }

    /**
     * Gets the name of this part
     *
     * @return The name of this part as a <tt>String</tt>
     */
    @Override
    public String getName() {
        return part.getName();
    }

    /**
     * Return the original filename in the client's filesystem.
     */
    @Override
    public String getFileName() {
        return part.getSubmittedFileName();
    }

    /**
     * Save file to given file destination
     * 
     * @param dest
     *            File Destination
     * @throws IOException
     */
    @Override
    public void save(File dest) throws IOException {

        // fix #3 Upload file not found exception
        File parentFile = dest.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }

        part.write(dest.getAbsolutePath());
    }

    @Override
    public boolean isEmpty() {
        return part.getSize() == 0;
    }

    @Override
    public byte[] getBytes() throws IOException {

        try (final InputStream in = getInputStream()) {
            if (in == null) {
                return new byte[0];
            }
            final ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_SIZE);
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead = -1;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            return out.toByteArray();
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(part);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj //
               || (part instanceof DefaultMultipartFile && part.equals(((DefaultMultipartFile) obj).part));
    }

    @Override
    public Object getOriginalResource() {
        return part;
    }

    @Override
    public void delete() throws IOException {
        part.delete();
    }

}
