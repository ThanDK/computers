import React from 'react';
import { notifySuccess } from '../../services/NotificationService';

const TruncatedText = ({ text }) => {
  const handleDoubleClick = () => {
    navigator.clipboard.writeText(text);
    notifySuccess(`Copied "${text}"`);
  };

  return (
    <div
      className="truncate-text"
      onDoubleClick={handleDoubleClick}
      title={`Double-click to copy: ${text}`}
    >
      {text || 'N/A'}
    </div>
  );
};

export default TruncatedText;