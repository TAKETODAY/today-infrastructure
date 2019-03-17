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
package test.web.demo.controller;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import cn.taketoday.web.RequestMethod;
import cn.taketoday.web.annotation.Controller;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.Multipart;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.multipart.MultipartFile;

/**
 * 
 * @author Today <br>
 *         2018-10-27 10:10
 */
@Controller
public final class FileController extends BaseController {

	private static final long serialVersionUID = -8675595052486971501L;

	public static final int IMG_WIDTH = 70;
	public static final int IMG_HEIGHT = 28;

	private static final String RAND_CODE = "randCode";

	@RequestMapping(value = "/file", method = RequestMethod.GET)
	public String index(HttpServletRequest request, HttpSession session) {
		return "/file/upload";
	}

	@ResponseBody
	@RequestMapping(value = { "/upload" }, method = RequestMethod.POST)
	public String upload(HttpServletRequest request, //
			HttpSession session, @Multipart MultipartFile uploadFile) throws IOException //
	{
		System.out.println(uploadFile.getContentType());

		String upload = "D:/www.yhj.com/webapps/upload/";

		String path = upload + uploadFile.getFileName();

		File file = new File(path);
		uploadFile.save(file);

		return "/upload/" + uploadFile.getFileName();
	}

	@ResponseBody
	@RequestMapping(value = { "/upload/multi" }, method = RequestMethod.POST)
	public String multiUpload(HttpServletRequest request, HttpSession session, HttpServletResponse response,
			@Multipart Set<MultipartFile> files) throws IOException {

		String upload = "D:/taketoday.cn/webapps/upload/";

		for (MultipartFile multipartFile : files) {

			String path = upload + multipartFile.getFileName();

			File file = new File(path);
			System.out.println(path);
			if (!multipartFile.save(file)) {
				return "<script>alert('upload error !')</script>";
//				response.getWriter().print("<script>alert('upload error !')</script>");
			}
		}
//		response.getWriter().print("<script>alert('upload success !')</script>");
		return "<script>alert('upload success !')</script>";
	}

	@RequestMapping(value = { "/download" }, method = RequestMethod.GET)
	public File download(String path) {

		return new File(path);
	}

	@GET("/display")
	public final BufferedImage display(HttpServletRequest request, HttpServletResponse response) throws IOException {

		response.setContentType("image/jpeg");
		return ImageIO.read(new File("D:/WebSite/data/doc/upload/logo.png"));
	}

	@GET("/void/captcha")
	public final void captcha_void(HttpServletRequest request, HttpServletResponse response) throws IOException {

		BufferedImage image = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics graphics = image.getGraphics();
		// 1.设置背景色
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);

		Graphics2D graphics2d = (Graphics2D) graphics;
		drawRandomNum(graphics2d, request);
		response.setContentType("image/jpeg");
		ImageIO.write(image, "jpg", response.getOutputStream());

		return;
	}

	@GET("captcha")
	public final BufferedImage captcha(HttpServletRequest request, HttpServletResponse response) throws IOException {

		BufferedImage image = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics graphics = image.getGraphics();
		// 1.设置背景色
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);

		Graphics2D graphics2d = (Graphics2D) graphics;
		drawRandomNum(graphics2d, request);

		return image;
	}

	private final void drawRandomNum(Graphics2D graphics, HttpServletRequest request) {
		String randCode = getCheckCode();
		request.getSession().setAttribute(RAND_CODE, randCode.toLowerCase());
		graphics.setColor(Color.RED);
		graphics.setFont(new Font(DEFAULT_FONT, Font.PLAIN, 25));
		int x = 2;
		Random rand = new Random();
		char code[] = randCode.toCharArray();
		for (int i = 0; i < 4; i++) {
			int degree = rand.nextInt() % 15;
			graphics.rotate(degree * Math.PI / 180, x, 20);// 设置旋转角度
			graphics.drawString(code[i] + "", x, 23);
			graphics.rotate(-degree * Math.PI / 180, x, 20);// 归位
			x += 16;
		}
	}

	public String getCheckCode() {
		String randCode = "";
		char[] code = "abcdefghijklmnopqrstuvwxyzABCD0123456789EFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
		Random rand = new Random();
		for (int i = 0; i < 4; i++) {
			randCode = randCode + code[rand.nextInt(72)];
		}
		rand = null;
		return randCode;
	}

}
