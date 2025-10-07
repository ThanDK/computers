import React from 'react';

const Footer = () => {
    return (
        <footer className="text-center p-4 mt-auto" style={{ backgroundColor: '#f8f9fa' }}>
            <div className="d-flex justify-content-center gap-4">
                <a href="/contact-us" className="text-dark text-decoration-none">
                    Contact Us
                </a>
                <a href="/how-to-order" className="text-dark text-decoration-none">
                    How to Order
                </a>
                <a href="/how-to-payment" className="text-dark text-decoration-none">
                    How to Payment
                </a>
            </div>
        </footer>
    );
};

export default Footer;