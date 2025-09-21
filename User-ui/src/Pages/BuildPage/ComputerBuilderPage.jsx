

import React, { useState, useEffect, useCallback, useContext } from 'react';
import { Container, Row, Col, Card, Button, Form, Alert, Spinner, Modal, InputGroup, NavDropdown } from 'react-bootstrap';
import { FaPlus, FaTrash, FaPen, FaCheckCircle, FaExclamationTriangle, FaMinus } from 'react-icons/fa'; 
import { getProductsByCategory, checkCompatibilityApi, saveComputerBuild, getSavedBuildsForUser, getBuildDetailsById } from '../api/buildApi.jsx'; 
import { debounce } from 'lodash';
import { CartContext } from '../context/CartContext';
import { useNavigate } from 'react-router-dom';

const ProductSelectionModal = ({ show, onHide, category, onSelectProduct }) => {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');

    useEffect(() => {
        if (show) {
            setLoading(true);
            getProductsByCategory(category.key)
                .then(data => {
                    const type = category.key.toLowerCase().replace('drives', '').replace('kits', '');
                    const correctlyTypedData = data.filter(p => p.type.toLowerCase() === type);
                    setProducts(correctlyTypedData);
                    setLoading(false);
                })
                .catch(err => { console.error(err); setLoading(false); });
        }
    }, [show, category.key]);
    
    const filteredProducts = products.filter(p => p.name.toLowerCase().includes(searchTerm.toLowerCase()));

    return (
        <Modal show={show} onHide={onHide} size="lg">
            <Modal.Header closeButton><Modal.Title>เลือก {category.label}</Modal.Title></Modal.Header>
            <Modal.Body>
                <Form.Control type="text" placeholder="ค้นหาสินค้า..." className="mb-3" value={searchTerm} onChange={e => setSearchTerm(e.target.value)} />
                {loading ? (<div className="text-center"><Spinner animation="border" /></div>) : (
                    <ul className="list-group">
                        {filteredProducts.map(product => (
                            <li key={product._id} className="list-group-item d-flex justify-content-between align-items-center">
                                <div><img src={product.imageUrl} alt={product.name} width="50" className="me-3" />{product.name}</div>
                                <div><span className="me-3">{product.price.toLocaleString()} ฿</span><Button variant="primary" size="sm" onClick={() => onSelectProduct(category.key, product)}>เลือก</Button></div>
                            </li>
                        ))}
                    </ul>
                )}
            </Modal.Body>
        </Modal>
    );
};


const ComputerBuilderPage = () => {
    const { addToCart } = useContext(CartContext);
    const navigate = useNavigate();
    const [isAddingToCart, setIsAddingToCart] = useState(false);

    const [build, setBuild] = useState({
        buildName: 'My Awesome PC Build',
        cpu: null, cooler: null, motherboard: null, case: null, power_supply: null,
        ramKits: [], gpus: [], storageDrives: []
    });
    const [totalPrice, setTotalPrice] = useState(0);
    const [compatibility, setCompatibility] = useState(null);
    const [isChecking, setIsChecking] = useState(false);
    const [modalState, setModalState] = useState({ show: false, category: { key: '', label: '' } });
    const [savedBuilds, setSavedBuilds] = useState([]);

    useEffect(() => {
        getSavedBuildsForUser().then(data => {
            if (data && Array.isArray(data)) { setSavedBuilds(data); }
        });
    }, []);

    const componentCategories = [
        { key: 'cpu', label: 'CPU', isMulti: false }, { key: 'cooler', label: 'CPU Cooler', isMulti: false },
        { key: 'motherboard', label: 'Motherboard', isMulti: false }, { key: 'ramKits', label: 'RAM', isMulti: true },
        { key: 'gpus', label: 'Video Card', isMulti: true }, { key: 'storageDrives', label: 'Storage', isMulti: true },
        { key: 'case', label: 'Case', isMulti: false }, { key: 'power_supply', label: 'Power Supply', isMulti: false },
    ];

    const handleOpenModal = (category) => setModalState({ show: true, category });
    const handleCloseModal = () => setModalState({ show: false, category: { key: '', label: '' } });

    const handleSelectProduct = (categoryKey, product) => {
        const categoryConfig = componentCategories.find(c => c.key === categoryKey);
        if (categoryConfig.isMulti) {
            setBuild(prev => {
                const existingItem = prev[categoryKey].find(item => item.partDetails._id === product._id);
                if (existingItem) {
                    const updatedItems = prev[categoryKey].map(item => item.partDetails._id === product._id ? { ...item, quantity: item.quantity + 1 } : item);
                    return { ...prev, [categoryKey]: updatedItems };
                } else {
                    return { ...prev, [categoryKey]: [...prev[categoryKey], { partDetails: product, quantity: 1 }] };
                }
            });
        } else { setBuild(prev => ({ ...prev, [categoryKey]: product })); }
        handleCloseModal();
    };

    const handleRemoveProduct = (categoryKey, productIdToRemove) => {
        const categoryConfig = componentCategories.find(c => c.key === categoryKey);
        if (categoryConfig.isMulti) {
            setBuild(prev => ({ ...prev, [categoryKey]: prev[categoryKey].filter(item => item.partDetails._id !== productIdToRemove) }));
        } else { setBuild(prev => ({ ...prev, [categoryKey]: null })); }
    };

    const handleUpdateQuantity = (categoryKey, productId, newQuantity) => {
        if (newQuantity < 1) { handleRemoveProduct(categoryKey, productId); return; }
        setBuild(prev => ({ ...prev, [categoryKey]: prev[categoryKey].map(item => item.partDetails._id === productId ? { ...item, quantity: newQuantity } : item) }));
    };
    
    const handleLoadBuild = async (buildId) => {
        if (!buildId) return;
        const result = await getBuildDetailsById(buildId);
        if (result.success) {
            const loadedData = result.data;
            setBuild({
                buildName: loadedData.buildName, cpu: loadedData.cpu, motherboard: loadedData.motherboard, cooler: loadedData.cooler,
                case: loadedData.caseDetail, power_supply: loadedData.psu, ramKits: loadedData.ramKits || [],
                gpus: loadedData.gpus || [], storageDrives: loadedData.storageDrives || [],
            });
            alert(`โหลด Build "${loadedData.buildName}" สำเร็จ!`);
        } else { alert("ไม่สามารถโหลดข้อมูล Build ได้"); }
    };

    useEffect(() => {
        let newTotal = 0;
        componentCategories.forEach(cat => {
            const item = build[cat.key];
            if (!item) return;
            if (cat.isMulti) { item.forEach(part => { newTotal += (part.partDetails.price * part.quantity); });
            } else { newTotal += item.price || 0; }
        });
        setTotalPrice(newTotal);
        setIsChecking(true);
        debouncedCheckCompatibility(build);
    }, [build]);

    const debouncedCheckCompatibility = useCallback(debounce(async (currentBuild) => {
        const result = await checkCompatibilityApi(currentBuild);
        setCompatibility(result); setIsChecking(false);
    }, 1000), []);

    const handleSaveBuild = () => {
        const toComponentMap = (items) => items.reduce((acc, item) => {
            acc[item.partDetails._id] = item.quantity;
            return acc;
        }, {});
        const request = {
            buildName: build.buildName, cpuId: build.cpu?._id, motherboardId: build.motherboard?._id, psuId: build.power_supply?._id,
            caseId: build.case?._id, coolerId: build.cooler?._id, ramKits: toComponentMap(build.ramKits),
            gpus: toComponentMap(build.gpus), storageDrives: toComponentMap(build.storageDrives),
        };
        saveComputerBuild(request).then(res => {
            if (res.success) {
                alert("บันทึก Build สำเร็จ!");
                getSavedBuildsForUser().then(data => setSavedBuilds(data || []));
            } else { alert("เกิดข้อผิดพลาดในการบันทึก Build"); }
        });
    };

    const handleAddBuildToCart = async () => {
        setIsAddingToCart(true);
        try {
            const toComponentMap = (items) => items.reduce((acc, item) => {
                acc[item.partDetails._id] = item.quantity; return acc;
            }, {});
            const saveBuildRequest = {
                buildName: build.buildName, cpuId: build.cpu?._id, motherboardId: build.motherboard?._id, psuId: build.power_supply?._id,
                caseId: build.case?._id, coolerId: build.cooler?._id, ramKits: toComponentMap(build.ramKits),
                gpus: toComponentMap(build.gpus), storageDrives: toComponentMap(build.storageDrives),
            };
            const savedBuildResponse = await saveComputerBuild(saveBuildRequest);
            if (!savedBuildResponse.success || !savedBuildResponse.id) { throw new Error("Failed to save build."); }
            const savedBuildId = savedBuildResponse.id;

            const addToCartRequest = { productId: savedBuildId, quantity: 1, itemType: 'BUILD' };
            await addToCart(addToCartRequest);
            
            alert(`"${build.buildName}" ถูกเพิ่มลงในตะกร้าแล้ว!`);
            navigate('/cart');
        } catch (error) {
            console.error("Error adding build to cart:", error);
            alert("เกิดข้อผิดพลาดในการเพิ่ม Build ลงตะกร้า: " + (error.message || "Please try again."));
        } finally { setIsAddingToCart(false); }
    };

    const ComponentRow = ({ category }) => (
        <Row className="mb-3 border-bottom pb-3 align-items-start">
            <Col md={2} className="fw-bold pt-2">{category.label}</Col>
            <Col md={7}>
                {category.isMulti ? (
                    build[category.key].length > 0 ? (
                        build[category.key].map(item => (
                            <div key={item.partDetails._id} className="d-flex align-items-center mb-2">
                                <img src={item.partDetails.imageUrl} alt={item.partDetails.name} width="60" className="me-3 border rounded" />
                                <div style={{ flexGrow: 1 }}><p className="mb-0">{item.partDetails.name}</p><small className="text-muted">ราคา: {(item.partDetails.price).toLocaleString()} ฿</small></div>
                                <div className="d-flex align-items-center">
                                    <Button size="sm" variant="outline-secondary" onClick={() => handleUpdateQuantity(category.key, item.partDetails._id, item.quantity - 1)}><FaMinus /></Button>
                                    <span className="mx-2 fw-bold">{item.quantity}</span>
                                    <Button size="sm" variant="outline-secondary" onClick={() => handleUpdateQuantity(category.key, item.partDetails._id, item.quantity + 1)}><FaPlus /></Button>
                                    <Button size="sm" variant="danger" className="ms-3" onClick={() => handleRemoveProduct(category.key, item.partDetails._id)}><FaTrash /></Button>
                                </div>
                            </div>
                        ))
                    ) : <p className="mb-0 text-muted pt-2">ยังไม่ได้เลือกสินค้า</p>
                ) : (
                    build[category.key] ? (
                        <div className="d-flex align-items-center"><img src={build[category.key].imageUrl} alt={build[category.key].name} width="60" className="me-3 border rounded" /><div><p className="mb-0">{build[category.key].name}</p><small className="text-muted">ราคา: {build[category.key].price.toLocaleString()} ฿</small></div></div>
                    ) : <p className="mb-0 text-muted pt-2">ยังไม่ได้เลือกสินค้า</p>
                )}
            </Col>
            <Col md={3} className="text-end pt-2">
                {category.isMulti ? (<Button variant="primary" onClick={() => handleOpenModal(category)}><FaPlus /> เพิ่ม</Button>)
                : (build[category.key] ? (<Button variant="outline-danger" size="sm" onClick={() => handleRemoveProduct(category.key)}><FaTrash /> ลบ</Button>)
                : (<Button variant="primary" onClick={() => handleOpenModal(category)}><FaPlus /> เลือก</Button>))}
            </Col>
        </Row>
    );

    return (
        <Container className="py-5">
            <h2 className="mb-4">จัดสเปคคอมพิวเตอร์ (Build your PC)</h2>
            {isChecking ? (<Alert variant="info"><Spinner as="span" animation="border" size="sm" role="status" aria-hidden="true" /> กำลังตรวจสอบความเข้ากันได้...</Alert>) 
             : compatibility && (
                <Alert variant={compatibility.isCompatible ? 'success' : 'warning'}>
                    <Alert.Heading className='d-flex align-items-center'>{compatibility.isCompatible ? <FaCheckCircle className="me-2"/> : <FaExclamationTriangle className="me-2"/>}ผลการตรวจสอบ</Alert.Heading>
                    {compatibility.errors.length > 0 && (<div className="text-danger"><strong>ข้อผิดพลาด:</strong><ul>{compatibility.errors.map((e, i) => <li key={i}>{e}</li>)}</ul></div>)}
                    {compatibility.warnings.length > 0 && (<div className="mt-2"><strong>คำเตือน:</strong><ul>{compatibility.warnings.map((w, i) => <li key={i}>{w}</li>)}</ul></div>)}
                    <hr /><p className="mt-2 mb-0"><strong>Estimated Wattage:</strong> {compatibility.totalWattage}W</p>
                </Alert>
            )}
            <Card>
                <Card.Header>
                    <Form.Group as={Row} className="align-items-center m-0">
                        <Form.Label column sm="2" className="fw-bold">ชื่อ Build</Form.Label>
                        <Col sm="6">
                            <InputGroup><Form.Control value={build.buildName} onChange={e => setBuild(prev => ({ ...prev, buildName: e.target.value }))} /><Button variant="outline-secondary"><FaPen /></Button></InputGroup>
                        </Col>
                        <Col sm="4" className="text-end">
                            {savedBuilds.length > 0 && (
                                <NavDropdown title="โหลด Build ที่บันทึกไว้" id="load-build-dropdown" variant="info">
                                    {savedBuilds.map(saved => (
                                        <NavDropdown.Item key={saved.id} onClick={() => handleLoadBuild(saved.id)}>
                                            {saved.buildName} (฿{Number(saved.totalPrice).toLocaleString()})
                                        </NavDropdown.Item>
                                    ))}
                                </NavDropdown>
                            )}
                        </Col>
                    </Form.Group>
                </Card.Header>
                <Card.Body className="p-4">{componentCategories.map(cat => (<ComponentRow key={cat.key} category={cat} />))}</Card.Body>
                <Card.Footer className="d-flex justify-content-between align-items-center bg-light p-3">
                    <div><h4>ราคารวม: <span className="text-danger fw-bold">{totalPrice.toLocaleString()} ฿</span></h4></div>
                    <div>
                        <Button variant="secondary" className="me-2" onClick={handleSaveBuild}>บันทึก Build</Button>
                        <Button variant="danger" size="lg" disabled={totalPrice === 0 || !compatibility?.isCompatible || isAddingToCart} onClick={handleAddBuildToCart}>
                            {isAddingToCart ? (<><Spinner as="span" animation="border" size="sm" /> กำลังดำเนินการ...</>) : ('เพิ่มลงตะกร้าทั้งหมด')}
                        </Button>
                    </div>
                </Card.Footer>
            </Card>
            <ProductSelectionModal show={modalState.show} onHide={handleCloseModal} category={modalState.category} onSelectProduct={handleSelectProduct} />
        </Container>
    );
};

export default ComputerBuilderPage;