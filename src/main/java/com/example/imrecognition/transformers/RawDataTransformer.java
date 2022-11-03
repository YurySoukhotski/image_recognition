package com.example.imrecognition.transformers;

public interface RawDataTransformer {
    RecognitionModel transform(String rawData);
}
