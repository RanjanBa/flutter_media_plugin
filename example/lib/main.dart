import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_media_plugin/exo_player_listener.dart';
import 'package:flutter_media_plugin/flutter_media_plugin.dart';
import 'package:flutter_media_plugin/playlist.dart';
import 'package:flutter_media_plugin/audio_player.dart';
import 'package:flutter_media_plugin/download_manager.dart';
import 'package:flutter_media_plugin/download.dart';
import 'package:flutter_media_plugin/video_player.dart';
import 'package:flutter_media_plugin/media/song.dart';
import 'package:flutter_media_plugin/utility.dart';
import 'package:flutter_media_plugin_example/downloadedMediaListTile.dart';
import 'package:flutter_media_plugin_example/songs.dart';
import 'package:flutter_media_plugin/media/media.dart';

AudioPlayer _audioPlayer;
VideoPlayer _videoPlayer;
DownloadManager _downloadManager;

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  FlutterMediaPlugin();

  _audioPlayer = FlutterMediaPlugin.audioPlayer;
  _videoPlayer = FlutterMediaPlugin.videoPlayer;
  _downloadManager = FlutterMediaPlugin.downloadManager;

  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final _animatedListKey = GlobalKey<AnimatedListState>();
  final _downloadedAnimatedListKey = GlobalKey<AnimatedListState>();

  int _position = 0, _audioLength = 0, _bufferPercent = 0;
  String _message = "";

  IconData _iconData;
  Function _function;

  Widget _bufferingWidget;
  String currentlyPlayingSongTitle = "";

  ExoPlayerListener<Song> _exoPlayerListener;
  DownloadManagerListener _downloadListener;
  VideoExoPlayerListener _videoExoPlayerListener;
  List<Download<Song>> _downloadedSongs = List();

  String _videoUrl =
      "https://firebasestorage.googleapis.com/v0/b/bodoentertainment-224710.appspot.com/o/videos%2Ftest.mp4?alt=media&token=d66bb13d-b9aa-4a2e-b572-59b63fdb1b6b";

  int _textureId;
  double _aspectRatio = 1.0;
  int _repeatMode = 0;
  bool _shuffleModeEnabled = false;

  @override
  void initState() {
    super.initState();
    //flutterMediaPlugin.videoPlayer.initialize(TypeOfPlace.asset, null);
    _audioPlayer.playWhenReady
        ? _iconData = Icons.pause
        : _iconData = Icons.play_arrow;

    _exoPlayerListener = ExoPlayerListener(
      onPlayerStateChanged: _onPlayerStateChanged,
      onPlaybackUpdate: _onPlaybackUpdate,
      onBufferedUpdate: _onBufferedUpdate,
      onTracksChanged: _onTracksChanged,
      onPlayerStatus: _onPlayerStatus,
      onRepeatModeChanged: _onRepeatModeChanged,
      onShuffleModeEnabledChanged: _onShuffleModeEnabledChanged,
      onPlaylistChanged: (Playlist<Song> playlist) {
        for (int i = _audioPlayer.playlistSize - 1; i >= 0; i--) {
          _animatedListKey.currentState.removeItem(i,
              (BuildContext context, Animation animation) {
            return SlideTransition(
              position: animation.drive(
                Tween(
                  begin: Offset(1, 0),
                  end: Offset(0, 0),
                ),
              ),
              child: Container(
                color: Colors.red,
                height: 60,
              ),
            );
          }, duration: Duration(milliseconds: 1000));
        }

        if (playlist != null) {
          for (int i = 0; i < playlist.getSize(); i++) {
            _animatedListKey.currentState.insertItem(i);
          }
        }
      },
      onMediaAddedToPlaylist: (String playlistName, int index, Song song) {
//        print("main: added song index: $index");
        _animatedListKey.currentState.insertItem(index);
      },
    );
    _audioPlayer.addExoPlayerListener(
      _exoPlayerListener,
    );

    _downloadListener = DownloadManagerListener(onInitialized: () {
      for (int i = 0; i < _downloadedSongs.length; i++) {
        _downloadedAnimatedListKey.currentState.removeItem(
          i,
          (context, animation) {
            return SlideTransition(
              position: animation
                  .drive(Tween(begin: Offset(1, 0), end: Offset(0, 0))),
              child: ListTile(
                title: Text(_downloadedSongs[i].media.title),
                trailing: Icon(Icons.delete),
              ),
            );
          },
        );
      }

      _downloadedSongs.clear();
      for (int i = 0;
          i < FlutterMediaPlugin.downloadManager.downloadedSongSize;
          i++) {
        _downloadedAnimatedListKey.currentState.insertItem(i);
        _downloadedSongs
            .add(FlutterMediaPlugin.downloadManager.downloadedSongAtIndex(i));
      }
      print("set state");
      setState(() {
      });
    }, onDownloadAdded: (index, download) {
      _downloadedAnimatedListKey.currentState.insertItem(index);
    }, onDownloadRemoved: (index, download) {
      _downloadedAnimatedListKey.currentState.removeItem(
        index,
        (context, animation) {
          return SlideTransition(
            position:
                animation.drive(Tween(begin: Offset(1, 0), end: Offset(0, 0))),
            child: ListTile(
              title: Text(download.media.title),
              trailing: Icon(Icons.delete),
            ),
          );
        },
      );
    });

    _downloadManager.addDownloadListener(_downloadListener);

    _videoExoPlayerListener = VideoExoPlayerListener(
        onTextureIdChanged: _onTextureIdChanged,
        onSurfaceSizeChanged: _onSurfaceSizeChanged);
    _videoPlayer.addVideoExoPlayer(_videoExoPlayerListener);

    _setIcons(_audioPlayer.playWhenReady);
    _bufferingWidget = SizedBox(
      height: 0,
      width: 0,
    );
  }

  @override
  void dispose() {
    super.dispose();
    _audioPlayer.removeExoPlayerListener(_exoPlayerListener);
    if (_downloadManager != null) {
      _downloadManager.removeDownloadListener(_downloadListener);
    }
    _videoPlayer.removeVideoExoPlayer(_videoExoPlayerListener);
//    print("Main dispose");
  }

  void _setIcons(bool isPlaying) {
    if (isPlaying) {
      _iconData = Icons.pause;
      _function = _audioPlayer.pause;
    } else {
      _iconData = Icons.play_arrow;
      _function = _audioPlayer.play;
    }
  }

  void _onTextureIdChanged(int textureId) {
    _textureId = textureId;
//    print("Texture id $_textureId");
    setState(() {});
  }

  void _onSurfaceSizeChanged(int width, int height) {
    _aspectRatio = width / height;
    setState(() {});
  }

  void _onPlayerStateChanged(bool playWhenReady, int playbackState) {
    if (!mounted) return;

    setState(() {
      if (playbackState == ExoPlayerUtility.STATE_BUFFERING) {
        _bufferingWidget = Center(
          child: CircularProgressIndicator(),
        );
      } else {
        _bufferingWidget = SizedBox(
          height: 0,
          width: 0,
        );
      }
      _setIcons(playWhenReady);
    });
  }

  void _onPlaybackUpdate(int position, int audioLength) {
    _position = position;
    _audioLength = audioLength;

    if (!mounted) return;

    setState(() {});
  }

  void _onBufferedUpdate(int percent) {
    _bufferPercent = percent;

    if (!mounted) return;

    setState(() {});
  }

  void _onTracksChanged(
      int windowIndex, int nextWindowIndex, Song _currentPlayingSong) {
//    print("window index : $windowIndex");
    if (!mounted) return;
    _audioLength = 0;
    _position = 0;

    if (_currentPlayingSong == null)
      currentlyPlayingSongTitle = "No Song";
    else
      currentlyPlayingSongTitle = _currentPlayingSong.title;

    setState(() {});
  }

  void _onRepeatModeChanged(int repeatMode, int nextWindowIndex) {
    setState(() {
      _repeatMode = repeatMode;
    });
  }

  void _onShuffleModeEnabledChanged(
      bool shuffleModeEnabled, int nextWindowIndex) {
    setState(() {
      _shuffleModeEnabled = shuffleModeEnabled;
    });
  }

  void _onPlayerStatus(String message) {
    _message = message;
    if (!mounted) return;

    setState(() {});
  }

  void _seekTo(double percent) {
    int position = (percent * _audioLength).toInt();
    _audioPlayer.seekTo(position);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
          actions: <Widget>[
            IconButton(
              icon: Icon(Icons.playlist_play),
              onPressed: () {
                Playlist<Song> playlist = new Playlist<Song>("New Playlist");
                Samples.songs.forEach((s) => playlist.addMedia(s));
                _audioPlayer.setPlaylist(playlist);
              },
            ),
            IconButton(
              icon: Icon(Icons.stop),
              onPressed: () {
                _audioPlayer.stop();
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
                      _audioPlayer.skipToPrevious();
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
                        _audioPlayer.skipToNext();
                      }),
                ],
              ),
            ),
            Center(
              child: Text(currentlyPlayingSongTitle),
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: <Widget>[
                RaisedButton(
                  onPressed: () async {
                    int cMode = _audioPlayer.repeatMode;
                    _audioPlayer.setRepeatMode((cMode + 1) % 3);
                  },
                  child: Text("RepeatMode Change"),
                ),
                Text("repeat mode : $_repeatMode")
              ],
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: <Widget>[
                RaisedButton(
                  onPressed: () async {
                    bool cShuffle = _audioPlayer.shuffleModeEnabled;
                    _audioPlayer.setShuffleModeEnabled(!cShuffle);
                  },
                  child: Text("ShuffleMode Change"),
                ),
                Text("shuffle mode : $_shuffleModeEnabled"),
              ],
            ),
            Center(
              child: Text(
                "Playlist",
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 20.0),
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
                    _audioPlayer.addSong(Samples.songs[index],
                        shouldPlay: true);
                  },
                  trailing: IconButton(
                    icon: Icon(Icons.file_download),
                    onPressed: () {
                      _downloadManager.download(
                          MediaType.song, Samples.songs[index]);
                    },
                  ),
                );
              },
              itemCount: Samples.songs.length,
            ),
            Container(
              alignment: Alignment.center,
              padding: const EdgeInsets.all(10.0),
              child: Text(
                _message,
                style: TextStyle(fontSize: 20.0, fontStyle: FontStyle.italic),
              ),
            ),
            Center(
              child: Text(
                "Playing Playlist",
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 20.0),
              ),
            ),
            AnimatedList(
              key: _animatedListKey,
              shrinkWrap: true,
              physics: ClampingScrollPhysics(),
              initialItemCount:
                  _audioPlayer.playlistSize > 0 ? _audioPlayer.playlistSize : 0,
              itemBuilder:
                  (BuildContext context, int index, Animation animation) {
//                print(_playlist.getMediaAtIndex(index).title);
                Song song = _audioPlayer.getSongAtIndex(index);
                String title = song != null ? song.title : "No song found";
                String artists = song != null ? song.artists : "No song found";
                return SlideTransition(
                  position: animation.drive(
                    Tween(
                      begin: Offset(1, 0),
                      end: Offset(0, 0),
                    ),
                  ),
                  child: ListTile(
                    title: Text(title),
                    subtitle: Text(artists),
                    onTap: () {
                      _audioPlayer.skipToIndex(index);
                    },
                  ),
                );
              },
            ),
            Center(
              child: Text(
                "Downloaded Songs ${_downloadedSongs.length}",
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 20.0),
              ),
            ),
            AnimatedList(
              key: _downloadedAnimatedListKey,
              shrinkWrap: true,
              initialItemCount: _downloadedSongs.length,
              itemBuilder: (context, index, animation) {
                Download<Media> download = _downloadedSongs[index];
                return SlideTransition(
                  position: animation
                      .drive(Tween(begin: Offset(1, 0), end: Offset(0, 0))),
                  child: DownloadedMediaListTile(
                    download: download,
                  ),
                );
              },
            ),
//            Container(
//              height: 600,
//              width: 50,
//              color: Colors.red,
//              child: _textureId != null
//                  ? AspectRatio(
//                      aspectRatio: _aspectRatio,
//                      child: Texture(textureId: _textureId),
//                    )
//                  : Container(
//                      color: Colors.blue,
//                    ),
//            ),
//            Row(
//              mainAxisAlignment: MainAxisAlignment.spaceAround,
//              children: <Widget>[
//                RaisedButton(
//                  child: Icon(Icons.cloud_download),
//                  onPressed: () {
//                    _videoPlayer.addAndPlay(TypeOfPlace.network, _videoUrl);
//                  },
//                ),
//                RaisedButton(
//                  child: Icon(Icons.play_arrow),
//                  onPressed: () {
//                    _videoPlayer.play();
//                  },
//                ),
//                RaisedButton(
//                  child: Icon(Icons.pause),
//                  onPressed: () {
//                    _videoPlayer.pause();
//                  },
//                ),
//              ],
//            )
          ],
        ),
      ),
    );
  }
}
