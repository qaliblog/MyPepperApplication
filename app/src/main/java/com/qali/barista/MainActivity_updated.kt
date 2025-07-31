package com.qali.barista

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qali.barista.ui.BaristaViewModel
import com.qali.barista.ui.FoodItem
import com.qali.barista.ui.theme.BaristaTheme
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.window.Dialog
import com.google.android.filament.utils.ModelViewer
import android.view.SurfaceView
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorOptions
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaristaTheme {
                val viewModel: BaristaViewModel = viewModel()
                BaristaApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaristaApp(viewModel: BaristaViewModel) {
    MaterialTheme {
        var selectedTab by remember { mutableStateOf(0) }
        val items by viewModel.items.collectAsState()
        var showAddItemDialog by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Barista", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Search, contentDescription = "Scan") },
                        label = { Text("Scan") },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.List, contentDescription = "Inventory") },
                        label = { Text("Inventory") },
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Info, contentDescription = "3D Models") },
                        label = { Text("3D Models") },
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 }
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddItemDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                }
            }
        ) { paddingValues ->
            when (selectedTab) {
                0 -> HomeScreen(paddingValues)
                1 -> ScanScreen(paddingValues) { barcode, modelFile ->
                    viewModel.addItem(barcode, 0.0, "Scanned item", modelFile)
                }
                2 -> InventoryScreen(items, paddingValues)
                3 -> Models3DScreen(paddingValues)
            }
        }

        if (showAddItemDialog) {
            AddItemDialog(
                onDismiss = { showAddItemDialog = false },
                onItemAdded = { name, price, description ->
                    viewModel.addItem(name, price, description)
                    showAddItemDialog = false
                }
            )
        }
    }
}

@Composable
fun HomeScreen(paddingValues: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Welcome to Barista",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Quick Actions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuickActionButton(
                            icon = Icons.Default.Search,
                            label = "Scan Item",
                            onClick = { /* Navigate to scan */ }
                        )
                        QuickActionButton(
                            icon = Icons.Default.Info,
                            label = "3D Models",
                            onClick = { /* Navigate to 3D models */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScanScreen(paddingValues: PaddingValues, onObjectDetected: (String, String?) -> Unit = { _, _ -> }) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<String?>(null) }
    var objectDetector by remember { mutableStateOf<ObjectDetector?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Initialize MediaPipe Object Detector
    LaunchedEffect(Unit) {
        try {
            hasCameraPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            
            // Initialize MediaPipe Object Detector
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("efficientdet_lite0.tflite")
                .build()
            
            val options = ObjectDetectorOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setMaxResults(5)
                .setScoreThreshold(0.5f)
                .setResultListener { detectionResult, image ->
                    // Process detection results on main thread
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        if (detectionResult.detections().isNotEmpty()) {
                            val detection = detectionResult.detections()[0]
                            val category = detection.categories()[0]
                            val label = category.categoryName()
                            if (label != "background" && scanResult != label) {
                                scanResult = label
                                val modelFile = if (label.lowercase().contains("pizza")) "pizza.glb" else null
                                onObjectDetected(label, modelFile)
                            }
                        }
                    }
                }
                .setErrorListener { _, error ->
                    android.util.Log.e("MediaPipe", "Object detection error: $error")
                    errorMessage = "Detection error: $error"
                }
                .build()
            
            objectDetector = ObjectDetector.createFromOptions(context, options)
        } catch (e: Exception) {
            errorMessage = "Failed to initialize object detector: ${e.message}"
            android.util.Log.e("ScanScreen", "Initialization error: ${e.message}")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    if (!hasCameraPermission) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Camera permission required to scan items.")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("Grant Camera Permission")
            }
        }
        return
    }

    if (errorMessage != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { errorMessage = null }) {
                Text("Retry")
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Camera preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        // Scan controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (scanResult != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Detected: $scanResult",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Button(
                                onClick = {
                                    val modelFile = if (scanResult!!.lowercase().contains("pizza")) "pizza.glb" else null
                                    onObjectDetected(scanResult!!, modelFile)
                                    scanResult = null
                                }
                            ) {
                                Text("Add to Inventory")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { scanResult = null }) {
                                Text("Scan Again")
                            }
                        }
                    }
                }
            } else {
                Button(
                    onClick = { 
                        isScanning = true
                        // Start real-time object detection
                        scanResult = null
                        isScanning = false
                    },
                    enabled = !isScanning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scanning...")
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Object Detection")
                    }
                }
            }
        }
    }

    LaunchedEffect(cameraProviderFuture, objectDetector) {
        try {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            
            imageAnalyzer.setAnalyzer(executor) { imageProxy ->
                try {
                    if (objectDetector != null) {
                        val bitmap = imageProxyToBitmap(imageProxy)
                        if (bitmap != null) {
                            val mediaImage = com.google.mediapipe.tasks.vision.core.VisionImage.fromBitmap(bitmap)
                            objectDetector?.detectAsync(mediaImage, imageProxy.imageInfo.timestamp)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ScanScreen", "Error processing frame: ${e.message}")
                } finally {
                    imageProxy.close()
                }
            }
            
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalyzer
            )
        } catch (e: Exception) {
            android.util.Log.e("ScanScreen", "Camera error: ${e.message}")
            errorMessage = "Camera error: ${e.message}"
        }
    }
}

fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
    return try {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
        val imageBytes = out.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        bitmap
    } catch (e: Exception) {
        android.util.Log.e("ImageProcessing", "Error converting image proxy to bitmap: ${e.message}")
        null
    }
}

@Composable
fun InventoryScreen(items: List<FoodItem>, paddingValues: PaddingValues) {
    var show3DModel by remember { mutableStateOf<String?>(null) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Inventory",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        if (items.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Empty Inventory",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No items in inventory",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(items) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                fontWeight = FontWeight.Bold
                            )
                            if (item.model3dUrl != null) {
                                TextButton(onClick = { show3DModel = item.model3dUrl }) {
                                    Text("View 3D Model")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (show3DModel != null) {
        Model3DDialog(modelFile = show3DModel!!) { show3DModel = null }
    }
}

@Composable
fun Model3DDialog(modelFile: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.size(400.dp, 400.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                try {
                    AndroidView(
                        factory = { context ->
                            val surfaceView = SurfaceView(context)
                            try {
                                val modelViewer = ModelViewer(surfaceView)
                                // Load the GLB model from assets
                                val assetManager = context.assets
                                val input = assetManager.open(modelFile)
                                val bytes = input.readBytes()
                                input.close()
                                val buffer = java.nio.ByteBuffer.wrap(bytes)
                                modelViewer.loadModelGlb(buffer)
                                modelViewer.transformToUnitCube()
                            } catch (e: Exception) {
                                android.util.Log.e("Model3D", "Failed to load 3D model: ${e.message}")
                            }
                            surfaceView
                        },
                        modifier = Modifier.size(300.dp, 300.dp)
                    )
                } catch (e: Exception) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "3D Model",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "3D Model Viewer",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "3D model loading is not available in this demo",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun Models3DScreen(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "3D Models",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "3D Models",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "View and manage 3D models of your food items",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { /* Implement 3D model viewer */ }
        ) {
            Icon(Icons.Default.Info, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("View Models")
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier.size(80.dp)
        ) {
            Icon(icon, contentDescription = label)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onItemAdded: (String, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Item") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && price.isNotBlank()) {
                        onItemAdded(
                            name,
                            price.toDoubleOrNull() ?: 0.0,
                            description
                        )
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}