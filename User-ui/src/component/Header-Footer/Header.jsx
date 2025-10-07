import React, { useState } from 'react';
import { Container, Navbar, Nav, Form, Button, Badge, NavDropdown, Image } from 'react-bootstrap';
import { FaSearch, FaUser, FaUserPlus, FaShoppingCart, FaSignOutAlt, FaCog, FaBoxOpen } from 'react-icons/fa';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext';
import { categoriesData as categories } from '../Product/categories';
import './Header.css';

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
        <Navbar bg="white" expand="lg" className="site-header sticky-top">
            <Container>
                <Navbar.Brand as={Link} to="/" className="fw-bold logo-color">IT SHOP</Navbar.Brand>
                <Navbar.Toggle aria-controls="responsive-navbar-nav" />
                <Navbar.Collapse id="responsive-navbar-nav">
                    {/* Desktop View Layout */}
                    <div className="d-none d-lg-flex w-100 align-items-center">
                        <Nav className="me-auto">
                            <Nav.Link as={Link} to="/builds">จัดสเปคคอม</Nav.Link>
                            <NavDropdown title="หมวดหมู่สินค้า" id="desktop-nav-dropdown">
                                {categories.map((category) => (
                                    <NavDropdown.Item key={category.slug} as={Link} to={`/products/category/${category.slug}`}>
                                        {category.name_th}
                                    </NavDropdown.Item>
                                ))}
                            </NavDropdown>
                        </Nav>

                        <Form className="search-form mx-auto" onSubmit={handleSearchSubmit}>
                            <Form.Control
                                type="search"
                                placeholder="ค้นหาสินค้า"
                                aria-label="Search"
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                            />
                            <Button className="btn-search" type="submit"><FaSearch color="white" /></Button>
                        </Form>

                        <Nav className="ms-auto align-items-center">
                            <Nav.Link as={Link} to="/cart" className="position-relative nav-icon-btn me-2">
                                <FaShoppingCart size="1.2rem" />
                                {user && itemCount > 0 && (
                                    <Badge pill bg="danger" className="position-absolute top-0 start-100 translate-middle" style={{ fontSize: '0.6em', padding: '0.4em 0.5em' }}>{itemCount}</Badge>
                                )}
                            </Nav.Link>
                            {user ? (
                                <NavDropdown
                                    title={
                                        user.profilePictureUrl ? (
                                            <Image src={user.profilePictureUrl} alt={user.name} className="profile-picture" />
                                        ) : (
                                            <div className="profile-icon-fallback"><FaUser size="1.2rem" /></div>
                                        )
                                    }
                                    id="user-nav-dropdown"
                                    align="end"
                                    className="profile-dropdown-toggle"
                                >
                                    <div className="user-dropdown-header">
                                        <div className="user-dropdown-name">{user.name}</div>
                                        <div className="text-muted small">{user.email}</div>
                                    </div>
                                    <NavDropdown.Divider />
                                    <NavDropdown.Item as={Link} to="/profile"><FaCog className="me-2" />จัดการบัญชี</NavDropdown.Item>
                                    <NavDropdown.Item as={Link} to="/profile/orders"><FaBoxOpen className="me-2" />คำสั่งซื้อ</NavDropdown.Item>
                                    <NavDropdown.Divider />
                                    <NavDropdown.Item onClick={handleLogout} className="text-danger"><FaSignOutAlt className="me-2" />ออกจากระบบ</NavDropdown.Item>
                                </NavDropdown>
                            ) : (
                                <>
                                    <Nav.Link as={Link} to="/login">เข้าสู่ระบบ</Nav.Link>
                                    <Nav.Link as={Link} to="/register">สมัครสมาชิก</Nav.Link>
                                </>
                            )}
                        </Nav>
                    </div>

                    {/* Mobile View Layout */}
                    <div className="d-lg-none mt-3">
                        <Nav className="flex-column">
                            <Form className="d-flex mb-3" onSubmit={handleSearchSubmit}>
                                <Form.Control type="search" placeholder="ค้นหาสินค้า" value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} />
                                <Button variant="danger" type="submit" className="ms-2"><FaSearch /></Button>
                            </Form>
                            <Nav.Link as={Link} to="/builds">จัดสเปคคอม</Nav.Link>
                            <NavDropdown title="หมวดหมู่สินค้า" id="mobile-nav-dropdown">
                                {categories.map((category) => (
                                    <NavDropdown.Item key={category.slug} as={Link} to={`/products/category/${category.slug}`}>{category.name_th}</NavDropdown.Item>
                                ))}
                            </NavDropdown>

                            {user ? (
                                <>
                                    <hr/>
                                    <Nav.Link as={Link} to="/cart" className="d-flex align-items-center">
                                        รถเข็นสินค้า
                                        {itemCount > 0 && <Badge bg="danger" pill className="ms-2">{itemCount}</Badge>}
                                    </Nav.Link>
                                    <NavDropdown title="จัดการข้อมูล" id="mobile-user-dropdown">
                                        <div className="user-dropdown-header">
                                            <div className="user-dropdown-name">{user.name}</div>
                                            <div className="text-muted small">{user.email}</div>
                                        </div>
                                        <NavDropdown.Divider />
                                        <NavDropdown.Item as={Link} to="/profile"><FaCog className="me-2" />จัดการบัญชี</NavDropdown.Item>
                                        <NavDropdown.Item as={Link} to="/profile/orders"><FaBoxOpen className="me-2" />คำสั่งซื้อ</NavDropdown.Item>
                                        <NavDropdown.Divider />
                                        <NavDropdown.Item onClick={handleLogout} className="text-danger"><FaSignOutAlt className="me-2" />ออกจากระบบ</NavDropdown.Item>
                                    </NavDropdown>
                                </>
                            ) : (
                                <>
                                    <hr/>
                                    <Nav.Link as={Link} to="/login">เข้าสู่ระบบ</Nav.Link>
                                    <Nav.Link as={Link} to="/register">สมัครสมาชิก</Nav.Link>
                                </>
                            )}
                        </Nav>
                    </div>
                </Navbar.Collapse>
            </Container>
        </Navbar>
    );
};

export default Header;