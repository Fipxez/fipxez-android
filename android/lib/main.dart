import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

void main() => runApp(const FipxezApp());

class FipxezApp extends StatelessWidget {
  const FipxezApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
    title: 'FIPXEZ', debugShowCheckedModeBanner: false,
    theme: ThemeData.dark(useMaterial3: true).copyWith(
      colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xff65f59a), brightness: Brightness.dark),
      scaffoldBackgroundColor: const Color(0xff07110d)),
    home: const HomePage());
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});
  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final url = TextEditingController(text: 'http://192.168.1.10:8000');
  final token = TextEditingController();
  final host = TextEditingController();
  final permission = TextEditingController();
  String status = 'Masukkan alamat backend PC dan owner token.';
  bool busy = false;
  List<dynamic> targets = [];
  List<dynamic> scans = [];

  Map<String,String> get headers => {'Content-Type':'application/json','X-Owner-Token':token.text.trim()};
  Uri endpoint(String path) => Uri.parse(url.text.trim().replaceAll(RegExp(r'/+$'), '') + path);

  Future<dynamic> request(String method,String path,{Map<String,dynamic>? body}) async {
    late http.Response res;
    if (method=='GET') {
      res=await http.get(endpoint(path),headers:headers).timeout(const Duration(seconds:15));
    } else {
      res=await http.post(endpoint(path),headers:headers,body:jsonEncode(body ?? {})).timeout(const Duration(seconds:15));
    }
    final data=res.body.isEmpty ? {} : jsonDecode(res.body);
    if (res.statusCode<200 || res.statusCode>=300) {
      throw Exception(data is Map ? (data['detail'] ?? res.body).toString() : res.body);
    }
    return data;
  }

  Future<void> perform(Future<void> Function() action) async {
    setState(() {busy=true; status='Menghubungkan…';});
    try { await action(); }
    catch(e) { setState(()=>status='Gagal: '+e.toString()); }
    finally { if(mounted) setState(()=>busy=false); }
  }

  Future<void> testConnection() => perform(() async {
    final data=await request('GET','/health') as Map<String,dynamic>;
    setState(()=>status='Backend terhubung: '+data['service'].toString()+' '+data['version'].toString());
  });

  Future<void> refreshData() => perform(() async {
    final t=await request('GET','/targets') as List<dynamic>;
    final s=await request('GET','/scans') as List<dynamic>;
    setState(() {targets=t; scans=s; status='Data diperbarui.';});
  });

  Future<void> addTarget() => perform(() async {
    final data=await request('POST','/targets',body:{'host':host.text.trim(),'permission_ref':permission.text.trim()});
    final t=await request('GET','/targets') as List<dynamic>;
    setState(() {targets=t; status='Target terdaftar: '+data['host'].toString(); host.clear(); permission.clear();});
  });

  Future<void> startScan(String id) => perform(() async {
    final data=await request('POST','/scans',body:{'target_id':id});
    final s=await request('GET','/scans') as List<dynamic>;
    setState(() {scans=s; status='Scan masuk antrean: '+data['id'].toString();});
  });

  @override
  void dispose() {url.dispose();token.dispose();host.dispose();permission.dispose();super.dispose();}

  Widget field(TextEditingController c,String label,{bool secret=false,String? hint}) =>
    Padding(padding:const EdgeInsets.only(bottom:10),child:TextField(controller:c,obscureText:secret,
      decoration:InputDecoration(labelText:label,hintText:hint,border:const OutlineInputBorder())));

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar:AppBar(title:const Text('FIPXEZ'),actions:[IconButton(onPressed:busy?null:refreshData,icon:const Icon(Icons.refresh))]),
    body:SafeArea(child:ListView(padding:const EdgeInsets.all(16),children:[
      const Text('PASSIVE SECURITY AUDIT',style:TextStyle(color:Color(0xff65f59a),letterSpacing:2,fontWeight:FontWeight.bold)),
      const SizedBox(height:14),
      Card(child:Padding(padding:const EdgeInsets.all(14),child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
        const Text('01 / Koneksi PC',style:TextStyle(fontSize:18,fontWeight:FontWeight.bold)),
        const SizedBox(height:12),field(url,'Backend URL',hint:'http://IP-PC:8000'),field(token,'Owner token',secret:true),
        FilledButton.icon(onPressed:busy?null:testConnection,icon:const Icon(Icons.link),label:const Text('TEST KONEKSI'))]))),
      Card(child:Padding(padding:const EdgeInsets.all(14),child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
        const Text('02 / Daftarkan target berizin',style:TextStyle(fontSize:18,fontWeight:FontWeight.bold)),
        const SizedBox(height:8),field(host,'Hostname',hint:'contoh: example.com'),field(permission,'Referensi izin / tiket'),
        FilledButton(onPressed:busy?null:addTarget,child:const Text('DAFTARKAN TARGET')),
        const SizedBox(height:8),
        const Text('Gunakan hanya aset milik sendiri atau yang memiliki izin tertulis. Pemeriksaan terbatas pada satu HTTPS GET dan security headers; tanpa exploit, brute force, atau port scan.',style:TextStyle(fontSize:12,color:Colors.white70))
      ]))),
      Card(child:Padding(padding:const EdgeInsets.all(14),child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
        const Text('03 / Target',style:TextStyle(fontSize:18,fontWeight:FontWeight.bold)),
        if(targets.isEmpty) const Text('Belum ada target. Tekan refresh setelah backend aktif.'),
        ...targets.map((item)=>ListTile(contentPadding:EdgeInsets.zero,title:Text(item['host'].toString()),
          subtitle:Text('Izin: '+item['permission_ref'].toString()),
          trailing:IconButton(icon:const Icon(Icons.play_arrow),onPressed:busy?null:()=>startScan(item['id'].toString()))))
      ]))),
      Card(child:Padding(padding:const EdgeInsets.all(14),child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
        const Text('04 / Riwayat scan',style:TextStyle(fontSize:18,fontWeight:FontWeight.bold)),
        if(scans.isEmpty) const Text('Belum ada scan.'),
        ...scans.map((item)=>ListTile(contentPadding:EdgeInsets.zero,title:Text(item['host'].toString()),
          subtitle:Text(item['status'].toString()+' • '+item['id'].toString())))
      ]))),
      Card(child:Padding(padding:const EdgeInsets.all(14),child:Text(status)))
    ])));
}
