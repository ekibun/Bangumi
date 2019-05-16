package soko.ekibun.videoplayer;

import soko.ekibun.videoplayer.bean.VideoEpisode;
import soko.ekibun.videoplayer.bean.VideoSubject;
import soko.ekibun.videoplayer.bean.VideoCache;
import soko.ekibun.videoplayer.bean.SubjectCache;

import soko.ekibun.videoplayer.callback.IListSubjectCacheCallback;
import soko.ekibun.videoplayer.callback.ISubjectCacheCallback;
import soko.ekibun.videoplayer.callback.IVideoCacheCallback;

interface IDownloadCacheProvider {
    void getCacheList(String site, in IListSubjectCacheCallback callback);
    void getSubjectCache(in VideoSubject subject, in ISubjectCacheCallback callback);
    void getEpisodeCache(in VideoSubject subject, in VideoEpisode episode, in IVideoCacheCallback callback);
}