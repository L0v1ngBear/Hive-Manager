package my.hive.shared.security;

import my.hive.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalStorageReferenceTest {

    @Test
    void privateReferenceRoundTripsOnlyForTheExpectedTenantAndModule() {
        String objectKey = "hive/tenant-a/finance/2026/07/22/file.pdf";
        String reference = InternalStorageReference.privateOssReference("/api", objectKey);

        assertThat(reference).startsWith("/api/storage/private/aliyun-oss/");
        assertThat(InternalStorageReference.requirePrivateOssObjectKey(
                reference,
                "/api",
                "tenant-a",
                Set.of("finance")
        )).isEqualTo(objectKey);

        assertThatThrownBy(() -> InternalStorageReference.requirePrivateOssObjectKey(
                reference,
                "/api",
                "tenant-b",
                Set.of("finance")
        )).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> InternalStorageReference.requirePrivateOssObjectKey(
                reference,
                "/api",
                "tenant-a",
                Set.of("sales-order")
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void publicTenantLogoReferenceCannotExposeAnotherModule() {
        String logoReference = InternalStorageReference.publicTenantLogoReference(
                "/api",
                "hive/tenant-a/tenant-logo/2026/07/22/logo.png"
        );

        assertThat(InternalStorageReference.requirePublicTenantLogoObjectKey(
                logoReference.substring("/api".length()),
                ""
        )).isEqualTo("hive/tenant-a/tenant-logo/2026/07/22/logo.png");

        String financeReference = InternalStorageReference.publicTenantLogoReference(
                "",
                "hive/tenant-a/finance/2026/07/22/invoice.pdf"
        );
        assertThatThrownBy(() -> InternalStorageReference.requirePublicTenantLogoObjectKey(financeReference, ""))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void storageReferencesRejectExternalUrlsAndMalformedTokens() {
        assertThatThrownBy(() -> InternalStorageReference.requirePrivateOssObjectKey(
                "https://evil.example/storage/private/aliyun-oss/value",
                "/api",
                "tenant-a",
                Set.of("finance")
        )).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> InternalStorageReference.requirePrivateOssObjectKey(
                "/api/storage/private/aliyun-oss/not+base64",
                "/api",
                "tenant-a",
                Set.of("finance")
        )).isInstanceOf(BusinessException.class);
    }
}
