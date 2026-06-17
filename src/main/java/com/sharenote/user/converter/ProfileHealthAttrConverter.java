package com.sharenote.user.converter;

import com.sharenote.user.dto.ProfileHealth;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProfileHealthAttrConverter implements AttributeConverter<ProfileHealth, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ProfileHealth attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public ProfileHealth convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return ProfileHealth.fromCode(dbData);
    }

}
