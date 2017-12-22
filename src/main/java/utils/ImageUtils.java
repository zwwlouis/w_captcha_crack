package utils;

import net.sf.javavp8decoder.imageio.WebPImageReader;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.util.Iterator;

/**
 * @author WeiWang Zhang
 * @date 2017/9/13.
 * @time 14:50.
 */
public class ImageUtils {

    /**
     * 读取图片
     *
     * @param srcFile
     * @return
     */
    public static BufferedImage getPic(String srcFile) {
        return getPic(new File(srcFile));
    }

    public static BufferedImage getPic(File file) {
        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 读取图片+图片预剪切
     *
     * @param srcFile 输入图片源
     * @param x       剪切点x坐标
     * @param y       剪切点y坐标
     * @param width   剪切矩形宽度
     * @param height  剪切矩形高度
     * @return
     */
    public static BufferedImage getCutPic(String srcFile, int x, int y, int width, int height) {
        FileInputStream fis = null;
        ImageInputStream iis = null;
        try {
            if (!new File(srcFile).exists()) {
                return null;
            }
            fis = new FileInputStream(srcFile);
            String ext = srcFile.substring(srcFile.lastIndexOf(".") + 1);
            Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName(ext);
            ImageReader reader = it.next();
            iis = ImageIO.createImageInputStream(fis);
            reader.setInput(iis, true);
            ImageReadParam param = reader.getDefaultReadParam();
            Rectangle rect = new Rectangle(x, y, width, height);
            param.setSourceRegion(rect);
            BufferedImage bi = reader.read(0, param);
            return bi;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                if (iis != null) {
                    iis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 图片拼接 （图片长宽一致）
     *
     * @param imgSlices 要拼接的图片列表
     * @param type      1横向拼接，2 纵向拼接
     */
    public static BufferedImage jointImage(BufferedImage[] imgSlices, int type) {
        //获得要拼接的图片数量
        int length = imgSlices.length;
        int[][] ImageRGB = new int[length][];
        for (int i = 0; i < length; i++) {
            int width = imgSlices[i].getWidth();
            int height = imgSlices[0].getHeight();
            ImageRGB[i] = new int[width * height];
            //获得图片的rgb数据
            imgSlices[i].getRGB(0, 0, width, height, ImageRGB[i], 0, width);
        }
        //拼接后图片的新宽高
        int newHeight = 0;
        int newWidth = 0;
        for (int i = 0; i < length; i++) {
            if (type == 1) {
                newHeight = newHeight > imgSlices[i].getHeight() ? newHeight : imgSlices[i].getHeight();
                newWidth += imgSlices[i].getWidth();
            } else if (type == 2) {// 纵向
                newWidth = newWidth > imgSlices[i].getWidth() ? newWidth : imgSlices[i].getWidth();
                newHeight += imgSlices[i].getHeight();
            }
        }
        // 生成新图片
        BufferedImage ImageNew = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        try {
            int height_i = 0;
            int width_i = 0;
            for (int i = 0; i < length; i++) {
                if (type == 1) {
                    ImageNew.setRGB(width_i, 0, imgSlices[i].getWidth(), newHeight, ImageRGB[i], 0, imgSlices[i].getWidth());
                    width_i += imgSlices[i].getWidth();
                } else if (type == 2) {
                    ImageNew.setRGB(0, height_i, newWidth, imgSlices[i].getHeight(), ImageRGB[i], 0, newWidth);
                    height_i += imgSlices[i].getHeight();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ImageNew;
    }

    /**
     * @param bufferedImage
     * @param outFile       输出图片源
     */
    public static void writeImg(BufferedImage bufferedImage, String outFile) {
        File tempOutFile = new File(outFile);
        if (!tempOutFile.getParentFile().exists()) {
            tempOutFile.getParentFile().mkdirs();
        }
        String ext = outFile.substring(outFile.lastIndexOf(".") + 1).trim();
        try {
            ImageIO.write(bufferedImage, ext, tempOutFile);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 存储灰度图像
     *
     * @param imgGray
     * @param outFile
     */
    public static void writeImg(int[][] imgGray, String outFile) {
        BufferedImage bufferedImage = generateGrayImage(imgGray);
        writeImg(bufferedImage, outFile);
    }


    //得到图片的灰度图像的像素值
    public static int[][] getGray(BufferedImage bufferedImage) {
        if (bufferedImage == null) {
            return new int[0][0];
        }
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        int[][] gray = new int[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int[] fullRgb = new int[3];
                fullRgb[0] = (bufferedImage.getRGB(i, j) & 0xff0000) >> 16;
                fullRgb[1] = (bufferedImage.getRGB(i, j) & 0xff00) >> 8;
                fullRgb[2] = (bufferedImage.getRGB(i, j) & 0xff);
                gray[i][j] = (fullRgb[0] * 19595 + fullRgb[1] * 38469 + fullRgb[2] * 7472) >> 16;
            }
        }
        return gray;
    }

    /**
     * 生成灰度图文件
     *
     * @param pixelArray
     * @return
     */
    public static BufferedImage generateGrayImage(int[][] pixelArray) {
        int width = pixelArray.length;
        if (width <= 0) {
            return null;
        }
        int height = pixelArray[0].length;
        BufferedImage bImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                bImg.setRGB(i, j, (new Color(pixelArray[i][j], pixelArray[i][j], pixelArray[i][j])).getRGB());
            }

        }
        return bImg;
    }

    /**
     * 简单地画出图片
     *
     * @param image
     */
    public static void showImage(Image image) {
        JFrame frame = new JFrame();
        frame.getContentPane().add(new JLabel(new ImageIcon(image)));
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    /**
     * 在指定位置画一条竖线
     *
     * @param img
     * @param dis
     * @param color
     * @return
     */
    public static BufferedImage drawVerticleLine(BufferedImage img, int dis, Color color) {
        int height = img.getHeight();
        for (int i = 0; i < height; i++) {
            img.setRGB(dis, i, color.getRGB());
        }
        return img;
    }

    /**
     * 在指定位置画一条横线
     *
     * @param img
     * @param dis
     * @param color
     * @return
     */
    public static BufferedImage drawHorizontalLine(BufferedImage img, int dis, Color color) {
        int width = img.getWidth();
        for (int i = 0; i < width; i++) {
            img.setRGB(i, dis, color.getRGB());
        }
        return img;
    }

    public static BufferedImage drawCross(BufferedImage img, int x, int y) {
        drawVerticleLine(img, x, Color.red);
        drawHorizontalLine(img, y, Color.red);
        return img;
    }

    /**
     * 在一张图片上叠加另一张图片
     *
     * @param under  位于下方的图片
     * @param over   位于上方的图片
     * @param startX 叠加的起始X位置
     * @param startY 叠加的起始Y位置
     * @param mode   叠加模式 0-加法  1-减法
     */
    public static void overlayImg(int[][] under, int[][] over, int startX, int startY,int mode) {
        int underWid = under.length;
        int underHei = under[0].length;
        int overWid = over.length;
        int overHei = over[0].length;
        //计算需要作图的高度，超出底图部分不作图
        int drawWidth = ((underWid - startX) > overWid) ? overWid : (underWid - startX);
        int drawHeight = ((underHei - startY) > overHei) ? overHei : (underHei - startY);
        for (int i = 0; i < drawWidth; i++) {
            for (int j = 0; j < drawHeight; j++) {
                int pix;
                if(mode == 0){
                    pix = under[startX + i][startY + j] + over[i][j];
                    pix = (pix > 255) ? 255 : pix;
                    pix = (pix < 0) ? 0 : pix;
                }else{
                    pix = under[startX + i][startY + j] - over[i][j];
                    pix = (pix < 0) ? -pix : pix;
                }
                under[startX + i][startY + j] = pix;
            }
        }
    }

    public static void main(String[] args) {
    }


}
