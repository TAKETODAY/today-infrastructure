package com.yhj.web.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.sun.management.OperatingSystemMXBean;
import com.yhj.web.core.Constant;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

@SuppressWarnings("restriction")
public final class Tools implements Constant {

	private static final long serialVersionUID = -8607909512364691460L;

	/**
	 * @param base64ImgStr
	 *            base64编码字符串
	 * @param outputImgPath
	 *            图片路径-具体到文件
	 * @return
	 * @Description: 将base64编码字符串转换为图片
	 * @Author:
	 * @CreateTime:
	 */
	public static boolean base64Str2Img(String base64ImgStr, final String outputImgPath) {
		try {
			base64ImgStr = base64ImgStr.replace("data:image/png;base64,", "");
			// 解密
			byte[] buf = new BASE64Decoder().decodeBuffer(base64ImgStr);
			// 处理数据
			for (int i = 0; i < buf.length; ++i) {
				if (buf[i] < 0) {
					buf[i] += 256;
				}
			}
			OutputStream out = new FileOutputStream(outputImgPath);
			out.write(buf);
			out.flush();
			out.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * 解码
	 * 
	 * @param str
	 * @return
	 */
	public static String decodeBase64(final String str) throws IOException {
		return new String(new BASE64Decoder().decodeBuffer(str));
	}

	public static void diskInfo() {
		File[] disks = File.listRoots();
		for (File file : disks) {
			System.out.print(file.getPath() + "\t");
			System.out.print("空闲未使用 = " + file.getFreeSpace() / 1024 / 1024 / 1024 + "GB\t");// 空闲空间
			System.out.print("已经使用 = " + file.getUsableSpace() / 1024 / 1024 / 1024 + "GB\t");// 可用空间
			System.out.print("总容量 = " + file.getTotalSpace() / 1024 / 1024 / 1024 + "GB\t");// 总空间
		}
	}

	public static String getCheckCode() {
		String randCode = "";
		char[] code = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
				't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y',
				'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
		Random rand = new Random();
		for (int i = 0; i < 4; i++) {
			randCode = randCode + code[rand.nextInt(72)];
		}
		rand = null;
		return randCode;
	}

	/**
	 * @Description: 根据图片地址转换为base64编码字符串
	 * @Author:
	 * @CreateTime:
	 * @return
	 * @throws IOException
	 */
	public static String getImage2Base64Str(String imgFilePath) throws IOException {
		InputStream inputStream = null;
		byte[] data = null;
		try {
			inputStream = new FileInputStream(imgFilePath);
			data = new byte[inputStream.available()];
			inputStream.read(data);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			inputStream.close();
		}
		// 加密
		return new BASE64Encoder().encode(data);
	}

	/**
	 * 
	 * @return
	 */
	public static String getMemInfo() {
		OperatingSystemMXBean mem = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		return mem.getFreePhysicalMemorySize() / 1024 / 1024 + "MB/" + 
				mem.getTotalPhysicalMemorySize() / 1024 / 1024 + "MB";
	}

	public static String getNowTime() {
		return new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(new Date());
	}

	public static String getTimePath(String[] names) {
		return "/" + names[0] + "/" + names[1] + "/" + names[2] + "/";
	}

	/**
	 * 得到系统信息
	 */
	public static void getSystemInfo() {
		Properties sysProperty = System.getProperties(); // 系统属性
		System.out.println("Java的运行环境版本：" + sysProperty.getProperty("java.version"));
		System.out.println("Java的运行环境供应商：" + sysProperty.getProperty("java.vendor"));
		System.out.println("Java供应商的URL：" + sysProperty.getProperty("java.vendor.url"));
		System.out.println("Java的安装路径：" + sysProperty.getProperty("java.home"));
		System.out.println("Java的虚拟机规范版本：" + sysProperty.getProperty("java.vm.specification.version"));
		System.out.println("Java的虚拟机规范供应商：" + sysProperty.getProperty("java.vm.specification.vendor"));
		System.out.println("Java的虚拟机规范名称：" + sysProperty.getProperty("java.vm.specification.name"));
		System.out.println("Java的虚拟机实现版本：" + sysProperty.getProperty("java.vm.version"));
		System.out.println("Java的虚拟机实现供应商：" + sysProperty.getProperty("java.vm.vendor"));
		System.out.println("Java的虚拟机实现名称：" + sysProperty.getProperty("java.vm.name"));
		System.out.println("Java运行时环境规范版本：" + sysProperty.getProperty("java.specification.version"));
		System.out.println("Java运行时环境规范供应商：" + sysProperty.getProperty("java.specification.vender"));
		System.out.println("Java运行时环境规范名称：" + sysProperty.getProperty("java.specification.name"));
		System.out.println("Java的类格式版本号：" + sysProperty.getProperty("java.class.version"));
		System.out.println("Java的类路径：" + sysProperty.getProperty("java.class.path"));
		System.out.println("加载库时搜索的路径列表：" + sysProperty.getProperty("java.library.path"));
		System.out.println("默认的临时文件路径：" + sysProperty.getProperty("java.io.tmpdir"));
		System.out.println("一个或多个扩展目录的路径：" + sysProperty.getProperty("java.ext.dirs"));
		System.out.println("操作系统的名称：" + sysProperty.getProperty("os.name"));
		System.out.println("操作系统的构架：" + sysProperty.getProperty("os.arch"));
		System.out.println("操作系统的版本：" + sysProperty.getProperty("os.version"));
		System.out.println("文件分隔符：" + sysProperty.getProperty("file.separator")); // 在 unix 系统中是＂／＂
		System.out.println("路径分隔符：" + sysProperty.getProperty("path.separator")); // 在 unix 系统中是＂:＂
		System.out.println("行分隔符：" + sysProperty.getProperty("line.separator")); // 在 unix 系统中是＂/n＂
		System.out.println("用户的账户名称：" + sysProperty.getProperty("user.name"));
		System.out.println("用户的主目录：" + sysProperty.getProperty("user.home"));
		System.out.println("用户的当前工作目录：" + sysProperty.getProperty("user.dir"));
	}

	public static boolean isAjax(HttpServletRequest request) {
		return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
	}

	public static boolean isMoblie(HttpServletRequest request) {
		boolean isMoblie = false;
		String[] mobileAgents = { "iphone", "android", "ipad", "phone", "mobile", "wap", "netfront", "java",
				"opera mobi", "opera mini", "ucweb", "windows ce", "symbian", "series", "webos", "sony", "blackberry",
				"dopod", "nokia", "samsung", "palmsource", "xda", "pieplus", "meizu", "midp", "cldc", "motorola",
				"foma", "docomo", "up.browser", "up.link", "blazer", "helio", "hosin", "huawei", "novarra", "coolpad",
				"webos", "techfaith", "palmsource", "alcatel", "amoi", "ktouch", "nexian", "ericsson", "philips",
				"sagem", "wellcom", "bunjalloo", "maui", "smartphone", "iemobile", "spice", "bird", "zte-", "longcos",
				"pantech", "gionee", "portalmmm", "jig browser", "hiptop", "benq", "haier", "^lct", "320x320",
				"240x320", "176x220", "w3c ", "acs-", "alav", "alca", "amoi", "audi", "avan", "benq", "bird", "blac",
				"blaz", "brew", "cell", "cldc", "cmd-", "dang", "doco", "eric", "hipt", "inno", "ipaq", "java", "jigs",
				"kddi", "keji", "leno", "lg-c", "lg-d", "lg-g", "lge-", "maui", "maxo", "midp", "mits", "mmef", "mobi",
				"mot-", "moto", "mwbp", "nec-", "newt", "noki", "oper", "palm", "pana", "pant", "phil", "play", "port",
				"prox", "qwap", "sage", "sams", "sany", "sch-", "sec-", "send", "seri", "sgh-", "shar", "sie-", "siem",
				"smal", "smar", "sony", "sph-", "symb", "t-mo", "teli", "tim-", "tosh", "tsm-", "upg1", "upsi", "vk-v",
				"voda", "wap-", "wapa", "wapi", "wapp", "wapr", "webc", "winw", "winw", "xda", "xda-",
				"Googlebot-Mobile" };
		if (request.getHeader("User-Agent") != null) {
			String client = request.getHeader("User-Agent");
			for (String mobileAgent : mobileAgents) {
				if (client.toLowerCase().indexOf(mobileAgent) >= 0 && client.toLowerCase().indexOf("windows nt") <= 0
						&& client.toLowerCase().indexOf("macintosh") <= 0) {
					isMoblie = true;
					break;
				}
			}
		}
		return isMoblie;
	}

	/**
	 * @param str
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String IsoToUtf8(String str) throws UnsupportedEncodingException {
		return new String(str.getBytes("iso-8859-1"), "utf-8");
	}

	/*public static void main(String[] args) throws IOException {

		String decodeBase64 = Tools.decodeBase64("ICy5YqxZB1uWSwcVLSNLcA==");
		System.out.println(decodeBase64);
		long start = System.currentTimeMillis();

		System.out.println(System.currentTimeMillis() - start + "ms");
	}*/

	/**
	 * 编码
	 * @param str
	 * @return
	 */
	public static String toBase64Str(String str) throws UnsupportedEncodingException {
		return new BASE64Encoder().encode(str.getBytes("utf-8"));
	}

	/**
	 * @param str
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String urlEncode(String str) throws UnsupportedEncodingException {
		return java.net.URLDecoder.decode(str, "utf-8");
	}

	/**
	 * 将具体时间封装成数组
	 */
	private String times[] = null;

	public Tools() {

	}

	public Tools(String time) {
		this.times = time.split("-");
	}

	public String getDate() {
		return times[2];
	}

	/**
	 * 
	 * @return
	 */
	public static String getDiskInfo() {
		File[] disks = File.listRoots();
		long free = 0;
		long all = 0;
		for (File file : disks) {
			free = free + file.getFreeSpace();
			all = all + file.getTotalSpace();
		}
		return free / 1024 / 1024 / 1024 + "GB/" + all / 1024 / 1024 / 1024 + "GB";
	}

	/**
	 * @param htmlStr
	 * @return
	 */
	public Set<String> getImageAddress(String htmlStr) {
		Set<String> pics = new HashSet<String>();
		String img = "";
		Pattern p_image;
		Matcher m_image;
		// String regEx_img = "<img.*src=(.*?)[^>]*?>"; //图片链接地址
		String regEx_img = "<img.*src\\s*=\\s*(.*?)[^>]*?>";
		p_image = Pattern.compile(regEx_img, Pattern.CASE_INSENSITIVE);
		m_image = p_image.matcher(htmlStr);
		while (m_image.find()) {
			// 得到<img />数据
			img = m_image.group();
			// 匹配<img>中的src数据
			Matcher m = Pattern.compile("src\\s*=\\s*\"?(.*?)(\"|>|\\s+)").matcher(img);
			while (m.find()) {
				pics.add(m.group(1));
			}
		}
		return pics;
	}

	public static String getFirstImagePath(String htmlStr) {
		try {
			// String regEx_img = "<img.*src=(.*?)[^>]*?>"; //图片链接地址
			String regEx_img = "<img.*src\\s*=\\s*(.*?)[^>]*?>";
			Pattern p_image = Pattern.compile(regEx_img, Pattern.CASE_INSENSITIVE);
			Matcher m_image = p_image.matcher(htmlStr);
			m_image.find();
			// 得到<img />数据
			// 匹配<img>中的src数据
			Matcher m = Pattern.compile("src\\s*=\\s*\"?(.*?)(\"|>|\\s+)").matcher(m_image.group());
			m.find();
			return m.group(1);
		} catch (Exception e) {
			return "";
		}
	}



	public static String delHtml(String htmlStr){
		return htmlStr.replaceAll("<[^>]+>", "").replaceAll("\\\\s*|\\t|\\r|\\n", "").replaceAll(" ", "");
	}
	
	public String getMonth() {
		return times[1];
	}

	public String getTime() {
		return times[3];
	}

	public String getYear() {
		return times[0];
	}
}
