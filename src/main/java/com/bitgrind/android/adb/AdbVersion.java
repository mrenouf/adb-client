package com.bitgrind.android.adb;

/**
 * Created by mrenouf on 3/10/17.
 */
class AdbVersion {
    static final AdbVersion ANY = new AdbVersion(0, 0, 0);
    final int major;
    final int minor;
    final int point;

    public AdbVersion(int major, int minor, int point) {
        this.major = major;
        this.minor = minor;
        this.point = point;
    }

    @Override
    public String toString() {
        return String.format("%d.%d.%d", major, minor, point);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AdbVersion that = (AdbVersion) o;

        if (major != that.major) return false;
        if (minor != that.minor) return false;
        return point == that.point;
    }

    @Override
    public int hashCode() {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + point;
        return result;
    }

    public boolean lessThan(AdbVersion minVersion) {
        return major < minVersion.major ||
                minor < minVersion.minor ||
                point < minVersion.point;
    }
}
