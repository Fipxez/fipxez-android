package com.fipxez.intel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF07111F)
private val Panel = Color(0xFF101F33)
private val Blue = Color(0xFF35B6FF)
private val Muted = Color(0xFF9CB0C8)

class MainActivity : ComponentActivity() {
    private var wifiNetworks by mutableStateOf<List<String>>(emptyList())
    private var wifiMessage by mutableStateOf("Tekan scan untuk membaca jaringan yang terlihat oleh perangkat.")
    private var deviceLocation by mutableStateOf("Lokasi belum diminta.")
    private var evidence by mutableStateOf<List<String>>(emptyList())

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) readDeviceLocation()
        else deviceLocation = "Izin lokasi ditolak. Aktifkan izin saat menggunakan aplikasi untuk fitur GPS dan Wi-Fi."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        evidence = getSharedPreferences("fipxez_evidence", MODE_PRIVATE)
            .getStringSet("items", emptySet())?.toList()?.sortedDescending() ?: emptyList()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Blue, background = Navy, surface = Panel,
                onBackground = Color.White, onSurface = Color.White
            )) {
                IntelApp(
                    wifiNetworks = wifiNetworks,
                    wifiMessage = wifiMessage,
                    locationText = deviceLocation,
                    evidence = evidence,
                    onScanWifi = ::scanWifi,
                    onGetLocation = ::requestDeviceLocation,
                    onOpenMap = ::openMap,
                    onAddEvidence = ::addEvidence,
                    onDeleteEvidence = ::deleteEvidence,
                    onExportEvidence = ::exportEvidence,
                    onOpenCctv = ::openCctv,
                    onPublicSearch = ::openPublicSearch
                )
            }
        }
    }

    private fun requestDeviceLocation() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) readDeviceLocation()
        else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    @Suppress("MissingPermission")
    private fun readDeviceLocation() {
        try {
            val manager = getSystemService(LOCATION_SERVICE) as LocationManager
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            val loc: Location? = providers.filter { manager.isProviderEnabled(it) }
                .mapNotNull { provider -> manager.getLastKnownLocation(provider) }
                .maxByOrNull { it.time }
            deviceLocation = if (loc == null) {
                "Belum ada lokasi terakhir. Aktifkan GPS, tunggu sebentar di area terbuka, lalu tekan Perbarui lokasi."
            } else {
                "Latitude: %.6f\nLongitude: %.6f\nAkurasi: %.0f meter\nWaktu: %s".format(
                    Locale.US, loc.latitude, loc.longitude, loc.accuracy,
                    SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale("id", "ID")).format(Date(loc.time))
                )
            }
        } catch (_: Exception) {
            deviceLocation = "Lokasi tidak tersedia. Periksa GPS dan izin lokasi perangkat."
        }
    }

    @Suppress("DEPRECATION")
    private fun scanWifi() {
        try {
            val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!fine && !coarse) {
                wifiMessage = "Izin lokasi diperlukan untuk membaca hasil scan Wi-Fi."
                requestDeviceLocation()
                return
            }
            val manager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (!manager.isWifiEnabled) {
                wifiMessage = "Wi-Fi belum aktif. Aktifkan Wi-Fi dari Quick Settings lalu scan lagi."
                return
            }
            manager.startScan()
            wifiNetworks = manager.scanResults.sortedByDescending { it.level }.map {
                val name = it.SSID.takeIf(String::isNotBlank) ?: "(hidden network)"
                "$name • ${it.capabilities} • ${it.level} dBm"
            }.distinct().take(30)
            wifiMessage = if (wifiNetworks.isEmpty()) "Hasil kosong atau scan dibatasi Android. Aktifkan layanan lokasi perangkat dan coba lagi beberapa saat." else "Ditemukan ${wifiNetworks.size} jaringan. Data berasal dari hasil scan Android; password tidak dibaca."
        } catch (_: SecurityException) {
            wifiMessage = "Android menolak izin scan. Periksa izin Lokasi aplikasi."
        } catch (_: Exception) {
            wifiMessage = "Scan gagal. Pastikan Wi-Fi aktif."
        }
    }

    private fun openMap() {
        val text = deviceLocation
        val lat = Regex("Latitude: ([0-9.\\-]+)").find(text)?.groupValues?.get(1)
        val lon = Regex("Longitude: ([0-9.\\-]+)").find(text)?.groupValues?.get(1)
        if (lat != null && lon != null) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon(Me)")))
        } else {
            deviceLocation = "Ambil lokasi perangkat terlebih dahulu sebelum membuka peta."
        }
    }

    private fun addEvidence(note: String) {
        val clean = note.trim()
        if (clean.isEmpty()) return
        val entry = "${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())} • $clean"
        evidence = (listOf(entry) + evidence).take(200)
        getSharedPreferences("fipxez_evidence", MODE_PRIVATE).edit().putStringSet("items", evidence.toSet()).apply()
    }

    private fun deleteEvidence(entry: String) {
        evidence = evidence.filterNot { it == entry }
        getSharedPreferences("fipxez_evidence", MODE_PRIVATE).edit().putStringSet("items", evidence.toSet()).apply()
    }

    private fun exportEvidence() {
        val body = evidence.joinToString("\n")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "FIPXEZ INTEL - Evidence Notes")
            putExtra(Intent.EXTRA_TEXT, body.ifBlank { "Belum ada catatan." })
        }
        startActivity(Intent.createChooser(intent, "Ekspor catatan evidence"))
    }

    private fun openCctv(address: String) {
        try {
            val uri = Uri.parse(address.trim())
            if (uri.scheme !in listOf("rtsp", "http", "https")) {
                return
            }
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) { }
    }

    private fun openPublicSearch(number: String) {
        val query = Uri.encode("\"$number\" laporan penipuan OR spam")
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query")))
    }
}

@Composable
private fun IntelApp(
    wifiNetworks: List<String>, wifiMessage: String, locationText: String, evidence: List<String>,
    onScanWifi: () -> Unit, onGetLocation: () -> Unit, onOpenMap: () -> Unit,
    onAddEvidence: (String) -> Unit, onDeleteEvidence: (String) -> Unit, onExportEvidence: () -> Unit,
    onOpenCctv: (String) -> Unit, onPublicSearch: (String) -> Unit
) {
    var selected by remember { mutableStateOf("Overview") }
    var phone by remember { mutableStateOf("") }
    var phoneResult by remember { mutableStateOf("Analisis lokal hanya memeriksa format dan petunjuk prefix; identitas pemilik tidak tersedia.") }
    var cctvUrl by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Scaffold(containerColor = Navy, bottomBar = {
        NavigationBar(containerColor = Panel) {
            listOf("Overview", "Phone OSINT", "Wi-Fi", "Location", "CCTV", "Evidence").forEach { item ->
                NavigationBarItem(selected = selected == item, onClick = { selected = item },
                    icon = { Icon(when(item) {
                        "Overview" -> Icons.Default.Dashboard
                        "Phone OSINT" -> Icons.Default.Search
                        "Wi-Fi" -> Icons.Default.Wifi
                        "Location" -> Icons.Default.MyLocation
                        "CCTV" -> Icons.Default.Videocam
                        else -> Icons.Default.Folder
                    }, contentDescription = item) }, label = { Text(item, fontSize = 9.sp) })
            }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp)) {
            item {
                Column {
                    Text("FIPXEZ", color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Text("INTEL", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                    Text("ANDROID INVESTIGATION TOOLKIT", color = Muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
                }
            }
            if (selected == "Overview") {
                item { InfoCard("MOBILE INTELLIGENCE • Modul aktif: Wi-Fi scan, GPS perangkat, catatan evidence, pencarian laporan publik, dan pembuka stream kamera berizin.") }
                item { ModuleCard("Phone OSINT", "Periksa format nomor dan cari laporan publik", Icons.Default.Phone) { selected = "Phone OSINT" } }
                item { ModuleCard("Wi-Fi Intelligence", "SSID, keamanan, kekuatan sinyal", Icons.Default.Wifi) { selected = "Wi-Fi" } }
                item { ModuleCard("Location", "GPS perangkat ini dan buka peta", Icons.Default.MyLocation) { selected = "Location" } }
                item { ModuleCard("CCTV Monitor", "Buka RTSP/HTTP stream dengan aplikasi pemutar yang kompatibel", Icons.Default.Videocam) { selected = "CCTV" } }
                item { ModuleCard("Evidence Center", "Simpan, hapus, dan ekspor catatan lokal", Icons.Default.Folder) { selected = "Evidence" } }
            }
            when (selected) {
                "Phone OSINT" -> {
                    item { SectionTitle("PHONE OSINT") }
                    item { OutlinedTextField(phone, { phone = it }, label = { Text("Nomor telepon") }, placeholder = { Text("+62 atau 08…") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth()) }
                    item { Button(onClick = {
                        val digits = phone.filter(Char::isDigit)
                        val prefix = when {
                            digits.startsWith("62811") || digits.startsWith("62812") || digits.startsWith("62813") || digits.startsWith("0811") || digits.startsWith("0812") || digits.startsWith("0813") -> "Prefix umum Telkomsel (indikatif; nomor bisa berpindah operator)."
                            digits.startsWith("62817") || digits.startsWith("62818") || digits.startsWith("62819") || digits.startsWith("0817") || digits.startsWith("0818") || digits.startsWith("0819") -> "Prefix umum XL (indikatif; nomor bisa berpindah operator)."
                            digits.startsWith("62855") || digits.startsWith("62856") || digits.startsWith("62857") || digits.startsWith("0855") || digits.startsWith("0856") || digits.startsWith("0857") -> "Prefix umum Indosat (indikatif; nomor bisa berpindah operator)."
                            else -> "Operator tidak dapat dipastikan dari prefix yang dikenali aplikasi."
                        }
                        phoneResult = if (digits.length in 9..15) "Format digit terlihat masuk akal. $prefix\nIni bukan verifikasi registrasi atau identitas pelanggan." else "Format nomor belum valid. Periksa kembali jumlah digit."
                    }, modifier = Modifier.fillMaxWidth()) { Text("ANALYZE NUMBER") } }
                    item { InfoCard(phoneResult) }
                    item { Button(onClick = { if (phone.isNotBlank()) onPublicSearch(phone) }, modifier = Modifier.fillMaxWidth()) { Text("CARI LAPORAN PUBLIK DI WEB") } }
                    item { InfoCard("Pencarian web dibuka hanya setelah tombol ditekan. Jangan masukkan nomor pribadi orang lain tanpa alasan yang sah.") }
                }
                "Wi-Fi" -> {
                    item { SectionTitle("WI-FI INTELLIGENCE") }
                    item { InfoCard(wifiMessage) }
                    item { Button(onClick = onScanWifi, modifier = Modifier.fillMaxWidth()) { Text("SCAN NEARBY WI-FI") } }
                    items(wifiNetworks) { InfoCard(it) }
                }
                "Location" -> {
                    item { SectionTitle("DEVICE LOCATION") }
                    item { InfoCard(locationText) }
                    item { Button(onClick = onGetLocation, modifier = Modifier.fillMaxWidth()) { Text("PERBARUI LOKASI PERANGKAT") } }
                    item { OutlinedButton(onClick = onOpenMap, modifier = Modifier.fillMaxWidth()) { Text("BUKA DI PETA") } }
                    item { InfoCard("Lokasi hanya berasal dari sensor/lokasi HP yang memberi izin. Ini bukan pelacak nomor atau perangkat pihak lain.") }
                }
                "CCTV" -> {
                    item { SectionTitle("AUTHORIZED CCTV STREAM") }
                    item { OutlinedTextField(cctvUrl, { cctvUrl = it }, label = { Text("RTSP / HTTP stream URL") }, placeholder = { Text("rtsp://kamera-anda/stream") }, modifier = Modifier.fillMaxWidth()) }
                    item { Button(onClick = { onOpenCctv(cctvUrl) }, modifier = Modifier.fillMaxWidth()) { Text("OPEN STREAM") } }
                    item { InfoCard("Masukkan alamat stream dan kredensial kamera milik sendiri/yang diizinkan. Android akan meneruskan URL ke aplikasi pemutar yang mendukung RTSP. Jika tidak ada pemutar kompatibel, stream tidak akan terbuka.") }
                }
                "Evidence" -> {
                    item { SectionTitle("EVIDENCE CENTER") }
                    item { OutlinedTextField(note, { note = it }, label = { Text("Catatan temuan / nomor kasus") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
                    item { Button(onClick = { onAddEvidence(note); note = "" }, modifier = Modifier.fillMaxWidth()) { Text("SIMPAN CATATAN") } }
                    item { OutlinedButton(onClick = onExportEvidence, modifier = Modifier.fillMaxWidth()) { Text("EKSPOR / BAGIKAN CATATAN") } }
                    if (evidence.isEmpty()) item { InfoCard("Belum ada catatan tersimpan di perangkat ini.") }
                    items(evidence) { entry ->
                        Surface(color = Panel, shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(entry, color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onDeleteEvidence(entry) }) { Icon(Icons.Default.Delete, "Hapus", tint = Color(0xFFFF7B7B)) }
                            }
                        }
                    }
                    item { InfoCard("Catatan disimpan lokal di aplikasi. Ekspor menggunakan menu berbagi Android; data tidak otomatis diunggah ke server.") }
                }
            }
        }
    }
}

@Composable private fun ModuleCard(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Blue, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(detail, color = Muted, fontSize = 11.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }
    }
}
@Composable private fun SectionTitle(value: String) { Text(value, color = Blue, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.4.sp) }
@Composable private fun InfoCard(value: String) { Surface(color = Panel, shape = RoundedCornerShape(14.dp)) { Text(value, color = Muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(14.dp)) } }
