class DownloadListener {
  final Function(int, String) _onDownloadChanged;
  DownloadListener({onDownloadChanged}): _onDownloadChanged = onDownloadChanged;

  void onDownloadChanged(int state, String id) {
    if (_onDownloadChanged != null) {
      _onDownloadChanged(state, id);
    }
  }
}