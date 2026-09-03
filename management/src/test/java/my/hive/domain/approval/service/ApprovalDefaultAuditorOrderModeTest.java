package my.hive.domain.approval.service;

import my.hive.domain.approval.mapper.ApprovalDefaultAuditorMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApprovalDefaultAuditorOrderModeTest {

    @Test
    void orderDefaultsToOrWhenNoConfigurationRowExists() {
        ApprovalDefaultAuditorMapper mapper = mock(ApprovalDefaultAuditorMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        ApprovalDefaultAuditorService subject = new ApprovalDefaultAuditorService();
        ReflectionTestUtils.setField(subject, "approvalDefaultAuditorMapper", mapper);

        assertEquals("OR", subject.resolveApprovalMode("tenant-a", "ORDER"));
        assertEquals("AND", subject.resolveApprovalMode("tenant-a", "FINANCE"));
    }
}
