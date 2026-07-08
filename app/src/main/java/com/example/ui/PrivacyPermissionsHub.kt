package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.viewmodel.DatingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PrivacyPermissionsCard(viewModel: DatingViewModel) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableStateOf(0) }
    
    var showContactsDialog by remember { mutableStateOf(false) }
    var showCameraVerificationDialog by remember { mutableStateOf(false) }

    // Check status of each permission dynamically
    val isCameraGranted = remember(refreshTrigger) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    
    val isPhotosGranted = remember(refreshTrigger) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    val isLocationGranted = remember(refreshTrigger) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    val isNotificationsGranted = remember(refreshTrigger) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Prior to Android 13, notifications are always granted
        }
    }

    val isMicGranted = remember(refreshTrigger) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    val isContactsGranted = remember(refreshTrigger) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }

    // Launchers for each permission
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.showNotification("📸 Camera Access Granted!")
            showCameraVerificationDialog = true
        } else {
            viewModel.showNotification("⚠️ Camera access is required for bio photos & verification.")
        }
        refreshTrigger++
    }

    val photosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.showNotification("🖼️ Photos Access Granted!")
        } else {
            viewModel.showNotification("⚠️ Photos access is required to upload custom pictures.")
        }
        refreshTrigger++
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.showNotification("📍 Location access granted for nearby matches!")
        } else {
            viewModel.showNotification("⚠️ Location access is required to find local singles.")
        }
        refreshTrigger++
    }

    val notificationsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.showNotification("🔔 Notifications enabled for alerts & chats!")
        } else {
            viewModel.showNotification("⚠️ Notifications disabled. You might miss matches.")
        }
        refreshTrigger++
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.showNotification("🎙️ Microphone access granted for voice notes!")
        } else {
            viewModel.showNotification("⚠️ Microphone is required for voice messages & calls.")
        }
        refreshTrigger++
    }

    val contactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.showNotification("👥 Contacts synced!")
            showContactsDialog = true
        } else {
            viewModel.showNotification("⚠️ Contacts permission is optional.")
        }
        refreshTrigger++
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, TealVibrant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("permissions_hub_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Privacy Shield",
                    tint = TealAccent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "🔒 Privacy & Permissions Center",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Manage system permissions to unlock premium social tools",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = NavyLight.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            // Permissions definitions
            val permissionsList = listOf(
                PermissionItemData(
                    title = "Camera Access",
                    desc = "Real-time face scan verification & active selfie capture",
                    isGranted = isCameraGranted,
                    onRequest = {
                        if (isCameraGranted) {
                            showCameraVerificationDialog = true
                        } else {
                            cameraLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    icon = Icons.Default.Face,
                    tag = "perm_camera"
                ),
                PermissionItemData(
                    title = "Photos & Media",
                    desc = "Upload high-fidelity profile photos from your local library",
                    isGranted = isPhotosGranted,
                    onRequest = {
                        if (android.os.Build.VERSION.SDK_INT >= 33) {
                            photosLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        } else {
                            photosLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    },
                    icon = Icons.Default.Add,
                    tag = "perm_photos"
                ),
                PermissionItemData(
                    title = "Geographic Location",
                    desc = "Precise coordinate syncing to display nearby online singles",
                    isGranted = isLocationGranted,
                    onRequest = {
                        locationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    icon = Icons.Default.LocationOn,
                    tag = "perm_location"
                ),
                PermissionItemData(
                    title = "Push Notifications",
                    desc = "Never miss instant match approvals & chat notification triggers",
                    isGranted = isNotificationsGranted,
                    onRequest = {
                        if (android.os.Build.VERSION.SDK_INT >= 33) {
                            notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.showNotification("🔔 Notifications are already permitted on your OS.")
                        }
                    },
                    icon = Icons.Default.Notifications,
                    tag = "perm_notifications"
                ),
                PermissionItemData(
                    title = "Microphone Audio",
                    desc = "Record high-fidelity voice notes & stream active voice calls",
                    isGranted = isMicGranted,
                    onRequest = {
                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    icon = Icons.Default.PlayArrow,
                    tag = "perm_microphone"
                ),
                PermissionItemData(
                    title = "Address Book Contacts",
                    desc = "Scan offline phone books to immediately add friends",
                    isGranted = isContactsGranted,
                    onRequest = {
                        if (isContactsGranted) {
                            showContactsDialog = true
                        } else {
                            contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    },
                    icon = Icons.Default.Person,
                    tag = "perm_contacts"
                )
            )

            permissionsList.forEachIndexed { index, perm ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { perm.onRequest() }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (perm.isGranted) TealAccent.copy(alpha = 0.12f)
                                else NavyLight.copy(alpha = 0.3f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = perm.icon,
                            contentDescription = perm.title,
                            tint = if (perm.isGranted) TealAccent else Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = perm.title,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = perm.desc,
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = if (perm.isGranted) TealAccent.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.12f),
                        border = BorderStroke(
                            1.dp,
                            if (perm.isGranted) TealAccent.copy(alpha = 0.4f) else Color.Red.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .testTag(perm.tag)
                            .clickable { perm.onRequest() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (perm.isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = if (perm.isGranted) "Granted" else "Denied",
                                tint = if (perm.isGranted) TealAccent else Color.Red,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (perm.isGranted) "GRANTED" else "DENIED",
                                color = if (perm.isGranted) TealAccent else Color.Red,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (index < permissionsList.lastIndex) {
                    Divider(color = NavyLight.copy(alpha = 0.15f), modifier = Modifier.padding(start = 50.dp))
                }
            }
        }
    }

    if (showContactsDialog) {
        ContactsSyncDialog(
            onDismiss = { showContactsDialog = false },
            viewModel = viewModel
        )
    }

    if (showCameraVerificationDialog) {
        VideoVerificationDialog(
            onDismiss = { showCameraVerificationDialog = false },
            viewModel = viewModel
        )
    }
}

@Composable
fun ContactsSyncDialog(
    onDismiss: () -> Unit,
    viewModel: DatingViewModel
) {
    var isScanning by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    
    // Simulated contact matches
    val matchedContacts = remember {
        listOf(
            ContactFriend("Sarah Miller", "🦊", "Hiking & coffee lover! Let's match up.", true),
            ContactFriend("Michael Davidson", "👾", "Retro arcade fanatic. Looking for casual chats.", false),
            ContactFriend("Emma Watson", "🎨", "Art teacher, looking for local paint buddies.", true),
            ContactFriend("David Johnson", "👩‍💻", "AI developer, tech purist.", false)
        )
    }

    LaunchedEffect(Unit) {
        scope.launch {
            delay(1800)
            isScanning = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "👥 Contacts Sync Manager",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = "Your phone address book is successfully securely synced.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(20.dp))

                if (isScanning) {
                    CircularProgressIndicator(color = TealAccent, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Scanning address book for active singles...", color = Color.Gray, fontSize = 12.sp)
                } else {
                    Text(
                        text = "Matched Offline Friends Found:",
                        color = TealAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        matchedContacts.forEach { friend ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(NavyLight, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(TealAccent.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(friend.emoji, fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(friend.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(friend.bio, color = Color.Gray, fontSize = 10.sp, maxLines = 1)
                                }
                                Button(
                                    onClick = {
                                        viewModel.showNotification("✨ Invite request sent to ${friend.name}!")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(if (friend.isRegistered) "ADD" else "INVITE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = NavyLight),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close Contacts Manager", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun VideoVerificationDialog(
    onDismiss: () -> Unit,
    viewModel: DatingViewModel
) {
    var step by remember { mutableStateOf("START") } // START, SCANNING, COMPLETE
    val scope = rememberCoroutineScope()
    var pulseState by remember { mutableStateOf(1f) }

    LaunchedEffect(step) {
        if (step == "SCANNING") {
            scope.launch {
                repeat(4) {
                    pulseState = 1.2f
                    delay(500)
                    pulseState = 0.9f
                    delay(500)
                }
                step = "COMPLETE"
                viewModel.showNotification("🛡️ Video Verification Successful! Blue Badge Unlocked.")
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🛡️ Bio Video Verification",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = "Confirm your identity via a secure 3D face scan.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(30.dp))

                when (step) {
                    "START" -> {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(NavyLight)
                                .border(2.dp, TealAccent.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Camera feed icon",
                                tint = TealAccent,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(30.dp))
                        
                        Button(
                            onClick = { step = "SCANNING" },
                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start 3D Face Scan", color = Color.White)
                        }
                    }
                    "SCANNING" -> {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .scale(pulseState)
                                .clip(CircleShape)
                                .background(TealAccent.copy(alpha = 0.1f))
                                .border(2.dp, TealAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Face Scan",
                                tint = TealAccent,
                                modifier = Modifier.size(60.dp)
                            )
                            // Scan line effect
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(TealAccent)
                                    .align(Alignment.Center)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(30.dp))
                        
                        Text(
                            text = "Processing neural markers...",
                            color = TealAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    "COMPLETE" -> {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(TealAccent.copy(alpha = 0.15f))
                                .border(3.dp, TealAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = TealAccent,
                                modifier = Modifier.size(70.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(30.dp))
                        
                        Text(
                            text = "Identity Verified Successfully!",
                            color = TealAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = NavyLight),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

data class ContactFriend(
    val name: String,
    val emoji: String,
    val bio: String,
    val isRegistered: Boolean
)

data class PermissionItemData(
    val title: String,
    val desc: String,
    val isGranted: Boolean,
    val onRequest: () -> Unit,
    val icon: ImageVector,
    val tag: String
)

@Composable
fun StartupPermissionsRequestDialog(
    onDismiss: () -> Unit,
    viewModel: DatingViewModel
) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableStateOf(0) }

    val isCameraGranted = remember(refreshTrigger) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    
    val isPhotosGranted = remember(refreshTrigger) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    val isLocationGranted = remember(refreshTrigger) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    val isNotificationsGranted = remember(refreshTrigger) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val isMicGranted = remember(refreshTrigger) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    val isContactsGranted = remember(refreshTrigger) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }

    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        refreshTrigger++
        val grantedCount = results.filter { it.value }.size
        viewModel.showNotification("🛡️ System updated: $grantedCount permissions configured!")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("startup_permissions_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header section
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(TealAccent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Shield Icon",
                        tint = TealAccent,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome to Find Correct! 🎉",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = "Please enable key system capabilities for the ultimate dating and match verification experience.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = NavyLight.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable List of Permissions inside Dialog
                Box(modifier = Modifier.heightIn(max = 280.dp)) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            PermissionDialogRow(
                                title = "Camera verification",
                                desc = "Profile photos & secure video verification badge",
                                isGranted = isCameraGranted,
                                icon = Icons.Default.Face
                            )
                        }
                        item {
                            PermissionDialogRow(
                                title = "Photos & Media",
                                desc = "Upload customizable lifestyle profile pictures",
                                isGranted = isPhotosGranted,
                                icon = Icons.Default.Add
                            )
                        }
                        item {
                            PermissionDialogRow(
                                title = "Precise Location",
                                desc = "Display matches closest to you in real-time",
                                isGranted = isLocationGranted,
                                icon = Icons.Default.LocationOn
                            )
                        }
                        item {
                            PermissionDialogRow(
                                title = "Match Notifications",
                                desc = "Instant alerts for chats, likes & system triggers",
                                isGranted = isNotificationsGranted,
                                icon = Icons.Default.Notifications
                            )
                        }
                        item {
                            PermissionDialogRow(
                                title = "Microphone Audio",
                                desc = "Record audio voice notes & connect in group calls",
                                isGranted = isMicGranted,
                                icon = Icons.Default.PlayArrow
                            )
                        }
                        item {
                            PermissionDialogRow(
                                title = "Address Book (Optional)",
                                desc = "Instantly discover offline friends on the app",
                                isGranted = isContactsGranted,
                                icon = Icons.Default.Person
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // One-click request button
                Button(
                    onClick = {
                        val list = mutableListOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.READ_CONTACTS
                        ).apply {
                            if (android.os.Build.VERSION.SDK_INT >= 33) {
                                add(Manifest.permission.READ_MEDIA_IMAGES)
                                add(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        }
                        multiplePermissionLauncher.launch(list.toTypedArray())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("startup_grant_all_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Check",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GRANT ALL PERMISSIONS",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Skip and continue to app",
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionDialogRow(
    title: String,
    desc: String,
    isGranted: Boolean,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavyLight.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    if (isGranted) TealAccent.copy(alpha = 0.12f)
                    else Color.Gray.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isGranted) TealAccent else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                text = desc,
                color = Color.Gray,
                fontSize = 10.sp,
                lineHeight = 12.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (isGranted) TealAccent.copy(alpha = 0.15f)
                    else Color.Red.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = if (isGranted) "Granted" else "Pending",
                tint = if (isGranted) TealAccent else Color.Red,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
