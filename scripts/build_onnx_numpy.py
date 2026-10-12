import onnx
from onnx import helper, TensorProto
import numpy as np
import os

def create_variety_model(output_path):
    # Input: image [1, 3, 224, 224]
    image = helper.make_tensor_value_info('image', TensorProto.FLOAT, [1, 3, 224, 224])
    
    # Outputs: logits [1, 7], embedding [1, 128]
    logits = helper.make_tensor_value_info('logits', TensorProto.FLOAT, [1, 7])
    embedding = helper.make_tensor_value_info('embedding', TensorProto.FLOAT, [1, 128])
    
    # Dummy weights for constants
    logits_val = np.random.randn(1, 7).astype(np.float32)
    embedding_val = np.random.randn(1, 128).astype(np.float32)
    
    # Nodes to output constants
    node1 = helper.make_node('Constant', [], ['logits'], value=helper.make_tensor('logits_val', TensorProto.FLOAT, [1, 7], logits_val.flatten()))
    node2 = helper.make_node('Constant', [], ['embedding'], value=helper.make_tensor('embedding_val', TensorProto.FLOAT, [1, 128], embedding_val.flatten()))
    
    # Create the graph
    graph = helper.make_graph([node1, node2], 'variety_model', [image], [logits, embedding])
    
    # Use Opset 15 and IR version 8 (widely compatible)
    model = helper.make_model(graph, producer_name='agroverse', ir_version=8)
    model.opset_import[0].version = 15
    
    onnx.save(model, output_path)
    print(f"Created {output_path} (IR v8, Opset 15)")

def create_disease_model(output_path):
    # Inputs: embedding [1, 128], variety_one_hot [1, 7]
    embedding = helper.make_tensor_value_info('embedding', TensorProto.FLOAT, [1, 128])
    variety_one_hot = helper.make_tensor_value_info('variety_one_hot', TensorProto.FLOAT, [1, 7])
    
    # Output: logits [1, 5]
    logits = helper.make_tensor_value_info('logits', TensorProto.FLOAT, [1, 5])
    
    # Dummy weights for constant
    logits_val = np.random.randn(1, 5).astype(np.float32)
    
    # Node to output constant
    node = helper.make_node('Constant', [], ['logits'], value=helper.make_tensor('logits_val', TensorProto.FLOAT, [1, 5], logits_val.flatten()))
    
    # Create the graph
    graph = helper.make_graph([node], 'disease_model', [embedding, variety_one_hot], [logits])
    
    # Use Opset 15 and IR version 8
    model = helper.make_model(graph, producer_name='agroverse', ir_version=8)
    model.opset_import[0].version = 15
    
    onnx.save(model, output_path)
    print(f"Created {output_path} (IR v8, Opset 15)")

if __name__ == "__main__":
    base_path = "backend/src/main/resources/models"
    if not os.path.exists(base_path):
        os.makedirs(base_path)
        
    create_variety_model(os.path.join(base_path, "crop_variety.onnx"))
    create_disease_model(os.path.join(base_path, "crop_disease.onnx"))
