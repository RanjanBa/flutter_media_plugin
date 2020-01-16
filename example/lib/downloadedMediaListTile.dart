import 'package:flutter_media_plugin/flutter_media_plugin.dart';
import 'package:flutter/material.dart';
import 'package:flutter_media_plugin/download.dart';
import 'package:flutter_media_plugin/media/media.dart';

class DownloadedMediaListTile extends StatefulWidget {
  final Download<Media> download;

  DownloadedMediaListTile({@required this.download});

  @override
  _DownloadedMediaListTileState createState() =>
      _DownloadedMediaListTileState();
}

class _DownloadedMediaListTileState extends State<DownloadedMediaListTile> {
  DownloadListener _downloadListener;

  int _downloadState;
  int _bytesDownloaded;
  double _percentDownloaded;

  @override
  void initState() {
    _downloadState = widget.download.state;
    _bytesDownloaded = widget.download.bytesDownloaded;
    _percentDownloaded = widget.download.percentDownloaded;
    _downloadListener = DownloadListener(onDownloadStateChanged: (int state) {
      _downloadState = state;
      setState(() {});
    }, onBytesDownloadedChanged: (bytesDownloaded) {
      _bytesDownloaded = bytesDownloaded;
      setState(() {});
    }, onDownloadPercentageChanged: (percent) {
      _percentDownloaded = percent;
      setState(() {});
    });

    widget.download.addDownloadListener(_downloadListener);

    super.initState();
  }

  @override
  void dispose() {
    widget.download.removeDownloadListener(_downloadListener);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ListTile(
      title: Text(widget.download.media.title),
      subtitle: Text(
          'state: $_downloadState, bytesDownloaded ${_bytesDownloaded}, percent: $_percentDownloaded'),
      trailing: IconButton(
        icon: Icon(Icons.delete),
        onPressed: () {
          FlutterMediaPlugin.downloadManager
              .downloadRemove(MediaType.song, widget.download.media);
        },
      ),
    );
  }
}
