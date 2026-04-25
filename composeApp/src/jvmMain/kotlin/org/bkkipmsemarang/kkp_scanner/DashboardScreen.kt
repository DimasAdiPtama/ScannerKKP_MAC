package org.bkkipmsemarang.kkp_scanner

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import scannerkkp.composeapp.generated.resources.Res
import scannerkkp.composeapp.generated.resources.logo_bkipm

@Composable
fun DashboardScreen(onScanClick: () -> Unit, onLogout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FA))
    ) {
        // Sidebar
        Sidebar(onLogout = onLogout)

        // Main Content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(32.dp)
        ) {
            Text(
                text = "Scan QR Code Kamu!",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Responsive Layout using BoxWithConstraints
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                // If width is less than 800dp, stack vertically. Otherwise, side-by-side.
                if (maxWidth < 800.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        ActiveQueuesCard(modifier = Modifier.fillMaxWidth())
                        
                        ScanButtonCard(
                            modifier = Modifier
                                .size(200.dp)
                                .align(Alignment.CenterHorizontally),
                            onClick = onScanClick
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Queue card takes remaining width
                        ActiveQueuesCard(modifier = Modifier.weight(1f))

                        // Scan button keeps fixed size
                        ScanButtonCard(
                            modifier = Modifier.size(200.dp),
                            onClick = onScanClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveQueuesCard(modifier: Modifier = Modifier) {
    var smkhpCount by remember { mutableStateOf(0) }
    var labCount by remember { mutableStateOf(0) }
    var csCount by remember { mutableStateOf(0) }

    // Real-time listener using addSnapshotListener
    DisposableEffect(Unit) {
        val db = FirestoreManager.db
        
        // Listener for SMKHP (root status = "active")
        val smkhpRegistration = db.collection("SMKHP")
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    e.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    smkhpCount = snapshots.size()
                }
            }

        // Listener for Laboratorium (LAB) (root status = "active")
        val labRegistration = db.collection("LAB")
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    e.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    labCount = snapshots.size()
                }
            }

        // Listener for CustomerService (root status = "active")
        val csRegistration = db.collection("CustomerService")
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    e.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    csCount = snapshots.size()
                }
            }

        // Cleanup listeners when component is disposed
        onDispose {
            smkhpRegistration.remove()
            labRegistration.remove()
            csRegistration.remove()
        }
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Antrian Aktif",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            QueueItem(name = "SMKHP", count = smkhpCount)
            Spacer(modifier = Modifier.height(12.dp))
            QueueItem(name = "Laboratorium", count = labCount)
            Spacer(modifier = Modifier.height(12.dp))
            QueueItem(name = "Customer Service", count = csCount)
        }
    }
}

@Composable
fun ScanButtonCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Scan QR Code",
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF0056FF)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Scan QR Code",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.Black
            )
        }
    }
}

@Composable
fun Sidebar(onLogout: () -> Unit) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Apakah Anda yakin ingin keluar dari aplikasi?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // Logo Section
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(Res.drawable.logo_bkipm),
                contentDescription = "Logo",
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "KEMENTERIAN\nKELAUTAN DAN PERIKANAN\nREPUBLIK INDONESIA",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                lineHeight = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Profile Section (Modified: Removed "AD" circle, kept Settings icon)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            // "AD" Circle removed
            
            // Settings Icon
            // Push to end or keep at start? 
            // If we remove the spacer with weight(1f), it stays at start. 
            // Let's keep it at start as a simple menu item, or push it to end if it acts like a header control.
            // Given the previous layout, putting it on the right was driven by the profile on the left.
            // Now, let's just place it on the right to keep consistency with "corner" placement, 
            // or maybe the user wants a clean look. I'll align it to the end (right) to separate it from the logo area.
            Spacer(modifier = Modifier.weight(1f)) 
            IconButton(onClick = { showLogoutDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color(0xFF1565C0)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Navigation
        NavigationItem(
            icon = Icons.Filled.Home,
            label = "Dashboard",
            isSelected = true
        )
        // Riwayat Navigation removed
    }
}

@Composable
fun NavigationItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean
) {
    val backgroundColor = if (isSelected) Color(0xFFF5F5F5) else Color.Transparent
    val contentColor = if (isSelected) Color.Black else Color.Gray

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { }
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = contentColor
        )
    }
}

@Composable
fun QueueItem(name: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(24.dp)) // Pill shape border
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color(0xFF616161), CircleShape), // Dark grey circle
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
            )
        }
    }
}
