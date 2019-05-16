package soko.ekibun.videoplayer.callback;

import soko.ekibun.videoplayer.bean.VideoCache;

interface IVideoCacheCallback {
    oneway void onFinish(in VideoCache result);
    oneway void onReject(String reason);
}