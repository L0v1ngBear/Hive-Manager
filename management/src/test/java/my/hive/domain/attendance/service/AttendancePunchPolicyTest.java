package my.hive.domain.attendance.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AttendancePunchPolicyTest {

    @Test
    void distanceIsZeroForTheSameCoordinate() {
        assertThat(AttendanceService.distanceMeters(31.2304, 121.4737, 31.2304, 121.4737)).isZero();
    }

    @Test
    void distanceUsesMetersAndAStableEarthRadius() {
        assertThat(AttendanceService.distanceMeters(0D, 0D, 0D, 1D))
                .isBetween(111_300D, 111_400D);
    }
}
