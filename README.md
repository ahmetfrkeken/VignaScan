# VignaScan

An Android application for **real-time, on-device classification of Black Gram (*Vigna mungo*) leaf diseases**, built to field-test the hybrid deep learning models developed for my MSc thesis in Computer Engineering (Bursa Technical University).

## Why this exists

Most plant disease classification research stops at a notebook with an accuracy number. This project closes the loop: it takes the trained models, converts them to a mobile-native format, and validates them **on a real device in the field** — where connectivity and compute are limited.

## Research background

The models integrated here come from a thesis studying hybrid CNN–ViT architectures for early, high-accuracy detection of four disease classes (Yellow Mosaic, Cercospora Leaf Spot, Leaf Crinkle, Insect Damage):

- **Architecture:** MobileNetV3-Small / SqueezeNet as feature extractors, fused with a Vision Transformer (ViT) for global attention
- **Hyperparameter optimization:** Grey Wolf Optimizer (GWO) and Particle Swarm Optimization (PSO), applied to learning rate and batch size
- **Validation:** 5-fold cross-validation with Weighted Random Sampling to correct class imbalance
- **Explainability:** Grad-CAM, to confirm the model attends to disease symptoms rather than background noise
- **Result:** up to 99.41% F1-score (SqueezeNet + ViT, GWO-optimized)

## What this app does

- Runs two lightweight variants (MobileNetV3-Small+Tiny-ViT, SqueezeNet+Tiny-ViT), converted to **PyTorch Mobile Lite**, directly on-device — no server round-trip
- Captures or loads a leaf image and returns a disease prediction with confidence

## Tech stack

- **Language:** Kotlin
- **ML runtime:** PyTorch Mobile (Lite Interpreter)
- **Platform:** Native Android

## Getting started

```bash
git clone https://github.com/ahmetfrkeken/VignaScan.git
```

1. Open the project in Android Studio (Giraffe or later recommended)
2. Let Gradle sync
3. Run on a device or emulator (API level TBD)

## Status

Research prototype built for thesis field-testing, not a production release.

## Author

**Ahmet Faruk Eken** — MSc, Computer Engineering, Bursa Technical University
[LinkedIn](https://www.linkedin.com/in/ahmet-faruk-eken-0b0a80150/) · [GitHub](https://github.com/ahmetfrkeken) · ekenahmetfaruk@gmail.com
