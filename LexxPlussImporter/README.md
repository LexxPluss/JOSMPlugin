# JOSM-LexxPlussImporter

このプラグインはJOSM-LexxPluss Exporterにて保存したピクセル座標系のファイルを読み込み、緯度経度系の座標に変換する。<br>
JOSM-LexxPluss Exporterにて作成したファイルを、他のエディタで編集して、ピクセル座標系を変更、もしくは座標の作成などを行ったものを、再度JOSMで表示できる緯度経度座標系に変換することを想定したプラグインとなっている。<br>
座標変換には、LexxPlussExporterが出力した座標変換系の変数を読み込み、LexxPlussExporterと逆の変換を行うことで、緯度経度座標系にまで復元する。

####  インストール方法

1. [JOSMのインストール](https://josm.openstreetmap.de/wiki/Ja:Download)
1.  提供されるlexxpluss_importer.jarファイルを~/.josm/pluginsにコピーする。
1. JOSMを起動し、プラグイン設定で有効化して再起動する。

#### 使用方法
1. JOSM-LEXXPLuss Exporterにて出力され、さらに編集が加えられたファイルを用意する。
1. JOSMから、ファイルを開くを選択し、ファイルフィルタとして、"Lexx Pluss format OSM (*.osm, *.xml)"を選択して、対象のファイルを開く。<br>
この時、すでに緯度経度の情報がある頂点はそのまま読み込まれ、緯度経度の情報がないものは、ピクセル座標からの変換が行われる。

#### ビルド方法、josmビルド環境構築
[JOSM-LexxPluss Exporter](../LexxPlussExporter/README.md)を参照。

