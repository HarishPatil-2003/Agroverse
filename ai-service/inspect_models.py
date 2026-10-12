import onnxruntime as ort

def inspect_model(path):
    print(f"\n--- Inspecting {path} ---")
    try:
        session = ort.InferenceSession(path)
        for i in session.get_inputs():
            print(f"Input: {i.name}, Shape: {i.shape}, Type: {i.type}")
        for o in session.get_outputs():
            print(f"Output: {o.name}, Shape: {o.shape}, Type: {o.type}")
    except Exception as e:
        print(f"Error: {e}")

inspect_model("model/crop_variety.onnx")
inspect_model("model/crop_disease.onnx")
