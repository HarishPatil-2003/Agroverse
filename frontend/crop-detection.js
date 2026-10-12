import { api } from './js/api.js';

export function initCropDetection() {
    const fileUpload = document.getElementById('file-upload');
    const analyzeBtn = document.getElementById('analyze-btn');
    const imagePreview = document.getElementById('image-preview');
    const placeholderPreview = document.querySelector('.placeholder-preview');
    const filenameText = document.getElementById('filename-text');
    const resultSection = document.getElementById('result-section');
    const resultImage = document.getElementById('result-image');
    const dropZone = document.getElementById('drop-zone');

    if (!fileUpload || !analyzeBtn) return;

    // UI Result Elements
    const diseaseName = document.getElementById('disease-name');
    const confidenceValue = document.getElementById('confidence-value');
    const confidenceFill = document.getElementById('confidence-fill');
    const treatmentList = document.querySelector('.treatment-list');

    // Handle file selection
    fileUpload.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (file) handleFile(file);
    });

    // Drag and Drop
    dropZone.addEventListener('dragover', (e) => { e.preventDefault(); dropZone.classList.add('drag-over'); });
    dropZone.addEventListener('dragleave', () => { dropZone.classList.remove('drag-over'); });
    dropZone.addEventListener('drop', (e) => {
        e.preventDefault();
        dropZone.classList.remove('drag-over');
        const file = e.dataTransfer.files[0];
        if (file && file.type.startsWith('image/')) handleFile(file);
    });

    function handleFile(file) {
        const reader = new FileReader();
        reader.onload = (e) => {
            imagePreview.src = e.target.result;
            imagePreview.style.display = 'block';
            placeholderPreview.style.display = 'none';
            filenameText.textContent = file.name;
            analyzeBtn.disabled = false;
            resultSection.style.display = 'none';
        };
        reader.readAsDataURL(file);
    }

    // REAL AI Analysis
    analyzeBtn.addEventListener('click', async () => {
        try {
            analyzeBtn.disabled = true;
            analyzeBtn.textContent = 'Analyzing with AI...';

            // Prepare data
            const result = await api.cropDetect({ imageName: filenameText.textContent });

            // Update UI with Results
            diseaseName.textContent = result.disease;
            confidenceValue.textContent = result.confidence;
            confidenceFill.style.width = result.confidence;
            
            treatmentList.innerHTML = result.treatment.map(t => `
                <li><i class="fas fa-check-circle"></i> ${t}</li>
            `).join('');

            // Show Results
            resultImage.src = imagePreview.src;
            resultSection.style.display = 'block';
            resultSection.scrollIntoView({ behavior: 'smooth', block: 'start' });

        } catch (error) {
            alert('Failed to analyze crop. Error: ' + error.message);
        } finally {
            analyzeBtn.textContent = 'Analyze Crop';
            analyzeBtn.disabled = false;
        }
    });
}

