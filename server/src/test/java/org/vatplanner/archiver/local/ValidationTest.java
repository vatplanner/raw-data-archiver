package org.vatplanner.archiver.local;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ValidationTest {
    @ParameterizedTest
    @ValueSource(strings = {
        "a", // minimum length
        "legacy", //
        "json_v3", //
        "abcdefghijklmnop", //
        "qrstuvwxyz._-", //
        "1234567890123456", // maximum length
    })
    public void testValidateDataFileFormatName_validSyntax_returnsTrue(String s) {
        // Arrange (nothing to do)

        // Act
        boolean result = Validation.validateDataFileFormatName(s);

        // Assert
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", // empty
        " ", // illegal character
        "/", // illegal character
        "\\", // illegal character
        "#", // illegal character
        "$", // illegal character
        "!", // illegal character
        "\"", // illegal character
        "'", // illegal character
        "%", // illegal character
        "&", // illegal character
        "12345678901234567", // 1 character too long
        "LEGACY", // only lower-case is allowed
    })
    public void testValidateDataFileFormatName_invalidSyntax_returnsFalse(String s) {
        // Arrange (nothing to do)

        // Act
        boolean result = Validation.validateDataFileFormatName(s);

        // Assert
        assertThat(result).isFalse();
    }
}
