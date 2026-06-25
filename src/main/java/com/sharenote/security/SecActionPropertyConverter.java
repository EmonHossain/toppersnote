package com.sharenote.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SecActionPropertyConverter implements AttributeConverter<SecAction, String> {

    @Override
    public String convertToDatabaseColumn(SecAction action) {
        if (action == null) {
            return null;
        }
        return action.getValue();
    }

    @Override
    public SecAction convertToEntityAttribute(String dbData) {

        if (dbData == null) {
            return null;
        }
        for (SecAction action : SecAction.values()) {
            if (action.getValue().equals(dbData)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown database column value : " + dbData);
    }

}
