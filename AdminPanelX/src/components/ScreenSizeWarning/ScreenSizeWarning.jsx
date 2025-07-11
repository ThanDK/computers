import React from 'react';
import { BsDisplay, BsEmojiWink } from 'react-icons/bs';
import './ScreenSizeWarning.css';

const ScreenSizeWarning = () => {
    return (
        <div className="screen-size-warning">
            <div className="warning-content">
                <BsDisplay className="warning-icon" />
                <h1>Screen is too small!</h1>
                <p>
                    This admin panel is designed for larger screens.
                    <br />
                    Please switch to a tablet or desktop for the best experience.
                </p>
                <BsEmojiWink className="warning-emoji" />
            </div>
        </div>
    );
};

export default ScreenSizeWarning;