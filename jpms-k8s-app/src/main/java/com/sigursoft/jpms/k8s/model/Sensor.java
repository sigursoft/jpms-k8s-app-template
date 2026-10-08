package com.sigursoft.jpms.k8s.model;

import java.util.regex.Pattern;

/**
 * A sensor. Instances are always valid: the id must match {@link #ID_PATTERN} and the description is required
 * and at most {@link #MAX_DESCRIPTION_LENGTH} characters long.
 *
 * @throws IllegalArgumentException if the id or the description is invalid
 */
public record Sensor(String id, String description) {

    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    public static final String ID_PATTERN = VALID_ID.pattern();

    public static final int MAX_DESCRIPTION_LENGTH = 1024;

    public Sensor {
        if (id == null || !VALID_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Property 'id' is required and must match " + ID_PATTERN);
        }
        if (description == null) {
            throw new IllegalArgumentException("Property 'description' is required");
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                    "Property 'description' must be at most " + MAX_DESCRIPTION_LENGTH + " characters");
        }
    }
}
