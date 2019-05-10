import 'package:flutter/material.dart';
import 'package:flutter_media_plugin/exo_player_listener.dart';
import 'package:flutter_media_plugin/flutter_media_plugin.dart';
import 'package:flutter_media_plugin/playlist.dart';
import 'package:flutter_media_plugin_example/songs.dart';
import 'package:flutter_media_plugin/audio_player.dart';

import 'package:flutter_media_plugin/video_player.dart';

FlutterMediaPlugin flutterMediaPlugin;

void main() {
  flutterMediaPlugin = FlutterMediaPlugin.initialize();
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  int _position = 0, _audioLength = 0, _percent = 0;
  String _message = "";

  IconData _iconData;
  Function _function;

  Widget _bufferingWidget;

  ExoPlayerListener _exoPlayerListener;
  VideoExoPlayerListener _videoExoPlayerListener;
  Playlist playlist;

  String _assetUri = "assets/videos/birwiBirwi.mp4";
  String _uri = "https://firebasestorage.googleapis.com/v0/b/bodoentertainment-224710.appspot.com/o/videos%2FBaidisina.mp4?alt=media&token=afd3ca71-6f49-4fd5-926c-b8a053c85d27";

  //"https://firebasestorage.googleapis.com/v0/b/bodoentertainment-224710.appspot.com/o/videos%2FBaidisina.mp4?alt=media&token=afd3ca71-6f49-4fd5-926c-b8a053c85d27";

  int _textureId;

  @override
  void initState() {
    super.initState();
    flutterMediaPlugin.audioPlayer.initialize();
    flutterMediaPlugin.videoPlayer.initialize(TypeOfPlace.asset, null);
    flutterMediaPlugin.audioPlayer.playWhenReady
        ? _iconData = Icons.pause
        : _iconData = Icons.play_arrow;

    _exoPlayerListener = ExoPlayerListener(
      onPlayerStateChanged: _onPlayerStateChanged,
      onPlaybackUpdate: _onPlaybackUpdate,
      onBufferedUpdate: _onBufferedUpdate,
      onMediaPeriodCreated: _onMediaPeriodCreated,
      onPlayerStatus: _onPlayerStatus,
    );
    _videoExoPlayerListener =
        VideoExoPlayerListener(onVideoInitialize: _onTextureIdChanged);
    flutterMediaPlugin.videoPlayer.addVideoExoPlayer(_videoExoPlayerListener);

    flutterMediaPlugin.audioPlayer.addExoPlayerListener(
      _exoPlayerListener,
    );
    _setIcons();
    _bufferingWidget = SizedBox(
      height: 0,
      width: 0,
    );

    playlist = new Playlist("New Playlist");

    for (Song s in Samples.songs) {
      playlist.addSong(s);
    }
  }

  @override
  void dispose() {
    super.dispose();
    flutterMediaPlugin.audioPlayer.removeExoPlayerListener(_exoPlayerListener);
    flutterMediaPlugin.videoPlayer.removeVideoExoPlayer(_videoExoPlayerListener);
    print("Main dispose");
  }

  void initializeVideo() {
    flutterMediaPlugin.videoPlayer.initialize(TypeOfPlace.network, _uri);
  }

  void _setIcons() {
    if (flutterMediaPlugin.audioPlayer.playWhenReady) {
      _iconData = Icons.pause;
      _function = flutterMediaPlugin.audioPlayer.pause;
    } else {
      _iconData = Icons.play_arrow;
      _function = flutterMediaPlugin.audioPlayer.play;
    }
  }

  void _onTextureIdChanged(int textureId) {
    _textureId = textureId;
    print("Texture id $_textureId");
    setState(() {});
  }

  void _onPlayerStatus(String message) {
    _message = message;
    if (!mounted) return;

    setState(() {});
  }

  void _onPlayerStateChanged(bool playWhenReady, int playbackState) {
    if (!mounted) return;

    setState(() {
      if (playbackState == C.STATE_BUFFERING) {
        _bufferingWidget = Center(
          child: CircularProgressIndicator(),
        );
      } else {
        _bufferingWidget = SizedBox(
          height: 0,
          width: 0,
        );
      }
      _setIcons();
    });
  }

  void _onPlaybackUpdate(int position, int audioLength) {
    _position = position;
    _audioLength = audioLength;

    if (!mounted) return;

    setState(() {});
  }

  void _onBufferedUpdate(int percent) {
    _percent = percent;

    if (!mounted) return;

    setState(() {});
  }

  String currentlyPlayingSongTitle = "";

  void _onMediaPeriodCreated(int windowIndex) {
    print(
        "window index : $windowIndex, length ${flutterMediaPlugin.audioPlayer.playlist.getSize()}");
    Song song = flutterMediaPlugin.audioPlayer.playlist.getSongAtIndex(windowIndex);
    if (song == null)
      currentlyPlayingSongTitle = "No Song";
    else
      currentlyPlayingSongTitle = song.title;
    if (!mounted) return;
    _audioLength = 0;
    _position = 0;
    setState(() {});
  }

  void _seekTo(double percent) {
    int position = (percent * _audioLength).toInt();
    flutterMediaPlugin.audioPlayer.seekTo(position);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
          actions: <Widget>[
            IconButton(
              icon: Icon(Icons.playlist_add),
              onPressed: () {
                initializeVideo();
                //_audioPlayer.setPlaylistAndSongIndex(playlist, 0);
              },
            ),
          ],
        ),
        body: ListView(
          children: <Widget>[
            Padding(
              padding: const EdgeInsets.all(10.0),
              child: Row(
                children: <Widget>[
                  Text(
                    "${(_position ~/ 1000) ~/ 60}:${(_position ~/ 1000) % 60}",
                    textAlign: TextAlign.center,
                  ),
                  Expanded(
                    flex: 4,
                    child: Slider(
                      value: _audioLength != 0
                          ? (_position / _audioLength)
                              .toDouble()
                              .clamp(0.0, 1.0)
                          : 0.0,
                      onChanged: (_value) => _seekTo(_value),
                    ),
                  ),
                  Text(
                    "${(_audioLength ~/ 1000) ~/ 60}:${(_audioLength ~/ 1000) % 60}",
                    textAlign: TextAlign.center,
                  ),
                ],
              ),
            ),
            _bufferingWidget,
            Padding(
              padding: const EdgeInsets.all(10.0),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  RaisedButton(
                    child: Icon(Icons.skip_previous),
                    onPressed: () {
                      flutterMediaPlugin.audioPlayer.skipToPrevious();
                    },
                  ),
                  RaisedButton(
                      child: Icon(_iconData),
                      onPressed: () {
                        _function();
                      }),
                  RaisedButton(
                      child: Icon(Icons.skip_next),
                      onPressed: () {
                        flutterMediaPlugin.audioPlayer.skipToNext();
                      }),
                ],
              ),
            ),
            Center(
              child: Text(currentlyPlayingSongTitle),
            ),
            Center(
              child: RaisedButton(
                onPressed: () {
                  flutterMediaPlugin.audioPlayer.setRepeatMode(C.REPEAT_MODE_ONE);
                },
                child: Text("Repeat one"),
              ),
            ),
            ListView.builder(
              shrinkWrap: true,
              physics: ClampingScrollPhysics(),
              itemBuilder: (BuildContext context, int index) {
                return ListTile(
                  title: Text(
                    '${Samples.songs[index].title}',
                  ),
                  onTap: () {
                    flutterMediaPlugin.audioPlayer.setPlaylistAndSongIndex(playlist, index);
                  },
                  onLongPress: () {
                    flutterMediaPlugin.audioPlayer.addAndPlay(Samples.songs[index]);
                  },
                );
              },
              itemCount: Samples.songs.length,
            ),
            Container(
              child: Text(_message),
            ),
            Container(
              height: 600,
              width: 50,
              color: Colors.red,
              child: _textureId != null
                  ? AspectRatio(
                      aspectRatio: flutterMediaPlugin.videoPlayer.aspectRatio,
                      child: Texture(textureId: _textureId),
                    )
                  : Container(
                      color: Colors.blue,
                    ),
            ),
            Wrap(
              alignment: WrapAlignment.center,
              children: <Widget>[
                RaisedButton(
                  child: Icon(Icons.play_arrow),
                  onPressed: () {
                    flutterMediaPlugin.videoPlayer.play();
                  },
                ),
                RaisedButton(
                  child: Icon(Icons.pause),
                  onPressed: () {
                    flutterMediaPlugin.videoPlayer.pause();
                  },
                ),
              ],
            )
          ],
        ),
      ),
    );
  }
}
