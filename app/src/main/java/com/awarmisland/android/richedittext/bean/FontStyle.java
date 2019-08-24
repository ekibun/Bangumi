package com.awarmisland.android.richedittext.bean;

/**
 * 字体样式
 */
public class FontStyle {

    public final static int NORMAL = 16;
    public final static int SMALL = 14;
    public final static int BIG = 18;

    public final static String BLACK = "#FF212121";
    public final static String GREY = "#FF878787";
    public final static String RED = "#FFF64C4C";
    public final static String BLUE = "#FF007AFF";

    public boolean isBold;
    public boolean isItalic;
    public boolean isUnderline;
    public boolean isStrike;
    public boolean isMask;
    public int fontSize;
    public int color;

}