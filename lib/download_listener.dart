class DownloadListener {
  final Function(int) _onDownloadChanged;
  DownloadListener({onDownloadChanged}): _onDownloadChanged = onDownloadChanged;

  void onPlayerStateChanged(int state) {
    if (_onDownloadChanged != null) {
      _onDownloadChanged(state);
    }
  }
}