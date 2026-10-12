import os
import json
import io
import numpy as np
from fastapi import FastAPI, File, UploadFile, HTTPException
from PIL import Image
import onnxruntime as ort

app = FastAPI(title="AgroVerse AI Service - Final Krishi Integration")

# Paths
MODEL_DIR = "model"
VARIETY_MODEL = os.path.join(MODEL_DIR, "crop_variety.onnx")
DISEASE_MODEL = os.path.join(MODEL_DIR, "crop_disease.onnx")
VARIETY_LABELS_PATH = "variety_labels.json"
DISEASE_LABELS_PATH = "disease_labels.json"
TREATMENTS_PATH = "treatments.json"

def load_json(path):
    if os.path.exists(path):
        with open(path, "r", encoding="utf-8") as f:
            return json.load(f)
    return []

VARIETY_LABELS = load_json(VARIETY_LABELS_PATH)
DISEASE_LABELS = load_json(DISEASE_LABELS_PATH)
TREATMENTS = load_json(TREATMENTS_PATH)

# Sessions
v_session = None
d_session = None

def load_session(path):
    if os.path.exists(path):
        try:
            return ort.InferenceSession(path, providers=['CPUExecutionProvider'])
        except Exception as e:
            print(f"Error loading {path}: {e}")
    return None

v_session = load_session(VARIETY_MODEL)
d_session = load_session(DISEASE_MODEL)

@app.get("/health")
def health():
    return {
        "status": "UP",
        "models": {"variety": v_session is not None, "disease": d_session is not None}
    }

def apply_background_suppression(image: Image.Image):
    # Convert to RGB array
    img = np.array(image.convert("RGB")).astype(np.float32) / 255.0
    r, g, b = img[:,:,0], img[:,:,1], img[:,:,2]
    
    # Calculate Saturation and Value (Brightness)
    max_c = np.maximum(r, np.maximum(g, b))
    min_c = np.minimum(r, np.minimum(g, b))
    sat = np.where(max_c > 1e-4, (max_c - min_c) / max_c, 0)
    v = max_c
    
    # Leaf Likelihood Heuristic (from Krishi Bandhu)
    score = np.full(r.shape, 0.25, dtype=np.float32)
    
    # Green dominance
    mask1 = (g >= r) & (g >= b) & (sat > 0.12) & (v > 0.08)
    score[mask1] += 0.55 * sat[mask1] * np.minimum(1.0, g[mask1] / (r[mask1] + b[mask1] + 0.05))
    
    # General saturation and brightness
    mask2 = (sat > 0.08) & ((r + g) > (b * 1.1)) & (v > 0.1)
    score[mask2] += 0.15
    
    # Suppression for very bright neutrals (sky/white walls)
    mask3 = (v > 0.92) & (sat < 0.12)
    score[mask3] *= 0.35
    
    # Suppression for low-sat darks (shadows)
    mask4 = (sat < 0.06) & (v > 0.45)
    score[mask4] *= 0.5
    
    # Clip and Apply
    leaf_mask = np.clip(score, 0.08, 1.0)
    leaf_mask = np.expand_dims(leaf_mask, axis=2)
    
    suppressed_img = img * leaf_mask
    return (suppressed_img * 255.0).astype(np.uint8)

def preprocess_image(image: Image.Image):
    # 1. Background Suppression
    suppressed_np = apply_background_suppression(image.resize((224, 224)))
    
    # 2. ImageNet Normalization
    img_data = suppressed_np.astype(np.float32) / 255.0
    mean = np.array([0.485, 0.456, 0.406], dtype=np.float32)
    std = np.array([0.229, 0.224, 0.225], dtype=np.float32)
    img_data = (img_data - mean) / std
    
    # 3. NCHW Conversion
    img_data = np.transpose(img_data, (2, 0, 1)) 
    img_data = np.expand_dims(img_data, axis=0).astype(np.float32)
    return img_data

def get_probs(logits):
    logits = logits.astype(np.float32)
    e_x = np.exp(logits - np.max(logits))
    return (e_x / e_x.sum(axis=0)).astype(np.float32)

@app.post("/predict")
async def predict(file: UploadFile = File(...), language: str = "en"):
    if not v_session or not d_session:
        raise HTTPException(status_code=503, detail="Models offline")

    try:
        contents = await file.read()
        image = Image.open(io.BytesIO(contents))
        input_tensor = preprocess_image(image)

        # Variety Inference
        v_outputs = v_session.run(['logits', 'embedding'], {'image': input_tensor})
        v_logits = v_outputs[0][0]
        v_embedding = v_outputs[1].astype(np.float32)
        
        v_probs = get_probs(v_logits)
        v_idx = np.argmax(v_probs)
        v_conf = float(v_probs[v_idx])
        variety = VARIETY_LABELS[v_idx] if v_idx < len(VARIETY_LABELS) else "Unknown"
        
        # Disease Inference
        variety_one_hot = np.zeros((1, len(VARIETY_LABELS)), dtype=np.float32)
        variety_one_hot[0, v_idx] = 1.0

        d_outputs = d_session.run(['logits'], {'embedding': v_embedding, 'variety_one_hot': variety_one_hot})
        d_logits = d_outputs[0][0]
        d_probs = get_probs(d_logits)
        d_idx = np.argmax(d_probs)
        d_conf = float(d_probs[d_idx])
        disease = DISEASE_LABELS[d_idx] if d_idx < len(DISEASE_LABELS) else "Unknown"

        # Final Score Logic
        display_conf = max(v_conf, d_conf) * 100
        if v_conf < 0.2: display_conf = v_conf * 100

        print(f"PREDICTED [{language}]: {variety} - {disease} ({display_conf:.2f}%)")

        if display_conf < 35.0:
            variety, disease, severity = "Unknown", "Unknown", "Low Confidence"
        else:
            severity = "Warning" if disease != "Healthy" else "None"

        # Localization Logic
        label_key = f"{variety.lower()}_{disease.replace(' ', '_').lower()}"
        lang_key = language.lower() if language.lower() in ["en", "hi", "mr"] else "en"
        
        details_entry = TREATMENTS.get(label_key, TREATMENTS.get("unknown"))
        localized_details = details_entry.get(lang_key, details_entry.get("en"))

        return {
            "success": True,
            "crop": variety,
            "disease": disease,
            "confidence": round(display_conf, 2),
            "severity": severity,
            "description": localized_details["description"],
            "treatments": localized_details["treatment"]
        }

    except Exception as e:
        print(f"ERROR: {e}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="127.0.0.1", port=8001)
