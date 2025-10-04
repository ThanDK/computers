import { useState, useCallback } from 'react';

export const useForm = (initialState, onSubmitCallback, validationRules = {}) => {
    const [formData, setFormData] = useState(initialState);
    const [errors, setErrors] = useState({});

    const validateField = useCallback((name, value) => {
        const rules = validationRules[name];
        if (!rules) return null;

        if (rules.required && !value) return rules.required;
        if (rules.minLength && value.length < rules.minLength.value) return rules.minLength.message;
        if (rules.maxLength && value.length > rules.maxLength.value) return rules.maxLength.message;
        if (rules.pattern && !rules.pattern.value.test(value)) return rules.pattern.message;
        
        return null;
    }, [validationRules]);

    const handleChange = useCallback((e) => {
        const { name, value, type, checked } = e.target;
        const fieldValue = type === 'checkbox' ? checked : value;

        setFormData(prevData => ({
            ...prevData,
            [name]: fieldValue
        }));

        // Validate field on change and update errors
        const error = validateField(name, fieldValue);
        setErrors(prevErrors => ({
            ...prevErrors,
            [name]: error
        }));
    }, [validateField]);

    const validateForm = useCallback(() => {
        let formErrors = {};
        let isValid = true;
        for (const fieldName in validationRules) {
            const error = validateField(fieldName, formData[fieldName] || '');
            if (error) {
                formErrors[fieldName] = error;
                isValid = false;
            }
        }
        setErrors(formErrors);
        return isValid;
    }, [formData, validationRules, validateField]);

    const handleSubmit = (e) => {
        e.preventDefault();
        if (validateForm()) {
            onSubmitCallback(formData);
        }
    };
    
    const updateFormData = useCallback((newData) => {
        setFormData(newData);
        
        setErrors({});
    }, []);

    return {
        formData,
        errors,
        handleChange,
        handleSubmit,
        setFormData: updateFormData,
        validate: validateForm, 
    };
};