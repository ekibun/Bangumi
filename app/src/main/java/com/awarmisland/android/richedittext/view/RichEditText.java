package com.awarmisland.android.richedittext.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatEditText;
import com.awarmisland.android.richedittext.bean.FontStyle;
import com.awarmisland.android.richedittext.bean.SpanPart;
import soko.ekibun.bangumi.util.HtmlTagHandler;
import soko.ekibun.bangumi.util.ResourceUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


/**
 * awarmisland
 * RichEditText 富文本
 */
public class RichEditText extends AppCompatEditText implements View.OnClickListener {
    public static final int EXCLUD_MODE = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE;
    public static final int EXCLUD_INCLUD_MODE = Spannable.SPAN_EXCLUSIVE_INCLUSIVE;
    public static final int INCLUD_INCLUD_MODE = Spannable.SPAN_INCLUSIVE_INCLUSIVE;

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
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        //setOnClickListener(this);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Spannable text = getText();
        if (text != null) {
            int action = event.getAction();

            if (action == MotionEvent.ACTION_UP) {
                int x = (int) event.getX();
                int y = (int) event.getY();

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
        return super.onTouchEvent(event);
    }

    @Override
    public void onClick(View view) {

        int start = getSelectionStart() - 1;
        if (start == -1) {
            start = 0;
        }
        if (start < -1) {
            return;
        }
        FontStyle fontStyle = getFontStyle(start, start);
        setBold(fontStyle.isBold);
        setItalic(fontStyle.isItalic);
        setUnderline(fontStyle.isUnderline);
        setStrike(fontStyle.isStrike);
        setMask(fontStyle.isMask);
        //setFontSize(fontStyle.fontSize);
        //setFontColor(fontStyle.color);
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

    public void setFontSize(int size) {
        setFontSizeSpan(size);
    }

    public void setFontColor(int color) {
        setForegroundColor(color);
    }

    public void setImage(HtmlTagHandler.ClickableImage clickSpan) {
        int start = getSelectionStart();
        SpannableString ss = new SpannableString("￼");
        ss.setSpan(clickSpan.getImage(), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(clickSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        getEditableText().insert(start, ss);// 设置ss要添加的位置
    }

    /**
     * bold italic
     *
     * @param isSet
     * @param type
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
     *
     * @param isSet
     */
    private void setUnderlineSpan(boolean isSet) {
        FontStyle fontStyle = new FontStyle();
        fontStyle.isUnderline = true;
        setSpan(fontStyle, isSet, UnderlineSpan.class);
    }

    /**
     * Strikethrough
     *
     * @param isSet
     */
    private void setStrikeSpan(boolean isSet) {
        FontStyle fontStyle = new FontStyle();
        fontStyle.isStrike = true;
        setSpan(fontStyle, isSet, StrikethroughSpan.class);
    }

    private void setMaskSpan(boolean isSet) {
        FontStyle fontStyle = new FontStyle();
        fontStyle.isMask = true;
        setSpan(fontStyle, isSet, HtmlTagHandler.MaskSpan.class);
    }

    /**
     * 设置 字体大小
     *
     * @param size
     */
    private void setFontSizeSpan(int size) {
        if (size == 0) {
            size = FontStyle.NORMAL;
        }
        FontStyle fontStyle = new FontStyle();
        fontStyle.fontSize = size;
        setSpan(fontStyle, true, AbsoluteSizeSpan.class);
    }

    /**
     * 设置字体颜色
     *
     * @param color
     */
    private void setForegroundColor(int color) {
        if (color == 0) {
            color = Color.parseColor(FontStyle.BLACK);
        }
        FontStyle fontStyle = new FontStyle();
        fontStyle.color = color;
        setSpan(fontStyle, true, ForegroundColorSpan.class);
    }

    /**
     * 通用set Span
     *
     * @param fontStyle
     * @param isSet
     * @param tClass
     * @param <T>
     */
    private <T> void setSpan(FontStyle fontStyle, boolean isSet, Class<T> tClass) {
        int start = getSelectionStart();
        int end = getSelectionEnd();
        int mode = EXCLUD_INCLUD_MODE;
        T[] spans = getEditableText().getSpans(start, end, tClass);
        //获取
        List<SpanPart> spanStyles = getOldFontSytles(spans, fontStyle);
        for (SpanPart spanStyle : spanStyles) {
            if (spanStyle.start < start) {
                if (start == end) {
                    mode = EXCLUD_MODE;
                }
                getEditableText().setSpan(getInitSpan(spanStyle), spanStyle.start, start, mode);
            }
            if (spanStyle.end > end) {
                getEditableText().setSpan(getInitSpan(spanStyle), end, spanStyle.end, mode);
            }
        }
        if (isSet) {
            if (start == end) {
                mode = INCLUD_INCLUD_MODE;
            }
            getEditableText().setSpan(getInitSpan(fontStyle), start, end, mode);
        }
    }

    /**
     * 获取当前 选中 spans
     *
     * @param spans
     * @param fontStyle
     * @param <T>
     * @return
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
                if (span instanceof AbsoluteSizeSpan) {
                    spanStyle.fontSize = ((AbsoluteSizeSpan) span).getSize();
                } else if (span instanceof ForegroundColorSpan) {
                    spanStyle.color = ((ForegroundColorSpan) span).getForegroundColor();
                }
                spanStyles.add(spanStyle);
                getEditableText().removeSpan(span);
            }
        }
        return spanStyles;
    }

    /**
     * 返回 初始化 span
     *
     * @param fontStyle
     * @return
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
        } else if (fontStyle.fontSize > 0) {
            return new AbsoluteSizeSpan(fontStyle.fontSize, true);
        } else if (fontStyle.color != 0) {
            return new ForegroundColorSpan(fontStyle.color);
        } else if (fontStyle.isMask) {
            int bgColor = ResourceUtil.INSTANCE.resolveColorAttr(getContext(), android.R.attr.textColorPrimary);
            int colorInv = ResourceUtil.INSTANCE.resolveColorAttr(getContext(), android.R.attr.textColorPrimaryInverse);
            return new HtmlTagHandler.MaskSpan(bgColor, colorInv, new WeakReference<TextView>(this));
        }
        return null;
    }

    /**
     * 获取某位置的  样式
     *
     * @param start
     * @param end
     * @return
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
            } else if (style instanceof AbsoluteSizeSpan) {
                fontStyle.fontSize = ((AbsoluteSizeSpan) style).getSize();
            } else if (style instanceof ForegroundColorSpan) {
                fontStyle.color = ((ForegroundColorSpan) style).getForegroundColor();
            } else if (style instanceof HtmlTagHandler.MaskSpan) {
                fontStyle.isMask = true;
            }
        }
        return fontStyle;
    }
}