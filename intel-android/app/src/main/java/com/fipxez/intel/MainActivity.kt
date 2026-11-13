package com.fipxez.intel

import android.Manifest
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.ui.viewinterop.AndroidView
import android.view.ViewGroup
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
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
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import kotlin.concurrent.thread

private val Navy = Color(0xFF05040B)
private val Panel = Color(0xFF111020)
private val Blue = Color(0xFF28BFFF)
private val Purple = Color(0xFF8B1E3F)
private val PurpleSoft = Color(0xFFE0A0AE)
private val Muted = Color(0xFFAAA4C2)

class MainActivity : ComponentActivity() {
    private var wifiNetworks by mutableStateOf<List<String>>(emptyList())
    private var wifiMessage by mutableStateOf("Tekan scan untuk membaca jaringan yang terlihat oleh perangkat.")
    private var deviceLocation by mutableStateOf("Lokasi belum diminta.")
    private var evidence by mutableStateOf<List<String>>(emptyList())
    private var cctvResults by mutableStateOf<List<String>>(emptyList())
    private var publicWifiResults by mutableStateOf<List<String>>(emptyList())
    private var publicWifiMessage by mutableStateOf("Cari hotspot Wi-Fi publik yang dipetakan di sekitar kota/alamat.")
    private var cctvSearchMessage by mutableStateOf("Cari kota atau alamat untuk menemukan titik kamera yang dipetakan secara publik.")

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
                primary = Purple, secondary = Blue, background = Navy, surface = Panel,
                onBackground = Color.White, onSurface = Color.White
            )) {
                IntelApp(
                    wifiNetworks = wifiNetworks,
                    wifiMessage = wifiMessage,
                    locationText = deviceLocation,
                    evidence = evidence,
                    onScanWifi = ::scanWifi,
                    publicWifiResults = publicWifiResults,
                    publicWifiMessage = publicWifiMessage,
                    onSearchPublicWifi = ::searchPublicWifi,
                    onCopyText = ::copyText,
                    onGetLocation = ::requestDeviceLocation,
                    onOpenMap = ::openMap,
                    onAddEvidence = ::addEvidence,
                    onDeleteEvidence = ::deleteEvidence,
                    onExportEvidence = ::exportEvidence,
                    onOpenCctv = ::openCctv,
                    onPublicSearch = ::openPublicSearch,
                    onCallerIdSearch = ::openCallerIdSearch,
                    onTelegramSearch = ::openTelegramPublicSearch,
                    cctvResults = cctvResults,
                    cctvSearchMessage = cctvSearchMessage,
                    onSearchPublicCctv = ::searchPublicCctv
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
                "$name • ${it.capabilities} • ${it.level} dBm • ${it.frequency} MHz"
            }.distinct()
            wifiMessage = if (wifiNetworks.isEmpty()) "Hasil kosong atau scan dibatasi Android. Aktifkan layanan lokasi perangkat dan coba lagi beberapa saat." else "Ditemukan ${wifiNetworks.size} jaringan. Data berasal dari hasil scan Android; password tidak dibaca."
        } catch (_: SecurityException) {
            wifiMessage = "Android menolak izin scan. Periksa izin Lokasi aplikasi."
        } catch (_: Exception) {
            wifiMessage = "Scan gagal. Pastikan Wi-Fi aktif."
        }
    }

    private fun copyText(value: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("FIPXEZ INTEL", value))
        wifiMessage = "Teks berhasil disalin."
    }

    private fun searchPublicWifi(place: String) {
        if (place.isBlank()) { publicWifiMessage = "Masukkan kota atau alamat."; return }
        publicWifiMessage = "Mencari hotspot Wi-Fi publik yang dipetakan..."
        publicWifiResults = emptyList()
        thread {
            try {
                val encoded = URLEncoder.encode(place.trim(), "UTF-8")
                val geoConn = URL("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=$encoded").openConnection() as HttpURLConnection
                geoConn.connectTimeout = 12000; geoConn.readTimeout = 15000
                geoConn.setRequestProperty("User-Agent", "FIPXEZ-INTEL-Android/0.3 public map lookup")
                val geo = geoConn.inputStream.bufferedReader().use { it.readText() }; geoConn.disconnect()
                val arr = org.json.JSONArray(geo)
                if (arr.length() == 0) throw IllegalStateException("Alamat tidak ditemukan.")
                val loc = arr.getJSONObject(0); val lat = loc.getDouble("lat"); val lon = loc.getDouble("lon")
                val query = """[out:json][timeout:25];(node["internet_access"~"wlan|public"](around:5000,$lat,$lon);way["internet_access"~"wlan|public"](around:5000,$lat,$lon);node["internet_access"="yes"](around:5000,$lat,$lon););out center tags;"""
                val conn = URL("https://overpass-api.de/api/interpreter").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"; conn.doOutput = true; conn.connectTimeout = 15000; conn.readTimeout = 30000
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                conn.setRequestProperty("User-Agent", "FIPXEZ-INTEL-Android/0.3")
                conn.outputStream.use { it.write(("data=" + URLEncoder.encode(query, "UTF-8")).toByteArray()) }
                val data = conn.inputStream.bufferedReader().use { it.readText() }; conn.disconnect()
                val elements = JSONObject(data).optJSONArray("elements") ?: org.json.JSONArray()
                val results = mutableListOf<Pair<Float,String>>()
                for (i in 0 until elements.length()) {
                    val e = elements.getJSONObject(i); val tags = e.optJSONObject("tags") ?: JSONObject()
                    val center = e.optJSONObject("center")
                    val y = if (e.has("lat")) e.optDouble("lat") else center?.optDouble("lat") ?: lat
                    val x = if (e.has("lon")) e.optDouble("lon") else center?.optDouble("lon") ?: lon
                    val dist = FloatArray(1); Location.distanceBetween(lat,lon,y,x,dist)
                    val title = tags.optString("name").ifBlank { tags.optString("operator").ifBlank { "Hotspot publik tanpa nama" } }
                    val access = tags.optString("internet_access").ifBlank { "akses tidak dirinci" }
                    val map = "https://www.openstreetmap.org/?mlat=$y&mlon=$x#map=19/$y/$x"
                    results.add(dist[0] to "$title • %.0f m • $access\nPeta: $map".format(Locale.US,dist[0]))
                }
                val sorted = results.sortedBy { it.first }.map { it.second }
                runOnUiThread {
                    publicWifiResults = sorted
                    publicWifiMessage = if (sorted.isEmpty()) "Tidak ada hotspot yang terpetakan dalam radius 5 km. Ini bukan hasil scan radio; cakupan tergantung kontribusi peta publik."
                    else "Ditemukan ${sorted.size} lokasi hotspot yang ditandai di OpenStreetMap dalam radius 5 km. Ini bukan daftar semua jaringan Wi-Fi."
                }
            } catch (e: Exception) { runOnUiThread { publicWifiMessage = "Pencarian hotspot gagal: ${e.message ?: "coba lagi nanti"}" } }
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
        val clean = number.trim()
        val query = Uri.encode("\"$clean\" (nama OR tag OR bisnis OR layanan OR spam OR penipuan OR kontak)")
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query")))
    }

    private fun openCallerIdSearch(number: String) {
        val clean = number.filter { it.isDigit() || it == '+' }
        val query = Uri.encode("\"$clean\" (site:getcontact.com OR site:truecaller.com OR site:tellows.co.id OR site:kredibel.com)")
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query")))
    }

    private fun openTelegramPublicSearch(identifier: String) {
        val clean = identifier.trim().removePrefix("@")
        if (clean.isBlank()) return
        val query = Uri.encode("\"$clean\" (site:t.me OR site:telegram.me OR Telegram)")
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query")))
    }

    private fun searchPublicCctv(place: String) {
        if (place.isBlank()) { cctvSearchMessage = "Masukkan nama kota, jalan, atau alamat."; return }
        cctvSearchMessage = "Mencari lokasi dan titik kamera yang dipetakan secara publik..."
        cctvResults = emptyList()
        thread {
            try {
                val encoded = URLEncoder.encode(place.trim(), "UTF-8")
                val geo = (URL("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=$encoded").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12000; readTimeout = 15000
                    setRequestProperty("User-Agent", "FIPXEZ-INTEL-Android/0.2 public map lookup")
                }
                val geoText = geo.inputStream.bufferedReader().use { it.readText() }; geo.disconnect()
                val geoArray = org.json.JSONArray(geoText)
                if (geoArray.length() == 0) throw IllegalStateException("Alamat tidak ditemukan. Coba nama jalan atau kota yang lebih lengkap.")
                val first = geoArray.getJSONObject(0)
                val lat = first.getDouble("lat"); val lon = first.getDouble("lon")
                val display = first.optString("display_name", place)
                val q = """[out:json][timeout:25];(node["man_made"="surveillance"](around:5000,$lat,$lon);way["man_made"="surveillance"](around:5000,$lat,$lon);relation["man_made"="surveillance"](around:5000,$lat,$lon);node["surveillance"](around:5000,$lat,$lon);way["surveillance"](around:5000,$lat,$lon););out center tags;"""
                val conn = URL("https://overpass-api.de/api/interpreter").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"; conn.doOutput = true; conn.connectTimeout = 15000; conn.readTimeout = 30000
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                conn.setRequestProperty("User-Agent", "FIPXEZ-INTEL-Android/0.2")
                conn.outputStream.use { it.write(("data=" + URLEncoder.encode(q, "UTF-8")).toByteArray()) }
                val response = conn.inputStream.bufferedReader().use { it.readText() }; conn.disconnect()
                val elements = JSONObject(response).optJSONArray("elements") ?: org.json.JSONArray()
                val found = mutableListOf<Pair<Float,String>>()
                for (i in 0 until elements.length()) {
                    val item = elements.getJSONObject(i); val tags = item.optJSONObject("tags") ?: JSONObject()
                    val center = item.optJSONObject("center")
                    val itemLat = if (item.has("lat")) item.optDouble("lat") else center?.optDouble("lat") ?: lat
                    val itemLon = if (item.has("lon")) item.optDouble("lon") else center?.optDouble("lon") ?: lon
                    val distance = FloatArray(1); Location.distanceBetween(lat,lon,itemLat,itemLon,distance)
                    val name = tags.optString("name").ifBlank { tags.optString("operator").ifBlank { "Kamera/pengawasan tanpa nama" } }
                    val kind = tags.optString("surveillance:type").ifBlank { tags.optString("camera:type").ifBlank { "jenis tidak dicantumkan" } }
                    val map = "https://www.openstreetmap.org/?mlat=$itemLat&mlon=$itemLon#map=19/$itemLat/$itemLon"
                    val stream = listOf("camera:stream", "surveillance:stream", "stream_url").firstNotNullOfOrNull { key -> tags.optString(key).takeIf { it.startsWith("http://") || it.startsWith("https://") || it.startsWith("rtsp://") } }
                    val streamLine = stream?.let { "\nPublic stream: $it" } ?: ""
                    found.add(distance[0] to "$name • %.0f m • $kind\nPeta: $map$streamLine".format(Locale.US,distance[0]))
                }
                val sorted = found.sortedBy { it.first }.map { it.second }
                runOnUiThread {
                    cctvResults = sorted
                    cctvSearchMessage = if (sorted.isEmpty()) "Lokasi ditemukan: $display. Belum ada titik kamera yang dipetakan dalam radius 3 km. Ini bukan bukti tidak ada CCTV; cakupan peta bergantung data publik."
                    else "Lokasi: $display\nDitemukan ${sorted.size} titik kamera/pengawasan dalam radius 3 km, diurutkan berdasarkan jarak. Titik peta tidak menjamin tersedia live stream publik."
                }
            } catch (e: Exception) {
                runOnUiThread { cctvSearchMessage = "Pencarian gagal: ${e.message ?: "periksa koneksi internet lalu coba lagi."}" }
            }
        }
    }
}

@Composable
private fun IntelApp(
    wifiNetworks: List<String>, wifiMessage: String, locationText: String, evidence: List<String>,
    onScanWifi: () -> Unit, publicWifiResults: List<String>, publicWifiMessage: String, onSearchPublicWifi: (String) -> Unit, onCopyText: (String) -> Unit,
    onGetLocation: () -> Unit, onOpenMap: () -> Unit,
    onAddEvidence: (String) -> Unit, onDeleteEvidence: (String) -> Unit, onExportEvidence: () -> Unit,
    onOpenCctv: (String) -> Unit, onPublicSearch: (String) -> Unit, onCallerIdSearch: (String) -> Unit,
    onTelegramSearch: (String) -> Unit,
    cctvResults: List<String>, cctvSearchMessage: String, onSearchPublicCctv: (String) -> Unit
) {
    var selected by remember { mutableStateOf("Overview") }
    var phone by remember { mutableStateOf("") }
    var telegramId by remember { mutableStateOf("") }
    var phoneResult by remember { mutableStateOf("Analisis lokal hanya memeriksa format dan petunjuk prefix; identitas pemilik tidak tersedia.") }
    var cctvUrl by remember { mutableStateOf("") }
    var wifiPlace by remember { mutableStateOf("") }
    var placeQuery by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Scaffold(containerColor = Navy, bottomBar = {
        NavigationBar(containerColor = Color(0xFF0D0B17), contentColor = PurpleSoft) {
            listOf("Overview", "Phone OSINT", "Wi-Fi", "Location", "Place CCTV", "CCTV", "Evidence").forEach { item ->
                NavigationBarItem(selected = selected == item, onClick = { selected = item },
                    icon = { Icon(when(item) {
                        "Overview" -> Icons.Default.Dashboard
                        "Phone OSINT" -> Icons.Default.Search
                        "Wi-Fi" -> Icons.Default.Wifi
                        "Location" -> Icons.Default.MyLocation
                        "Place CCTV" -> Icons.Default.Map
                        "CCTV" -> Icons.Default.Videocam
                        "CCTV PLAYER" -> Icons.Default.PlayArrow
                        else -> Icons.Default.Folder
                    }, contentDescription = item) }, label = { Text(item, fontSize = 9.sp) })
            }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp)) {
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFF35101D), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.Radar, null, tint = PurpleSoft, modifier = Modifier.padding(10.dp).size(28.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("FIPXEZ INTEL", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                            Text("MOBILE INTELLIGENCE TOOLKIT", color = PurpleSoft, fontSize = 9.sp, letterSpacing = 1.5.sp)
                        }
                    }
                }
            }
            if (selected == "Overview") {
                item { InfoCard("MOBILE INTELLIGENCE • Modul aktif: Wi-Fi scan, GPS perangkat, catatan evidence, pencarian laporan publik, dan pembuka stream kamera berizin.") }
                item { ModuleCard("Phone OSINT", "Periksa format nomor dan cari laporan publik", Icons.Default.Phone) { selected = "Phone OSINT" } }
                item { ModuleCard("Wi-Fi Intelligence", "SSID, keamanan, kekuatan sinyal", Icons.Default.Wifi) { selected = "Wi-Fi" } }
                item { ModuleCard("Location", "GPS perangkat ini dan buka peta", Icons.Default.MyLocation) { selected = "Location" } }
                item { ModuleCard("Place & CCTV Finder", "Cari kamera yang dipetakan di sekitar alamat tujuan", Icons.Default.Map) { selected = "Place CCTV" } }
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
                    item { Button(onClick = { if (phone.isNotBlank()) onPublicSearch(phone) }, modifier = Modifier.fillMaxWidth()) { Text("CARI NAMA & JEJAK NOMOR DI WEB") } }
                    item { OutlinedButton(onClick = { if (phone.isNotBlank()) onCallerIdSearch(phone) }, modifier = Modifier.fillMaxWidth()) { Text("CEK DIREKTORI CALLER ID / TAG") } }
                    item { InfoCard("Pencarian dilakukan dari HP lu ke sumber web publik; orang lain tidak perlu memasang FIPXEZ. Hasil nama/tag hanya muncul bila dipublikasikan atau diindeks sumber tersebut. FIPXEZ tidak mengambil database privat Getcontact/Truecaller dan hasil perlu diverifikasi.") }
                    item { SectionTitle("TELEGRAM PUBLIC FOOTPRINT") }
                    item { OutlinedTextField(telegramId, { telegramId = it }, label = { Text("Username / ID Telegram") }, placeholder = { Text("@username") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedButton(onClick = { if (telegramId.isNotBlank()) onTelegramSearch(telegramId) }, modifier = Modifier.fillMaxWidth()) { Text("CARI JEJAK PUBLIK TELEGRAM") } }
                    item { InfoCard("Mencari username atau ID yang memang muncul di halaman/profil publik dan hasil indeks web. Tidak membuka akun privat, pesan, nomor tersembunyi, atau alamat pribadi.") }
                }
                "Wi-Fi" -> {
                    item { SectionTitle("WI-FI INTELLIGENCE") }
                    item { InfoCard(wifiMessage) }
                    item { Button(onClick = onScanWifi, modifier = Modifier.fillMaxWidth()) { Text("SCAN NEARBY WI-FI") } }
                    items(wifiNetworks) { InfoCard(it) }
                    item { Divider(color = Muted) }
                    item { SectionTitle("PUBLIC HOTSPOT MAP") }
                    item { OutlinedTextField(wifiPlace, { wifiPlace = it }, label = { Text("Kota / alamat tujuan") }, placeholder = { Text("Contoh: Bandung, Indonesia") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedButton(onClick = { onSearchPublicWifi(wifiPlace) }, modifier = Modifier.fillMaxWidth()) { Text("CARI HOTSPOT PUBLIK DI LOKASI") } }
                    item { InfoCard(publicWifiMessage) }
                    items(publicWifiResults) { result ->
                        InfoCard(result)
                        val link = Regex("Peta: (https?://\\S+)").find(result)?.groupValues?.get(1)
                        if (link != null) TextButton(onClick = { onCopyText(link) }) { Text("SALIN TAUTAN PETA") }
                    }
                }
                "Location" -> {
                    item { SectionTitle("DEVICE LOCATION") }
                    item { InfoCard(locationText) }
                    item { Button(onClick = onGetLocation, modifier = Modifier.fillMaxWidth()) { Text("PERBARUI LOKASI PERANGKAT") } }
                    item { OutlinedButton(onClick = onOpenMap, modifier = Modifier.fillMaxWidth()) { Text("BUKA DI PETA") } }
                    item { InfoCard("Lokasi hanya berasal dari sensor/lokasi HP yang memberi izin. Ini bukan pelacak nomor atau perangkat pihak lain.") }
                }
                "Place CCTV" -> {
                    item { SectionTitle("LOCATION & PUBLIC CCTV FINDER") }
                    item { OutlinedTextField(placeQuery, { placeQuery = it }, label = { Text("Kota / jalan / alamat tujuan") }, placeholder = { Text("Contoh: Pasirlayung, Bandung") }, modifier = Modifier.fillMaxWidth()) }
                    item { Button(onClick = { onSearchPublicCctv(placeQuery) }, modifier = Modifier.fillMaxWidth()) { Text("CARI CCTV TERDEKAT") } }
                    item { InfoCard(cctvSearchMessage) }
                    items(cctvResults) { result ->
                        InfoCard(result)
                        val mapLink = Regex("Peta: (https?://\\S+)").find(result)?.groupValues?.get(1)
                        val streamLink = Regex("Public stream: (\\S+)").find(result)?.groupValues?.get(1)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (mapLink != null) TextButton(onClick = { onCopyText(mapLink) }) { Text("SALIN URL PETA") }
                            if (streamLink != null) TextButton(onClick = { cctvUrl = streamLink; selected = "CCTV" }) { Text("PUTAR LIVE") }
                        }
                    }
                    item { InfoCard("Radius pencarian 5 km. Sumber: OpenStreetMap dan Overpass API. Tombol salin menyalin tautan peta. Tombol PUTAR LIVE hanya muncul bila ada URL stream yang dipublikasikan pada data peta; sebagian besar titik tidak menyediakan feed.") }
                }
                "CCTV" -> {
                    item { SectionTitle("AUTHORIZED CCTV STREAM") }
                    item { OutlinedTextField(cctvUrl, { cctvUrl = it }, label = { Text("RTSP / HTTP stream URL") }, placeholder = { Text("rtsp://kamera-anda/stream") }, modifier = Modifier.fillMaxWidth()) }
                    item { Button(onClick = { if (cctvUrl.startsWith("http://") || cctvUrl.startsWith("https://") || cctvUrl.startsWith("rtsp://")) selected = "CCTV PLAYER" }, modifier = Modifier.fillMaxWidth()) { Text("PUTAR DI APLIKASI") } }
                    item { InfoCard("Pemutar internal menggunakan Media3. Masukkan URL stream kamera milik sendiri atau yang memang dipublikasikan dan boleh diakses. Stream dengan autentikasi, format, atau protokol yang tidak didukung mungkin gagal diputar.") }
                }
                "CCTV PLAYER" -> {
                    item { SectionTitle("LIVE CCTV PLAYER") }
                    item { if (cctvUrl.isNotBlank()) CctvPlayer(cctvUrl) else InfoCard("Belum ada URL stream.") }
                    item { TextButton(onClick = { selected = "CCTV" }) { Text("KEMBALI") } }
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
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF39202A))) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = PurpleSoft, modifier = Modifier.size(28.dp))
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

@Composable private fun CctvPlayer(url: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(factory = { ctx ->
        PlayerView(ctx).apply {
            this.player = player
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 620)
        }
    }, modifier = Modifier.fillMaxWidth().height(230.dp))
}
