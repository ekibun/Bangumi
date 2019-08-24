package com.awarmisland.android.richedittext.handle;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.*;
import kotlin.Pair;
import soko.ekibun.bangumi.ui.topic.PostAdapter;
import soko.ekibun.bangumi.ui.topic.ReplyDialog;
import soko.ekibun.bangumi.util.HtmlTagHandler;

import java.util.ArrayList;

/**
 * Created by awarmisland on 2018/9/10.
 */
public class CustomHtml {


    /**
     * Option for {@link #toHtml(Spanned, int)}: Wrap consecutive lines of text delimited by '\n'
     * inside &lt;p&gt; elements. {@link BulletSpan}s are ignored.
     */
    public static final int TO_HTML_PARAGRAPH_LINES_CONSECUTIVE = 0x00000000;

    /**
     * The bit which indicates if lines delimited by '\n' will be grouped into &lt;p&gt; elements.
     */
    private static final int TO_HTML_PARAGRAPH_FLAG = 0x00000001;

    private CustomHtml() {
    }

    /**
     * @deprecated use {@link #toHtml(Spanned, int)} instead.
     */
    @Deprecated
    public static String toHtml(Spanned text) {
        return toHtml(text, TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
    }

    public static String toHtml(Spanned text, int option) {
        StringBuilder out = new StringBuilder();
        withinHtml(out, text, option);
        return out.toString();
    }

    private static void withinHtml(StringBuilder out, Spanned text, int option) {
        withinDiv(out, text, 0, text.length(), option);
    }

    private static void withinDiv(StringBuilder out, Spanned text, int start, int end,
                                  int option) {
        int next;
        for (int i = start; i < end; i = next) {
            next = text.nextSpanTransition(i, end, QuoteSpan.class);
            QuoteSpan[] quotes = text.getSpans(i, next, QuoteSpan.class);

            for (QuoteSpan quote : quotes) {
                out.append("<blockquote>");
            }

            withinBlockquote(out, text, i, next, option);

            for (QuoteSpan quote : quotes) {
                out.append("</blockquote>\n");
            }
        }
    }

    private static void withinBlockquote(StringBuilder out, Spanned text, int start, int end,
                                         int option) {
        if ((option & TO_HTML_PARAGRAPH_FLAG) == TO_HTML_PARAGRAPH_LINES_CONSECUTIVE) {
            withinBlockquoteConsecutive(out, text, start, end);
        } else {
            withinBlockquoteIndividual(out, text, start, end);
        }
    }

    private static void withinBlockquoteIndividual(StringBuilder out, Spanned text, int start,
                                                   int end) {
        int next;
        for (int i = start; i <= end; i = next) {
            next = TextUtils.indexOf(text, '\n', i, end);
            if (next < 0) {
                next = end;
            }

            if (next == i) {
                out.append("\n"); //"<br>\n"
            } else {
                withinParagraph(out, text, i, next);
            }

            next++;
        }
    }

    private static void withinBlockquoteConsecutive(StringBuilder out, Spanned text, int start,
                                                    int end) {
        int next;
        for (int i = start; i < end; i = next) {
            next = TextUtils.indexOf(text, '\n', i, end);
            if (next < 0) {
                next = end;
            }

            int nl = 0;
            while (next < end && text.charAt(next) == '\n') {
                nl++;
                next++;
            }
            withinParagraph(out, text, i, next - nl);
            //支持换行
            for (int l = 0; l < nl; l++) {
                out.append("\n"); //"<br>"
            }
        }
    }

    private static void withinParagraph(StringBuilder out, Spanned text, int start, int end) {
        int next;
        for (int i = start; i < end; i = next) {
            next = text.nextSpanTransition(i, end, CharacterStyle.class);
            CharacterStyle[] style = text.getSpans(i, next, CharacterStyle.class);
            for (int j = 0; j < style.length; j++) {
                if (style[j] instanceof StyleSpan) {
                    int s = ((StyleSpan) style[j]).getStyle();

                    if ((s & Typeface.BOLD) != 0) {
                        out.append("[b]");
                    }
                    if ((s & Typeface.ITALIC) != 0) {
                        out.append("[i]");
                    }
                }
                if (style[j] instanceof UnderlineSpan) {
                    out.append("[u]");
                }
                if (style[j] instanceof StrikethroughSpan) {
                    out.append("[s]");
                }
                if (style[j] instanceof PostAdapter.Companion.CustomURLSpan) {
                    out.append("[url=");
                    out.append(((PostAdapter.Companion.CustomURLSpan) style[j]).getUrl());
                    out.append("]");
                }
                if (style[j] instanceof ImageSpan) {

                    String source = ((ImageSpan) style[j]).getSource();
                    if (source == null) {
                        Drawable drawable = ((ImageSpan) style[j]).getDrawable();
                        if (drawable instanceof ReplyDialog.UrlDrawable) {
                            source = ((ReplyDialog.UrlDrawable) drawable).getUrl();
                        }
                    }
                    if (source != null && source.startsWith("/img/smiles/")) {
                        ArrayList<Pair<String, String>> emojiList = ReplyDialog.Companion.getEmojiList();
                        for (Pair<String, String> emojiInfo : emojiList) {
                            if (emojiInfo.getSecond().contains(source)) {
                                source = emojiInfo.getFirst();
                                break;
                            }
                        }
                    }
                    if (source != null && source.startsWith("(")) {
                        out.append(source);
                    } else if (source != null) {
                        out.append("[img]");
                        out.append(source);
                        out.append("[/img]");
                    }

                    // Don't output the dummy character underlying the image.
                    i = next;
                }
                if (style[j] instanceof RelativeSizeSpan) {
                    float sizeEm = ((RelativeSizeSpan) style[j]).getSizeChange();
                    out.append("[size=" + sizeEm + "]");
                }
                if (style[j] instanceof ForegroundColorSpan) {
                    int color = ((ForegroundColorSpan) style[j]).getForegroundColor();
                    out.append(String.format("[color=#%06X]", 0xFFFFFF & color));
                }
                if (style[j] instanceof HtmlTagHandler.MaskSpan) {
                    out.append("[mask]");
                }
            }
            withinStyle(out, text, i, next);
            for (int j = style.length - 1; j >= 0; j--) {
                if (style[j] instanceof HtmlTagHandler.MaskSpan) {
                    out.append("[/mask]");
                }
                if (style[j] instanceof ForegroundColorSpan) {
                    out.append("[/color]");
                }
                if (style[j] instanceof RelativeSizeSpan) {
                    out.append("[/size]");
                }
                if (style[j] instanceof PostAdapter.Companion.CustomURLSpan) {
                    out.append("[/url]");
                }
                if (style[j] instanceof StrikethroughSpan) {
                    out.append("[/s]");
                }
                if (style[j] instanceof UnderlineSpan) {
                    out.append("[/u]");
                }
                if (style[j] instanceof StyleSpan) {
                    int s = ((StyleSpan) style[j]).getStyle();

                    if ((s & Typeface.ITALIC) != 0) {
                        out.append("[/i]");
                    }
                    if ((s & Typeface.BOLD) != 0) {
                        out.append("[/b]");
                    }
                }
            }
        }
    }

    private static void withinStyle(StringBuilder out, CharSequence text, int start, int end) {
        out.append(text.subSequence(start, end));
    }
}