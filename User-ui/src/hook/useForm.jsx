

import { useState, useCallback } from 'react';

export const useForm = (initialState, onSubmitCallback) => {
    const [formData, setFormData] = useState(initialState);

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData(prevData => ({
            ...prevData,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    const handleSubmit = (e) => {
        e.preventDefault();
      
        onSubmitCallback(formData);
    };
    
   
    const updateFormData = useCallback((newData) => {
        setFormData(newData);
    }, []);

    return {
        formData,
        handleChange,
        handleSubmit,
        setFormData: updateFormData
    };
};