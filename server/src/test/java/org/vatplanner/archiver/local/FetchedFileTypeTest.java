package org.vatplanner.archiver.local;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class FetchedFileTypeTest {
    public static Stream<Arguments> dataProviderFileNameAndExpectedType() {
        return Stream.of(
            Arguments.of("20210102T111009Z_meta.json", FetchedFileType.META_DATA), //
            Arguments.of("20210102T111009Z_vatsim-data.json", FetchedFileType.RAW_VATSIM_DATA_FILE), //
            Arguments.of("20210102T111009Z_vatsim-data.txt", FetchedFileType.RAW_VATSIM_DATA_FILE) //
        );
    }

    @ParameterizedTest
    @MethodSource("dataProviderFileNameAndExpectedType")
    public void testByFileName_knownFileName_returnsExpectedType(String fileName, FetchedFileType expectedType) {
        // Arrange (nothing to do)

        // Act
        FetchedFileType result = FetchedFileType.byFileName(fileName);

        // Assert
        assertThat(result).isSameAs(expectedType);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "something.json", "20210102T111009Z_vatsim-data.json.nope",
        "20210102T111009Z_vatsim-data.txt.", "20210102T111009Z_meta.json2" })
    public void testByFileName_unknownFileName_returnsNull(String fileName) {
        // Arrange (nothing to do)

        // Act
        FetchedFileType result = FetchedFileType.byFileName(fileName);

        // Assert
        assertThat(result).isNull();
    }
}
