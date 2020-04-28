package com.awarmisland.android.richedittext.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.Spannable;
import android.text.style.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatEditText;
import com.awarmisland.android.richedittext.bean.FontStyle;
import com.awarmisland.android.richedittext.bean.SpanPart;
import soko.ekibun.bangumi.util.span.MaskSpan;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


/**
 * awarmisland
 * RichEditText 富文本
 */
public class RichEditText extends AppCompatEditText implements View.OnClickListener {

    private int upX = 0;
    private int upY = 0;

    public RichEditText(Context context) {
        super(context);
        initView();
    }

    public RichEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public RichEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        setOnClickListener(this);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            upX = (int) event.getX();
            upY = (int) event.getY();
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onClick(View view) {
        Spannable text = getText();
        int x = upX;
        int y = upY;
        if (text != null) {
            x -= getTotalPaddingLeft();
            y -= getTotalPaddingTop();

            x += getScrollX();
            y += getScrollY();

            Layout layout = getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            ClickableSpan[] link = text.getSpans(off, off,
                    ClickableSpan.class);

            if (link.length != 0) {
                link[0].onClick(this);
            }
        }
    }

    /**
     * public setting
     */
    public FontStyle getFontStyle() {
        return getFontStyle(getSelectionStart(), getSelectionEnd());
    }

    public void setBold(boolean isBold) {
        setStyleSpan(isBold, Typeface.BOLD);
    }

    public void setItalic(boolean isItalic) {
        setStyleSpan(isItalic, Typeface.ITALIC);
    }

    public void setUnderline(boolean isUnderline) {
        setUnderlineSpan(isUnderline);
    }

    public void setStrike(boolean isStreak) {
        setStrikeSpan(isStreak);
    }

    public void setMask(boolean isMask) {
        setMaskSpan(isMask);
    }

//    public void setImage(HtmlTagHandler.ClickableImage clickSpan) {
//        int start = getSelectionStart();
//        SpannableString ss = new SpannableString("￼");
//        ss.setSpan(clickSpan.getImage(), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//        ss.setSpan(clickSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//        getEditableText().insert(start, ss);// 设置ss要添加的位置
//    }

    /**
     * bold italic
     */
    private void setStyleSpan(boolean isSet, int type) {
        FontStyle fontStyle = new FontStyle();
        if (type == Typeface.BOLD) {
            fontStyle.isBold = true;
        } else if (type == Typeface.ITALIC) {
            fontStyle.isItalic = true;
        }
        setSpan(fontStyle, isSet, StyleSpan.class);
    }

    /**
     * underline
     */
    private void setUnderlineSpan(boolean isSet) {
        FontStyle fontStyle = new FontStyle();
        fontStyle.isUnderline = true;
        setSpan(fontStyle, isSet, UnderlineSpan.class);
    }

    /**
     * Strike through
     */
    private void setStrikeSpan(boolean isSet) {
        FontStyle fontStyle = new FontStyle();
        fontStyle.isStrike = true;
        setSpan(fontStyle, isSet, StrikethroughSpan.class);
    }

    private void setMaskSpan(boolean isSet) {
        FontStyle fontStyle = new FontStyle();
        fontStyle.isMask = true;
        setSpan(fontStyle, isSet, MaskSpan.class);
    }

    /**
     * 通用set Span
     */
    private <T> void setSpan(FontStyle fontStyle, boolean isSet, Class<T> tClass) {
        int start = getSelectionStart();
        int end = getSelectionEnd();
        int mode = Spannable.SPAN_EXCLUSIVE_INCLUSIVE;
        T[] spans = getEditableText().getSpans(start, end, tClass);
        //获取
        List<SpanPart> spanStyles = getOldFontSytles(spans, fontStyle);
        for (SpanPart spanStyle : spanStyles) {
            if (spanStyle.start < start) {
                if (start == end) {
                    mode = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE;
                }
                getEditableText().setSpan(getInitSpan(spanStyle), spanStyle.start, start, mode);
            }
            if (spanStyle.end > end) {
                getEditableText().setSpan(getInitSpan(spanStyle), end, spanStyle.end, mode);
            }
        }
        if (isSet) {
            if (start == end) {
                mode = Spannable.SPAN_INCLUSIVE_INCLUSIVE;
            }
            getEditableText().setSpan(getInitSpan(fontStyle), start, end, mode);
        }
    }

    /**
     * 获取当前 选中 spans
     */
    private <T> List<SpanPart> getOldFontSytles(T[] spans, FontStyle fontStyle) {
        List<SpanPart> spanStyles = new ArrayList<>();
        for (T span : spans) {
            boolean isRemove = false;
            if (span instanceof StyleSpan) {//特殊处理 styleSpan
                int style_type = ((StyleSpan) span).getStyle();
                if ((fontStyle.isBold && style_type == Typeface.BOLD)
                        || (fontStyle.isItalic && style_type == Typeface.ITALIC)) {
                    isRemove = true;
                }
            } else {
                isRemove = true;
            }
            if (isRemove) {
                SpanPart spanStyle = new SpanPart(fontStyle);
                spanStyle.start = getEditableText().getSpanStart(span);
                spanStyle.end = getEditableText().getSpanEnd(span);
                spanStyles.add(spanStyle);
                getEditableText().removeSpan(span);
            }
        }
        return spanStyles;
    }

    /**
     * 返回 初始化 span
     */
    private CharacterStyle getInitSpan(FontStyle fontStyle) {
        if (fontStyle.isBold) {
            return new StyleSpan(Typeface.BOLD);
        } else if (fontStyle.isItalic) {
            return new StyleSpan(Typeface.ITALIC);
        } else if (fontStyle.isUnderline) {
            return new UnderlineSpan();
        } else if (fontStyle.isStrike) {
            return new StrikethroughSpan();
        } else if (fontStyle.isMask) {
            MaskSpan span = new MaskSpan();
            span.setTextView(new WeakReference<TextView>(this));
            return span;
        }
        return null;
    }

    /**
     * 获取某位置的 样式
     */
    private FontStyle getFontStyle(int start, int end) {
        FontStyle fontStyle = new FontStyle();
        CharacterStyle[] characterStyles = getEditableText().getSpans(start, end, CharacterStyle.class);
        for (CharacterStyle style : characterStyles) {
            if (style instanceof StyleSpan) {
                int type = ((StyleSpan) style).getStyle();
                if (type == Typeface.BOLD) {
                    fontStyle.isBold = true;
                } else if (type == Typeface.ITALIC) {
                    fontStyle.isItalic = true;
                }
            } else if (style instanceof UnderlineSpan) {
                fontStyle.isUnderline = true;
            } else if (style instanceof StrikethroughSpan) {
                fontStyle.isStrike = true;
            } else if (style instanceof MaskSpan) {
                fontStyle.isMask = true;
            }
        }
        return fontStyle;
    }
}