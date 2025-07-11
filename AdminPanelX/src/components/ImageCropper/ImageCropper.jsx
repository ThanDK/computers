import React, { useState, useRef } from 'react';
import { Modal, Button } from 'react-bootstrap';
import ReactCrop, { centerCrop, makeAspectCrop } from 'react-image-crop';
import 'react-image-crop/dist/ReactCrop.css';

// --- Helper Functions ---

// Creates a centered crop area with a specific aspect ratio.
function centerAspectCrop(mediaWidth, mediaHeight, aspect) {
    return centerCrop(
        makeAspectCrop({ unit: '%', width: 90 }, aspect, mediaWidth, mediaHeight),
        mediaWidth,
        mediaHeight,
    );
}

// Generates a cropped image File object from a source image and crop data.
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
            // Return a new File object
            resolve(new File([blob], fileName, { type: 'image/jpeg' }));
        }, 'image/jpeg', 0.9); // Use JPEG format for good compression
    });
}


/**
 * A reusable modal component for cropping images.
 * @param {object} props
 * @param {string} props.imageSrc - The source of the image to be cropped (Data URL).
 * @param {boolean} props.show - Controls the visibility of the modal.
 * @param {function} props.onHide - Function to call when the modal should be closed.
 * @param {function} props.onCropComplete - Callback that receives the final cropped File object.
 * @param {number} [props.aspect=1] - The aspect ratio for the crop (e.g., 1 for square).
 */
function ImageCropper({ imageSrc, show, onHide, onCropComplete, aspect = 1 }) {
    const [crop, setCrop] = useState();
    const [completedCrop, setCompletedCrop] = useState(null);
    const imageRef = useRef(null);

    const onImageLoad = (e) => {
        imageRef.current = e.currentTarget;
        const { width, height } = e.currentTarget;
        setCrop(centerAspectCrop(width, height, aspect));
    };

    const handleConfirm = async () => {
        if (completedCrop?.width && completedCrop?.height && imageRef.current) {
            const croppedFile = await getCroppedImg(imageRef.current, completedCrop, 'cropped-component.jpg');
            onCropComplete(croppedFile); // Send the result back to the parent
            onHide(); // Close the modal
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
                        aspect={aspect}
                        className="d-inline-block"
                    >
                        <img ref={imageRef} src={imageSrc} onLoad={onImageLoad} alt="Crop" style={{ maxHeight: '70vh' }}/>
                    </ReactCrop>
                )}
            </Modal.Body>
            <Modal.Footer>
                <Button variant="secondary" onClick={onHide}>
                    Cancel
                </Button>
                <Button variant="primary" onClick={handleConfirm} disabled={!completedCrop?.width}>
                    Confirm Crop
                </Button>
            </Modal.Footer>
        </Modal>
    );
}

export default ImageCropper;