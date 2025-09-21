import React, { useState } from 'react';
import { Container, Navbar, Nav, Form, InputGroup, Button, Badge, NavDropdown } from 'react-bootstrap';
import { FaSearch, FaUser, FaUserPlus, FaShoppingCart, FaSignOutAlt, FaCog } from 'react-icons/fa'; 
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext'; 
import './Header.css';

const categories = [
    { name: 'CPU / หน่วยประมวลผล', slug: 'cpu' },
    { name: 'Mainboard / เมนบอร์ด', slug: 'motherboard' }, 
    { name: 'RAM / หน่วยความจำ', slug: 'ram' },
    { name: 'VGA / การ์ดจอ', slug: 'gpu' },
    { name: 'SSD / อุปกรณ์จัดเก็บข้อมูล', slug: 'storage' },
    { name: 'Power Supply / พาวเวอร์ซัพพลาย', slug: 'psu' },
    { name: 'Case / เคส', slug: 'case' }, 
    { name: 'Cooler / ชุดระบายความร้อน', slug: 'cooler' } 
];

const Header = () => {
    
    const { user, logout } = useAuth();
    const { itemCount } = useCart(); 
    const [searchTerm, setSearchTerm] = useState('');
    const navigate = useNavigate();

    const handleSearchSubmit = (e) => {
        e.preventDefault();
        if (searchTerm.trim()) {
            navigate(`/search?q=${encodeURIComponent(searchTerm.trim())}`);
            setSearchTerm('');
        }
    };
    
   
    const handleLogout = () => {
        logout();
        navigate('/'); 
    };

    return (
        <Navbar bg="white" expand="lg" className="border-bottom shadow-sm py-3 sticky-top">
            <Container>
                <Navbar.Brand as={Link} to="/" className="fw-bold logo-color">IT SHOP</Navbar.Brand>
                <Navbar.Toggle aria-controls="basic-navbar-nav" />
                <Navbar.Collapse id="basic-navbar-nav">
                    <Nav className="me-auto">
                        <Nav.Link as={Link} to="/builds">จัดสเปคคอม</Nav.Link> 
                        <NavDropdown title="หมวดหมู่สินค้า" id="basic-nav-dropdown">
                            {categories.map((category) => (
                                <NavDropdown.Item 
                                    key={category.slug} 
                                    as={Link} 
                                    to={`/products/category/${category.slug}`}
                                >
                                    {category.name}
                                </NavDropdown.Item>
                            ))}
                        </NavDropdown>
                    </Nav>

                    <Form className="d-flex my-2 my-lg-0 mx-auto" style={{ maxWidth: '400px', width: '100%' }} onSubmit={handleSearchSubmit}>
                        <InputGroup>
                            <Form.Control
                                type="search"
                                placeholder="ค้นหาสินค้า"
                                aria-label="Search"
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                            />
                            <Button variant="danger" id="button-search" type="submit">
                                <FaSearch />
                            </Button>
                        </InputGroup>
                    </Form>
                    
                    <Nav className="ms-lg-auto align-items-center">
                        {}
                        <Nav.Link as={Link} to="/cart" className="position-relative me-3">
                            <FaShoppingCart size="1.4rem" />
                            {}
                            {user && itemCount > 0 && (
                                <Badge pill bg="danger" className="position-absolute top-0 start-100 translate-middle" style={{ fontSize: '0.6em', padding: '0.4em 0.5em' }}>
                                    {itemCount}
                                </Badge>
                            )}
                        </Nav.Link>

                        {user ? (
                         
                           <NavDropdown 
                                title={
                                    <div className="d-flex align-items-center">
                                        <FaUser size="1.4rem" className="me-2" />
                                        <span>{user.name || user.email}</span>
                                    </div>
                                } 
                                id="user-nav-dropdown" 
                                align="end"
                            >
                                <NavDropdown.Item as={Link} to="/profile">
                                    <FaCog className="me-2" />
                                    จัดการบัญชี
                                </NavDropdown.Item>
                                <NavDropdown.Item as={Link} to="/profile/orders" >
                                    <FaCog className="me-2" />
                                    คำสั่งซื้อ
                                </NavDropdown.Item>
                                <NavDropdown.Divider />
                                <NavDropdown.Item onClick={handleLogout} className="text-danger">
                                    <FaSignOutAlt className="me-2" />
                                    ออกจากระบบ
                                </NavDropdown.Item>
                            </NavDropdown>
                        ) : (
                            
                            <>
                                <Nav.Link as={Link} to="/login" className="d-flex align-items-center">
                                    <FaUser className="me-2" /> เข้าสู่ระบบ
                                </Nav.Link>
                                <Nav.Link as={Link} to="/register" className="d-flex align-items-center">
                                    <FaUserPlus className="me-2" /> สมัครสมาชิก
                                </Nav.Link>
                            </>
                        )}
                    </Nav>
                </Navbar.Collapse>
            </Container>
        </Navbar>
    );
};

export default Header;