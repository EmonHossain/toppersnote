package com.sharenote.user.converter;

import com.sharenote.user.dto.Gender;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class GenderAttributeConverter implements AttributeConverter<Gender, Integer> {

    // convertToDatabaseColumn
    @Override
    public Integer convertToDatabaseColumn(Gender attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    // convertToEntityAttribute
    @Override
    public Gender convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return Gender.fromValue(dbData);
    }

}
