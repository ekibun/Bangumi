package soko.ekibun.videoplayer.callback;

import soko.ekibun.videoplayer.bean.VideoEpisode;

interface IListEpisodeCallback {
    oneway void onFinish(in List<VideoEpisode> result);
    oneway void onReject(String reason);
}