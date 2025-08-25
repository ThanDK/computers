import React, { useState, useRef } from 'react';
import { Modal, Button, ButtonGroup } from 'react-bootstrap';
import ReactCrop, { centerCrop, makeAspectCrop } from 'react-image-crop';
import 'react-image-crop/dist/ReactCrop.css';

// ฟังก์ชัน helper สำหรับสร้างกรอบ crop เริ่มต้นให้อยู่กลางรูปภาพ
function centerAspectCrop(mediaWidth, mediaHeight, aspect) {
    if (!aspect) { 
        // ถ้าไม่ได้ล็อค aspect ratio ก็ให้เป็นกรอบสี่เหลี่ยมอิสระกลางๆ
        return centerCrop(
            { unit: '%', width: 90, height: 90 },
            mediaWidth,
            mediaHeight
        );
    }
    // ถ้ามีการล็อค aspect ratio ก็ให้สร้างกรอบตามนั้นแล้วจัดให้อยู่กลาง
    return centerCrop(
        makeAspectCrop({ unit: '%', width: 90 }, aspect, mediaWidth, mediaHeight),
        mediaWidth,
        mediaHeight,
    );
}

// ฟังก์ชันสำคัญที่ใช้ canvas 'วาด' ส่วนที่ crop ออกมา แล้วแปลงเป็นไฟล์รูปภาพจริงๆ
async function getCroppedImg(image, crop, fileName) {
    const canvas = document.createElement('canvas');
    const scaleX = image.naturalWidth / image.width;
    const scaleY = image.naturalHeight / image.height;
    canvas.width = crop.width;
    canvas.height = crop.height;
    const ctx = canvas.getContext('2d');

    ctx.drawImage(
        image,
        crop.x * scaleX,
        crop.y * scaleY,
        crop.width * scaleX,
        crop.height * scaleY,
        0,
        0,
        crop.width,
        crop.height
    );

    // แปลงสิ่งที่วาดใน canvas ให้กลายเป็น File object
    return new Promise((resolve) => {
        canvas.toBlob(blob => {
            if (!blob) {
                console.error('Canvas is empty');
                return;
            }
            resolve(new File([blob], fileName, { type: 'image/jpeg' }));
        }, 'image/jpeg', 0.9); // กำหนดคุณภาพของไฟล์ jpeg
    });
}


function ImageCropper({ imageSrc, show, onHide, onCropComplete, aspect = 1 }) {
    const [crop, setCrop] = useState();
    // completedCrop คือ state ที่จะเก็บค่าพิกัดสุดท้ายหลังจาก user ปล่อยเมาส์
    const [completedCrop, setCompletedCrop] = useState(null);
    const [currentAspect, setCurrentAspect] = useState(aspect);
    const imageRef = useRef(null);


    // ฟังก์ชันนี้จะทำงานตอนรูปโหลดเสร็จ เพื่อคำนวณและกำหนดพื้นที่ crop เริ่มต้นให้
    const onImageLoad = (e) => {
        imageRef.current = e.currentTarget;
        const { width, height } = e.currentTarget;
        setCrop(centerAspectCrop(width, height, currentAspect));
    };
    
    // จัดการตอนกดปุ่มเปลี่ยน aspect ratio แล้วคำนวณกรอบ crop ใหม่
    const handleAspectChange = (newAspect) => {
        setCurrentAspect(newAspect);
        if (imageRef.current) {
            const { width, height } = imageRef.current;
            setCrop(centerAspectCrop(width, height, newAspect));
        }
    };

    // ตอนกดยืนยัน ก็จะเรียก getCroppedImg เพื่อเอาไฟล์รูปที่ตัดแล้ว และส่งค่านั้นกลับไปหา parent component
    const handleConfirm = async () => {
        if (completedCrop?.width && completedCrop?.height && imageRef.current) {
            const croppedFile = await getCroppedImg(imageRef.current, completedCrop, 'cropped-component.jpg');
            onCropComplete(croppedFile);
            onHide();
        }
    };

    return (
        <Modal show={show} onHide={onHide} centered size="lg" backdrop="static">
            <Modal.Header closeButton>
                <Modal.Title>Crop Image</Modal.Title>
            </Modal.Header>
            <Modal.Body className="text-center bg-dark">
                {imageSrc && (
                    <ReactCrop
                        crop={crop}
                        onChange={c => setCrop(c)}
                        onComplete={c => setCompletedCrop(c)}
                        aspect={currentAspect}
                        className="d-inline-block"
                    >
                        {/* ref ตรงนี้สำคัญมาก เพราะเราต้องใช้ element ของรูปในการคำนวณ */}
                        <img ref={imageRef} src={imageSrc} onLoad={onImageLoad} alt="Crop" style={{ maxHeight: '70vh' }}/>
                    </ReactCrop>
                )}
            </Modal.Body>
            <Modal.Footer className="d-flex justify-content-between">
                {/* กลุ่มปุ่มสำหรับเปลี่ยน Aspect Ratio ของกรอบ crop */}
                <ButtonGroup>
                    <Button variant="outline-secondary" active={currentAspect === 16/9} onClick={() => handleAspectChange(16/9)}>16:9</Button>
                    <Button variant="outline-secondary" active={currentAspect === 9/16} onClick={() => handleAspectChange(9/16)}>9:16</Button>
                    <Button variant="outline-secondary" active={currentAspect === 1} onClick={() => handleAspectChange(1)}>Square Only</Button>
                    <Button variant="outline-secondary" active={!currentAspect} onClick={() => handleAspectChange(undefined)}>Free Square</Button>
                </ButtonGroup>
                
                <div>
                    <Button variant="secondary" onClick={onHide} className="me-2">
                        Cancel
                    </Button>
                    <Button variant="primary" onClick={handleConfirm} disabled={!completedCrop?.width}>
                        Confirm Crop
                    </Button>
                </div>
            </Modal.Footer>
        </Modal>
    );
}

export default ImageCropper;