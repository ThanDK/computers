// This file centralizes validation rules to be shared across components,
// ensuring consistency with the backend DTOs.

export const addressValidation = {
    contactName: {
        required: 'Contact name is required',
        minLength: { value: 2, message: 'Contact name must be at least 2 characters' },
        maxLength: { value: 100, message: 'Contact name cannot exceed 100 characters' },
        pattern: {
            // Replaced \p{L}\p{N} with a more compatible character set
            value: /^[A-Za-z0-9\s.'-]+$/,
            message: 'Contact name contains invalid characters'
        }
    },
    phoneNumber: {
        required: 'Phone number is required',
        minLength: { value: 9, message: 'Phone number must be at least 9 digits' },
        maxLength: { value: 15, message: 'Phone number cannot exceed 15 digits' },
        pattern: {
            value: /^[0-9+-]+$/,
            message: 'Phone number must contain only digits, +, or -'
        }
    },
    line1: {
        required: 'Address line 1 is required',
        maxLength: { value: 255, message: 'Address line 1 cannot exceed 255 characters' },
        pattern: {
            // Replaced \p{L}\p{N} with a more compatible character set
            value: /^[A-Za-z0-9\s-/.#,'()]+$/,
            message: 'Address line 1 contains invalid characters'
        }
    },
    line2: {
        maxLength: { value: 255, message: 'Address line 2 cannot exceed 255 characters' },
        pattern: {
            // Replaced \p{L}\p{N} with a more compatible character set
            value: /^[A-Za-z0-9\s-/.#,'()]*$/,
            message: 'Address line 2 contains invalid characters'
        }
    },
    subdistrict: {
        required: 'Subdistrict is required',
        maxLength: { value: 100, message: 'Subdistrict cannot exceed 100 characters' },
        pattern: {
            // Replaced \p{L} with a more compatible character set
            value: /^[A-Za-z\s-]+$/,
            message: 'Subdistrict contains invalid characters'
        }
    },
    district: {
        required: 'District is required',
        maxLength: { value: 100, message: 'District cannot exceed 100 characters' },
        pattern: {
            // Replaced \p{L} with a more compatible character set
            value: /^[A-Za-z\s-]+$/,
            message: 'District contains invalid characters'
        }
    },
    province: {
        required: 'Province is required',
        maxLength: { value: 100, message: 'Province cannot exceed 100 characters' },
        pattern: {
            // Replaced \p{L} with a more compatible character set
            value: /^[A-Za-z\s-]+$/,
            message: 'Province contains invalid characters'
        }
    },
    zipCode: {
        required: 'Zip code is required',
        minLength: { value: 5, message: 'Zip code must be at least 5 characters' },
        maxLength: { value: 10, message: 'Zip code cannot exceed 10 characters' },
        pattern: {
            value: /^[0-9A-Za-z-]+$/,
            message: 'Zip code contains invalid characters'
        }
    }
};