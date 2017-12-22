package utils;

/**
 * @author WeiWang Zhang
 * @date 2017/10/26.
 * @time 15:50.
 * 提供滑动行为的模块
 */
public class MoveBehavior {

    /**
     * 生成移动矩阵，矩阵三行分别为 xOffset, yOffset, deltat
     *
     * @param xOffset x方向需要移动的距离
     * @return
     */
    public static long[][] getMoveMat(long xOffset) {
        //y轴方向作10以内的位移
        long yOffset = 5;
        //确定一个随机滑动时间（与滑动距离相关）
        long time = Math.round(1 + xOffset / 100) * 1000;
        //确定移动步数（20—30步）
        int length = (int) Math.round(10 * Math.random() + 20);
        //获取移动矩阵
        //        yOffset = 10;
        //        time = 2000;
        //        length = 25;
        return hesitate(xOffset, yOffset, time, length);
    }

    /**
     * 在平移的基础上画一个大圆
     *
     * @param xOffset
     * @param yOffset
     * @param radius
     * @param time
     * @param step
     * @return
     */
    public static long[][] drawCycle(long xOffset, long yOffset, long radius, long time, int step) {
        //得到基础移动矩阵
        long[][] baseMat = randomMove(xOffset, yOffset, time, step);
        double angDelta = 2 * Math.PI / step;
        //        double[] theta = new double[step];
        double theta = 0;
        for (int i = 0; i < step; i++) {
            theta += angDelta;
            baseMat[0][i] += Math.round(-radius * Math.sin(theta));
            baseMat[1][i] += Math.round(radius - radius * Math.cos(theta));
        }
        return baseMat;
    }

    /**
     * 模仿x轴方向反复犹豫的行为
     *
     * @param xOffset x方向需要移动的距离
     * @param time    移动的花费时间
     * @param length  移动的次数
     * @return
     */
    public static long[][] hesitate(long xOffset, long yOffset, long time, int length) {
        long[][] moveMat = new long[3][];
        double[] param = new double[2];
        double amp = 2 * xOffset / length;
        double rad = 1.3 * Math.PI;
        moveMat[0] = getOffsetSequence(xOffset, length, "cos", amp, rad,0.0);
        moveMat[1] = getOffsetSequence(yOffset, length, "random");
        moveMat[2] = getTimeSequence(time, length);
        return moveMat;
    }

    /**
     * 模仿x轴方向先快后慢的方式
     *
     * @param xOffset x方向需要移动的距离
     * @param time    移动的花费时间
     * @param length  移动的次数
     * @return
     */
    public static long[][] slowToFast(long xOffset, long yOffset, long time, int length) {
        long[][] moveMat = new long[3][];
        double[] param = new double[2];
        double amp = 1.1 * xOffset / length;
        double rad = 2 * Math.PI;
        double alpha = Math.PI;
        moveMat[0] = getOffsetSequence(xOffset, length, "cos", amp, rad,alpha);
        moveMat[1] = getOffsetSequence(yOffset, length, "uniform");
        moveMat[2] = getTimeSequence(time, length);
        return moveMat;
    }

    /**
     * y轴方向震荡移动
     *
     * @param xOffset x方向需要移动的距离
     * @param time    移动的花费时间
     * @param length  移动的次数
     * @return
     */
    public static long[][] yVibrate(long xOffset, long yOffset, long time, int length) {
        long[][] moveMat = new long[3][];
        double[] param = new double[2];
        double amp = 5; //幅度
        double rad = (2 + 4 * Math.random()) * Math.PI; //弧度,振动1~3个周期
        moveMat[0] = getOffsetSequence(xOffset, length, "uniform");
        moveMat[1] = getOffsetSequence(yOffset, length, "cos", amp, rad);
        moveMat[2] = getTimeSequence(time, length);
        return moveMat;
    }

    /**
     * 随机移动
     *
     * @param xOffset x方向需要移动的距离
     * @param time    移动的花费时间
     * @param length  移动的次数
     * @return
     */
    public static long[][] randomMove(long xOffset, long yOffset, long time, int length) {
        long[][] moveMat = new long[3][];
        double[] param = new double[2];
        moveMat[0] = getOffsetSequence(xOffset, length, "random");
        moveMat[1] = getOffsetSequence(yOffset, length, "uniform");
        moveMat[2] = getTimeSequence(time, length);
        return moveMat;
    }


    /**
     * 得到时间序列
     *
     * @param totalTime
     * @param length
     * @return
     */
    public static long[] getTimeSequence(long totalTime, int length) {
        long[] timeSeq = new long[length];
        double timeSum = 0;
        long delta = totalTime / length;
        for (int i = 1; i < length; i++) {
            timeSeq[i] = delta;
        }
        return timeSeq;
    }

    /**
     * 得到位移序列
     *
     * @param distance
     * @return
     */
    public static long[] getOffsetSequence(long distance, int length, String way, double... paramArray) {
        long[] offset = new long[length];
        long sumDis = 0;
        long delta = distance / length;
        switch (way) {
            case "uniform":
                for (int i = 0; i < length; i++) {
                    offset[i] = delta;
                    sumDis += delta;
                }
                break;
            case "cos":
                //paramArray共两位，分别为0-幅度  1-总弧度 2-轴向偏移量
                double amp = paramArray[0];
                double rad = paramArray[1] / length;
                double alpha = paramArray[2];
                for (int i = 0; i < length; i++) {
                    offset[i] = Math.round(amp * Math.cos(rad * i-alpha));
                    sumDis += offset[i];
                }
                break;
            case "random":
                //随机移动方式
                for (int i = 0; i < length; i++) {
                    offset[i] = Math.round(2 * Math.random() * delta);
                    sumDis += offset[i];
                }
                break;
            default:
                //默认为均匀变化
                for (int i = 0; i < length; i++) {
                    offset[i] = delta;
                    sumDis += delta;
                }
                break;
        }
        //查看还缺多少距离，然后均匀添加到每一个offset上
        long lack = distance - sumDis;
        delta = lack / length;
        long lackOflack = lack - delta * length;
        if (lackOflack < 0) {
            lackOflack += length;
            delta -= 1;
        }
        for (int i = 0; i < length; i++) {
            if (i < lackOflack) {
                offset[i] += delta + 1;
            } else {
                offset[i] += delta;
            }
        }
        return offset;
    }
}
