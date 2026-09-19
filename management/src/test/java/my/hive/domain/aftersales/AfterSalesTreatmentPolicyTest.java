package my.hive.domain.aftersales;

import my.hive.domain.aftersales.service.AfterSalesTreatmentPolicy;
import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AfterSalesTreatmentPolicyTest {
    @Test void repeatedTreatmentsFinishIndependently() {
        assertThrows(BusinessException.class, () -> AfterSalesTreatmentPolicy.requireClosable(List.of("resolved", "processing")));
        assertThrows(BusinessException.class, () -> AfterSalesTreatmentPolicy.requireClosable(List.of("resolved", "waiting_follow_up")));
        assertThrows(BusinessException.class, () -> AfterSalesTreatmentPolicy.requireClosable(List.of("unresolved")));
        assertDoesNotThrow(() -> AfterSalesTreatmentPolicy.requireClosable(List.of("unresolved", "resolved")));
        assertDoesNotThrow(() -> AfterSalesTreatmentPolicy.requireClosable(List.of()));
    }
    @Test void completedTreatmentCannotBeRewritten() {
        assertDoesNotThrow(() -> AfterSalesTreatmentPolicy.requireEditable("processing"));
        assertThrows(BusinessException.class, () -> AfterSalesTreatmentPolicy.requireEditable("resolved"));
        assertThrows(BusinessException.class, () -> AfterSalesTreatmentPolicy.requireEditable("waiting_follow_up"));
    }
}
