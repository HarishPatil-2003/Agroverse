window.initCropDetectionPage = function () {
  console.log("Crop detection page initialized");

  const input = document.getElementById("cropImageInput");
  const browseBtn = document.getElementById("browseCropBtn");
  const analyzeBtn = document.getElementById("analyzeCropBtn");
  const preview = document.getElementById("cropPreview");
  const fileName = document.getElementById("cropFileName");
  const resultSection = document.getElementById("cropResult");
  const resultContent = document.getElementById("cropResultContent");

  const uploadBox = document.getElementById("cropUploadBox");
  const emptyState = document.getElementById("previewEmptyState");
  const clearBtn = document.getElementById("clearPreviewBtn");
  const cameraBtn = document.getElementById("cameraBtn");
  const cameraFeed = document.getElementById("cameraFeed");
  const captureBtn = document.getElementById("captureBtn");
  const newAnalyzeBtn = document.getElementById("newAnalyzeBtn");
  let stream = null;

  if (!input || !browseBtn || !analyzeBtn) {
    console.error("Crop detection elements missing. Check pages/crop.html.");
    return;
  }

  let selectedFile = null;

  browseBtn.addEventListener("click", () => {
    input.click();
  });

  if (uploadBox) {
    uploadBox.addEventListener("dragover", (e) => {
      e.preventDefault();
      uploadBox.classList.add("drag-over");
    });
    uploadBox.addEventListener("dragleave", (e) => {
      e.preventDefault();
      uploadBox.classList.remove("drag-over");
    });
    uploadBox.addEventListener("drop", (e) => {
      e.preventDefault();
      uploadBox.classList.remove("drag-over");
      if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
        input.files = e.dataTransfer.files;
        input.dispatchEvent(new Event('change'));
      }
    });
  }

  input.addEventListener("change", () => {
    selectedFile = input.files[0];

    if (!selectedFile) return;

    if (selectedFile.size > 10 * 1024 * 1024) {
      alert("File size exceeds 10MB limit.");
      input.value = "";
      selectedFile = null;
      return;
    }

    if (!selectedFile.type.startsWith("image/")) {
      alert("Please select a valid image file.");
      input.value = "";
      selectedFile = null;
      return;
    }

    if (preview) {
      preview.src = URL.createObjectURL(selectedFile);
      preview.style.display = "block";
      if (emptyState) emptyState.style.display = "none";
      if (clearBtn) clearBtn.style.display = "block";
    }

    if (fileName) {
      fileName.textContent = selectedFile.name;
    }

    analyzeBtn.disabled = false;
  });

  if (clearBtn) {
    clearBtn.addEventListener("click", () => {
      input.value = "";
      selectedFile = null;
      if (preview) {
        preview.src = "";
        preview.style.display = "none";
      }
      if (emptyState) emptyState.style.display = "block";
      clearBtn.style.display = "none";
      if (fileName) fileName.textContent = "No image selected";
      analyzeBtn.disabled = true;
      if (resultSection) resultSection.style.display = "none";
      
      if (stream) {
        stream.getTracks().forEach(track => track.stop());
        if (cameraFeed) cameraFeed.style.display = "none";
        if (captureBtn) captureBtn.style.display = "none";
      }
    });
  }

  if (cameraBtn) {
    cameraBtn.addEventListener("click", async () => {
      try {
        stream = await navigator.mediaDevices.getUserMedia({ video: true });
        if (cameraFeed) {
          cameraFeed.srcObject = stream;
          cameraFeed.play();
          cameraFeed.style.display = "block";
          if (captureBtn) captureBtn.style.display = "block";
          if (preview) preview.style.display = "none";
          if (emptyState) emptyState.style.display = "none";
          if (clearBtn) clearBtn.style.display = "block";
        }
      } catch (err) {
        console.error("Camera error:", err);
        alert("Could not access camera. Please check permissions.");
      }
    });
  }

  if (captureBtn) {
    captureBtn.addEventListener("click", () => {
      const canvas = document.createElement("canvas");
      canvas.width = cameraFeed.videoWidth;
      canvas.height = cameraFeed.videoHeight;
      const ctx = canvas.getContext("2d");
      ctx.translate(canvas.width, 0);
      ctx.scale(-1, 1);
      ctx.drawImage(cameraFeed, 0, 0);
      
      canvas.toBlob((blob) => {
        selectedFile = new File([blob], "captured_crop.jpg", { type: "image/jpeg" });
        if (preview) {
          preview.src = URL.createObjectURL(selectedFile);
          preview.style.display = "block";
          cameraFeed.style.display = "none";
          captureBtn.style.display = "none";
        }
        if (fileName) fileName.textContent = "captured_crop.jpg";
        analyzeBtn.disabled = false;
        
        if (stream) {
          stream.getTracks().forEach(track => track.stop());
        }
      }, "image/jpeg");
    });
  }

  if (newAnalyzeBtn) {
    newAnalyzeBtn.addEventListener("click", () => {
      if (clearBtn) clearBtn.click();
    });
  }

  analyzeBtn.addEventListener("click", async () => {
    if (!selectedFile) {
      alert("Please select a crop image first.");
      return;
    }

    const formData = new FormData();
    formData.append("image", selectedFile);
    // Pass current language so backend returns localized treatment text
    const lang = localStorage.getItem("agroverse_language") || "en";
    formData.append("language", lang);

    analyzeBtn.disabled = true;
    analyzeBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Analyzing...';

    try {
      const response = await window.apiFetch("/api/crop-detection/analyze", {
        method: "POST",
        body: formData
      });

      const data = response;

      try {
        if (data && data.crop) {
          localStorage.setItem('agroverse_last_diagnosis', JSON.stringify({
            crop: data.crop,
            disease: data.disease,
            confidence: data.confidence,
            timestamp: new Date().toISOString()
          }));
        }
      } catch (_) {}

      if (resultSection) {
        resultSection.style.display = "block";
        resultSection.hidden = false;
      }

      if (resultContent) {
        // D1 FIX: Backend returns data.crop (not data.variety), data.treatments (array),
        //          data.pesticideWarning (not data.safetyNotice), data.diagnosticCertainty
        const cropName = data.crop || "Unknown";
        const diseaseName = data.disease || "Unknown";
        const isHealthy = !diseaseName || diseaseName.toLowerCase().includes("healthy");
        const isUncertain = !data.success || cropName === "Uncertain" || diseaseName.toLowerCase().includes("uncertain") || (data.confidence || 0) < 40;
        const certaintyLabel = (data.diagnosticCertainty || "ANALYZED").replace(/_/g, " ");

        // D6 FIX: treatments is an array — render as bullet list
        const treatmentsList = Array.isArray(data.treatments) ? data.treatments : (data.treatments ? [data.treatments] : []);
        const treatmentsHtml = treatmentsList.length > 0
          ? `<ul style="margin: 0; padding-left: 18px; font-size: 13px; color: var(--text-secondary, #4b5563); line-height: 1.8;">${treatmentsList.map(t => `<li>${t}</li>`).join('')}</ul>`
          : `<p style="margin: 0; font-size: 13px; color: var(--text-secondary, #4b5563);">No specific treatment required. Monitor the crop regularly.</p>`;

        // Honest low-confidence disclosure
        const lowConfBanner = isUncertain ? `
          <div style="margin-bottom: 16px; padding: 12px 16px; background: #fef9c3; border: 1px solid #fde047; border-radius: 10px; font-size: 12px; color: #713f12; display: flex; align-items: flex-start; gap: 10px;">
            <i class="fas fa-info-circle" style="font-size: 16px; flex-shrink: 0; margin-top: 1px;"></i>
            <span><strong>Low Confidence Result:</strong> The AI model could not make a reliable identification from this image. This may be because the crop is not in the training set, the image quality is low, or the angle is unusual. Please retake the photo in good lighting with the leaf filling the frame, or consult your local <strong>Krishi Vigyan Kendra (KVK)</strong> for a confirmed field diagnosis.</span>
          </div>` : '';

        // Honest model disclaimer
        const modelDisclaimer = `
          <div style="margin-top: 14px; padding: 10px 14px; background: var(--bg-muted, #f8fafc); border: 1px solid var(--border-light, #e5e7eb); border-radius: 8px; font-size: 11px; color: var(--text-muted, #6b7280); line-height: 1.5;">
            <i class="fas fa-flask" style="margin-right: 5px;"></i>
            <strong>Research Preview:</strong> This AI model is in active development and may not accurately identify all crop types or diseases. Always verify results with a certified agricultural expert or KVK officer before taking action.
          </div>`;

        resultContent.innerHTML = `
          ${lowConfBanner}
          <div style="display: flex; gap: 30px; align-items: stretch; flex-wrap: wrap;">
            <!-- Left: Image -->
            <div style="flex: 0 0 160px;">
              <img src="${URL.createObjectURL(selectedFile)}" style="width: 160px; height: 160px; object-fit: cover; border-radius: 16px; border: 4px solid var(--border-light, #e5e7eb); box-shadow: 0 4px 12px rgba(0,0,0,0.05);" alt="Analyzed Crop">
              <div style="margin-top: 10px; text-align: center;">
                <span class="badge" style="background: #e0f2fe; color: #0369a1; font-weight: 700; font-size: 11px;">
                  <i class="fas fa-tag"></i> ${cropName}
                </span>
              </div>
            </div>

            <!-- Middle: Detection & Confidence -->
            <div style="flex: 1; min-width: 260px; display: flex; flex-direction: column; justify-content: center;">
              <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 12px; flex-wrap: wrap;">
                <h3 style="margin: 0; font-size: 22px; font-weight: 800; color: var(--text-primary, #111827);">${diseaseName}</h3>
                <span class="badge" style="background: ${isUncertain ? '#fef9c3' : (isHealthy ? '#dcfce7' : '#fee2e2')}; color: ${isUncertain ? '#713f12' : (isHealthy ? '#16a34a' : '#ef4444')}; font-weight: 700; font-size: 11px;">
                  ${isUncertain ? 'LOW CONFIDENCE' : (isHealthy ? 'CLEAN & HEALTHY' : 'DISEASE DETECTED')}
                </span>
                <span class="badge" style="background: #f1f5f9; color: #475569; font-weight: 600; font-size: 10px;">
                  ${certaintyLabel}
                </span>
              </div>
              
              <div style="margin-bottom: 8px; display: flex; justify-content: space-between; font-size: 13px; color: var(--text-primary, #111827); font-weight: 600;">
                <span>Model Confidence</span>
                <span>${data.confidence || 0}%</span>
              </div>
              <div style="width: 100%; height: 8px; background: var(--border-light, #e5e7eb); border-radius: 10px; overflow: hidden; margin-bottom: 12px;">
                <div style="width: ${Math.min(data.confidence || 0, 100)}%; height: 100%; background: ${(data.confidence || 0) >= 70 ? '#16a34a' : ((data.confidence || 0) >= 40 ? '#f59e0b' : '#ef4444')}; border-radius: 10px; transition: width 0.6s ease;"></div>
              </div>
              <div style="font-size: 11px; color: var(--text-muted, #6b7280);">
                <i class="fas fa-bolt"></i> In-process Java ONNX inference &bull; Farmer ID: <strong>${data.farmerId || 'AGV-DEMO'}</strong>
              </div>
            </div>

            <!-- Right: Treatment & Prevention -->
            <div style="flex: 1.5; min-width: 280px; border-left: 1px solid var(--border-light, #e5e7eb); padding-left: 24px; display: flex; flex-direction: column; justify-content: flex-start;">
              <h4 style="margin: 0 0 8px 0; font-size: 14px; font-weight: 700; color: var(--text-primary, #111827); display: flex; align-items: center; gap: 8px;">
                <i class="fas fa-prescription-bottle-alt" style="color: #16a34a;"></i> Suggested Treatment Steps
              </h4>
              ${treatmentsHtml}
              
              ${data.description ? `
                <h4 style="margin: 14px 0 6px 0; font-size: 14px; font-weight: 700; color: var(--text-primary, #111827); display: flex; align-items: center; gap: 8px;">
                  <i class="fas fa-book-open" style="color: #16a34a;"></i> Description
                </h4>
                <p style="margin: 0; font-size: 13px; color: var(--text-secondary, #4b5563); line-height: 1.5;">${data.description}</p>
              ` : ''}
            </div>
          </div>
          
          ${data.pesticideWarning ? `
            <div style="margin-top: 18px; padding: 12px 16px; background: #fffbeb; border: 1px solid #fde68a; border-radius: 10px; font-size: 12px; color: #92400e; display: flex; align-items: center; gap: 10px;">
              <i class="fas fa-exclamation-triangle" style="font-size: 16px; flex-shrink: 0;"></i>
              <span><strong>Advisory Safety Notice:</strong> ${data.pesticideWarning}</span>
            </div>
          ` : ''}

          <div style="display: flex; gap: 14px; margin-top: 20px; border-top: 1px solid var(--border-light, #e5e7eb); padding-top: 16px; justify-content: flex-end;">
             <button id="askAssistantBtn" type="button" style="padding: 10px 20px; background: #16a34a; color: white; border: none; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: background 0.2s;">
               <i class="fas fa-robot"></i> Consult AI Assistant
             </button>
          </div>
          ${modelDisclaimer}
        `;

        document.getElementById('askAssistantBtn')?.addEventListener('click', () => {
          const chatInput = document.getElementById('chat-input') || document.querySelector('.chat-input-field');
          const chatWidget = document.getElementById('agro-chatbot') || document.getElementById('chatbot-widget') || document.querySelector('.chat-modal-container');
          if (chatWidget) chatWidget.style.display = 'flex';
          if (chatInput) {
            chatInput.value = `My ${cropName} crop was analyzed and the result was: ${diseaseName} (${data.confidence}% confidence). What preventive care or treatment should I take?`;
            chatInput.focus();
          }
        });
      }
    } catch (error) {
      console.error("Crop analysis failed:", error);
      if (error.status === 401 || (error.message && error.message.toLowerCase().includes("unauthorized"))) {
        alert("Please log in to analyze crops and save your diagnosis history.");
        window.location.hash = "#/login";
      } else {
        alert("Crop analysis failed: " + (error.message || "Server error"));
      }
    } finally {
      analyzeBtn.disabled = false;
      analyzeBtn.innerHTML = '<i class="fas fa-search"></i> Analyze Crop';
    }
  });
};

export function initCropDetectionPage() {
  if (typeof window.initCropDetectionPage === 'function') {
    return window.initCropDetectionPage();
  }
}
