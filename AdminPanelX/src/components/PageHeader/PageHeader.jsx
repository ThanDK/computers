import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Spinner } from 'react-bootstrap';
import { BsArrowLeft, BsArrowCounterclockwise } from 'react-icons/bs';
import './PageHeader.css';


function PageHeader({ 
    title, 
    subtitle, 
    showBackButton = false, 
    onBack,
    onRefresh,
    isRefreshing = false 
}) {
    const navigate = useNavigate();

    const handleBack = onBack || (() => navigate(-1));

    return (
        <div className="page-header-container">
            <div className="page-header-main">
                {showBackButton && (
                    <Button 
                        variant="link" 
                        onClick={handleBack} 
                        className="back-button"
                        aria-label="Go back"
                    >
                        <BsArrowLeft />
                    </Button>
                )}
                <div className="page-header-titles">
                    <h1>{title}</h1>
                    {subtitle && <p>{subtitle}</p>}
                </div>
            </div>

            <div className="page-header-actions">
                {onRefresh && (
                    <Button 
                        variant="outline-secondary"
                        onClick={onRefresh}
                        disabled={isRefreshing}
                        className="d-flex align-items-center gap-2"
                    >
                        {isRefreshing ? (
                            <>
                                <Spinner as="span" animation="border" size="sm" role="status" aria-hidden="true" />
                                <span>Refreshing...</span>
                            </>
                        ) : (
                            <>
                                <BsArrowCounterclockwise />
                                <span>Refresh</span>
                            </>
                        )}
                    </Button>
                )}
            </div>
        </div>
    );
}

export default PageHeader;