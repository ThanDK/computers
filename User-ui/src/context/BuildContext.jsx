import React, { createContext, useState, useContext, useCallback } from 'react';

// NOTE: componentCategories import removed to avoid circular dependency issues.
// It will be imported directly in components that need it.

const BuildContext = createContext(null);

/**
 * Custom hook to use the BuildContext.
 * @returns {object} The build context value.
 */
export const useBuild = () => {
    const context = useContext(BuildContext);
    if (context === undefined) {
        throw new Error('useBuild must be used within a BuildProvider');
    }
    return context;
};

const INITIAL_STATE = {
    id: null, // Added: To track the ID of the build being edited
    buildName: 'My New PC Build',
    parts: {},
    compatibility: { errors: [], warnings: [], totalWattage: 0, isCompatible: true },
    totalPrice: 0,
};

export const BuildProvider = ({ children }) => {
    const [build, setBuild] = useState(INITIAL_STATE);
    const [isLoading, setIsLoading] = useState(false);

    const setBuildName = (name) => {
        setBuild(prev => ({ ...prev, buildName: name }));
    };

    const addComponentToBuild = useCallback((category, component) => {
        setBuild(prev => {
            const newParts = { ...prev.parts };
            const categoryKey = category.key;

            if (component._id && !component.id) component.id = component._id;

            if (category.multiple) {
                const existing = newParts[categoryKey] ? [...newParts[categoryKey]] : [];
                const newItem = {
                    partDetails: component,
                    quantity: 1,
                    instanceId: Date.now() + Math.random()
                };
                existing.push(newItem);
                newParts[categoryKey] = existing;
            } else {
                newParts[categoryKey] = component;
            }
            return { ...prev, parts: newParts };
        });
    }, []);

    const removeComponentFromBuild = useCallback((categoryKey, instanceId, componentCategories) => {
        setBuild(prev => {
            const newParts = { ...prev.parts };
            const category = componentCategories.find(c => c.key === categoryKey);

            if (category.multiple) {
                newParts[categoryKey] = (newParts[categoryKey] || []).filter(p => p.instanceId !== instanceId);
            } else {
                newParts[categoryKey] = null;
            }
            return { ...prev, parts: newParts };
        });
    }, []);

    const clearBuild = () => {
        setBuild(INITIAL_STATE);
    };

    const loadBuildForEdit = (buildId, existingBuildData) => {
        const initialParts = {
            cpu: existingBuildData.cpu,
            motherboard: existingBuildData.motherboard,
            ramKits: existingBuildData.ramKits,
            gpus: existingBuildData.gpus,
            storageDrives: existingBuildData.storageDrives,
            psu: existingBuildData.psu,
            caseDetail: existingBuildData.caseDetail,
            cooler: existingBuildData.cooler
        };
        setBuild({
            ...INITIAL_STATE,
            id: buildId, // Changed: Store the ID of the build being edited
            buildName: existingBuildData.buildName,
            parts: initialParts
        });
    };

    const contextValue = {
        build,
        setBuild,
        isLoading,
        setIsLoading,
        setBuildName,
        addComponentToBuild,
        removeComponentFromBuild,
        clearBuild,
        loadBuildForEdit,
    };

    return (
        <BuildContext.Provider value={contextValue}>
            {children}
        </BuildContext.Provider>
    );
};