import React from 'react';
import { Container } from 'react-bootstrap';
import { useSearchParams } from 'react-router-dom';
import ExploreComponent from '../../component/Product/ExploreComponent';

const SearchResultsPage = () => {
    
    const [searchParams] = useSearchParams();
    const query = searchParams.get('q') || ''; 

    return (
        <Container className="my-5">
            <h2 className="mb-4">
                {}
                {query ? `ผลการค้นหาสำหรับ: "${query}"` : 'กรุณาใส่คำเพื่อค้นหา'}
            </h2>

            {}
            <ExploreComponent searchQuery={query} />
        </Container>
    );
};

export default SearchResultsPage;