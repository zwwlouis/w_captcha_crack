package crack;

import expForErr.ErrorCatch;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import utils.ImageUtils;
import utils.PageOperation;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 完美世界滑动验证码破解
 */
public class PWCaptchaCrack {
    private static String IMG_DIV_CLASS = "mCaptchaImgDiv";
    private static String MOVE_BTN_CLASS = "mCaptchaSlideBorder";
    private static Integer[][] offsetMat = new Integer[40][2];
    private static int col = 20;
    private static int row = 2;
    private static boolean imgSave = false;
    //验证码操作总次数
    private static int opNum = 200;
    private static String INDEX_URL = "http://captchas.wanmei.com/demo/mCaptcha/forMachine?capType=embed";
    private static int totalNum = 0; //总次数
    private static int successNum = 0;//成功次数
    private static int interceptNum = 0;//被学习算法拦截次数
    private static int errNum = 0;//识别错误次数
    private static PageOperation po;
    private static ErrorCatch ec;
    private static String ERROR_IMG_PATH = "src/main/resources/failed/";


    public static void main(String[] args) throws InterruptedException {
        long start = System.currentTimeMillis();
        po = new PageOperation();
        ec = new ErrorCatch();
        //打开网页
        po.openPage(INDEX_URL);
        while (totalNum < opNum) {
            System.out.println("第 " + (totalNum+1) + " 次验证:");
            if (totalNum != 0) {
                //如果不是第一次，则先进行刷新操作
                po.refreshPage();
            }
            try {
                //破解验证码
                crackCaptcha();
            } catch (IOException e) {
                e.printStackTrace();
            }catch (Exception e) {
                e.printStackTrace();
            }
            DateFormat format = new SimpleDateFormat("dd日 HH:mm:ss");
            System.out.printf("当前时间为 %s\n", format.format(new Date()));
        }
        System.out.println("共计尝试 " + totalNum + " 次滑动验证码");
        System.out.println("成功破解 " + successNum + " 次");
        System.out.printf("被识别为机器操作 %d 次\n",interceptNum);
        System.out.printf("识别错误 %d 次\n",errNum);
//        System.out.printf("成功率为 %.1f %%\n", successNum * 100.0 / totalNum);
        po.closePage();
        long end = System.currentTimeMillis();
        System.out.printf("总计用时 %.1f s\n", (end - start) / 1000.0);
    }

    /**
     * 滑动验证码破解
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private static void crackCaptcha() throws IOException, InterruptedException {
        //设置进行存储
        setOpSave();
        //最多尝试5次
        for (int i = 0; i < 5; i++) {
            if(totalNum >= opNum) {
                break;
            }
            //识别验证码计数
            totalNum++;
            if (i != 0) {
                //如果不是第一次，则点击验证码的刷新按钮
                WebElement freshButton = po.findByClass("sliderImgRefreshBtn");
                po.click(freshButton);
                //等待2s图片加载
                Thread.sleep(2000);
            }
            //获得原始图片
            String url = po.findImgUrl(IMG_DIV_CLASS);
            String originImg = po.downloadImg(url,"origin-img");
            //获得图片偏移矩阵
            offsetMat = po.getOffsetMat(IMG_DIV_CLASS, row, col);
            //获得剪切图片阵列
            BufferedImage[] imgSlices = po.getImgSlices(originImg, offsetMat, 13,60,imgSave,"imgSlice");
            //拼接图片
            BufferedImage bgImg = po.mergeImg(imgSlices, row, col, imgSave, "final-image");
            //获得滑块图片
            BufferedImage blockImg = ImageUtils.getCutPic(originImg, 260, 0, 61, 120);
            //计算滑动距离
            int moveX = po.getMoveDis(bgImg, blockImg, imgSave);

            //找到滑块按钮
            WebElement moveBtn = po.findByClass(MOVE_BTN_CLASS);
            //移动滑块,补偿+11
            int shiftX = moveX + 11;
            System.out.println("需要移动的距离为：" + shiftX);

            po.comMove(moveBtn, shiftX);

            String text = "";
            for (int j = 0; j < 3; j++) {
                By alertTextBy = By.cssSelector(".sliderImgAlert p.text");
                WebElement alertText = po.findElement(alertTextBy);
                text = alertText.getAttribute("innerHTML");
                if (text.contains("验证通过")) {
                    //验证成功
                    successNum++;
                    System.out.println("验证通过");
                    Thread.sleep(1000);
                    return;
                }
                Thread.sleep(1000);
            }
            //重复读取三遍还没有出现成功信息则判定为失败，根据失败类型做操作
            if (text != null && !text.trim().isEmpty()) {
                System.out.println(text);
                if (text.contains("次数过多")) {
                    System.out.println("失败：操作次数过多，需要刷新");
                } else if (text.contains("机器操作")) {
                    interceptNum++;
                    System.out.println("失败：被识别为机器操作");
                } else if (text.contains("验证失败")) {
                    errNum++;
                    System.out.println("失败：识别图片出错！");
                    //识别出现错误，记录出错图片
                    ec.saveError(moveX, bgImg, blockImg, po.bg_laplace, po.block_laplace);
                }
            }else{
                System.out.println("失败：未知错误！");
            }
        }
    }

    public static void setOpSave() {
        /**
         * 该页面只有在id=opSave的checkbox勾选后才会进行数据存储
         * 该checkbox只为机器可见，避免人为操作被误打了机器标签
         */
        By by = By.cssSelector("#opSave");
        WebElement checkbox = po.findElement(by);
        String js = "$(\"#opSave\").click()";
        //如果选项框未选中则进行点击
        if (!checkbox.isSelected()) {
            po.executeJs(js);
        }
        //        System.out.println(checkbox.isSelected());
    }
}
