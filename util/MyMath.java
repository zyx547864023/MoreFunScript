package real_combat.util;

import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

public class MyMath {
    public static final Vector2f ZERO = new Vector2f();
    public static final Random RANDOM = new Random();
    public static double RAD_PER_DEG = 0.01745329251;
    public static double DEG_PER_RAD = 57.29577951308232;
    public static final float DEVIATION = 1.0E-4F;

    public MyMath() {
    }

    public static double pow(double a, double b) {
        double r = 1.0;
        int exp = (int)b;

        for(double base = a; exp != 0; exp >>= 1) {
            if ((exp & 1) != 0) {
                r *= base;
            }

            base *= base;
        }

        double b_faction = b - (double)((int)b);
        long tmp = Double.doubleToLongBits(a);
        long tmp2 = (long)(b_faction * (double)(tmp - 4606921280493453312L)) + 4606921280493453312L;
        return r * Double.longBitsToDouble(tmp2);
    }

    public static double toRadians(double angDeg) {
        return RAD_PER_DEG * angDeg;
    }

    public static double toDegrees(double angRad) {
        return DEG_PER_RAD * angRad;
    }

    public static double sqrt(double a) {
        return pow(a, 0.5);
    }

    public static float clamp(float min, float max, float value) {
        return max < min ? clamp(value, max, min) : Math.max(min, Math.min(max, value));
    }

    public static float clamp01(float value) {
        return clamp(0.0F, 1.0F, value);
    }

    public static float getRandomAngle() {
        return RANDOM.nextFloat() * 360.0F;
    }

    public static float getGaussian() {
        float value = (float)RANDOM.nextGaussian();
        value = clamp(-4.0F, 4.0F, value);
        return value;
    }

    public static float getNormalizedGaussian() {
        float value = (float)RANDOM.nextGaussian();
        value = clamp(-1.0F, 1.0F, value / 4.0F);
        return value;
    }

    public static boolean rollChance(float chance) {
        return rollChance(chance, RANDOM);
    }

    public static boolean rollChance(float chance, Random random) {
        if (chance >= 1.0F) {
            return true;
        } else if (chance <= 0.0F) {
            return false;
        } else {
            return random.nextFloat() < chance;
        }
    }

    public static boolean roll() {
        return RANDOM.nextBoolean();
    }

    public static float lerp(float from, float to, float factor) {
        return (to - from) * factor + from;
    }

    public static float lerpAndStopWhileClose(float from, float to, float factor) {
        float result = lerp(from, to, factor);
        return Math.abs(to - result) < 1.0E-4F ? to : result;
    }

    public static float getEffectLevel(float elapsed, float in, float full, float out) {
        if (elapsed < in) {
            return elapsed / in;
        } else {
            float inAndFull = in + full;
            if (elapsed >= in && elapsed <= inAndFull) {
                return 1.0F;
            } else {
                return elapsed > inAndFull && elapsed < inAndFull + out ? 1.0F - (elapsed - inAndFull) / out : 0.0F;
            }
        }
    }

    public static Vector2f getRandomPointInFan(Vector2f point, float a, float b, float angle, float minRange) {
        float r = (float)((double)a * Math.sqrt((double)RANDOM.nextFloat()));
        float fi = (float)(6.283185307179586 * (double)RANDOM.nextFloat());
        float x = (float)((double)r * Math.cos((double)fi));
        float y = (float)((double)(b / a * r) * Math.sin((double)fi));
        Vector2f result = new Vector2f(x, y);
        VectorUtils.rotate(result, angle);
        return Vector2f.add(point, result, (Vector2f)null);
    }

    public static void main(String[] args) {
        System.out.println("测试从1到1亿，平方根的用时（秒）");
        long startTime = System.currentTimeMillis();
        double[] origin = new double[100000000];
        double[] compared = new double[100000000];
        double total = 0.0;
        double error = 0.0;

        int i;
        for(i = 1; i <= 100000000; ++i) {
            origin[i - 1] = Math.sqrt((double)i);
        }

        long endTime = System.currentTimeMillis();
        System.out.println("普通Math.sqrt:" + (double)(endTime - startTime) / 1000.0);
        startTime = System.currentTimeMillis();

        for(i = 1; i <= 100000000; ++i) {
            compared[i - 1] = sqrt((double)i);
        }

        endTime = System.currentTimeMillis();
        System.out.println("魔法Math.sqrt:" + (double)(endTime - startTime) / 1000.0);

        for(i = 0; i < origin.length; ++i) {
            total += origin[i];
            double variance = compared[i] - origin[i];
            error += Math.abs(variance / origin[i]);
        }

        System.out.println("误差:" + error / (double)origin.length * 100.0 + "%");
    }
}
