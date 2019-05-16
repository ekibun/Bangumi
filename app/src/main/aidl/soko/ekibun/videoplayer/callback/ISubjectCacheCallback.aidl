package soko.ekibun.videoplayer.callback;

import soko.ekibun.videoplayer.bean.SubjectCache;

interface ISubjectCacheCallback {
    oneway void onFinish(in SubjectCache result);
    oneway void onReject(String reason);
}