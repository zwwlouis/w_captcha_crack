package expForErr;

import utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author WeiWang Zhang
 * @date 2017/10/11.
 * @time 20:19.
 * 对于识别错误的图片进行存储处理
 */
public class ErrorCatch {
    private int index;
    public final static String ERROR_IMG_PATH = "src/main/resources/failed/";
    private String dateString = "";
    public final static String picExt = ".png";
    public final static String IMG_NAME = "img";
    public final static String BLOCK_NAME = "blcok";
    public final static String IMG_LAPLACE_NAME = "img-laplace";
    public final static String BLOCK_LAPLACE_NAME = "block-laplace";

    public ErrorCatch() {
        //获得当前日期格式化信息
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        dateString = format.format(new Date());
        //根据日期创建路径
        String filePath = ERROR_IMG_PATH + dateString;
        File dir = new File(filePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        //获得该路径下所有文件
        File[] list = dir.listFiles();
        //当前文件序号为文件数量+1
        index = list.length + 1;
    }

    /**
     * 存储错误图片，
     *
     * @param moveX        错误计算的移动距离
     * @param img          背景图
     * @param block        滑块图
     * @param imgLaplace   拉普拉斯变换后的背景图
     * @param blockLaplace 拉普拉斯变换后的滑块图
     */
    public void saveError(int moveX, BufferedImage img, BufferedImage block, BufferedImage imgLaplace, BufferedImage blockLaplace) {
        //根据当前的存储序号，新建文件夹
        String filePath = ERROR_IMG_PATH + dateString + "/" + index;
        File dir = new File(filePath);
        if (!dir.exists()) {
            dir.mkdir();
        }
        //依次存储四张图片
        ImageUtils.writeImg(img, filePath + "/" + ErrorCatch.IMG_NAME + picExt);
        ImageUtils.writeImg(block, filePath + "/" + ErrorCatch.BLOCK_NAME + picExt);
        //在判断错误的位置上画一条红色竖线
        ImageUtils.drawVerticleLine(imgLaplace, moveX, Color.red);
        ImageUtils.writeImg(imgLaplace, filePath + "/" + ErrorCatch.IMG_LAPLACE_NAME + picExt);
        ImageUtils.writeImg(blockLaplace, filePath + "/" + ErrorCatch.BLOCK_LAPLACE_NAME + picExt);
        //存储完成后增加文件序号
        index++;
    }


}
