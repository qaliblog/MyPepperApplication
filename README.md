# Barista - Cafe & Restaurant Management App

Barista is a modern Android application designed specifically for cafe and restaurant owners to manage their food inventory, scan items, and view 3D models of their products.

## Features

### 🏠 Home Screen
- Welcome dashboard with quick action buttons
- Easy navigation to scan items or view 3D models
- Overview of recent activities

### 📱 Scan Screen
- Camera integration for scanning food items
- Barcode scanning capabilities using ML Kit
- Quick item addition to inventory

### 📦 Inventory Management
- View all food items in a clean, organized list
- Add new items with name, price, and description
- Delete items from inventory
- Search items by barcode

### 🎨 3D Models
- View 3D models of food items
- Support for various 3D file formats
- Interactive 3D model viewer

## Technical Features

### Database
- Room database for persistent storage
- Local SQLite database for offline functionality
- Efficient data management with DAO pattern

### Architecture
- MVVM (Model-View-ViewModel) architecture
- Repository pattern for data access
- Kotlin Coroutines for asynchronous operations
- Jetpack Compose for modern UI

### Dependencies
- **Camera**: Android CameraX for camera functionality
- **Scanning**: ML Kit for barcode scanning
- **3D Rendering**: Filament for 3D model rendering
- **Database**: Room for local data storage
- **UI**: Jetpack Compose with Material 3 design

## Setup

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Build and run on device

## Permissions

The app requires the following permissions:
- Camera: For scanning food items and barcodes
- Storage: For saving 3D models and images

## Package Structure

```
com.qali.barista/
├── data/
│   ├── FoodItem.kt
│   ├── FoodItemDao.kt
│   ├── AppDatabase.kt
│   └── FoodItemRepository.kt
├── ui/
│   ├── BaristaViewModel.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── MainActivity.kt
```

## Usage

1. **Adding Items**: Use the floating action button to add new food items
2. **Scanning**: Navigate to the Scan tab to scan barcodes or take photos
3. **Inventory**: View and manage all items in the Inventory tab
4. **3D Models**: Access 3D models in the 3D Models tab

## Development

This app is built with modern Android development practices:
- Kotlin as the primary language
- Jetpack Compose for UI
- Room for database management
- MVVM architecture pattern
- Material 3 design system

## Assets

- Place your TFLite object detection model (e.g., ssd_mobilenet_v1.tflite) in `app/src/main/assets/`.
- Place your 3D model files (e.g., pizza.glb) in `app/src/main/assets/`.

## License

This project is licensed under the MIT License.
