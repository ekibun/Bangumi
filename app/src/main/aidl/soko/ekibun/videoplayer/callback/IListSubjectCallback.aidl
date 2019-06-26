package soko.ekibun.videoplayer.callback;

import soko.ekibun.videoplayer.bean.VideoSubject;

interface IListSubjectCallback {
    oneway void onFinish(in List<VideoSubject> reason);
    oneway void onReject(String reason);
}