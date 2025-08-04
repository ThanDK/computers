// src/services/ValidationService.js

import { COMPONENT_CONFIG } from '../config/ComponentFormConfig.jsx';

/**
 * A simple and direct validation service.
 * Checks that every required field has a value.
 * @param {object} formData - The current state of the form data.
 * @param {string} selectedType - The type of component being validated.
 * @returns {string[]} An array of error messages. An empty array means the data is valid.
 */
export function validateComponentData(formData, selectedType) {
    const errors = [];
    
    // An array of all required fields to check.
    const requiredFields = [
        { name: 'name', label: 'Component Name' },
        { name: 'mpn', label: 'MPN' },
        { name: 'brandId', label: 'Brand' },
        { name: 'price', label: 'Price' },
    ];

    // Add required fields for the "Add" page.
    if (formData.quantity !== undefined) {
        requiredFields.push({ name: 'quantity', label: 'Initial Stock' });
    }

    // Add specific required fields based on component type.
    const specificConfig = COMPONENT_CONFIG[selectedType];
    if (specificConfig?.fields) {
        specificConfig.fields.forEach(field => {
            if (field.required) {
                requiredFields.push({ name: field.name, label: field.label });
            }
        });
    }

    // Loop through and check each required field.
    requiredFields.forEach(field => {
        const value = formData[field.name];
        // Check for undefined, null, empty string, or empty array.
        if (value === undefined || value === null || value === '' || (Array.isArray(value) && value.length === 0)) {
            errors.push(`${field.label} is required.`);
        }
    });

    return errors;
}