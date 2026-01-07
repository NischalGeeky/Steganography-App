import os
from PIL import Image
import torch
import torch.nn as nn
from torch.utils.data import Dataset, DataLoader
from torchvision import transforms, models
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, classification_report

# -----------------------------
# Configuration
# -----------------------------
DATASET_ROOT = "dataset"   # change if needed
BATCH_SIZE = 16
NUM_EPOCHS = 50
LEARNING_RATE = 1e-4
PATIENCE = 5
DEVICE = "cuda" if torch.cuda.is_available() else "cpu"
RANDOM_STATE = 42

# -----------------------------
# Step 1: Collect image paths
# -----------------------------
cover_dirs = ["cover", "cover_new"]
stego_dirs = ["stego", "stego_new"]

image_paths = []
labels = []

# Cover images → label 0
for d in cover_dirs:
    folder = os.path.join(DATASET_ROOT, d)
    for f in os.listdir(folder):
        if f.lower().endswith((".png", ".jpg", ".jpeg")):
            image_paths.append(os.path.join(folder, f))
            labels.append(0)

# Stego images → label 1
for d in stego_dirs:
    folder = os.path.join(DATASET_ROOT, d)
    for f in os.listdir(folder):
        if f.lower().endswith((".png", ".jpg", ".jpeg")):
            image_paths.append(os.path.join(folder, f))
            labels.append(1)

print(f"Total images: {len(image_paths)}")
print(f"Cover: {labels.count(0)}, Stego: {labels.count(1)}")

# -----------------------------
# Step 2: Train / Val / Test split
# -----------------------------
X_train, X_temp, y_train, y_temp = train_test_split(
    image_paths,
    labels,
    test_size=0.30,
    stratify=labels,
    random_state=RANDOM_STATE
)

X_val, X_test, y_val, y_test = train_test_split(
    X_temp,
    y_temp,
    test_size=0.50,
    stratify=y_temp,
    random_state=RANDOM_STATE
)

print(f"Train: {len(X_train)}, Val: {len(X_val)}, Test: {len(X_test)}")

# -----------------------------
# Step 3: Dataset class
# -----------------------------
class StegoDataset(Dataset):
    def __init__(self, paths, labels, transform=None):
        self.paths = paths
        self.labels = labels
        self.transform = transform

    def __len__(self):
        return len(self.paths)

    def __getitem__(self, idx):
        img = Image.open(self.paths[idx]).convert("RGB")
        label = self.labels[idx]
        if self.transform:
            img = self.transform(img)
        return img, label

# -----------------------------
# Step 4: Transforms & loaders
# -----------------------------
transform = transforms.Compose([
    transforms.Resize((256, 256)),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.5, 0.5, 0.5],
                         std=[0.5, 0.5, 0.5])
])

train_ds = StegoDataset(X_train, y_train, transform)
val_ds   = StegoDataset(X_val, y_val, transform)
test_ds  = StegoDataset(X_test, y_test, transform)

train_loader = DataLoader(train_ds, batch_size=BATCH_SIZE, shuffle=True)
val_loader   = DataLoader(val_ds, batch_size=BATCH_SIZE, shuffle=False)
test_loader  = DataLoader(test_ds, batch_size=BATCH_SIZE, shuffle=False)

# -----------------------------
# Step 5: Model (ResNet18 + Dropout)
# -----------------------------
model = models.resnet18(weights="IMAGENET1K_V1")
model.fc = nn.Sequential(
    nn.Dropout(p=0.25),
    nn.Linear(model.fc.in_features, 2)
)
model.to(DEVICE)

# -----------------------------
# Step 6: Loss & optimizer
# -----------------------------
criterion = nn.CrossEntropyLoss()
optimizer = torch.optim.Adam(model.parameters(), lr=LEARNING_RATE)

# -----------------------------
# Step 7: Training with Early Stopping
# -----------------------------
best_val_loss = float("inf")
patience_counter = 0

for epoch in range(NUM_EPOCHS):
    # ---- Train ----
    model.train()
    train_loss = 0.0

    for images, labels in train_loader:
        images, labels = images.to(DEVICE), labels.to(DEVICE)

        optimizer.zero_grad()
        outputs = model(images)
        loss = criterion(outputs, labels)
        loss.backward()
        optimizer.step()

        train_loss += loss.item()

    train_loss /= len(train_loader)

    # ---- Validate ----
    model.eval()
    val_loss = 0.0

    with torch.no_grad():
        for images, labels in val_loader:
            images, labels = images.to(DEVICE), labels.to(DEVICE)
            outputs = model(images)
            loss = criterion(outputs, labels)
            val_loss += loss.item()

    val_loss /= len(val_loader)

    print(
        f"Epoch [{epoch+1}/{NUM_EPOCHS}] "
        f"Train Loss: {train_loss:.4f} | Val Loss: {val_loss:.4f}"
    )

    # ---- Early stopping ----
    if val_loss < best_val_loss:
        best_val_loss = val_loss
        patience_counter = 0
        torch.save(model.state_dict(), "best_stego_model.pt")
    else:
        patience_counter += 1
        if patience_counter >= PATIENCE:
            print("Early stopping triggered.")
            break

# -----------------------------
# Step 8: Test Evaluation
# -----------------------------
model.load_state_dict(torch.load("best_stego_model.pt"))
model.eval()

y_true, y_pred = [], []

with torch.no_grad():
    for images, labels in test_loader:
        images = images.to(DEVICE)
        outputs = model(images)
        preds = torch.argmax(outputs, dim=1).cpu().numpy()

        y_pred.extend(preds)
        y_true.extend(labels.numpy())

print("\nTest Accuracy:", accuracy_score(y_true, y_pred))
print("\nClassification Report:\n")
print(classification_report(y_true, y_pred, target_names=["Cover", "Stego"]))
