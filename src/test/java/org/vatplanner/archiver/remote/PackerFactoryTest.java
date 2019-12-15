package org.vatplanner.archiver.remote;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class PackerFactoryTest {

    @DataProvider
    public static Object[] dataProviderPackerMethods() {
        return PackerMethods.values();
    }

    @Test
    @UseDataProvider("dataProviderPackerMethods")
    public void testCreatePacker_anyPackerMethod_returnsNonNull(PackerMethods method) {
        // Arrange
        PackerFactory factory = new PackerFactory();

        // Act
        Packer result = factory.createPacker(method);

        // Assert
        assertThat(result, is(notNullValue()));
    }

    @Test
    @UseDataProvider("dataProviderPackerMethods")
    public void testCreatePacker_anyPackerMethodSecondCall_returnsNewInstance(PackerMethods method) {
        // Arrange
        PackerFactory factory = new PackerFactory();
        Packer first = factory.createPacker(method);

        // Act
        Packer second = factory.createPacker(method);

        // Assert
        assertThat(second, is(not(sameInstance(first))));
    }

    @Test
    @UseDataProvider("dataProviderPackerMethods")
    public void testCreatePacker_anyPackerMethodSecondCall_returnsSameClass(PackerMethods method) {
        // Arrange
        PackerFactory factory = new PackerFactory();
        Packer first = factory.createPacker(method);

        // Act
        Packer second = factory.createPacker(method);

        // Assert
        assertThat(second.getClass(), is(equalTo(first.getClass())));
    }

    @Test
    public void testCreatePacker_zipUncompressed_returnsUncompressedZipPacker() {
        // Arrange
        PackerFactory factory = new PackerFactory();

        // Act
        Packer result = factory.createPacker(PackerMethods.ZIP_UNCOMPRESSED);

        // Assert
        assertThat(result, is(instanceOf(UncompressedZipPacker.class)));
    }

    @Test
    public void testCreatePacker_zipDeflateSingleThreaded_returnsSingleThreadedZipDeflatePacker() {
        // Arrange
        PackerFactory factory = new PackerFactory();

        // Act
        Packer result = factory.createPacker(PackerMethods.ZIP_DEFLATE_SINGLETHREADED);

        // Assert
        assertThat(result, is(instanceOf(SingleThreadedZipDeflatePacker.class)));
    }

    @Test
    public void testCreatePacker_zipDeflateMultiThreaded_returnsMultiThreadedZipDeflatePacker() {
        // Arrange
        PackerFactory factory = new PackerFactory();

        // Act
        Packer result = factory.createPacker(PackerMethods.ZIP_DEFLATE_MULTITHREADED);

        // Assert
        assertThat(result, is(instanceOf(MultiThreadedZipDeflatePacker.class)));
    }
}
