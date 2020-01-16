import 'package:flutter_media_plugin/media/media.dart';

class Download<T extends Media> {
  T _media;
  int _state;
  int _bytesDownloaded;
  double _percentDownloaded;

  Download(
      this._media, this._state, this._bytesDownloaded, this._percentDownloaded);

  Set<DownloadListener> _downloadListeners = Set();

  T get media => _media;

  int get state => _state;

  int get bytesDownloaded => _bytesDownloaded;

  double get percentDownloaded => _percentDownloaded;

  void addDownloadListener(DownloadListener listener) {
    _downloadListeners.add(listener);
  }

  void removeDownloadListener(DownloadListener listener) {
    _downloadListeners.remove(listener);
  }

  void updateDownloadState(int state) {
    if (_state == state) {
      return;
    }

    for (DownloadListener listener in _downloadListeners) {
      listener.onDownloadStateChanged(state);
    }

    _state = state;
  }

  void updateBytesDownloaded(int bytesDownloaded) {
    if (_bytesDownloaded == bytesDownloaded) {
      return;
    }

    for (DownloadListener listener in _downloadListeners) {
      listener.onBytesDownloadedChanged(bytesDownloaded);
    }

    _bytesDownloaded = bytesDownloaded;
  }

  void updatePercentDownloaded(double percent) {
    if (_percentDownloaded == percent) {
      return;
    }

    for (DownloadListener listener in _downloadListeners) {
      listener.onDownloadPercentageChanged(percent);
    }

    _percentDownloaded = percent;
  }
}

class DownloadListener {
  final Function(int) _onDownloadStateChanged;
  final Function(int) _onBytesDownloadedChanged;
  final Function(double) _onDownloadPercentageChanged;

  DownloadListener({
    Function(int) onDownloadStateChanged,
    Function(int) onBytesDownloadedChanged,
    Function(double) onDownloadPercentageChanged,
  })  : _onBytesDownloadedChanged = onBytesDownloadedChanged,
        _onDownloadStateChanged = onDownloadStateChanged,
        _onDownloadPercentageChanged = onDownloadPercentageChanged;

  onDownloadStateChanged(int state) {
    if (_onDownloadStateChanged != null) {
      _onDownloadStateChanged(state);
    }
  }

  onBytesDownloadedChanged(int byteDownloaded) {
    if (_onBytesDownloadedChanged != null) {
      _onBytesDownloadedChanged(byteDownloaded);
    }
  }

  onDownloadPercentageChanged(double percent) {
    if (_onDownloadPercentageChanged != null) {
      _onDownloadPercentageChanged(percent);
    }
  }
}
