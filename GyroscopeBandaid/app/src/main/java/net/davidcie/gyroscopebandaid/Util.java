package net.davidcie.gyroscopebandaid;

public class Util {
    public static String printArray(float[] array) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            builder.append(" [");
            builder.append(i);
            builder.append("]=");
            builder.append(array[i]);
        }
        return builder.toString().trim();
    }

    /**
     * Low-pass filter implementation.
     *
     * @see <a href="http://en.wikipedia.org/wiki/Low-pass_filter#Algorithmic_implementation">Algorithmic_implementation</a>
     * Alpha is the time smoothing constant for the low-pass filter. 0 <= alpha <= 1;
     * a smaller value basically means more smoothing. All credits go to Thom Nichols;
     * see <a href="http://blog.thomnichols.org/2011/08/smoothing-sensor-data-with-a-low-pass-filter">his article</a>
     * and <a href="http://stackoverflow.com/a/5780505/1121352">StackOverflow answer</a>.
     */
    static float lowPass(float alpha, float current, float prev) {

        return prev + alpha * (current - prev);
    }

    static float[][] resizeSecondDimension(float[][] currentFilter, int newSize) {
        if (currentFilter == null || newSize < 1) return currentFilter;

        int firstDimension = currentFilter.length;
        if (firstDimension == 0) return currentFilter;

        int secondDimension = currentFilter[0].length;
        if (secondDimension == newSize) return currentFilter;

        float newArray[][] = new float[firstDimension][newSize];
        for (int a = 0; a < firstDimension; a++) {
            System.arraycopy(
                    currentFilter[a], 0,
                    newArray[a], 0,
                    Math.min(newSize, secondDimension));
        }

        return newArray;
    }

    public static boolean isValidFloat(Object value, Float minValue, Float maxValue) {
        if (value == null) return false;
        Float test = Float.valueOf(value.toString());
        return !(test.isNaN() || test.isInfinite() || test < minValue || test > maxValue);
    }

    public static Float round(Float number, int decimalPlaces) {
        String rounded = String.format("%." + decimalPlaces + "f", number);
        return Float.valueOf(rounded);
    }
}