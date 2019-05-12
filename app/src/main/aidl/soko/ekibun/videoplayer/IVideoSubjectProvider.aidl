package soko.ekibun.videoplayer;

import soko.ekibun.videoplayer.bean.VideoSubject;
import soko.ekibun.videoplayer.bean.VideoEpisode;

import soko.ekibun.videoplayer.callback.ISubjectCallback;
import soko.ekibun.videoplayer.callback.IListEpisodeCallback;

interface IVideoSubjectProvider{
    void refreshSubject(in VideoSubject subject, in ISubjectCallback callback);
    void refreshEpisode(in VideoSubject subject, in IListEpisodeCallback callback);
}