import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author WeiWang Zhang
 * @date 2017/9/13.
 * @time 14:50.
 */
public class ImageUtils {

    /**
     * 读取图片
     * @param srcFile
     * @return
     */
    public static BufferedImage getPic(String srcFile){
        try {
            return ImageIO.read(new File(srcFile));
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
     * @param files      要拼接的文件列表
     * @param type       1横向拼接，2 纵向拼接
     * @param targetFile 输出文件
     */
    public static void jointImage2(String[] files, int type, String targetFile) {
        //获得要拼接的图片数量
        int length = files.length;
        File[] src = new File[length];
        BufferedImage[] images = new BufferedImage[length];
        int[][] ImageArrays = new int[length][];
        for (int i = 0; i < length; i++) {
            try {
                src[i] = new File(files[i]);
                images[i] = ImageIO.read(src[i]);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            int width = images[i].getWidth();
            int height = images[i].getHeight();
            ImageArrays[i] = new int[width * height];
            ImageArrays[i] = images[i].getRGB(0, 0, width, height, ImageArrays[i], 0, width);
        }
        int newHeight = 0;
        int newWidth = 0;
        for (int i = 0; i < images.length; i++) {
            // 横向
            if (type == 1) {
                newHeight = newHeight > images[i].getHeight() ? newHeight : images[i].getHeight();
                newWidth += images[i].getWidth();
            } else if (type == 2) {// 纵向
                newWidth = newWidth > images[i].getWidth() ? newWidth : images[i].getWidth();
                newHeight += images[i].getHeight();
            }
        }
        if (type == 1 && newWidth < 1) {
            return;
        }
        if (type == 2 && newHeight < 1) {
            return;
        }
        // 生成新图片
        try {
            BufferedImage ImageNew = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            int height_i = 0;
            int width_i = 0;
            for (int i = 0; i < images.length; i++) {
                if (type == 1) {
                    ImageNew.setRGB(width_i, 0, images[i].getWidth(), newHeight, ImageArrays[i], 0, images[i].getWidth());
                    width_i += images[i].getWidth();
                } else if (type == 2) {
                    ImageNew.setRGB(0, height_i, newWidth, images[i].getHeight(), ImageArrays[i], 0, newWidth);
                    height_i += images[i].getHeight();
                }
            }
//            //输出想要的图片
//            ImageIO.write(ImageNew, targetFile.split("\\.")[1], new File(targetFile));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * 图片拼接 （图片长宽一致）
     *
     * @param imgSlices 要拼接的图片列表
     * @param type       1横向拼接，2 纵向拼接
     *
     */
    public static BufferedImage jointImage(BufferedImage[] imgSlices, int type) {
        //获得要拼接的图片数量
        int length = imgSlices.length;
        int[][] ImageRGB = new int[length][];
        int width = imgSlices[0].getWidth();
        int height = imgSlices[0].getHeight();
        for (int i = 0; i < length; i++) {
            ImageRGB[i] = new int[width * height];
            //获得图片的rgb数据
            imgSlices[i].getRGB(0, 0, width, height, ImageRGB[i], 0, width);
        }
        //拼接后图片的新宽高
        int newHeight = 0;
        int newWidth = 0;
        if (type == 1) {
            // 横向
            newHeight = height;
            newWidth = width * length;
        } else if (type == 2) {
            // 纵向
            newHeight = height * length;
            newWidth = width;
        }

        // 生成新图片
        BufferedImage ImageNew = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        try {
            for (int i = 0; i < length; i++) {
                if (type == 1) {
                    ImageNew.setRGB(width * i, 0, width, newHeight, ImageRGB[i], 0, width);
                } else if (type == 2) {
                    ImageNew.setRGB(0, height * i, newWidth, height, ImageRGB[i], 0, newWidth);
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
        String ext = outFile.substring(outFile.lastIndexOf(".")+1).trim();
        try {
            ImageIO.write(bufferedImage, ext, tempOutFile);
        } catch (IOException e) {
            e.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
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
        if (width<=0){
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
     * 拉普拉斯变换
     * @param gray
     * @return
     */
    public static int[][] laplace(int[][] gray) {
        int width = gray.length;
        if (width <= 0){
            return null;
        }
        int height = gray[0].length;
        double[][] mask = {{0.0, -1.0, 0.0}, {-1.0, 4.0, -1.0}, {0.0, -1.0, 0.0}};
        return filter(mask, gray, width, height,20);
    }

    /**
     * 图片滤波处理
     *
     * @param mask
     * @param gray
     * @param w
     * @param h
     * @param thres 阈值
     * @return
     */
    public static int[][] filter(double[][] mask, int[][] gray, int w, int h, int thres) {
        int mh = mask.length;
        int mw = mask[1].length;
        double maskSum = 0;
        for (int i = 0; i < mw; i++) {
            for (int j = 0; j < mh; j++) {
                maskSum += mask[i][j];
            }
        }
        int[][] d = new int[w][h];


        for (int i = 0; i < w - mw + 1; i++) {
            for (int j = 0 / 2; j < h - mh + 1; j++) {
                int s = 0;
                for (int m = 0; m < mh; m++) {
                    for (int n = 0; n < mw; n++) {
                        s = s + (int) (mask[m][n] * gray[i + m][j + n]);
                    }
                }
                if (maskSum != 0) s /= maskSum;
                if (s < thres) s = 0;
                if (s > 255) s = 255;
                d[i + mw / 2][j + mh / 2] = s;
            }
        }

        return d;
    }
}
