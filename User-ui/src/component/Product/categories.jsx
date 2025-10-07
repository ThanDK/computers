export const categoriesData = [
    { slug: 'cpu', name_en: 'CPU', name_th: 'หน่วยประมวลผล' },
    { slug: 'motherboard', name_en: 'Mainboard', name_th: 'เมนบอร์ด' },
    { slug: 'ram', name_en: 'RAM', name_th: 'หน่วยความจำ' },
    { slug: 'gpu', name_en: 'VGA', name_th: 'การ์ดจอ' },
    { slug: 'storage', name_en: 'SSD', name_th: 'อุปกรณ์จัดเก็บข้อมูล' },
    { slug: 'psu', name_en: 'Power Supply', name_th: 'พาวเวอร์ซัพพลาย' },
    { slug: 'case', name_en: 'Case', name_th: 'เคส' }, 
    { slug: 'cooler', name_en: 'Cooler', name_th: 'ชุดระบายความร้อน' } 
];

/**
 * Retrieves the category name by its slug. Defaults to Thai.
 * @param {string} slug - The slug of the category (e.g., 'cpu').
 * @param {string} lang - The desired language ('en' or 'th'). Defaults to 'th'.
 * @returns {string|null} The name of the category in the specified language, or null if not found.
 */
export const getCategoryNameBySlug = (slug, lang = 'th') => {
    const category = categoriesData.find(cat => cat.slug.toLowerCase() === slug.toLowerCase());
    if (!category) {
        return null;
    }
    
    const nameKey = `name_${lang}`;
    // Fallback to English name if the specified language name doesn't exist, then to the first available name.
    return category[nameKey] || category.name_en || Object.values(category).find(val => typeof val === 'string');
};