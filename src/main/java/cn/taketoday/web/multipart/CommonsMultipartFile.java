/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.fileupload.FileItem;

import cn.taketoday.web.exception.InternalServerException;

/**
 * Commons file upload implement.
 * 
 * @author Today <br>
 * 
 *         2018-07-11 15:48:00
 */
@SuppressWarnings("serial")
public class CommonsMultipartFile implements MultipartFile {

    private final FileItem fileItem;

    /**
     * Create an instance wrapping the given FileItem.
     * 
     * @param fileItem
     *            the FileItem to wrap
     */
    public CommonsMultipartFile(FileItem fileItem) {
        this.fileItem = fileItem;
    }

    public final FileItem getFileItem() {
        return this.fileItem;
    }

    @Override
    public String getName() {
        return this.fileItem.getFieldName();
    }

    @Override
    public String getContentType() {
        return this.fileItem.getContentType();
    }

    @Override
    public boolean isEmpty() {
        return (this.getSize() == 0);
    }

    @Override
    public long getSize() {
        return this.fileItem.getSize();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return this.fileItem.getInputStream();
    }

    @Override
    public String getFileName() {
        return fileItem.getName();
    }

    @Override
    public boolean save(File dest) {

        try {

            // fix #3 Upload file not found exception
            File parentFile = dest.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }

            fileItem.write(dest);
            return true;
        } //
        catch (Exception e) {
            throw new InternalServerException("File: [" + getFileName() + "] upload failure.", e);
        }
    }

    @Override
    public byte[] getBytes() {
        return fileItem.get();
    }

    @Override
    public Object getOriginalResource() {
        return fileItem;
    }

    @Override
    public void delete() throws IOException {
        fileItem.delete();
    }

}
