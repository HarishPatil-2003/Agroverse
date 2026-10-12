/**
 * AgroVerse Crop Detection Module
 * 100% Dynamic ONNX-driven detection logic with Camera & Device Selection.
 */

let selectedCropFile = null;
let cameraStream = null;

async function initCropDetection() {
    console.log("INITIALIZING CROP DETECTION...");
    
    const elements = {
        cropImageInput: document.getElementById('cropImageInput'),
        analyzeCropBtn: document.getElementById('analyzeCropBtn'),
        cropPreviewImg: document.getElementById('cropPreviewImg'),
        cropPreviewName: document.getElementById('cropPreviewName'),
        cropResultBox: document.getElementById('cropResultBox'),
        dropZone: document.getElementById('drop-zone'),
        openCameraBtn: document.getElementById('openCameraBtn'),
        capturePhotoBtn: document.getElementById('capturePhotoBtn'),
        cameraFeed: document.getElementById('camera-feed'),
        cameraCanvas: document.getElementById('camera-canvas')
    };

    if (!elements.cropImageInput || !elements.analyzeCropBtn) return;

    // Reset
    selectedCropFile = null;
    elements.analyzeCropBtn.disabled = true;
    stopCamera();

    // ── File Selection ───────────────────────────────────────────────
    elements.cropImageInput.onchange = (e) => {
        if (e.target.files[0]) handleCropFile(e.target.files[0]);
    };

    // ── Camera Operations ────────────────────────────────────────────
    if (elements.openCameraBtn) {
        elements.openCameraBtn.onclick = async () => {
            try {
                console.log("Requesting camera access...");
                
                // 1. Basic permission check
                if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
                    throw new Error("Your browser does not support camera access.");
                }

                // 2. Try to get stream
                // We use a broader constraint first to let the browser/OS show the picker
                cameraStream = await navigator.mediaDevices.getUserMedia({ 
                    video: { 
                        width: { ideal: 1280 },
                        height: { ideal: 720 },
                        facingMode: "environment" 
                    } 
                });

                elements.cameraFeed.srcObject = cameraStream;
                elements.cameraFeed.onloadedmetadata = () => {
                    elements.cameraFeed.play();
                    elements.cameraFeed.style.display = 'block';
                    elements.cropPreviewImg.style.display = 'none';
                    elements.capturePhotoBtn.style.display = 'block';
                    
                    const placeholder = document.querySelector('.placeholder-preview');
                    if (placeholder) placeholder.style.display = 'none';
                };

                console.log("Camera stream active.");

            } catch (err) {
                console.error("Camera Error:", err);
                let msg = "Could not access camera. ";
                if (err.name === 'NotAllowedError') {
                    msg += "Please click the camera icon in your address bar and 'Allow' access.";
                } else if (err.name === 'NotFoundError') {
                    msg += "No camera found on this device.";
                } else {
                    msg += err.message || "Please check your system settings.";
                }
                alert(msg);
            }
        };
    }

    if (elements.capturePhotoBtn) {
        elements.capturePhotoBtn.onclick = () => {
            const ctx = elements.cameraCanvas.getContext('2d');
            elements.cameraCanvas.width = elements.cameraFeed.videoWidth;
            elements.cameraCanvas.height = elements.cameraFeed.videoHeight;
            ctx.drawImage(elements.cameraFeed, 0, 0);
            
            elements.cameraCanvas.toBlob((blob) => {
                const file = new File([blob], `captured_${Date.now()}.jpg`, { type: "image/jpeg" });
                handleCropFile(file);
                stopCamera();
            }, 'image/jpeg');
        };
    }

    function stopCamera() {
        if (cameraStream) {
            cameraStream.getTracks().forEach(t => t.stop());
            cameraStream = null;
        }
        if (elements.cameraFeed) elements.cameraFeed.style.display = 'none';
        if (elements.capturePhotoBtn) elements.capturePhotoBtn.style.display = 'none';
    }

    function handleCropFile(file) {
        selectedCropFile = file;
        elements.cropPreviewName.textContent = file.name;
        
        const reader = new FileReader();
        reader.onload = (e) => {
            elements.cropPreviewImg.src = e.target.result;
            elements.cropPreviewImg.style.display = 'block';
            const placeholder = document.querySelector('.placeholder-preview');
            if (placeholder) placeholder.style.display = 'none';
        };
        reader.readAsDataURL(file);
        
        elements.analyzeCropBtn.disabled = false;
        if (elements.cropResultBox) elements.cropResultBox.style.display = 'none';
    }

    // ── Analysis ─────────────────────────────────────────────────────
    elements.analyzeCropBtn.onclick = async () => {
        if (!selectedCropFile) {
            alert(typeof i18next !== 'undefined' ? i18next.t('crop.uploadFirst') : "Please upload a crop image first.");
            return;
        }

        elements.analyzeCropBtn.disabled = true;
        const analyzingText = typeof i18next !== 'undefined' ? i18next.t('common.analyzing') : 'Analyzing...';
        elements.analyzeCropBtn.innerHTML = `<i class="fas fa-spinner fa-spin"></i> ${analyzingText}`;

        const formData = new FormData();
        formData.append("image", selectedCropFile);
        formData.append("language", localStorage.getItem('agroverse_language') || 'en');

        try {
            const res = await apiFetch("/api/crop-detection/analyze", {
                method: "POST",
                body: formData
            });
            renderCropResult(res);
        } catch (err) {
            console.error("Analysis Failed:", err);
            const failMsg = typeof i18next !== 'undefined' ? i18next.t('crop.detectionFailed') : "Analysis failed: ";
            alert(failMsg + err.message);
        } finally {
            elements.analyzeCropBtn.disabled = false;
            elements.analyzeCropBtn.textContent = typeof i18next !== 'undefined' ? i18next.t('crop.analyzeCrop') : "Analyze Crop";
        }
    };

    // ── Reset / New Analysis ─────────────────────────────────────────
    const newAnalysisBtn = document.getElementById('newAnalysisBtn');
    if (newAnalysisBtn) {
        newAnalysisBtn.onclick = () => {
            // Reset Variables
            selectedCropFile = null;
            stopCamera();

            // Clear Previews
            elements.cropPreviewImg.src = "";
            elements.cropPreviewImg.style.display = 'none';
            elements.cropPreviewName.textContent = "";
            
            const placeholder = document.querySelector('.placeholder-preview');
            if (placeholder) placeholder.style.display = 'flex';

            // Hide Results
            if (elements.cropResultBox) {
                elements.cropResultBox.style.display = 'none';
            }

            // Reset Input
            elements.cropImageInput.value = "";
            elements.analyzeCropBtn.disabled = true;

            // Scroll to top
            window.scrollTo({ top: 0, behavior: 'smooth' });
        };
    }
}

function renderCropResult(result) {
    const box = document.getElementById('cropResultBox');
    if (!box) return;

    document.getElementById('disease-name').textContent = `${result.crop} — ${result.disease}`;
    document.getElementById('confidence-value').textContent = `${result.confidence}%`;
    document.getElementById('confidence-fill').style.width = `${result.confidence}%`;
    
    const badge = document.getElementById('status-badge');
    badge.textContent = result.severity;
    badge.className = `status-badge ${result.disease === 'Healthy' ? 'healthy' : 'warning'}`;

    document.getElementById('disease-desc').textContent = result.description;
    
    const list = document.getElementById('treatment-list');
    list.innerHTML = (result.treatments || []).map(t => `<li><i class="fas fa-check-circle"></i> ${t}</li>`).join('');

    const resImg = document.getElementById('result-image');
    const prevImg = document.getElementById('cropPreviewImg');
    if (resImg && prevImg) resImg.src = prevImg.src;

    box.style.display = 'block';
    box.scrollIntoView({ behavior: 'smooth' });
}

window.initCropDetection = initCropDetection;
