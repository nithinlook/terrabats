package mathutil;

public final class MathFunc {
    public static final double PI = java.lang.Math.PI;

    public static double lerp(double a, double b, double w) {
        return a * (1.0 - w) + b * w;
    }

    public static double clamp(double x, double a, double b) {
        if (a > b) {
            double t = a;
            a = b;
            b = t;
        }
        return java.lang.Math.min(java.lang.Math.max(x, a), b);
    }

    public static int clamp(int x, int a, int b) {
        if (a > b) {
            int t = a;
            a = b;
            b = t;
        }
        return java.lang.Math.min(java.lang.Math.max(x, a), b);
    }

    public static float clamp(float x, float a, float b) {
        if (a > b) {
            float t = a;
            a = b;
            b = t;
        }
        return java.lang.Math.min(java.lang.Math.max(x, a), b);
    }

    public static long clamp(long x, long a, long b) {
        if (a > b) {
            long t = a;
            a = b;
            b = t;
        }
        return java.lang.Math.min(java.lang.Math.max(x, a), b);
    }

    public static double normalize(double x, double a, double b) {
        return (x - a) / (b - a);
    }

    public static double toDegrees(double rad){
        return java.lang.Math.toDegrees(rad);
    }

    public static double toRadians(double deg){
        return java.lang.Math.toRadians(deg);
    }

    public static double toMM(double inches){
        return inches*25.4;
    }
    public static double toInches(double MM){
        return MM/25.4;
    }
}