import { COMPONENT_CONFIG } from '../config/ComponentFormConfig.jsx';

/**
 * ฟังก์ชันสำหรับตรวจสอบความถูกต้องของข้อมูลฟอร์ม component
 * โดยจะเช็คว่า field ที่จำเป็นทุกช่องมีค่าอยู่หรือไม่
 * @param {object} formData - State ปัจจุบันของข้อมูลในฟอร์ม
 * @param {string} selectedType - ประเภทของ component ที่กำลังถูกตรวจสอบ
 * @returns {string[]} Array ของข้อความ error. ถ้า Array ว่างหมายถึงข้อมูลถูกต้อง
 */
export function validateComponentData(formData, selectedType) {
    const errors = [];
    
    const requiredFields = [
        { name: 'name', label: 'Component Name' },
        { name: 'mpn', label: 'MPN' },
        { name: 'brandId', label: 'Brand' },
        { name: 'price', label: 'Price' },
    ];

    if (formData.quantity !== undefined) {
        requiredFields.push({ name: 'quantity', label: 'Initial Stock' });
    }

    const specificConfig = COMPONENT_CONFIG[selectedType];
    if (specificConfig?.fields) {
        specificConfig.fields.forEach(field => {
            if (field.required) {
                requiredFields.push({ name: field.name, label: field.label });
            }
        });
    }

    requiredFields.forEach(field => {
        const value = formData[field.name];
        if (value === undefined || value === null || value === '' || (Array.isArray(value) && value.length === 0)) {
            errors.push(`${field.label} is required.`);
        }
    });

    return errors;
}