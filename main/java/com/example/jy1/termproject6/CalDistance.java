package com.example.jy1.termproject6;

/**
 * Created by 임지영 on 2017-11-11.
 */

public class CalDistance {
    public double theta, dist;
    public double bef_lat, bef_long, cur_lat, cur_long;

    public CalDistance() {
        this.theta = 0;
        this.dist = 0;
    }


    public double getDistance(double bef_lat, double bef_long, double cur_lat, double cur_long) {

        this.bef_lat = bef_lat;
        this.bef_long = bef_long;
        this.cur_lat = cur_lat;
        this.cur_long = cur_long;

        theta = bef_long - cur_long;
        dist = Math.sin(deg2rad(bef_lat)) * Math.sin(deg2rad(cur_lat)) + Math.cos(deg2rad(bef_lat))
                * Math.cos(deg2rad(cur_lat)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);

        dist = dist * 60 * 1.1515;
        dist = dist * 1.609344;    // 단위 mile 에서 km 변환.
        dist = dist * 1000.0;      // 단위  km 에서 m 로 변환
        return dist; // 단위 m
    }

    // 주어진 도(degree) 값을 라디언으로 변환
    private double deg2rad(double deg) {
        return (double) (deg * Math.PI / (double) 180d);
    }

    // 주어진 라디언(radian) 값을 도(degree) 값으로 변환
    private double rad2deg(double rad) {
        return (double) (rad * (double) 180d / Math.PI);
    }
}
