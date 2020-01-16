enum MediaType {
  song,
  video
}

abstract class Media {
  String _key;
  String _title;
  String _url;

  String get key => _key;
  String get title => _title;
  String get url => _url;



  Map<String, dynamic> toJson() {
    print("media.dart: Media to json");
    return null;
  }

  factory Media.fromMap(Map<String, dynamic> map) {
    print("media.dart: Media from map");
    return null;
  }
}