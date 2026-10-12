import onnxruntime as ort
import numpy as np
import json

v_sess = ort.InferenceSession('model/crop_variety.onnx', providers=['CPUExecutionProvider'])
d_sess = ort.InferenceSession('model/crop_disease.onnx', providers=['CPUExecutionProvider'])

VARIETY_LABELS = json.load(open('variety_labels.json'))
DISEASE_LABELS = json.load(open('disease_labels.json'))

print('Variety labels:', VARIETY_LABELS)
print('Disease labels:', DISEASE_LABELS)
print()

for seed in [0, 42, 99, 137, 256, 512, 777]:
    np.random.seed(seed)
    tensor = np.random.rand(1, 3, 224, 224).astype(np.float32)
    mean = np.array([0.485, 0.456, 0.406])
    std  = np.array([0.229, 0.224, 0.225])
    for c in range(3):
        tensor[0, c] = (tensor[0, c] - mean[c]) / std[c]

    v_out = v_sess.run(['logits', 'embedding'], {'image': tensor})
    v_logits = v_out[0][0]
    v_emb    = v_out[1]

    e_v = np.exp(v_logits - np.max(v_logits))
    v_probs = e_v / e_v.sum()
    v_idx = int(np.argmax(v_probs))

    v_one_hot = np.zeros((1, len(VARIETY_LABELS)), dtype=np.float32)
    v_one_hot[0, v_idx] = 1.0

    d_out = d_sess.run(['logits'], {'embedding': v_emb, 'variety_one_hot': v_one_hot})
    d_logits = d_out[0][0]
    e_d = np.exp(d_logits - np.max(d_logits))
    d_probs = e_d / e_d.sum()
    d_idx = int(np.argmax(d_probs))

    v_name = VARIETY_LABELS[v_idx]
    d_name = DISEASE_LABELS[d_idx]
    v_conf = float(v_probs[v_idx]) * 100
    d_conf = float(d_probs[d_idx]) * 100
    v_all = [round(float(p)*100, 1) for p in v_probs]
    d_all = [round(float(p)*100, 1) for p in d_probs]

    print(f"Seed {seed:3d}: Variety={v_name} ({v_conf:.1f}%)  Disease={d_name} ({d_conf:.1f}%)")
    print(f"         V-probs: {list(zip(VARIETY_LABELS, v_all))}")
    print(f"         D-probs: {list(zip(DISEASE_LABELS, d_all))}")
    print()

# Test with a solid-color green image (simulating a leaf)
print("--- Solid Green Image Test ---")
tensor = np.zeros((1, 3, 224, 224), dtype=np.float32)
# Green pixel: R=0, G=200, B=0 -> normalized
tensor[0, 0] = (0.0 - 0.485) / 0.229    # R channel
tensor[0, 1] = (200.0/255.0 - 0.456) / 0.224  # G channel
tensor[0, 2] = (0.0 - 0.406) / 0.225    # B channel

v_out = v_sess.run(['logits', 'embedding'], {'image': tensor})
v_logits = v_out[0][0]
v_emb = v_out[1]
e_v = np.exp(v_logits - np.max(v_logits))
v_probs = e_v / e_v.sum()
v_idx = int(np.argmax(v_probs))
v_one_hot = np.zeros((1, len(VARIETY_LABELS)), dtype=np.float32)
v_one_hot[0, v_idx] = 1.0
d_out = d_sess.run(['logits'], {'embedding': v_emb, 'variety_one_hot': v_one_hot})
d_logits = d_out[0][0]
e_d = np.exp(d_logits - np.max(d_logits))
d_probs = e_d / e_d.sum()
d_idx = int(np.argmax(d_probs))
print(f"Green: Variety={VARIETY_LABELS[v_idx]} ({float(v_probs[v_idx])*100:.1f}%)  Disease={DISEASE_LABELS[d_idx]} ({float(d_probs[d_idx])*100:.1f}%)")
print(f"V-probs: {list(zip(VARIETY_LABELS, [round(float(p)*100,1) for p in v_probs]))}")
print(f"D-probs: {list(zip(DISEASE_LABELS, [round(float(p)*100,1) for p in d_probs]))}")
print()
print("All logits variety:", v_logits)
print("All logits disease:", d_logits)
