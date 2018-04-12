package com.yhj.web.utils;

import java.io.File;
import java.io.InputStream;

public final class FileUtils extends org.apache.commons.io.FileUtils{

	public static boolean saveFileByInputStream(InputStream is, String savePath) {
		File file = new File(savePath);
		try {
			FileUtils.copyInputStreamToFile(is, file);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
