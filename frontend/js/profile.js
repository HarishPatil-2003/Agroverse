import { auth } from './auth.js';
import { api } from './api.js';
import { supabase } from './supabaseClient.js';

export function initProfile() {
    console.log("Initializing Profile Page...");
    
    // DOM Elements - Display
    const elements = {
        avatar: document.getElementById('profile-avatar'),
        nameMain: document.getElementById('display-name-main'),
        farmerId: document.getElementById('display-farmer-id'),
        locationMain: document.getElementById('display-location-main'),
        phoneMain: document.getElementById('display-phone-main'),
        emailMain: document.getElementById('display-email-main'),
        joinedDate: document.getElementById('display-joined-date'),
        fullName: document.getElementById('display-full-name'),
        dob: document.getElementById('display-dob'),
        gender: document.getElementById('display-gender'),
        farmType: document.getElementById('display-farm-type'),
        landHolding: document.getElementById('display-land-holding'),
        experience: document.getElementById('display-experience'),
        address: document.getElementById('display-address'),
        completionPrompt: document.getElementById('profile-completion-prompt')
    };

    // DOM Elements - Actions/Modals
    const editBtn = document.getElementById('edit-profile-btn');
    const editModal = document.getElementById('edit-profile-modal');
    const editForm = document.getElementById('edit-profile-form');
    const closeEditModal = document.getElementById('close-edit-modal');
    const cancelEditBtn = document.getElementById('btn-cancel-edit');
    
    const photoOptionsBtn = document.getElementById('open-photo-options');
    const photoOptionsModal = document.getElementById('photo-options-modal');
    const closePhotoModal = document.getElementById('close-photo-modal');
    const btnUploadFile = document.getElementById('btn-upload-file');
    const btnTakePhoto = document.getElementById('btn-take-photo');
    const fileInput = document.getElementById('profile-file-input');
    
    const cameraModal = document.getElementById('camera-modal');
    const cameraVideo = document.getElementById('camera-video');
    const cameraCanvas = document.getElementById('camera-canvas');
    const btnCapture = document.getElementById('btn-capture');
    const btnRetake = document.getElementById('btn-retake');
    const btnSavePhoto = document.getElementById('btn-save-photo');
    const closeCamera = document.getElementById('close-camera');

    let cameraStream = null;
    let isProfileLoading = false;
    let isProfileLoaded = false;
    let currentProfile = null;

    function normalizeProfile(data) {
        if (!data) return null;
        
        // Handle both user-requested schema and legacy/Supabase variations
        return {
            id: data.id || null,
            farmerId: data.farmer_id || data.farmer_code || data.farmerId || "N/A",
            fullName: data.full_name || data.fullName || `${data.firstName || ""} ${data.lastName || ""}`.trim() || "Farmer",
            firstName: data.firstName || (data.full_name ? data.full_name.split(' ')[0] : ""),
            lastName: data.lastName || (data.full_name ? data.full_name.split(' ')[1] || "" : ""),
            email: data.email || "Email not added",
            phone: data.phone || "Phone not added",
            city: data.city || data.location?.split(',')[0]?.trim() || "",
            state: data.state || data.location?.split(',')[1]?.trim() || "",
            address: data.address || data.location || "",
            pincode: data.pincode || "",
            dateOfBirth: data.date_of_birth || data.dateOfBirth || "Not added yet",
            gender: data.gender || "Not added yet",
            farmType: data.farm_type || data.farmType || "Not added yet",
            landHolding: data.land_holding || data.landHolding || null,
            landUnit: data.land_unit || data.landUnit || "Acres",
            experienceYears: data.experience_years || data.experienceYears || null,
            profileImageUrl: data.avatar_url || data.profile_image_url || data.profileImageUrl || null,
            joinedAt: data.created_at || data.joinedAt || null,
            profileCompleted: !!(data.full_name || data.fullName)
        };
    }

    async function loadProfileData() {
        if (isProfileLoading) return;
        
        console.log("Profile page loading started...");
        isProfileLoading = true;
        isProfileLoaded = false;
        
        try {
            const user = await auth.getSession();
            
            if (!user) {
                console.warn("No user session found");
                if (elements.nameMain) elements.nameMain.textContent = "Please login";
                return;
            }

            await auth.loadFarmerProfile();
            const rawProfile = auth.farmerProfile || user;
            
            currentProfile = normalizeProfile(rawProfile);
            console.log("Normalized Profile for UI:", currentProfile);
            
            if (!currentProfile) throw new Error("Could not load profile data");

            isProfileLoaded = true;
            renderProfileUI(currentProfile);
            
        } catch (err) {
            console.error("Profile load failed:", err);
            if (elements.nameMain) elements.nameMain.textContent = "Error loading profile";
        } finally {
            isProfileLoading = false;
        }
    }

    function renderProfileUI(profile) {
        if (!profile) return;

        // Render Display Fields with null checks
        if (elements.nameMain) elements.nameMain.textContent = profile.fullName;
        if (elements.farmerId) elements.farmerId.textContent = profile.farmerId;
        if (elements.locationMain) {
            const loc = (profile.city && profile.state) ? `${profile.city}, ${profile.state}` : (profile.city || profile.state || profile.address || "Location not added");
            elements.locationMain.textContent = loc;
        }
        if (elements.phoneMain) elements.phoneMain.textContent = profile.phone;
        if (elements.emailMain) elements.emailMain.textContent = profile.email;
        
        if (profile.joinedAt && elements.joinedDate) {
            const date = new Date(profile.joinedAt);
            elements.joinedDate.textContent = date.toLocaleDateString('en-IN', { month: 'long', year: 'numeric' });
        }

        if (elements.fullName) elements.fullName.textContent = profile.fullName;
        if (elements.dob) elements.dob.textContent = profile.dateOfBirth;
        if (elements.gender) elements.gender.textContent = profile.gender;
        if (elements.farmType) elements.farmType.textContent = profile.farmType;
        
        if (elements.landHolding) {
            if (profile.landHolding) {
                elements.landHolding.textContent = `${profile.landHolding} ${profile.landUnit}`;
            } else {
                elements.landHolding.textContent = "Not added yet";
            }
        }
        
        if (elements.experience) {
            elements.experience.textContent = profile.experienceYears ? `${profile.experienceYears} Years` : "Not added yet";
        }
        
        if (elements.address) {
            let fullAddress = profile.address || "";
            if (profile.city && !fullAddress.includes(profile.city)) fullAddress += (fullAddress ? ", " : "") + profile.city;
            if (profile.state && !fullAddress.includes(profile.state)) fullAddress += (fullAddress ? ", " : "") + profile.state;
            if (profile.pincode && !fullAddress.includes(profile.pincode)) fullAddress += (fullAddress ? " - " : "") + profile.pincode;
            elements.address.textContent = fullAddress || "Not added yet";
        }

        if (elements.avatar) {
            if (profile.profileImageUrl) {
                elements.avatar.src = profile.profileImageUrl;
            } else {
                elements.avatar.src = `https://ui-avatars.com/api/?name=${encodeURIComponent(profile.fullName)}&background=2ecc71&color=fff&size=128`;
            }
        }

        if (elements.completionPrompt) {
            elements.completionPrompt.style.display = profile.profileCompleted ? 'none' : 'block';
        }
    }

    // Modal Control
    const toggleModal = (modal, show) => {
        if (modal) modal.style.display = show ? 'flex' : 'none';
    };

    if (editBtn) {
        editBtn.addEventListener('click', () => {
            const p = currentProfile; // Use the normalized profile for the form
            if (!p) return;

            // Pre-fill form
            const form = editForm;
            if (form) {
                form.firstName.value = p.firstName || '';
                form.lastName.value = p.lastName || '';
                form.phone.value = p.phone || '';
                form.dateOfBirth.value = p.dateOfBirth || '';
                form.gender.value = p.gender || '';
                form.farmType.value = p.farmType || '';
                form.landHolding.value = p.land_holding || p.landHolding || '';
                form.landUnit.value = p.land_unit || p.landUnit || 'Acres';
                form.experienceYears.value = p.experience_years || p.experienceYears || '';
                form.address.value = p.address || '';
                form.city.value = p.city || '';
                form.state.value = p.state || '';
            }
            
            toggleModal(editModal, true);
        });
    }

    closeEditModal?.addEventListener('click', () => toggleModal(editModal, false));
    cancelEditBtn?.addEventListener('click', () => toggleModal(editModal, false));

    if (editForm) {
        editForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            if (!currentProfile) {
                alert("Please wait until your profile loads.");
                return;
            }

            const formData = new FormData(editForm);
            const data = Object.fromEntries(formData.entries());
            
            try {
                // Update via canonical Backend API (Spring Boot persistent PostgreSQL)
                await api.updateProfile(data);

                await auth.loadFarmerProfile();
                await loadProfileData();
                toggleModal(editModal, false);
                alert("Profile updated successfully!");
                
                if (window.refreshUserIdentityUI) await window.refreshUserIdentityUI();
            } catch (err) {
                alert("Error updating profile: " + (err.message || "Update failed"));
            }
        });
    }

    // --- Photo Upload Logic (Supabase Storage) ---
    async function performPhotoUpload(file) {
        if (!file) return;

        try {
            const user = await auth.getSession();
            if (!user || !user.id) {
                alert("Please login before uploading a profile photo.");
                return;
            }

            // 1. Validation
            const allowedTypes = ["image/jpeg", "image/png", "image/webp"];
            if (!allowedTypes.includes(file.type)) {
                alert("Only JPG, PNG, or WEBP images are allowed.");
                return;
            }
            if (file.size > 2 * 1024 * 1024) {
                alert("Image must be smaller than 2MB.");
                return;
            }

            console.log("[STORAGE] Uploading to Supabase Storage...");
            const fileExt = file.name ? file.name.split(".").pop().toLowerCase() : "jpg";
            const filePath = `${user.id}/avatar-${Date.now()}.${fileExt}`;

            // 2. Upload to Storage
            const { data: uploadData, error: uploadError } = await supabase.storage
                .from("profile-photos")
                .upload(filePath, file, {
                    cacheControl: "3600",
                    upsert: true
                });

            if (uploadError) {
                console.error("[STORAGE] Upload error:", uploadError);
                throw new Error("Storage upload failed. Ensure 'profile-photos' bucket is public and RLS policies are set.");
            }

            // 3. Get Public URL
            const { data: { publicUrl } } = supabase.storage
                .from("profile-photos")
                .getPublicUrl(filePath);

            console.log("[STORAGE] Public URL generated:", publicUrl);

            // 4. Update Database
            const { error: updateError } = await supabase
                .from("farmers")
                .update({
                    avatar_url: publicUrl,
                    profile_image_url: publicUrl, // Legacy support
                    updated_at: new Date().toISOString()
                })
                .eq("id", user.id);

            if (updateError) {
                console.error("[DB] Avatar update error:", updateError);
                throw new Error("Photo uploaded, but database update failed.");
            }

            // 5. Update UI
            if (elements.avatar) elements.avatar.src = publicUrl;
            await auth.loadFarmerProfile();
            if (window.refreshUserIdentityUI) await window.refreshUserIdentityUI();
            
            alert("Profile photo updated successfully!");

        } catch (err) {
            console.error("Photo upload failed:", err);
            alert("Photo upload failed: " + err.message);
        }
    }

    // Photo Management Listeners
    if (photoOptionsBtn) {
        photoOptionsBtn.addEventListener('click', () => {
            if (!auth.farmerProfile) {
                alert("Please wait until your profile loads.");
                return;
            }
            toggleModal(photoOptionsModal, true);
        });
    }
    closePhotoModal?.addEventListener('click', () => toggleModal(photoOptionsModal, false));

    if (btnUploadFile) {
        btnUploadFile.addEventListener('click', () => {
            if (fileInput) fileInput.click();
            toggleModal(photoOptionsModal, false);
        });
    }

    if (fileInput) {
        fileInput.addEventListener('change', async (e) => {
            const file = e.target.files[0];
            if (file) {
                await performPhotoUpload(file);
                e.target.value = ''; // Reset input
            }
        });
    }

    // Camera Logic
    if (btnTakePhoto) {
        btnTakePhoto.addEventListener('click', async () => {
            if (!auth.farmerProfile) {
                alert("Please wait until your profile loads.");
                return;
            }
            toggleModal(photoOptionsModal, false);
            toggleModal(cameraModal, true);
            
            try {
                cameraStream = await navigator.mediaDevices.getUserMedia({ video: true });
                if (cameraVideo) cameraVideo.srcObject = cameraStream;
            } catch (err) {
                alert("Camera access denied or not available.");
                toggleModal(cameraModal, false);
            }
        });
    }

    if (btnCapture) {
        btnCapture.addEventListener('click', () => {
            if (!cameraVideo || !cameraCanvas) return;
            cameraCanvas.width = cameraVideo.videoWidth;
            cameraCanvas.height = cameraVideo.videoHeight;
            const ctx = cameraCanvas.getContext('2d');
            ctx.drawImage(cameraVideo, 0, 0);
            
            cameraVideo.style.display = 'none';
            cameraCanvas.style.display = 'block';
            btnCapture.style.display = 'none';
            if (btnRetake) btnRetake.style.display = 'inline-block';
            if (btnSavePhoto) btnSavePhoto.style.display = 'inline-block';
        });
    }

    if (btnRetake) {
        btnRetake.addEventListener('click', () => {
            if (cameraVideo) cameraVideo.style.display = 'block';
            if (cameraCanvas) cameraCanvas.style.display = 'none';
            if (btnCapture) btnCapture.style.display = 'inline-block';
            btnRetake.style.display = 'none';
            if (btnSavePhoto) btnSavePhoto.style.display = 'none';
        });
    }

    if (btnSavePhoto) {
        btnSavePhoto.addEventListener('click', async () => {
            if (!cameraCanvas) return;
            cameraCanvas.toBlob(async (blob) => {
                const file = new File([blob], "capture.jpg", { type: "image/jpeg" });
                await performPhotoUpload(file);
                stopCamera();
                toggleModal(cameraModal, false);
            }, 'image/jpeg');
        });
    }

    const stopCamera = () => {
        if (cameraStream) {
            cameraStream.getTracks().forEach(track => track.stop());
            cameraStream = null;
        }
        if (cameraVideo) {
            cameraVideo.srcObject = null;
            cameraVideo.style.display = 'block';
        }
        if (cameraCanvas) cameraCanvas.style.display = 'none';
        if (btnCapture) btnCapture.style.display = 'inline-block';
        if (btnRetake) btnRetake.style.display = 'none';
        if (btnSavePhoto) btnSavePhoto.style.display = 'none';
    };

    closeCamera?.addEventListener('click', () => {
        stopCamera();
        toggleModal(cameraModal, false);
    });

    loadProfileData();
}

window.initProfilePage = initProfile;