package com.sharenote.user.converter;

import com.sharenote.user.dto.ProfileCompletionStatus;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProfileCompltConverter implements AttributeConverter<ProfileCompletionStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ProfileCompletionStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public ProfileCompletionStatus convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return ProfileCompletionStatus.fromValue(dbData);
    }
}