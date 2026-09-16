/*
 * Decompiled with CFR 0.152.
 */
package dev.dedek.greatsage.voice;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NumberSpeech {
    private NumberSpeech() {
    }

    private static String hundreds(int digit) {
        return switch (digit) {
            case 1 -> "num.100";
            case 3 -> "num.300";
            case 6 -> "num.600";
            case 8 -> "num.800";
            default -> null;
        };
    }

    private static String thousands(int digit) {
        return switch (digit) {
            case 1 -> "num.1000";
            case 3 -> "num.3000";
            case 8 -> "num.8000";
            default -> null;
        };
    }

    public static List<String> of(double value) {
        ArrayList<String> out = new ArrayList<String>();
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            out.add("num.0");
            return out;
        }
        long rounded = Math.round(Math.abs(value));
        if (rounded == 0L) {
            out.add("num.0");
            return out;
        }
        long spoken = rounded;
        if (rounded >= 10000L) {
            long scale = 1L;
            while (spoken / scale >= 100L) {
                scale *= 10L;
            }
            spoken = rounded / scale * scale;
            out.add("num.approx");
        }
        long oku = spoken / 100000000L;
        long man = spoken % 100000000L / 10000L;
        long rest = spoken % 10000L;
        if (oku > 0L) {
            NumberSpeech.block(out, (int)oku);
            out.add("num.100000000");
        }
        if (man > 0L) {
            NumberSpeech.block(out, (int)man);
            out.add("num.10000");
        }
        if (rest > 0L) {
            NumberSpeech.block(out, (int)rest);
        }
        return out;
    }

    private static void block(List<String> out, int value) {
        String composed;
        int thousand = value / 1000;
        int hundred = value / 100 % 10;
        int ten = value / 10 % 10;
        int unit = value % 10;
        if (thousand > 0) {
            composed = NumberSpeech.thousands(thousand);
            if (composed != null) {
                out.add(composed);
            } else {
                out.add("num." + thousand);
                out.add("num.1000");
            }
        }
        if (hundred > 0) {
            composed = NumberSpeech.hundreds(hundred);
            if (composed != null) {
                out.add(composed);
            } else {
                out.add("num." + hundred);
                out.add("num.100");
            }
        }
        if (ten > 0) {
            if (ten > 1) {
                out.add("num." + ten);
            }
            out.add("num.10");
        }
        if (unit > 0) {
            out.add("num." + unit);
        }
    }

    public static List<String> percent(double value) {
        List<String> out = NumberSpeech.of(Math.round(value));
        out.add("num.percent");
        return out;
    }

    public static String written(double value) {
        long rounded = Math.round(value);
        if (Math.abs(rounded) >= 1000000L) {
            return String.format(Locale.ROOT, "%.1fM", (double)rounded / 1000000.0);
        }
        if (Math.abs(rounded) >= 1000L) {
            return String.format(Locale.ROOT, "%.1fk", (double)rounded / 1000.0);
        }
        return String.valueOf(rounded);
    }
}

