import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Spinner } from 'react-bootstrap';
import { BsArrowLeft, BsArrowCounterclockwise } from 'react-icons/bs';
import './PageHeader.css';

/**
 * A consistent header for pages.
 * @param {object} props
 * @param {string} props.title - The main title of the page.
 * @param {string} [props.subtitle] - The smaller text below the title.
 * @param {boolean} [props.showBackButton=false] - Whether to show the back button.
 * @param {Function} [props.onBack] - Custom function for the back button. Defaults to navigate(-1).
 * @param {Function} [props.onRefresh] - Function to call when the refresh button is clicked.
 * @param {boolean} [props.isRefreshing=false] - If true, shows a spinner on the refresh button.
 */
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
                {/* --- FIX: Refresh Button is rendered here --- */}
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