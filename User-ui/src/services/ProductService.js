import api from '../api/api';

export const searchProducts = async (criteria) => {
    try {
        const params = new URLSearchParams();

        if (criteria.category) params.append('category', criteria.category);
        if (criteria.keyword) params.append('keyword', criteria.keyword);
        if (criteria.brands && criteria.brands.length > 0) {
            criteria.brands.forEach(brandId => params.append('brands', brandId));
        }
        if (criteria.minPrice) params.append('minPrice', criteria.minPrice);
        if (criteria.maxPrice) params.append('maxPrice', criteria.maxPrice);
        if (criteria.sortBy) params.append('sortBy', criteria.sortBy);
        params.append('page', criteria.page || 0);
        if (criteria.size) params.append('size', criteria.size);
        
        if (criteria.specs) {
            for (const key in criteria.specs) {
                const values = criteria.specs[key];
                if (Array.isArray(values) && values.length > 0) {
                    values.forEach(value => params.append(`specs[${key}]`, value));
                }
            }
        }

        const response = await api.get(`/products?${params.toString()}`);
        return response.data;
    } catch (error) {
        console.error("Error searching products:", error);
        throw error;
    }
};

export const getProductById = async (productId) => {
    try {
        const response = await api.get(`/products/${productId}`);
        return response.data;
    } catch (error) {
        console.error(`Error fetching product with ID ${productId}:`, error);
        throw error;
    }
};

export const getAvailableFilters = async (category) => {
    try {
        const params = new URLSearchParams();
        if (category) {
            params.append('category', category);
        }
        const response = await api.get(`/products/filters?${params.toString()}`);
        return response.data;
    } catch (error) {
        console.error("Error fetching available filters:", error);
        throw error;
    }
};