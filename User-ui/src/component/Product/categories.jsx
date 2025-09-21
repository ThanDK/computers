

export const categoriesData = [
    { name: 'CPU / หน่วยประมวลผล', slug: 'cpu' },
    { name: 'Mainboard / เมนบอร์ด', slug: 'motherboard' },
    { name: 'RAM / หน่วยความจำ', slug: 'ram' },
    { name: 'VGA / การ์ดจอ', slug: 'gpu' },
    { name: 'SSD / อุปกรณ์จัดเก็บข้อมูล', slug: 'storage' },
    { name: 'Power Supply / พาวเวอร์ซัพพลาย', slug: 'psu' },
    { name: 'Case / เคส', slug: 'case' }, 
    { name: 'Cooler / ชุดระบายความร้อน', slug: 'cooler' } 
];


export const getCategoryNameBySlug = (slug) => {
    const category = categoriesData.find(cat => cat.slug === slug);
    return category ? category.name : null;
};