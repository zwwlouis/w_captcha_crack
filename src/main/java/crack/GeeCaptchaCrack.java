package crack;

import expForErr.ErrorCatch;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import utils.PageOperation;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author WeiWang Zhang
 * @date 2017/12/20.
 * @time 18:00.
 * 极验验证码破解
 */
public class GeeCaptchaCrack {
    //完整图片对应的class
    private static String FULLIMG_DIV_CLASS = "gt_cut_fullbg_slice";
    private static String FULL_IMAGE_NAME= "origin-full-image";
    //带扣空图片对应的class
    private static String IMG_DIC_CLASS = "gt_cut_bg_slice";
    private static String IMAGE_NAME= "origin-image";
    //拖动按钮对应class
    private static String MOVE_BTN_CLASS = "gt_slider_knob";
    //滑块对应的class
    private static String MOVE_BLOCK_CLASS = "gt_slice";
    //刷新按钮对应的class
    private static String REFRESH_BTN_CLASS = "gt_refresh_button";
    //标志图标对应classk
    private static String SIGN_BLOCK_CLASS = "gt_ajax_tip";
    //验证码状态
    private static int CAPTCHA_STATUS_READY = 0;
    private static int CAPTCHA_STATUS_SUCCESS = 1;
    private static int CAPTCHA_STATUS_FAIL = 2;
    private static int CAPTCHA_STATUS_LOCK = 3;
    private static int CAPTCHA_STATUS_FORBIDDEN = 4;
    private static int CAPTCHA_STATUS_ERROR = 5;


    private static Integer[][] offsetMat = new Integer[52][2];
    private static int col = 26;
    private static int row = 2;
    private static boolean imgSave = false;
    //验证码操作总次数
    private static int opNum = 20;
    private static String INDEX_URL = "https://www.tripadvisor.cn/RegistrationController?flow=core_combined&pid=427&returnTo=%2F&fullscreen=true";
    private static int totalNum = 0; //总次数
    private static int successNum = 0;//成功次数
    private static int interceptNum = 0;//被学习算法拦截次数
    private static int errNum = 0;//识别错误次数
    private static PageOperation po;
    private static ErrorCatch ec;
    private static String RESULT_SAVE_PATH ="src/main/resources/result-geetest/";
    private static String ERROR_IMG_PATH = "src/main/resources/failed/";


    public static void main(String[] args) throws InterruptedException {
        long start = System.currentTimeMillis();
        ec = new ErrorCatch();
        po = new PageOperation();
        po.setSavePath(RESULT_SAVE_PATH);
        //打开网页
        po.openPage(INDEX_URL);
        while (totalNum < opNum) {
            if (totalNum != 0) {
                //如果不是第一次，则先进行页面刷新操作
                po.refreshPage();
            }
            try {
                //破解验证码
                crackCaptcha();
            } catch (IOException e) {
                e.printStackTrace();
            }catch (WebDriverException e) {
                e.printStackTrace();
                //打开网页
                po = new PageOperation();
                po.setSavePath(RESULT_SAVE_PATH);
                po.openPage(INDEX_URL);
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
        //在页面刷新前进行10次尝试
        for (int i = 0; i < 10; i++) {
            if (i != 0) {
                if(getCaptchaStatus(false) == CAPTCHA_STATUS_ERROR){
                    break;
                }else {
                    //找到滑块按钮
                    WebElement moveBtn = po.findByClass(MOVE_BTN_CLASS);
                    po.moveTo(moveBtn);
                    Thread.sleep(500);
                    //如果不是第一次，则点击验证码的刷新按钮
                    WebElement freshButton = po.findByClass(REFRESH_BTN_CLASS);
                    po.click(freshButton);
                    //等待1s图片加载
                    Thread.sleep(500);
                }
            }
            //等待验证码状态为Ready
            if(!waitForReady(1000,4)){
                break;
            }
            //识别验证码计数
            if(totalNum < opNum) {
                totalNum++;
            }else{
                break;
            }
            System.out.printf("开始第 %d次验证",totalNum);
            //获得原始图片
            String url = po.findImgUrl(FULLIMG_DIV_CLASS);
            String originFullImg = po.downloadWebp(url,FULL_IMAGE_NAME);
            //获得带有滑块缺口的图片
            url = po.findImgUrl(IMG_DIC_CLASS);
            String originImg = po.downloadWebp(url,IMAGE_NAME);

            //获得图片偏移矩阵
            offsetMat = po.getOffsetMat(FULLIMG_DIV_CLASS, row, col);

            //剪切拼接完整图片
            BufferedImage[] fullImgSlices = po.getImgSlices(originFullImg, offsetMat,10,58, imgSave,"");
            BufferedImage fullBgImg = po.mergeImg(fullImgSlices, row, col, imgSave,FULL_IMAGE_NAME);

            //剪切拼接带缺口图片
            BufferedImage[] imgSlices = po.getImgSlices(originImg, offsetMat,10,58, imgSave,"");
            BufferedImage bgImg = po.mergeImg(imgSlices, row, col, imgSave,IMAGE_NAME);

            //计算滑动距离
            int moveX = po.getGeeMoveDis(fullBgImg, bgImg, imgSave);

            //找到滑块按钮
            WebElement moveBtn = po.findByClass(MOVE_BTN_CLASS);
            //移动滑块,补偿+11
            int shiftX = moveX-4;
            System.out.println("需要移动的距离为：" + shiftX);
            po.comMove(moveBtn, shiftX);
            //进行5次判断
            int status;
            for (int j = 0; j < 5; j++) {
                status = getCaptchaStatus(false);
                if(status == CAPTCHA_STATUS_SUCCESS){
                    System.out.println("验证成功！");
                    successNum ++;
                    break;
                }else if(status == CAPTCHA_STATUS_FAIL){
                    System.out.println("验证失败！");
                    ec.saveError(moveX, bgImg, fullBgImg, po.delta_gray, null);
                    errNum++;
                    return;
                }else if(status == CAPTCHA_STATUS_FORBIDDEN){
                    System.out.println("被识别为机器操作！");
                    interceptNum++;
                    Thread.sleep(3000);
                    break;
                }else if(status == CAPTCHA_STATUS_ERROR){
                    System.out.println("出现异常！");
                    //出现异常不作数
                    totalNum--;
                    break;
                }
                Thread.sleep(300);
            }
//
        }
    }

    /**
     * 得到验证码当前状态
     * @param print 是否打印结果
     * @return
     */
    private static int getCaptchaStatus(boolean print){
        WebElement checkSign = po.findByClass(SIGN_BLOCK_CLASS);
        String className = checkSign.getAttribute("class");
        String[] classes = className.split(" ");
        String statusName = "";
        if(classes.length > 1){
            statusName = classes[1];
        }
        if(print){
            System.out.printf("当前状态：%s\n", statusName);
        }
        switch(statusName){
            case "gt_ready":
                return CAPTCHA_STATUS_READY;
            case "gt_success":
                return CAPTCHA_STATUS_SUCCESS;
            case "gt_lock":
                return CAPTCHA_STATUS_LOCK;
            case "gt_fail":
                return CAPTCHA_STATUS_FAIL;
            case "gt_forbidden":
                return CAPTCHA_STATUS_FORBIDDEN;
            case "gt_error":
                return CAPTCHA_STATUS_ERROR;
            default:
                return CAPTCHA_STATUS_ERROR;
        }
    }

    /**
     * 等待验证码状态为ready
     * @param timeMilli
     * @param num
     */
    private static boolean waitForReady(long timeMilli, int num){
        for (int i = 0; i < num; i++) {
            if(getCaptchaStatus(false)==CAPTCHA_STATUS_READY){
                return true;
            }
            try {
                Thread.sleep(timeMilli);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

}
