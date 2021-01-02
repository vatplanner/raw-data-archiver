package org.vatplanner.archiver.local;

import java.util.regex.Pattern;

/**
 * Validation utility method.
 */
public class Validation {
    private static final Pattern PATTERN_FORMAT_NAME = Pattern.compile("^[a-z0-9_\\-\\.]{1,16}$");

    private Validation() {
        // utility class, hide constructor
    }

    /**
     * Checks if the given string is valid for use as a data file format name.
     * <p>
     * Data file format names must be lower-case, have a length of 1 to 16
     * characters and only use a-z, 0-9, dot, underscore or hyphen.
     * </p>
     * 
     * @param s string to check for format name validity
     * @return true if given string is valid as a data file format name
     */
    public static boolean validateDataFileFormatName(String s) {
        return PATTERN_FORMAT_NAME.matcher(s).matches();
    }
}
