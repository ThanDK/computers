import React, { useState, useEffect, useCallback } from 'react';
import { Container, Row, Col, Button, Form, InputGroup, Spinner, ListGroup } from 'react-bootstrap';
import { useParams, useNavigate } from 'react-router-dom';
import { FaCheckCircle, FaExclamationTriangle, FaPen, FaTrash } from 'react-icons/fa';
import ComponentSelectorModal from '../../component/Product/ComponentSelectorModal';
import { useCart } from '../../context/CartContext';
import * as BuildService from '../../services/BuildService';
import { notifySuccess, notifyError } from '../../services/NotificationService';

const componentCategories = [
    { key: 'cpu', name: 'CPU', multiple: false, dbType: 'CPU' },
    { key: 'cooler', name: 'CPU Cooler', multiple: false, dbType: 'Cooler' },
    { key: 'motherboard', name: 'Motherboard', multiple: false, dbType: 'Motherboard' },
    { key: 'ramKits', name: 'RAM', multiple: true, dbType: 'RAM' },
    { key: 'gpus', name: 'Video Card', multiple: true, dbType: 'GPU' },
    { key: 'storageDrives', name: 'Storage', multiple: true, dbType: 'Storage' },
    { key: 'caseDetail', name: 'Case', multiple: false, dbType: 'Case' },
    { key: 'psu', name: 'Power Supply', multiple: false, dbType: 'PSU' },
];

const PcBuilder = () => {
    const { buildId } = useParams();
    const navigate = useNavigate();
    const isEditing = !!buildId;

    const { addToCart, isUpdating: isCartUpdating } = useCart();

    const [buildName, setBuildName] = useState('My Awesome PC Build');
    const [parts, setParts] = useState({});
    const [loading, setLoading] = useState(isEditing);
    const [saving, setSaving] = useState(false);
    const [checkingCompatibility, setCheckingCompatibility] = useState(true);
    const [compatibility, setCompatibility] = useState({ errors: [], warnings: [], totalWattage: 0, isCompatible: true });
    const [totalPrice, setTotalPrice] = useState(0);
    const [isSelectorOpen, setIsSelectorOpen] = useState(false);
    const [selectingCategory, setSelectingCategory] = useState(null);

    const checkCompatibilityCallback = useCallback(async (currentParts) => {
        setCheckingCompatibility(true);
        const getPartId = (part) => part?.id || part?._id || null;
        const getMultiPartIds = (partArray) =>
            (partArray || []).reduce((acc, item) => {
                const partId = getPartId(item.partDetails);
                if (partId) {
                    acc[partId] = (acc[partId] || 0) + item.quantity;
                }
                return acc;
            }, {});

        const requestBody = {
            cpuId: getPartId(currentParts.cpu),
            motherboardId: getPartId(currentParts.motherboard),
            psuId: getPartId(currentParts.psu),
            caseId: getPartId(currentParts.caseDetail),
            coolerId: getPartId(currentParts.cooler),
            ramKits: getMultiPartIds(currentParts.ramKits),
            gpus: getMultiPartIds(currentParts.gpus),
            storageDrives: getMultiPartIds(currentParts.storageDrives),
        };

        const result = await BuildService.checkCompatibility(requestBody);
        setCompatibility(result);
        setCheckingCompatibility(false);
    }, []);

    useEffect(() => {
        if (isEditing) {
            const fetchBuild = async () => {
                try {
                    const data = await BuildService.getBuildById(buildId);
                    setBuildName(data.buildName);
                    const initialParts = {
                        cpu: data.cpu, motherboard: data.motherboard, ramKits: data.ramKits,
                        gpus: data.gpus, storageDrives: data.storageDrives, psu: data.psu,
                        caseDetail: data.caseDetail, cooler: data.cooler
                    };
                    setParts(initialParts);
                } catch (error) {
                    console.error("Failed to fetch build details:", error);
                    notifyError('Could not find the requested build or you do not have permission to view it.');
                    navigate('/builds');
                } finally {
                    setLoading(false);
                }
            };
            fetchBuild();
        } else {
            checkCompatibilityCallback({});
        }
    }, [buildId, isEditing, navigate, checkCompatibilityCallback]);

    useEffect(() => {
        const calculateTotalPrice = (currentParts) => {
            let total = 0;
            componentCategories.forEach(category => {
                const partData = currentParts[category.key];
                if (!partData) return;

                if (category.multiple) {
                    partData.forEach(item => {
                        const price = item.partDetails?.price || 0;
                        const quantity = item.quantity || 1;
                        total += price * quantity;
                    });
                } else {
                    const price = partData.price || 0;
                    total += price;
                }
            });
            return total;
        };
        
        setTotalPrice(calculateTotalPrice(parts));

        if (!loading) {
            checkCompatibilityCallback(parts);
        }
    }, [parts, loading, checkCompatibilityCallback]);
    
    const handleOpenSelector = (category) => {
        setSelectingCategory(category);
        setIsSelectorOpen(true);
    };

    const handleComponentSelected = (selectedComponent) => {
        if (!selectedComponent || !(selectedComponent.id || selectedComponent._id)) {
            console.error("Selected component is invalid:", selectedComponent);
            return;
        }

        setParts(prev => {
            const newParts = { ...prev };
            const categoryKey = selectingCategory.key;
            if (selectedComponent._id && !selectedComponent.id) selectedComponent.id = selectedComponent._id;

            if (selectingCategory.multiple) {
                const existing = newParts[categoryKey] ? [...newParts[categoryKey]] : [];
                const newItem = { 
                    partDetails: selectedComponent, 
                    quantity: 1,
                    instanceId: Date.now() + Math.random()
                };
                existing.push(newItem);
                newParts[categoryKey] = existing;
            } else {
                newParts[categoryKey] = selectedComponent;
            }
            return newParts;
        });
        setIsSelectorOpen(false);
        setSelectingCategory(null);
    };

    const handleRemoveComponent = (categoryKey, idToRemove) => {
        setParts(prev => {
            const newParts = { ...prev };
            const category = componentCategories.find(c => c.key === categoryKey);
            if (category.multiple) {
                newParts[categoryKey] = (newParts[categoryKey] || []).filter(p => p.instanceId !== idToRemove);
            } else {
                newParts[categoryKey] = null;
            }
            return newParts;
        });
    };

    const handleSave = async () => {
        setSaving(true);
        const getPartId = (part) => part?.id || part?._id || null;
        const getMultiPartIds = (partArray) => (partArray || []).reduce((acc, item) => ({ ...acc, [getPartId(item.partDetails)]: item.quantity }), {});

        const request = {
            buildName,
            cpuId: getPartId(parts.cpu), motherboardId: getPartId(parts.motherboard),
            psuId: getPartId(parts.psu), caseId: getPartId(parts.caseDetail), coolerId: getPartId(parts.cooler),
            ramKits: getMultiPartIds(parts.ramKits),
            gpus: getMultiPartIds(parts.gpus),
            storageDrives: getMultiPartIds(parts.storageDrives),
        };

        try {
            if (isEditing) {
                await BuildService.updateExistingBuild(buildId, request);
            } else {
                await BuildService.saveNewBuild(request);
            }
            notifySuccess(isEditing ? 'Build updated successfully!' : 'New build saved successfully!');
            navigate('/builds');
        } catch (error) {
            console.error("Failed to save build:", error);
            const errorMessage = error.response?.data?.message || 'An error occurred while saving the build.';
            notifyError(errorMessage);
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return <div className="text-center py-5"><Spinner animation="border" /> <p>Loading Build...</p></div>;
    }

    return (
        <Container className="py-4 bg-light">
            {selectingCategory && (
                <ComponentSelectorModal show={isSelectorOpen} onHide={() => setIsSelectorOpen(false)} category={selectingCategory} onSelect={handleComponentSelected} />
            )}
            <h2 className="mb-4">จัดสเปคคอมพิวเตอร์ (Build your PC)</h2>
            
            <div className="mb-4">
                {checkingCompatibility ? (
                     <div className="p-3 rounded bg-secondary-subtle">
                        <div className="d-flex align-items-center">
                            <Spinner animation="border" size="sm" className="me-2" />
                            <strong>กำลังตรวจสอบความเข้ากันได้...</strong>
                        </div>
                    </div>
                ) : (
                    <>
                        {compatibility.errors.length > 0 && (
                            <div className="p-3 rounded mb-3 bg-danger-subtle">
                                <h5 className="mb-2"><FaExclamationTriangle className="me-2 text-danger" />พบปัญหาความเข้ากันได้</h5>
                                <ul className="mb-0 ps-4">
                                    {compatibility.errors.map((e, i) => (
                                        <li key={`err-${i}`}><small className="text-danger">{e}</small></li>
                                    ))}
                                </ul>
                            </div>
                        )}
                        {compatibility.warnings.length > 0 && (
                             <div className="p-3 rounded mb-3 bg-warning-subtle">
                                <h5 className="mb-2"><FaExclamationTriangle className="me-2 text-warning" />มีคำเตือน</h5>
                                 <ul className="mb-0 ps-4">
                                    {compatibility.warnings.map((w, i) => (
                                        <li key={`warn-${i}`}><small className="text-dark">{w}</small></li>
                                    ))}
                                </ul>
                            </div>
                        )}
                        <div className="p-3 rounded bg-body-tertiary">
                             {compatibility.errors.length === 0 && compatibility.warnings.length === 0 && (
                                <p className="mb-2 text-success"><FaCheckCircle className="me-2" /><strong>เข้ากันได้ทั้งหมด</strong></p>
                             )}
                            <small>Estimated Wattage: <strong>{compatibility.totalWattage}W</strong></small>
                        </div>
                    </>
                )}
            </div>

            <div className="bg-white border rounded">
                <Row className="p-3 border-bottom m-0">
                    <Col md={2} className="d-flex align-items-center"><strong>ชื่อ Build</strong></Col>
                    <Col>
                        <InputGroup>
                            <Form.Control value={buildName} onChange={(e) => setBuildName(e.target.value)} />
                            <Button variant="outline-secondary"><FaPen /></Button>
                        </InputGroup>
                    </Col>
                </Row>
                {componentCategories.map((category) => {
                    const { key, name, multiple } = category;
                    const partData = parts[key];
                    const hasPart = partData && (!Array.isArray(partData) || partData.length > 0);
                    return (
                        <Row key={key} className="p-3 border-bottom m-0 d-flex align-items-center">
                            <Col md={2}><strong>{name}</strong></Col>
                            <Col md={7}>
                                {hasPart ? (
                                    multiple ? (
                                        <ListGroup variant="flush">
                                            {partData.map((item, index) => (
                                                <ListGroup.Item key={item.instanceId} className="d-flex justify-content-between align-items-center px-0 py-1 border-0">
                                                    <span>{item.quantity}x {item.partDetails.name}</span>
                                                    <Button size="sm" variant="link" className="text-danger" onClick={() => handleRemoveComponent(key, item.instanceId)}>
                                                        Remove
                                                    </Button>
                                                </ListGroup.Item>
                                            ))}
                                        </ListGroup>
                                    ) : (
                                        <span>{partData.name}</span>
                                    )
                                ) : (<span className="text-muted">ยังไม่ได้เลือกสินค้า</span>)}
                            </Col>
                            <Col md={3} className="text-end">
                                {multiple ? (
                                    <Button variant="primary" onClick={() => handleOpenSelector(category)}>+ เพิ่ม</Button>
                                ) : (
                                    <>
                                        <Button variant="primary" onClick={() => handleOpenSelector(category)}>+ {hasPart ? 'เปลี่ยน' : 'เพิ่ม'}</Button>
                                        {hasPart && (
                                            <Button variant="outline-danger" className="ms-2" onClick={() => handleRemoveComponent(key, partData.id || partData._id)}><FaTrash /></Button>
                                        )}
                                    </>
                                )}
                            </Col>
                        </Row>
                    );
                })}
                <Row className="p-3 m-0 d-flex justify-content-between align-items-center bg-body-tertiary">
                    <Col md="auto"><h3>ราคารวม: <span className="text-danger">฿{totalPrice.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span></h3></Col>
                    <Col md="auto">
                        <Button variant="success" className="me-2" onClick={handleSave} disabled={saving || compatibility.errors.length > 0 || checkingCompatibility}>
                            {saving ? <><Spinner size="sm" /> Saving...</> : (isEditing ? 'บันทึกการแก้ไข' : 'บันทึก Build ใหม่')}
                        </Button>
                        
                    </Col>
                </Row>
            </div>
        </Container>
    );
};

export default PcBuilder;