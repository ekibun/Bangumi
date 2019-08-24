package com.awarmisland.android.richedittext.bean;

/**
 * span 样式记录
 */
public class SpanPart extends FontStyle {
    public int start;
    public int end;

    public SpanPart(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public SpanPart(FontStyle fontStyle) {
        this.isBold = fontStyle.isBold;
        this.isItalic = fontStyle.isItalic;
        this.isStrike = fontStyle.isStrike;
        this.isMask = fontStyle.isMask;
        this.isUnderline = fontStyle.isUnderline;
        this.fontSize = fontStyle.fontSize;
        this.color = fontStyle.color;
    }
}