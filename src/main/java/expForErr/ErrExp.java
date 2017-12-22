package expForErr;

import utils.GraphProcess;
import utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.Buffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author WeiWang Zhang
 * @date 2017/10/12.
 * @time 10:28.
 * 对之前识别错误的图片做再处理，以求找到更好的识别算法
 */
public class ErrExp {
    public final static String EXP_BASE_PATH = "src/main/resources/exp/";
    private static BufferedImage img;
    private static BufferedImage block;
    public final static String TWO_VALUE_IMG = "two-value-img";
    public final static String IMG_LAPLACE = "img-laplace";
    public final static String TWO_VALUE_BLOCK = "two-value-block";
    public final static String BLOCK_LAPLACE = "block-laplace";
    public final static DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    public static void main(String[] args) {
        //设置开始检查的时间,该时间之前的文件自动忽略
        String startExpDateStr = "2017-10-25";
        Date startExpDate, fileDate;
        try {
            startExpDate = format.parse(startExpDateStr);
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }
        //打开错误存储路径
        File dir = new File(ErrorCatch.ERROR_IMG_PATH);
        if (!dir.exists()) return;
        //找到所有日期包
        File[] datePackages = dir.listFiles();
        for (File dateDir : datePackages) {
            String errDate = dateDir.getName();
            try {
                fileDate = format.parse(errDate);
            } catch (ParseException e) {
                System.out.println("文件日期名有误！");
                e.printStackTrace();
                continue;
            }
            if(fileDate.before(startExpDate)) continue;
            System.out.println("测试数据日期：" + errDate);
            //在日期包下找到所有的错误包
            File[] errPackages = dateDir.listFiles();
            for (File errDir : errPackages) {
                int index = Integer.parseInt(errDir.getName());
                System.out.println("序号：" + index);
                String savePath = EXP_BASE_PATH + errDate + "/" + index + "/";
                File img_file = findFile(errDir, ErrorCatch.IMG_NAME + ErrorCatch.picExt);
                File block_file = findFile(errDir, ErrorCatch.BLOCK_NAME + ErrorCatch.picExt);
                if (img_file == null || block_file == null) {
                    System.out.println("未找到文件：" + errDir.getPath());
                    break;
                }
                img = ImageUtils.getPic(img_file);
                block = ImageUtils.getPic(block_file);

                int[][] imgGray = ImageUtils.getGray(img);
//                GraphProcess.twoValue(imgGray, 235);
                int[][] imgGrayLa = GraphProcess.laplace(imgGray);


                int[][] blockGray = ImageUtils.getGray(block);
                //对滑块做纯色处理
                GraphProcess.twoValue(blockGray, 1);
                //清除滑块中间的黑点
                GraphProcess.clearInnerSpot(blockGray);
                //提取滑块边缘
                int[][] blockGrayLa = GraphProcess.laplace(blockGray);

                int[] center = GraphProcess.findCenter(blockGray,0);


//                //对滑块做左半边置0操作（nice!）
//                for (int i = 1; i < blockGrayLa.length-1; i++) {
//                    for (int j = 1; j < blockGrayLa[0].length-1; j++) {
//                        if(i<blockGrayLa.length*0.7){
//                            blockGrayLa[i][j] = 0;
//                        }
//                    }
//                }
                //切割滑块左上部分
                int[][] topCutter = GraphProcess.constantImg(center[0]+15,center[1]-10,-255);
                int[][] leftCutter = GraphProcess.constantImg(center[0]-10,center[1]+15,-255);
                ImageUtils.overlayImg(blockGrayLa,topCutter,0,0,0);
                ImageUtils.overlayImg(blockGrayLa,leftCutter,0,0,0);

                //对滑块做高斯滤波操作（nice!）
                blockGrayLa = GraphProcess.gaussian(blockGrayLa);



                int posX = GraphProcess.matchBlock(imgGrayLa,blockGrayLa);


                BufferedImage imgGrayLaBF = ImageUtils.generateGrayImage(imgGrayLa);
                //在滑块的计算位置上画上红线
                ImageUtils.drawVerticleLine(imgGrayLaBF,posX, Color.red);

                //将滑块画在原图上
                ImageUtils.overlayImg(imgGrayLa,blockGrayLa,posX,0,0);
                //存储
                ImageUtils.writeImg(imgGray, savePath + TWO_VALUE_IMG + ErrorCatch.picExt);
                ImageUtils.writeImg(imgGrayLaBF, savePath + IMG_LAPLACE + ErrorCatch.picExt);
                //画出滑块中心
                ImageUtils.writeImg(ImageUtils.drawCross(ImageUtils.generateGrayImage(blockGray),center[0],center[1]), savePath + TWO_VALUE_BLOCK + ErrorCatch.picExt);
                ImageUtils.writeImg(blockGrayLa, savePath + BLOCK_LAPLACE + ErrorCatch.picExt);
                ImageUtils.writeImg(imgGrayLa,savePath + IMG_LAPLACE +"_withBlock"+ ErrorCatch.picExt);
            }
        }
    }

    private static File findFile(File dir, final String FileName) {
        File[] fList = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.equals(FileName);
            }
        });
        if (fList == null || fList.length == 0) {
            return null;
        } else {
            return fList[0];
        }
    }
}
