package org.vatplanner.archiver.remote;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.CartesianProductTest;
import org.vatplanner.archiver.common.PackerMethod;

public class PackerFactoryTest {
    public static CartesianProductTest.Sets dataProviderPackerMethodsAndBoolean() {
        return new CartesianProductTest.Sets()
            .addAll(asList(PackerMethod.values()))
            .add(true, false);
    }

    public static CartesianProductTest.Sets dataProviderTarMethodsAndBoolean() {
        return new CartesianProductTest.Sets()
            .addAll(Arrays.stream(PackerMethod.values()).filter(x -> x.name().startsWith("TAR_")))
            .add(true, false);
    }

    @CartesianProductTest(factory = "dataProviderPackerMethodsAndBoolean")
    public void testCreatePacker_anyPackerMethod_returnsNonNull(PackerMethod method, boolean autoSelectMultiThreading) {
        // Arrange
        PackerFactory factory = createFactory(autoSelectMultiThreading);

        // Act
        Packer result = factory.createPacker(method);

        // Assert
        assertThat(result).isNotNull();
    }

    @CartesianProductTest(factory = "dataProviderPackerMethodsAndBoolean")
    public void testCreatePacker_anyPackerMethodSecondCall_returnsNewInstance(PackerMethod method, boolean autoSelectMultiThreading) {
        // Arrange
        PackerFactory factory = createFactory(autoSelectMultiThreading);
        Packer first = factory.createPacker(method);

        // Act
        Packer second = factory.createPacker(method);

        // Assert
        assertThat(second).isNotSameAs(first);
    }

    @CartesianProductTest(factory = "dataProviderPackerMethodsAndBoolean")
    public void testCreatePacker_anyPackerMethodSecondCall_returnsSameClass(PackerMethod method, boolean autoSelectMultiThreading) {
        // Arrange
        PackerFactory factory = createFactory(autoSelectMultiThreading);
        Packer first = factory.createPacker(method);

        // Act
        Packer second = factory.createPacker(method);

        // Assert
        assertThat(second).hasSameClassAs(first);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void testCreatePacker_zipUncompressed_returnsUncompressedZipPacker(boolean autoSelectMultiThreading) {
        // Arrange
        PackerFactory factory = createFactory(autoSelectMultiThreading);

        // Act
        Packer result = factory.createPacker(PackerMethod.ZIP_UNCOMPRESSED);

        // Assert
        assertThat(result).isInstanceOf(UncompressedZipPacker.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void testCreatePacker_zipDeflateSingleThreaded_returnsSingleThreadedZipDeflatePacker(boolean autoSelectMultiThreading) {
        // Arrange
        PackerFactory factory = createFactory(autoSelectMultiThreading);

        // Act
        Packer result = factory.createPacker(PackerMethod.ZIP_DEFLATE_SINGLETHREADED);

        // Assert
        assertThat(result).isInstanceOf(SingleThreadedZipDeflatePacker.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void testCreatePacker_zipDeflateMultiThreaded_returnsMultiThreadedZipDeflatePacker(boolean autoSelectMultiThreading) {
        // Arrange
        PackerFactory factory = createFactory(autoSelectMultiThreading);

        // Act
        Packer result = factory.createPacker(PackerMethod.ZIP_DEFLATE_MULTITHREADED);

        // Assert
        assertThat(result).isInstanceOf(MultiThreadedZipDeflatePacker.class);
    }

    @CartesianProductTest(factory = "dataProviderTarMethodsAndBoolean")
    public void testCreatePacker_tarMethod_returnsTarPacker(PackerMethod tarMethod, boolean autoSelectMultiThreading) {
        // Arrange
        PackerFactory factory = createFactory(autoSelectMultiThreading);

        // Act
        Packer result = factory.createPacker(tarMethod);

        // Assert
        assertThat(result).isInstanceOf(TarPacker.class);
    }

    @Test
    public void testCreatePacker_genericZipDeflateWithoutMultiThreading_returnsSingleThreadedZipDeflatePacker() {
        // Arrange
        PackerFactory factory = createFactory(false);

        // Act
        Packer result = factory.createPacker(PackerMethod.ZIP_DEFLATE);

        // Assert
        assertThat(result).isInstanceOf(SingleThreadedZipDeflatePacker.class);
    }

    @Test
    public void testCreatePacker_genericZipDeflateWithMultiThreading_returnsMultiThreadedZipDeflatePacker() {
        // Arrange
        PackerFactory factory = createFactory(true);

        // Act
        Packer result = factory.createPacker(PackerMethod.ZIP_DEFLATE);

        // Assert
        assertThat(result).isInstanceOf(MultiThreadedZipDeflatePacker.class);
    }

    private PackerFactory createFactory(boolean autoSelectMultiThreading) {
        return new PackerFactory(
            new PackerConfiguration()
                .setAutoSelectMultiThreading(autoSelectMultiThreading) //
        );
    }
}
