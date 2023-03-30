# JOSM-LexxPlussExporter

このプラグインはOSMフォーマットでの保存時に緯度経度情報を画像のピクセル座標に変換する。

####  インストール方法

1. [JOSMのインストール](https://josm.openstreetmap.de/wiki/Ja:Download)
2. PicLayerプラグインのインストール( 本プラグインの動作には画像をレイヤーとして使用するためのPicLayerプラグインが必須である。 )
  - JOSMを開いてメニューからPresets/Preferences/PluginsのSearchに"PicLayer"と検索してinstall.これで，画像ファイルを挿入できるようになる．
3. 提供されるlexxpluss.jarファイルを~/.josm/pluginsにコピーする。
4. JOSMを起動し、プラグイン設定で有効化して再起動する。

#### 使用方法
1. PicLayerによる画像レイヤーを一つ用意する。複数存在する場合は一番優先度の高いレイヤーが対象になる。
2. 図形レイヤーに図形を描画する。
3. 「名前をつけて保存」で拡張子をOSMに指定する。もしくは保存ファイル種別をOSM Server Files LexxPluss formatに指定する。ただし保存ファイル名の拡張子の形式が優先される。
4. 保存する。

#### ビルド方法
josmビルド環境を構築し、ソースファイルをjosm/pluginsに展開する。
antコマンドによるビルドが可能な環境でjosm/plugins/LexxPLussExporterフォルダでant installを実行する。
ネットワーク接続がないとビルドできないので注意。

またJOSMホームページサーバの不調により、ビルドできなかったりJOSM起動時に警告が出ることがある。こちらで出来ることは無いので直るまでしばらく待つしかない。

Ubunti18.04環境での一例
```
$ cd ~
$ git clone git@github.com:LexxPluss/JOSMPlugin.git
$ mv ~/JOSMPlugin/LexxPlussExporter ~/workspace/josm/plugins
$ cd ~/workspace/josm/plugins/PicLayer
$ sudo update-alternatives --config java  # java-11-openjdk-amd64 を選択( josm本体はJav11でビルドする必要があるらしい )
$ sudo update-alternatives --config javac  # java-11-openjdk-amd64 を選択( josm本体はJava11でビルドする必要があるらしい )
$ export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
$ ant # PicLayerがビルド済みでPicLayer.jarができている必要がある
$ cd ~/workspace/josm/plugins/LexxPlussExporter
$ ant install
```

#### josmビルド環境構築
参考：[JOSMプラグイン開発環境を整備する - Qiita](https://qiita.com/yuuhayashi@github/items/670803d442887d831b49)

Ubuntu18.04環境での一例(大体そのとおり実行すれば、josmビルド環境の構築ができるはずです)
```
$ sudo apt install openjdk-8-jre openjdk-8-jdk openjdk-11-jre openjdk-11-jdk ant  # Java開発環境のインストール
$ mkdir -p ~/workspace  # この下にjosmビルド環境を構築していく
$ cd ~/workspace
$ svn co https://josm.openstreetmap.de/osmsvn/applications/editors/josm  # ベースとなるコードをチェックアウト
$ sudo update-alternatives --config java  # java-8-openjdk を選択( josm本体はJava8でビルドする必要があるらしい )
$ sudo update-alternatives --config javac  # java-8-openjdk を選択( josm本体はJava8でビルドする必要があるらしい )
$ cd ~/workspace/josm/core
$ export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/jre
$ ant  # josm本体のビルド(動作確認)
```
