#!/bin/sh

echo '<?xml version="1.0" encoding="UTF-8" standalone="no" ?>
<gpx xmlns="http://www.topografix.com/GPX/1/1" creator="" version="1.1"
     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
     xsi:schemaLocation="http://www.topografix.com/GPX/1/1 ">http://www.topografix.com/GPX/1/1/gpx.xsd">
<trk><name>Grid lines</name>'
thr=0.000898315
step=0.0000008983
for i in $(seq -f '%.9f' -$thr $step $thr); do
    echo "<trkseg><trkpt lat=\"$i\" lon=\"-$thr\"></trkpt><trkpt lat=\"$i\" lon=\"$thr\"></trkpt></trkseg>"
done
for i in $(seq -f '%.9f' -$thr $step $thr); do
    echo "<trkseg><trkpt lat=\"$thr\" lon=\"$i\"></trkpt><trkpt lat=\"-$thr\" lon=\"$i\"></trkpt></trkseg>"
done
echo "</trk></gpx>"
