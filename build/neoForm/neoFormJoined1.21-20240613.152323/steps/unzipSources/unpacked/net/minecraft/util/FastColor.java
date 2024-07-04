package net.minecraft.util;

public class FastColor {
    public static int as8BitChannel(float pValue) {
        return Mth.floor(pValue * 255.0F);
    }

    public static class ABGR32 {
        public static int alpha(int pPackedColor) {
            return pPackedColor >>> 24;
        }

        public static int red(int pPackedColor) {
            return pPackedColor & 0xFF;
        }

        public static int green(int pPackedColor) {
            return pPackedColor >> 8 & 0xFF;
        }

        public static int blue(int pPackedColor) {
            return pPackedColor >> 16 & 0xFF;
        }

        public static int transparent(int pPackedColor) {
            return pPackedColor & 16777215;
        }

        public static int opaque(int pPackedColor) {
            return pPackedColor | 0xFF000000;
        }

        public static int color(int pAlpha, int pBlue, int pGreen, int pRed) {
            return pAlpha << 24 | pBlue << 16 | pGreen << 8 | pRed;
        }

        public static int color(int pAlpha, int pPackedColor) {
            return pAlpha << 24 | pPackedColor & 16777215;
        }

        public static int fromArgb32(int p_350301_) {
            return p_350301_ & -16711936 | (p_350301_ & 0xFF0000) >> 16 | (p_350301_ & 0xFF) << 16;
        }
    }

    public static class ARGB32 {
        public static int alpha(int pPackedColor) {
            return pPackedColor >>> 24;
        }

        public static int red(int pPackedColor) {
            return pPackedColor >> 16 & 0xFF;
        }

        public static int green(int pPackedColor) {
            return pPackedColor >> 8 & 0xFF;
        }

        public static int blue(int pPackedColor) {
            return pPackedColor & 0xFF;
        }

        public static int color(int pAlpha, int pRed, int pGreen, int pBlue) {
            return pAlpha << 24 | pRed << 16 | pGreen << 8 | pBlue;
        }

        public static int color(int pRed, int pGreen, int pBlue) {
            return color(255, pRed, pGreen, pBlue);
        }

        public static int multiply(int pPackedColourOne, int pPackedColorTwo) {
            return color(
                alpha(pPackedColourOne) * alpha(pPackedColorTwo) / 255,
                red(pPackedColourOne) * red(pPackedColorTwo) / 255,
                green(pPackedColourOne) * green(pPackedColorTwo) / 255,
                blue(pPackedColourOne) * blue(pPackedColorTwo) / 255
            );
        }

        public static int lerp(float pDelta, int pMin, int pMax) {
            int i = Mth.lerpInt(pDelta, alpha(pMin), alpha(pMax));
            int j = Mth.lerpInt(pDelta, red(pMin), red(pMax));
            int k = Mth.lerpInt(pDelta, green(pMin), green(pMax));
            int l = Mth.lerpInt(pDelta, blue(pMin), blue(pMax));
            return color(i, j, k, l);
        }

        public static int opaque(int pColor) {
            return pColor | 0xFF000000;
        }

        public static int color(int pAlpha, int pColor) {
            return pAlpha << 24 | pColor & 16777215;
        }

        public static int colorFromFloat(float pAlpha, float pRed, float pGreen, float pBlue) {
            return color(
                FastColor.as8BitChannel(pAlpha), FastColor.as8BitChannel(pRed), FastColor.as8BitChannel(pGreen), FastColor.as8BitChannel(pBlue)
            );
        }

        public static int average(int p_350662_, int p_350786_) {
            return color(
                (alpha(p_350662_) + alpha(p_350786_)) / 2,
                (red(p_350662_) + red(p_350786_)) / 2,
                (green(p_350662_) + green(p_350786_)) / 2,
                (blue(p_350662_) + blue(p_350786_)) / 2
            );
        }
    }
}
