package com.unlimited.sports.globox.common.utils;

import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GlobalCoordinates;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DistanceUtils {
    /**
     * 使用Geodesy库计算两个经纬度之间的距离（单位：公里）
     * 使用WGS84椭球体模型，精度更高
     *
     * @param lat1 起点纬度
     * @param lon1 起点经度
     * @param lat2 终点纬度
     * @param lon2 终点经度
     * @return 距离（公里，保留2位小数）
     */
    public static BigDecimal calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // 创建起点和终点坐标
        GlobalCoordinates start = new GlobalCoordinates(lat1, lon1);
        GlobalCoordinates end = new GlobalCoordinates(lat2, lon2);

        // 使用WGS84椭球体模型（GPS使用的标准）
        GeodeticCalculator calculator = new GeodeticCalculator();

        // 计算距离（返回米）
        double distanceInMeters = calculator.calculateGeodeticCurve(
                Ellipsoid.WGS84,
                start,
                end
        ).getEllipsoidalDistance();

        // 转换为公里并保留2位小数
        BigDecimal distanceInKm = BigDecimal.valueOf(distanceInMeters / 1000.0);
        return distanceInKm.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算矩形边界框（Bounding Box）
     * 用于在给定中心点和距离的情况下，计算包含该范围的矩形边界
     *
     * 使用 Geodesy 库精确计算，考虑地球椭球体形状
     *
     * @param centerLat 中心点纬度
     * @param centerLng 中心点经度
     * @param distanceKm 距离（公里）
     * @return 边界框数组 [minLat, maxLat, minLng, maxLng]
     */
    public static double[] calculateBoundingBox(double centerLat, double centerLng, double distanceKm) {
        GeodeticCalculator calculator = new GeodeticCalculator();
        GlobalCoordinates center = new GlobalCoordinates(centerLat, centerLng);

        // 距离转换为米
        double distanceInMeters = distanceKm * 1000;

        // 计算北方向的点（方位角 0°）
        GlobalCoordinates north = calculator.calculateEndingGlobalCoordinates(
                Ellipsoid.WGS84,
                center,
                0.0,  // 方位角：0° = 正北
                distanceInMeters
        );

        // 计算南方向的点（方位角 180°）
        GlobalCoordinates south = calculator.calculateEndingGlobalCoordinates(
                Ellipsoid.WGS84,
                center,
                180.0,  // 方位角：180° = 正南
                distanceInMeters
        );

        // 计算东方向的点（方位角 90°）
        GlobalCoordinates east = calculator.calculateEndingGlobalCoordinates(
                Ellipsoid.WGS84,
                center,
                90.0,  // 方位角：90° = 正东
                distanceInMeters
        );

        // 计算西方向的点（方位角 270°）
        GlobalCoordinates west = calculator.calculateEndingGlobalCoordinates(
                Ellipsoid.WGS84,
                center,
                270.0,  // 方位角：270° = 正西
                distanceInMeters
        );

        // 返回边界框
        double minLat = south.getLatitude();
        double maxLat = north.getLatitude();
        double minLng = west.getLongitude();
        double maxLng = east.getLongitude();

        return new double[]{minLat, maxLat, minLng, maxLng};
    }

    /**
     * 计算矩形边界框（简化版本，使用近似算法）
     * 适用于小范围（< 100公里）的快速计算
     *
     * @param lat 中心纬度
     * @param lng 中心经度
     * @param distanceKm 距离（公里）
     * @return 边界框数组 [minLat, maxLat, minLng, maxLng]
     */
    public static double[] calculateBoundingBoxApprox(double lat, double lng, double distanceKm) {
        // 纬度变化（1度纬度约等于111公里）
        double latChange = distanceKm / 111.0;

        // 经度变化（随纬度变化，考虑地球曲率）
        double lngChange = distanceKm / (111.0 * Math.cos(Math.toRadians(lat)));

        double minLat = lat - latChange;
        double maxLat = lat + latChange;
        double minLng = lng - lngChange;
        double maxLng = lng + lngChange;

        return new double[]{minLat, maxLat, minLng, maxLng};
    }
}
