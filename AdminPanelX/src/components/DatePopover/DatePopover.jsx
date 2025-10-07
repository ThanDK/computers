import React from 'react';
import { Button, OverlayTrigger, Popover } from 'react-bootstrap';
import { BsCalendar } from 'react-icons/bs';
import { format } from 'date-fns';
import './DatePopover.css';

// Function to safely format dates, handling null or invalid values
const formatDate = (dateString) => {
    if (!dateString) {
        return 'N/A';
    }
    try {
        const date = new Date(dateString);
        return format(date, 'dd MMM yyyy, HH:mm');
    } catch (error) {
        console.error("Error formatting date:", error);
        return 'Invalid Date';
    }
};

function DatePopover({ createdAt, updatedAt }) {
    const popover = (
        <Popover id="popover-timestamps" className="timestamp-popover">
            <Popover.Header as="h3">Timestamps</Popover.Header>
            <Popover.Body>
                <div className="timestamp-entry">
                    <span className="timestamp-label">Created At:</span>
                    <span>{formatDate(createdAt)}</span>
                </div>
                <div className="timestamp-entry mt-2">
                    <span className="timestamp-label">Updated At:</span>
                    <span>{formatDate(updatedAt)}</span>
                </div>
            </Popover.Body>
        </Popover>
    );

    return (
        <OverlayTrigger trigger="click" placement="left" overlay={popover} rootClose>
            <Button variant="outline-secondary" size="sm" className="action-btn">
                <BsCalendar />
            </Button>
        </OverlayTrigger>
    );
}

export default DatePopover;