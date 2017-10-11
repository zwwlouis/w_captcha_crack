import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PWCaptchaCrack {
    private static String IMG_DIV_CLASS = "mCaptchaImgDiv";
    private static String MOVE_BTN_CLASS = "mCaptchaSlideBorder";
    private static Integer[][] offsetMat = new Integer[40][2];
    private static int col = 20;
    private static int row = 2;
    private static boolean imgSave = true;
    //验证码操作总次数
    private static int opNum = 50;
    private static String INDEX_URL = "http://captchas.wanmei.com/demo/mCaptcha/forMachine?capType=embed";
    private static WebDriver driver;
    private static int totalNum = 0;
    private static int successNum = 0;
    private static List<String> errorPics = new ArrayList<>();
    private static PageOperation po;



    public static void main(String[] args) throws InterruptedException {
        po = new PageOperation();
        //打开网页
        po.openPage(INDEX_URL);
        for (int i = 0; i < opNum; i++) {
            System.out.println("第 " + i + " 次验证:");
            if (i != 0) {
                //如果不是第一次，则先进行刷新操作
                po.refreshPage();
            }
            try {
                //破解验证码
                crackCaptcha();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("共计尝试 "+totalNum+" 次滑动验证码");
        System.out.println("成功破解 "+successNum+" 次");
        System.out.printf("成功率为 %.2f\n",successNum*100.0/totalNum);
        po.closePage();
    }

    /**
     * 滑动验证码破解
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private static void crackCaptcha() throws IOException, InterruptedException {
        //获得原始图片
        String url = po.findImgUrl(IMG_DIV_CLASS);
        String originImg = po.downloadImg(url);
        //获得图片偏移矩阵
        offsetMat = po.getOffsetMat(IMG_DIV_CLASS,row,col);
        //获得剪切图片阵列
        BufferedImage[] imgSlices = po.getImgSlices(originImg, offsetMat, imgSave);
        //拼接图片
        BufferedImage bgImg = po.mergeImg(imgSlices,row,col, imgSave);
        //获得滑块图片
        BufferedImage blockImg =  ImageUtils.getCutPic(originImg, 260, 0, 61, 120);
        //计算滑动距离,补偿+1
        int moveX = po.getMoveDis(bgImg, blockImg, imgSave)+1;
        System.out.println("需要移动的距离为：" + moveX);
        //找到滑块按钮
        WebElement moveBtn = po.findByClass(MOVE_BTN_CLASS);
        //移动滑块
        po.move(moveBtn,moveX);

        for (int i = 0; i < 3; i++) {
            By alertTextBy = By.cssSelector(".sliderImgAlert p.text");
            WebElement alertText = po.findElement(alertTextBy);
            String text = alertText.getAttribute("innerHTML");
            System.out.println(text);
            if (text.contains("验证通过")) {
                //验证成功
                successNum++;
                Thread.sleep(1000);
                return;
            } else if (text.contains("次数过多")) {
                //超过次数，返回刷新
                Thread.sleep(1000);
                return;
            } else if (text.contains("验证失败")) {
                //等待动画结束
            }
            Thread.sleep(1000);
        }
    }

}
