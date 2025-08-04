import React, { useState, useRef } from 'react';
import { Modal, Button, ButtonGroup } from 'react-bootstrap'; // Import ButtonGroup
import ReactCrop, { centerCrop, makeAspectCrop } from 'react-image-crop';
import 'react-image-crop/dist/ReactCrop.css';

// --- Helper Functions (No changes here) ---
function centerAspectCrop(mediaWidth, mediaHeight, aspect) {
    if (!aspect) { // Handle freeform case
        return centerCrop(
            { unit: '%', width: 90, height: 90 },
            mediaWidth,
            mediaHeight
        );
    }
    return centerCrop(
        makeAspectCrop({ unit: '%', width: 90 }, aspect, mediaWidth, mediaHeight),
        mediaWidth,
        mediaHeight,
    );
}

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

    return new Promise((resolve) => {
        canvas.toBlob(blob => {
            if (!blob) {
                console.error('Canvas is empty');
                return;
            }
            resolve(new File([blob], fileName, { type: 'image/jpeg' }));
        }, 'image/jpeg', 0.9);
    });
}


function ImageCropper({ imageSrc, show, onHide, onCropComplete, aspect = 1 }) {
    const [crop, setCrop] = useState();
    const [completedCrop, setCompletedCrop] = useState(null);
    const [currentAspect, setCurrentAspect] = useState(aspect);
    const imageRef = useRef(null);


    const onImageLoad = (e) => {
        imageRef.current = e.currentTarget;
        const { width, height } = e.currentTarget;
        setCrop(centerAspectCrop(width, height, currentAspect));
    };
    
    const handleAspectChange = (newAspect) => {
        setCurrentAspect(newAspect);
        if (imageRef.current) {
            const { width, height } = imageRef.current;
            setCrop(centerAspectCrop(width, height, newAspect));
        }
    };

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
                        <img ref={imageRef} src={imageSrc} onLoad={onImageLoad} alt="Crop" style={{ maxHeight: '70vh' }}/>
                    </ReactCrop>
                )}
            </Modal.Body>
            <Modal.Footer className="d-flex justify-content-between">
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