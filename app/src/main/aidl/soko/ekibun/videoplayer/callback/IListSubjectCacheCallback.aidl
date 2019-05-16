package soko.ekibun.videoplayer.callback;

import soko.ekibun.videoplayer.bean.SubjectCache;

interface IListSubjectCacheCallback {
    oneway void onFinish(in List<SubjectCache> result);
    oneway void onReject(String reason);
}