import api from '../api/api.jsx';

export const getCurrentUserProfile = async () => {
    try {
        const response = await api.get('/profile/me');
        return response.data;
    } catch (error) {
        console.error("Error fetching user profile:", error);
        throw error;
    }
};

export const updateUserProfile = async (profileUpdateData, profilePictureFile) => {
    try {
        const submissionData = new FormData();

        const profileDataJson = {
            name: profileUpdateData.name,
            email: profileUpdateData.email,
        };

        if (profileUpdateData.password) {
            profileDataJson.password = profileUpdateData.password;
        }

        const profileDataBlob = new Blob([JSON.stringify(profileDataJson)], {
            type: 'application/json'
        });

        submissionData.append('profileData', profileDataBlob);

        if (profilePictureFile) {
            submissionData.append('file', profilePictureFile);
        }

        const response = await api.put('/profile', submissionData);

        return response.data;
    } catch (error) {
        console.error("Error updating user profile:", error);
        throw error;
    }
};