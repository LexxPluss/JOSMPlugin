// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.lexxpluss;

import org.openstreetmap.josm.data.coor.LatLon;

/**
 * 
 * LexxPluss Plugin Utility
 * @author LexxPluss
 *
 */
public class LexxPlussUtil {

    public static double atanh(double a) {
        final double mult;
        // check the sign bit of the raw representation to handle -0
        if (Double.doubleToRawLongBits(a) < 0) {
            a = Math.abs(a);
            mult = -0.5d;
        } else {
            mult = 0.5d;
        }
        return mult * Math.log((1.0d + a) / (1.0d - a));
    }

    public static LatLon DesToUtm(LatLon coor) {
        double f, r, n, Am, n1, n2, n3, n4, n5;
        double lon0, lon, lat0, lat, gd, ed, E0, N0, k0, t;
        double Smlat, rk0;
        int	i;
        double A[] = new double[6];
        double a[] = new double[6];
        
        f = 1.0/ 298.257223563;//WGS84 地球の扁平率
        r = 6378137.00000000; //GRS80 & WGS84 地球長辺半径

        double latitude = coor.lat();
        double longitude = coor.lon();

        E0 = (latitude < 0.0)? 0.0 : 500*1000;// 経度オフセット(500km)
        // 南半球オフセット(10000km)
        N0 = (longitude < 0.0)? 10000.0*1000.0 : 0.0;
        k0 = 0.9996;//UTM座標系の場合

        n = f/(2.0-f);
        n1=n;
        n2=n1*n;
        n3=n2*n;
        n4=n3*n;
        n5=n4*n;
        A[0] =  1 + n2/4.0 + n4/64.0 ;
        rk0  = r*k0/(1+n1);
        Am   = rk0*A[0];

        A[1] = -3.0/2.0* (n1 - n3/8.0 -n5/64.0);
        A[2] = 15.0/16.0* (n2 - n4/4.0);
        A[3] = -35.0/48.0 * (n3 -5.0/16.0*n5);
        A[4] = 315.0/512.0 *n4;
        A[5] = -693.0/1280.0 *n5;
    
        a[1] = 1.0/2.0*n1 - 2.0/3.0 *n2 + 5.0/16.0*n3 +41.0/180.0*n4 - 127.0/288.0*n5;
        a[2] = 13.0/48.0*n2- 3.0/5.0*n3 + 557.0/1440.0*n4 +281.0/630.0*n5;
        a[3] = 61.0/240.0*n3 - 103.0/140.0*n4 + 15061.0/26880.0*n5;
        a[4] = 49561.0/161280.0*n4 -179.0/168.0*n5;
        a[5] = 34729.0/80640.0*n5;
    
        //double Zone = (int)(longitude/6.0)+31;// 統計のUTMザーン番号
        lon0  = (double)(int)(longitude/6.0)*6.0 + 3.0; // UTMゾーンでの中心経度
        lat0 = 0.0;//赤道固定

        lon0 = lon0*Math.PI / 180.0;
        lat0 = lat0*Math.PI / 180.0;
    
        lon  = longitude*Math.PI / 180.0;
        lat  = latitude*Math.PI / 180.0;

        //緯度原点の調整分の計算
	    Smlat = Am*lat0;
	    for(i = 1; i <= 5; i++) {
	        Smlat = Smlat + rk0*A[i]*Math.sin(2.0*((double)i)*lat0);
        }

        t  = Math.sinh(atanh(Math.sin(lat)) - 2*Math.sqrt(n)/(1.0+n) * atanh( 2*Math.sqrt(n)/(1.0+n) * Math.sin(lat)) ) ;
        gd = Math.atan(t/Math.cos(lon-lon0));
        ed = atanh(Math.sin(lon-lon0)/Math.sqrt(1+t*t));

        double E =E0 + Am*ed;
        double N =N0 + Am*gd;
    
        for(i = 1; i <= 5;i ++) {
            E = E + Am*a[i]*Math.cos(2.0*((double)i)*gd)*Math.sinh(2.0*((double)i)*ed);
            N = N + Am*a[i]*Math.sin(2.0*((double)i)*gd)*Math.cosh(2.0*((double)i)*ed) ;
        }
        N = N - Smlat;// 緯度原点の調整
    
        return new LatLon(E, N);
    }
}
