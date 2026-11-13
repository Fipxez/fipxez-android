package com.fipxez.intel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private val Navy = Color(0xFF07111F)
private val Panel = Color(0xFF101F33)
private val Blue = Color(0xFF35B6FF)
private val Muted = Color(0xFF9CB0C8)

class MainActivity : ComponentActivity() {
    private var wifiPermissionGranted by mutableStateOf(false)
    private var wifiNetworks by mutableStateOf<List<String>>(emptyList())
    private var wifiMessage by mutableStateOf("Pemindaian hanya menampilkan jaringan yang terdeteksi oleh Android; tidak membuka password.")
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        wifiPermissionGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (wifiPermissionGranted) scanWifi()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Blue,
                    background = Navy,
                    surface = Panel,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                IntelApp(
                    onScanWifi = {
                        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (fine || coarse) scanWifi()
                        else permissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    },
                    wifiNetworks = wifiNetworks,
                    wifiMessage = wifiMessage
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun scanWifi() {
        try {
            val manager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            manager.startScan()
            wifiNetworks = manager.scanResults
                .sortedByDescending { it.level }
                .map { "${it.SSID.ifBlank { "(hidden network)" }} • ${it.capabilities} • ${it.level} dBm" }
                .distinct()
                .take(30)
            wifiMessage = if (wifiNetworks.isEmpty()) {
                "Belum ada hasil. Aktifkan Wi-Fi dan layanan lokasi, lalu coba lagi. Android dapat membatasi frekuensi pemindaian."
            } else {
                "Menampilkan hingga 30 jaringan yang terdeteksi. Password jaringan tidak dapat dibaca dari pemindaian."
            }
        } catch (_: SecurityException) {
            wifiMessage = "Izin lokasi diperlukan untuk membaca hasil pemindaian Wi-Fi di perangkat ini."
        } catch (_: Exception) {
            wifiMessage = "Pemindaian tidak tersedia saat ini. Pastikan Wi-Fi aktif."
        }
    }
}

@Composable
private fun IntelApp(
    onScanWifi: () -> Unit,
    wifiNetworks: List<String>,
    wifiMessage: String
) {
    var selected by remember { mutableStateOf("Overview") }
    var phone by remember { mutableStateOf("") }
    var phoneResult by remember { mutableStateOf("Masukkan nomor untuk memeriksa format dan negara. Aplikasi tidak mengungkap identitas privat dari nomor.") }
    val menu = listOf("Overview", "Phone OSINT", "Wi-Fi", "Location", "CCTV", "Evidence")

    Scaffold(
        containerColor = Navy,
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                val items = listOf("Overview", "Phone OSINT", "Wi-Fi", "Evidence")
                items.forEach { item ->
                    NavigationBarItem(
                        selected = selected == item,
                        onClick = { selected = item },
                        icon = {
                            Icon(
                                when (item) {
                                    "Overview" -> Icons.Default.Dashboard
                                    "Phone OSINT" -> Icons.Default.Search
                                    "Wi-Fi" -> Icons.Default.Wifi
                                    else -> Icons.Default.Folder
                                }, contentDescription = item
                            )
                        },
                        label = { Text(item, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 22.dp, bottom = 24.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("FIPXEZ", color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                        Text("INTEL", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                        Text("ANDROID INVESTIGATION TOOLKIT", color = Muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
                    }
                    Surface(color = Color(0xFF12334A), shape = RoundedCornerShape(50)) {
                        Text("MOBILE • 0.1", color = Blue, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 10.sp)
                    }
                }
            }
            item {
                Surface(color = Panel, shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("COMMAND OVERVIEW", color = Muted, fontSize = 10.sp, letterSpacing = 1.4.sp)
                        Text("Investigate smarter.", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Text("Satu aplikasi Android untuk analisis informasi publik, pemeriksaan jaringan, dan pengelolaan bukti.", color = Muted, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusPill("OSINT", "PUBLIC DATA")
                            StatusPill("NETWORK", "LOCAL SCAN")
                        }
                    }
                }
            }
            item { Text("INTELLIGENCE MODULES", color = Muted, fontSize = 11.sp, letterSpacing = 1.5.sp) }
            item {
                ModuleCard("Phone OSINT", "Analisis format nomor dan petunjuk dari sumber publik", Icons.Default.Phone, Blue) {
                    selected = "Phone OSINT"
                }
            }
            item {
                ModuleCard("Wi-Fi Intelligence", "Pindai SSID, jenis keamanan, dan kekuatan sinyal", Icons.Default.Wifi, Blue) {
                    selected = "Wi-Fi"
                }
            }
            item {
                ModuleCard("Location", "Tampilkan lokasi perangkat ini setelah izin diberikan", Icons.Default.MyLocation, Blue) {
                    selected = "Location"
                }
            }
            item {
                ModuleCard("CCTV Monitor", "Ruang integrasi kamera milik sendiri atau yang diotorisasi", Icons.Default.Videocam, Blue) {
                    selected = "CCTV"
                }
            }
            item {
                ModuleCard("Evidence Center", "Catatan kasus dan dokumentasi temuan", Icons.Default.Folder, Blue) {
                    selected = "Evidence"
                }
            }
            when (selected) {
                "Phone OSINT" -> {
                    item { SectionTitle("PHONE OSINT") }
                    item {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Nomor telepon") },
                            placeholder = { Text("+62 atau 08…") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Button(
                            onClick = {
                                val digits = phone.filter { it.isDigit() }
                                phoneResult = when {
                                    digits.length < 8 || digits.length > 15 -> "Format nomor belum valid. Periksa kembali digit yang dimasukkan."
                                    digits.startsWith("62") || digits.startsWith("0") -> "Format terlihat seperti nomor Indonesia. Operator dan identitas pemilik tidak dapat dipastikan hanya dari nomor."
                                    else -> "Nomor tersimpan untuk analisis format. Negara/operator belum dapat dipastikan."
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("ANALYZE NUMBER") }
                    }
                    item { InfoCard(phoneResult) }
                    item { InfoCard("Gunakan temuan publik sebagai petunjuk, bukan bukti identitas. Nama dan alamat pribadi tidak ditarik atau ditebak oleh aplikasi.") }
                }
                "Wi-Fi" -> {
                    item { SectionTitle("WI-FI INTELLIGENCE") }
                    item { InfoCard(wifiMessage) }
                    item {
                        Button(onClick = onScanWifi, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.WifiFind, null)
                            Spacer(Modifier.width(8.dp))
                            Text("SCAN NEARBY WI-FI")
                        }
                    }
                    if (wifiNetworks.isNotEmpty()) {
                        items(wifiNetworks) { network -> InfoCard(network) }
                    }
                }
                "Location" -> {
                    item { SectionTitle("DEVICE LOCATION") }
                    item { InfoCard("Modul ini disiapkan untuk menampilkan lokasi HP ini setelah izin lokasi diberikan. Tidak melacak nomor atau perangkat orang lain.") }
                }
                "CCTV" -> {
                    item { SectionTitle("CCTV MONITOR") }
                    item { InfoCard("Integrasi live view memerlukan alamat stream dan autentikasi kamera yang memang boleh diakses. Tidak ada pemindaian atau pengambilalihan kamera secara otomatis.") }
                }
                "Evidence" -> {
                    item { SectionTitle("EVIDENCE CENTER") }
                    item { InfoCard("Ruang dokumentasi kasus. Versi awal menampilkan fondasi antarmuka; penyimpanan terenkripsi dan ekspor laporan akan ditambahkan setelah pengujian build.") }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(title: String, subtitle: String) {
    Surface(color = Color(0xFF0A2A40), shape = RoundedCornerShape(9.dp)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text(title, color = Blue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Muted, fontSize = 8.sp)
        }
    }
}

@Composable
private fun ModuleCard(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color(0xFF0A2A40), shape = RoundedCornerShape(12.dp)) {
                Icon(icon, null, tint = tint, modifier = Modifier.padding(12.dp).size(24.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(detail, color = Muted, fontSize = 11.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }
    }
}

@Composable
private fun SectionTitle(value: String) {
    Text(value, color = Blue, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.4.sp)
}

@Composable
private fun InfoCard(value: String) {
    Surface(color = Panel, shape = RoundedCornerShape(14.dp)) {
        Text(value, color = Muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(14.dp))
    }
}
