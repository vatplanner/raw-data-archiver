package org.vatplanner.archiver.remote;

import org.vatplanner.archiver.common.PackerMethod;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import static com.tngtech.java.junit.dataprovider.DataProviders.crossProduct;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class PackerFactoryTest {

    @DataProvider
    public static Object[][] dataProviderPackerMethods() {
        PackerMethod[] methods = PackerMethod.values();
        Object[][] out = new Object[methods.length][1];

        for (int i = 0; i < methods.length; i++) {
            out[i][0] = methods[i];
        }

        return out;
    }

    @DataProvider
    public static Object[][] dataProviderTarMethods() {
        return new Object[][]{
            {PackerMethod.TAR_BZIP2},
            {PackerMethod.TAR_DEFLATE},
            {PackerMethod.TAR_GZIP},
            {PackerMethod.TAR_LZMA},
            {PackerMethod.TAR_UNCOMPRESSED},
            {PackerMethod.TAR_XZ}
        };
    }

    @DataProvider
    public static Object[][] dataProviderBooleans() {
        return new Object[][]{
            {true},
            {false}
        };
    }

    @DataProvider
    public static Object[][] dataProviderPackerMethodsAndBoolean() {
        return crossProduct(dataProviderPackerMethods(), dataProviderBooleans());
    }

    @DataProvider
    public static Object[][] dataProviderTarMethodsAndBoolean() {
        return crossProduct(dataProviderTarMethods(), dataProviderBooleans());
    }

    @Test
    @UseDataProvider("dataProviderPackerMethodsAndBoolean")
    public void testCreatePacker_anyPackerMethod_returnsNonNull(PackerMethod method, boolean autoSelectMultiThreading) {
        // Arrange
        PackerFactory factory = createFactory(autoSelectMultiThreading);

        // Act
        Packer result = factory.createPacker(method);

        // Assert
        assertThat(result, is(notNullValue()));
    }

    @Test
    @UseDataProvider("dataProviderPackerMethodsAndBoolean")
    public void testCreatePacker_anyPackerMethodSecondCall_returnsNewInstance(PackerMethod method, boolean autoSelectMultiThreading) {
        // Arrange
        PackerFactory factory = createFactory(autoSelectMultiThreading);
        Packer first = factory.createPacker(method);

        // Act
        Packer second = factory.createPacker(method);

        // Assert
        assertThat(second, is(not(sameInstance(first))));
    }

    @Test
    @UseDataProvider("dataProviderPackerMethodsAndBoolean")
    public void testCreatePacker_anyPackerMethodSecondCall_returnsSameClass(PackerMethod method, boolean autoSelectMultiThreading) {
        // Arrange
        PackerFactory factory = createFactory(autoSelectMultiThreading);
        Packer first = factory.createPacker(method);

        // Act
        Packer second = factory.createPacker(method);

        // Assert
        assertThat(second.getClass(), is(equalTo(first.getClass())));
    }

    @Test
    @DataProvider({"true", "false"})
    public void testCreatePacker_zipUncompressed_returnsUncompressedZipPacker(boolean autoSelectMultiThreading) {
        // Arrange
        PackerFactory factory = createFactory(autoSelectMultiThreading);

        // Act
        Packer result = factory.createPacker(PackerMethod.ZIP_UNCOMPRESSED);

        // Assert
        assertThat(result, is(instanceOf(UncompressedZipPacker.class)));
    }

    @Test
    @DataProvider({"true", "false"})
    public void testCreatePacker_zipDeflateSingleThreaded_returnsSingleThreadedZipDeflatePacker(boolean autoSelectMultiThreading) {
        // Arrange
        PackerFactory factory = createFactory(autoSelectMultiThreading);

        // Act
        Packer result = factory.createPacker(PackerMethod.ZIP_DEFLATE_SINGLETHREADED);

        // Assert
        assertThat(result, is(instanceOf(SingleThreadedZipDeflatePacker.class)));
    }

    @Test
    @DataProvider({"true", "false"})
    public void testCreatePacker_zipDeflateMultiThreaded_returnsMultiThreadedZipDeflatePacker(boolean autoSelectMultiThreading) {
        // Arrange
        PackerFactory factory = createFactory(autoSelectMultiThreading);

        // Act
        Packer result = factory.createPacker(PackerMethod.ZIP_DEFLATE_MULTITHREADED);

        // Assert
        assertThat(result, is(instanceOf(MultiThreadedZipDeflatePacker.class)));
    }

    @Test
    @UseDataProvider("dataProviderTarMethodsAndBoolean")
    public void testCreatePacker_tarMethod_returnsTarPacker(PackerMethod tarMethod, boolean autoSelectMultiThreading) {
        // Arrange
        PackerFactory factory = createFactory(autoSelectMultiThreading);

        // Act
        Packer result = factory.createPacker(tarMethod);

        // Assert
        assertThat(result, is(instanceOf(TarPacker.class)));
    }

    @Test
    public void testCreatePacker_genericZipDeflateWithoutMultiThreading_returnsSingleThreadedZipDeflatePacker() {
        // Arrange
        PackerFactory factory = createFactory(false);

        // Act
        Packer result = factory.createPacker(PackerMethod.ZIP_DEFLATE);

        // Assert
        assertThat(result, is(instanceOf(SingleThreadedZipDeflatePacker.class)));
    }

    @Test
    public void testCreatePacker_genericZipDeflateWithMultiThreading_returnsMultiThreadedZipDeflatePacker() {
        // Arrange
        PackerFactory factory = createFactory(true);

        // Act
        Packer result = factory.createPacker(PackerMethod.ZIP_DEFLATE);

        // Assert
        assertThat(result, is(instanceOf(MultiThreadedZipDeflatePacker.class)));
    }

    private PackerFactory createFactory(boolean autoSelectMultiThreading) {
        return new PackerFactory(
                new PackerConfiguration()
                        .setAutoSelectMultiThreading(autoSelectMultiThreading)
        );
    }
}
