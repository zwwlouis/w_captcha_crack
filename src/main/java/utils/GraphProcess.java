package utils;

import java.util.Map;

/**
 * @author WeiWang Zhang
 * @date 2017/10/13.
 * @time 16:46.
 * 图像处理相关操作
 */
public class GraphProcess {
    /**
     * 拉普拉斯变换（边缘提取）
     * @param gray
     * @return
     */
    public static int[][] laplace(int[][] gray) {
        int width = gray.length;
        int height = gray[0].length;
        double[][] mask = {{0.0, -1.0, 0.0}, {-1.0, 4.0, -1.0}, {0.0, -1.0, 0.0}};
        return filter(mask, gray, width, height,20);
    }

    /**
     * 高斯变换（平滑处理）
     * @param gray
     * @return
     */
    public static int[][] gaussian(int[][] gray){
        int width = gray.length;
        int height = gray[0].length;
        double[][] mask = {{1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}};
        return filter(mask, gray, width, height,0);
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

    /**
     * 匹配滑块位置
     * @param img 背景边缘图
     * @param block 滑块边缘图
     * @return
     */
    public static int matchBlock(int[][] img, int[][] block) {
        int imgW = img.length;
        int imgH = img[0].length;
        int bW = block.length;
        int bH = block[0].length;
        int maxSum = 0;
        int matchPos = 0;
        for (int i = 0; i < imgW - bW + 1; i++) {
            int sum = 0;
            for (int j = 0; j < bW; j++) {
                for (int k = 0; k < bH; k++) {
                    sum += img[i + j][k] * block[j][k];
                }
            }
            if (sum > maxSum) {
                maxSum = sum;
                matchPos = i;
            }
        }
        return matchPos;
    }

    /**
     * 计算图片中心位置，返回数组第一个为X方向中心位置，第二个为Y方向中心位置
     * @param gph
     * @param thres 忽略灰度值小于thres的像素点
     * @return
     */
    public static int[] findCenter(int[][] gph,int thres){
        int width = gph.length;
        int height = gph[0].length;
        int pixNum = 0;
        int xSum = 0;
        int ySum = 0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if(gph[i][j] > thres){
                    xSum += i;
                    ySum += j;
                    pixNum++;
                }
            }
        }
        int[] center = new int[2];
        center[0] = xSum/pixNum;
        center[1] = ySum/pixNum;
        return center;
    }

    /**
     * 删除滑块中的黑点
     * @param gph
     */
    public static void clearInnerSpot(int[][] gph){
        for (int i = 1; i < gph.length-1; i++) {
            for (int j = 1; j < gph[0].length-1; j++) {
                int sum = 0;
                sum += gph[i+1][j];
                sum += gph[i-1][j];
                sum += gph[i][j+1];
                sum += gph[i][j-1];
                if(sum >= (255*3)){
                    gph[i][j] = 255;
                }
            }
        }
    }

    /**
     * 对灰度图像做二值处理，大于阈值的为255，小于阈值的为0
     * @param gray
     * @param thres
     * @return
     */
    public static void twoValue(int[][] gray, int thres){
        if (gray == null){
            return;
        }
        int width = gray.length;
        int height = gray[0].length;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (gray[i][j] > thres) {
                    gray[i][j] = 255;
                } else {
                    gray[i][j] = 0;
                }
            }
        }
    }


    /**
     * 生成一个常量矩阵
     * @param val
     * @return
     */
    public static int[][] constantImg(int wid,int hei, int val){
        int[][] con = new int[wid][hei];
        for (int i = 0; i < wid; i++) {
            for (int j = 0; j < hei; j++) {
                con[i][j] = val;
            }
        }
        return con;
    }




}
