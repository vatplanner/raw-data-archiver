package org.vatplanner.archiver.local;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Identifies file types related to fetched data.
 */
public enum FetchedFileType {
    RAW_VATSIM_DATA_FILE(".*vatsim-data\\.(txt|json)$"),
    META_DATA(".*meta\\.json$");

    private final Pattern fileNamePattern;

    private FetchedFileType(String fileNamePattern) {
        this.fileNamePattern = Pattern.compile(fileNamePattern);
    }

    public static FetchedFileType byFileName(String fileName) {
        for (FetchedFileType type : FetchedFileType.values()) {
            if (type.matchesFileName(fileName)) {
                return type;
            }
        }

        return null;
    }

    private boolean matchesFileName(String fileName) {
        Matcher matcher = fileNamePattern.matcher(fileName);
        return matcher.matches();
    }
}
