package com.awarmisland.android.richedittext.bean;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.style.ImageSpan;

public class RichImageSpan extends ImageSpan {
    private Uri mUri;

    public RichImageSpan(Context context, Bitmap b, Uri uri) {
        super(context, b);
        mUri = uri;
    }

    @Override
    public String getSource() {
        return mUri.toString();
    }
}