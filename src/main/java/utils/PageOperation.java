package utils;

import net.sf.javavp8decoder.imageio.WebPImageReader;
import net.sf.javavp8decoder.imageio.WebPImageReaderSpi;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author WeiWang Zhang
 * @date 2017/10/9.
 * @time 12:05.
 */
public class PageOperation {
    private WebDriver driver;
    private Actions actions;
    private String DEFAULT_ORIGIN_IMG_NAME = "origin-image";
    private String savePath = "src/main/resources/result/";
    private String picExt = ".png";
    //用于临时存储图片变换结果，错误处理时使用
    public BufferedImage bg_laplace;
    public BufferedImage block_laplace;
    public BufferedImage delta_gray;
    private final Pattern URL_PATTERN = Pattern.compile("url\\(\"(.*)\"\\)");
    private final Pattern BG_POS_PATTERN = Pattern.compile(".*background-position: (-*\\d*)px (-*\\d*).*");

    public PageOperation() {
        System.setProperty("webdriver.chrome.driver", "D:/workspace/chromedriver_win32/chromedriver.exe");
        driver = new ChromeDriver();
        actions = new Actions(driver);
        //设置加载等待时间
        driver.manage().timeouts().pageLoadTimeout(-1, TimeUnit.SECONDS);
        driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public void openPage(String url) {
        driver.get(url);
    }

    public void closePage() {
        driver.close();
    }

    public void refreshPage() {
        driver.navigate().refresh();
    }

    /**
     * 通过class找到页面元素
     *
     * @param divClass
     * @return
     */
    public WebElement findByClass(String divClass) {
        By by = By.cssSelector("." + divClass);
        return findElement(by);
    }

    public WebElement findElement(By by) {
        return driver.findElement(by);
    }

    /**
     * 点击目标元素
     *
     * @param element
     */
    public void click(WebElement element) {
        actions.click(element).perform();
    }
    public void moveTo(WebElement element) {
        actions.moveToElement(element).perform();
    }

    /**
     * 在页面中执行指定js代码
     *
     * @param js
     */
    public void executeJs(String js) {
        ((JavascriptExecutor) driver).executeScript(js);
    }

    public WebDriver getDriver() {
        return driver;
    }

    public Actions getActions() {
        return actions;
    }

    /**
     * 获取原始图url
     *
     * @param divClass 包含图片的标签class
     * @return
     */
    public String findImgUrl(String divClass) {
        String url = null;
        By picBy = By.cssSelector("." + divClass);
        WebElement picBlock = driver.findElement(picBy);
        String bgi = picBlock.getCssValue("background-image");
        //        String style = document.select("[class=" + divClass + "]").first().attr("style");
        Matcher matcher = URL_PATTERN.matcher(bgi);
        if (matcher.find()) {
            url = matcher.group(1);
        }
        return url;
    }

    /**
     * 得到原始图片，下载到本地
     *
     * @param url      图片地址
     * @param fileName 下载后图片名称
     * @return
     * @throws IOException
     */
    public String downloadImg(String url, String fileName, String ext) throws IOException {
        if (!ext.startsWith(".")) {
            ext = "." + ext;
        }
        String filePath;
        if (StringUtil.isBlank(fileName)) {
            filePath = savePath + DEFAULT_ORIGIN_IMG_NAME + ext;
        } else {
            filePath = savePath + fileName + ext;
        }
        //信任所有证书
        try {
            CertificationTrusted.trustAllHttpsCertificates();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //将URL图片资源保存至本地文件
        FileUtils.copyURLToFile(new URL(url), new File(filePath));
        return filePath;
    }

    public String downloadImg(String url,String fileName) throws IOException {
        return downloadImg(url, fileName, picExt);
    }

    public String downloadWebp(String url, String fileName) throws IOException {
        String webpPath = downloadImg(url, fileName, ".webp");
        File file = new File(webpPath);
        //获得父路径
        String dir = file.getParent();
        //得到转换的目标文件
        String targetPath = dir + "/" + fileName + picExt;
        //得到dwebp.exe路径
        String dwebp = "E:\\Program Files\\libwebp-0.4.2-windows-x64\\bin\\dwebp.exe";
        String[] args = new String[]{dwebp, webpPath, "-o", targetPath};
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return targetPath;
    }

    /**
     * 获取图片排列的偏移矩阵
     *
     * @param divClass 包含图片的标签class
     * @param row      图片分割行数
     * @param col      图片分割列数
     */
    public Integer[][] getOffsetMat(String divClass, int row, int col) {
        Document document = Jsoup.parse(driver.getPageSource());
        Elements elements = document.select("[class=" + divClass + "]");
        int i = 0;
        Integer[][] offsetMat = new Integer[row * col][2];
        for (Element element : elements) {
            Matcher matcher = BG_POS_PATTERN.matcher(element.toString());
            if (matcher.find()) {
                String width = matcher.group(1);
                String height = matcher.group(2);
                offsetMat[i][0] = Integer.parseInt(width);
                offsetMat[i++][1] = Integer.parseInt(height);
            } else {
                throw new RuntimeException("解析异常");
            }
        }
        return offsetMat;
    }

    /**
     * 根据偏移矩阵对原始图片进行剪切
     *
     * @param imgPath   原始图片路径
     * @param offsetMat 偏移矩阵
     * @param imgSave   是否保存中间结果
     * @param fileName  保存文件名，如果为空则不保存
     * @return
     */
    public BufferedImage[] getImgSlices(String imgPath, Integer[][] offsetMat, int sWidth, int sHeight, boolean imgSave, String fileName) {
        Integer len = offsetMat.length;
        BufferedImage[] biList = new BufferedImage[len];
        for (int i = 0; i < len; i++) {
            biList[i] = ImageUtils.getCutPic(imgPath, -offsetMat[i][0], -offsetMat[i][1], sWidth, sHeight);
        }
        if (imgSave && !StringUtil.isBlank(fileName)) {
            for (int i = 0; i < len; i++) {
                //在本地存储图片
                ImageUtils.writeImg(biList[i], savePath + "slices/" + fileName + "_" + i + picExt);
            }
        }
        return biList;
    }

    /**
     * 合并剪切后的图片
     *
     * @param imgSlices 图片片段列表
     * @param row       拼接行数
     * @param col       拼接列数
     * @param imgSave   是否保存中间结果
     * @param fileName  保存文件名，如果为空则不保存
     * @return
     */
    public BufferedImage mergeImg(BufferedImage[] imgSlices, int row, int col, boolean imgSave, String fileName) {
        //图片行向量
        BufferedImage[] imgRow = new BufferedImage[col];
        //图片列向量
        BufferedImage[] imgCol = new BufferedImage[row];
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                //提取图片阵列的第i行
                imgRow[j] = imgSlices[i * col + j];
            }
            //对第i行进行拼接
            imgCol[i] = ImageUtils.jointImage(imgRow, 1);
            if (imgSave && !StringUtil.isBlank(fileName)) {
                ImageUtils.writeImg(imgCol[i], savePath + fileName + "-row-" + i + picExt);
            }
        }
        BufferedImage resultImg = ImageUtils.jointImage(imgCol, 2);
        if (imgSave && !StringUtil.isBlank(fileName)) {
            ImageUtils.writeImg(resultImg, savePath + fileName + picExt);
        }
        return resultImg;
    }

    /**
     * 得到极验验证码的移动距离
     * @param full 完整图片
     * @param part 带缺口的图片
     * @param isSave 是否保存中间结果
     * @return
     */
    public int getGeeMoveDis(BufferedImage full, BufferedImage part, boolean isSave){
        int[][] fullGray = ImageUtils.getGray(full);
        int[][] partGray = ImageUtils.getGray(part);
        if (isSave) {
            ImageUtils.writeImg(ImageUtils.generateGrayImage(fullGray), savePath + "full-gray" + picExt);
            ImageUtils.writeImg(ImageUtils.generateGrayImage(partGray), savePath + "part-gray" + picExt);
        }
        ImageUtils.overlayImg(fullGray,partGray,0,0,1);
        if(isSave){
            ImageUtils.writeImg(ImageUtils.generateGrayImage(fullGray),savePath+"delta-gray"+picExt);
        }
        int width = fullGray.length;
        int height = fullGray[0].length;
        int distence = 0;
        for (int i = 0; i < width; i++) {
            int sumPixel = 0;
            for (int j = 0; j < height; j++) {
                if(fullGray[i][j]<50){
                    fullGray[i][j] = 0;
                }
                sumPixel += fullGray[i][j];
            }
            if(sumPixel > 400){
                distence = i;
                break;
            }
        }
        delta_gray = ImageUtils.generateGrayImage(fullGray);

        return distence;
    }

    /**
     * 计算滑块激动距离移动
     *
     * @param background 背景图片
     * @param block      滑块图片
     * @param isSave     是否保存中间结果
     * @return
     */
    public int getMoveDis(BufferedImage background, BufferedImage block, boolean isSave) {
        //获得灰度图像
        int[][] bgGrayPixel = ImageUtils.getGray(background);
        int[][] blockGrayPixel = ImageUtils.getGray(block);
        //对滑块做纯色处理
        GraphProcess.twoValue(blockGrayPixel, 1);
        //清除滑块中间的黑点
        GraphProcess.clearInnerSpot(blockGrayPixel);
        if (isSave) {
            ImageUtils.writeImg(ImageUtils.generateGrayImage(bgGrayPixel), savePath + "final-gray" + picExt);
            ImageUtils.writeImg(ImageUtils.generateGrayImage(blockGrayPixel), savePath + "block-gray" + picExt);
        }
        //找到滑块的中心
        int[] center = GraphProcess.findCenter(blockGrayPixel, 0);
        //拉普拉斯变换
        bgGrayPixel = GraphProcess.laplace(bgGrayPixel);
        blockGrayPixel = GraphProcess.laplace(blockGrayPixel);
        bg_laplace = ImageUtils.generateGrayImage(bgGrayPixel);
        block_laplace = ImageUtils.generateGrayImage(blockGrayPixel);
        if (isSave) {
            //保存拉布拉斯变换结果
            ImageUtils.writeImg(bg_laplace, savePath + "final-gray-laplace" + picExt);
            ImageUtils.writeImg(block_laplace, savePath + "block-gray-laplace" + picExt);
        }
        //切割滑块左上部分
        int[][] topCutter = GraphProcess.constantImg(center[0] + 15, center[1] - 10, -255);
        int[][] leftCutter = GraphProcess.constantImg(center[0] - 10, center[1] + 15, -255);
        ImageUtils.overlayImg(blockGrayPixel, topCutter, 0, 0,0);
        ImageUtils.overlayImg(blockGrayPixel, leftCutter, 0, 0,0);
        //对滑块做高斯滤波操作（nice!）
        blockGrayPixel = GraphProcess.gaussian(blockGrayPixel);
        //匹配滑块
        return GraphProcess.matchBlock(bgGrayPixel, blockGrayPixel);
    }

    /**
     * 通过鼠标拖动页面元素
     *
     * @param element
     * @param distance 需要移动的距离
     * @throws InterruptedException
     */
    public void simpleMove(WebElement element, int distance) throws InterruptedException {
        //按下鼠标左键
        actions.clickAndHold(element).perform();

        int xMoveDistance = 0;
        //每次移动的量
        int xStep = 10;
        while (xMoveDistance < distance) {
            if (xMoveDistance + xStep > distance) {
                xStep = distance - xMoveDistance;
            }
            xMoveDistance += xStep;
            actions.moveByOffset(xStep, 0).perform();
            //每0.1s移动一次
            Thread.sleep(100);
        }
        //松开鼠标左键
        actions.release().perform();
    }

    /**
     * 复杂移动方式
     *
     * @param element
     * @param distance
     * @throws InterruptedException
     */
    public void comMove(WebElement element, int distance) throws InterruptedException {
        //按下鼠标左键
        actions.clickAndHold(element).perform();
        //模拟鼠标点击后的停顿
        Thread.sleep((long) (Math.random() * 300) + 100);
        long [][] moveMat = MoveBehavior.getMoveMat(distance);
        //FIXME 验证滑动总距离
        int length = moveMat[0].length;
        long moveSum = 0;
        for (int i = 0; i < length ; i++) {
            moveMouse((int)moveMat[0][i],(int)moveMat[1][i]);
            moveSum += moveMat[0][i];
            Thread.sleep(moveMat[2][i]);
        }
        if(moveSum != distance){
            System.out.println("计算出错！");
        }
        moveMouse(-5,0);
        Thread.sleep(500);
        moveMouse(3,0);
        Thread.sleep(300);
        moveMouse(2,0);
        //模拟鼠标点击后的停顿
        Thread.sleep((long) (Math.random() * 1000));

        //松开鼠标左键
        actions.release().perform();
    }


    /**
     * 移动鼠标
     *
     * @param xOffset x方向位移
     * @param yOffset y方向位移
     */
    public void moveMouse(int xOffset, int yOffset) {
        actions.moveByOffset(xOffset, yOffset).perform();
    }


    public static void main(String[] args) {

        File webp = new File("D:\\test\\result\\origin-image.webp");
        File target = new File("D:\\test\\result\\test.png");

        String webpPath = null;
        String targetPath = null;
        targetPath = target.getAbsolutePath();
        try {
            webpPath = webp.getCanonicalPath();
            targetPath = target.getCanonicalPath();

        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] arg = new String[]{"E:\\Program Files\\libwebp-0.4.2-windows-x64\\bin\\dwebp.exe", webpPath, "-o", targetPath};

        String exeStr = "dwebp " + webpPath + " -o " + targetPath;
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(arg);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(process);

    }

}
