import React from 'react';
import { Container, Button } from 'react-bootstrap';
import { Link } from 'react-router-dom';

const NotFoundPage = () => {
    return (
        <Container className="text-center d-flex flex-column justify-content-center align-items-center" style={{ minHeight: '100vh' }}>
            <h1 style={{ fontSize: '6rem', fontWeight: 'bold' }}>404</h1>
            <h2>Page Not Found</h2>
            <p className="text-muted">
                Sorry, the page you are looking for does not exist.
            </p>
            <Button as={Link} to="/" variant="primary" className="mt-3">
                Go Back to Home
            </Button>
        </Container>
    );
};

export default NotFoundPage;