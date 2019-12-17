package org.vatplanner.archiver.local;

/**
 * Identifies file types related to fetched data.
 */
public enum FetchedFileType {
    RAW_VATSIM_DATA_FILE("vatsim-data.txt"),
    META_DATA("meta.json");

    private final String fileNameSuffix;

    private FetchedFileType(String fileNameSuffix) {
        this.fileNameSuffix = fileNameSuffix;
    }

    public static FetchedFileType byFileName(String fileName) {
        if (fileName.endsWith(FetchedFileType.META_DATA.fileNameSuffix)) {
            return FetchedFileType.META_DATA;
        } else if (fileName.endsWith(FetchedFileType.RAW_VATSIM_DATA_FILE.fileNameSuffix)) {
            return FetchedFileType.RAW_VATSIM_DATA_FILE;
        } else {
            return null;
        }
    }
}
