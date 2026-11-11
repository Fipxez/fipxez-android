import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

void main() => runApp(const FipxezApp());
const bg = Color(0xff070b12), panel = Color(0xff101824), panel2 = Color(0xff151f2d);
const neon = Color(0xff55f2a5), muted = Color(0xff8b9aab);

class FipxezApp extends StatelessWidget {
  const FipxezApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
    title: 'FIPXEZ', debugShowCheckedModeBanner: false,
    theme: ThemeData(brightness: Brightness.dark, scaffoldBackgroundColor: bg,
      colorScheme: ColorScheme.fromSeed(seedColor: neon, brightness: Brightness.dark), useMaterial3: true),
    home: const HomePage());
}

class AuditRecord {
  AuditRecord({required this.host, required this.ticket, required this.code, required this.date, required this.headers, required this.note});
  final String host, ticket, note;
  final int code;
  final DateTime date;
  final Map<String,String> headers;
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});
  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final host = TextEditingController(), ticket = TextEditingController();
  final records = <AuditRecord>[];
  bool busy = false;
  String status = 'READY';
  String message = 'Mobile engine siap. Tambahkan target yang memang lu berizin untuk periksa.';
  AuditRecord? selected;

  bool validHost(String value) {
    final h = value.trim().toLowerCase();
    if (h.isEmpty || h.contains('://') || h.contains('/') || h.contains(':') || h.contains('@')) return false;
    return RegExp(r'^(?=.{1,253}$)([a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,63}$').hasMatch(h);
  }

  Future<void> runAudit() async {
    final h = host.text.trim().toLowerCase(), t = ticket.text.trim();
    if (!validHost(h)) { setState(() { status='INPUT ERROR'; message='Masukkan hostname valid saja, contoh: domainmiliklu.com'; }); return; }
    if (t.length < 3) { setState(() { status='INPUT ERROR'; message='Isi referensi izin/tiket minimal 3 karakter.'; }); return; }
    setState(() { busy=true; status='CHECKING'; message='Mengirim satu permintaan HTTPS untuk membaca security headers…'; selected=null; });
    final client = http.Client();
    try {
      final req = http.Request('GET', Uri.https(h, '/'))..followRedirects=false;
      final response = await client.send(req).timeout(const Duration(seconds:18));
      final wanted = ['strict-transport-security','content-security-policy','x-content-type-options','x-frame-options','referrer-policy','permissions-policy','content-type','server'];
      final headers = <String,String>{};
      response.headers.forEach((k,v) { if (wanted.contains(k.toLowerCase())) headers[k.toLowerCase()] = v; });
      final result = AuditRecord(host:h,ticket:t,code:response.statusCode,date:DateTime.now(),headers:headers,
        note: response.statusCode >= 300 && response.statusCode < 400 ? 'Server mengirim redirect. Pemeriksaan berhenti pada respons awal.' : 'Satu HTTPS GET selesai. Ini observasi konfigurasi, bukan bukti kerentanan.');
      if (!mounted) return;
      setState(() { records.insert(0,result); selected=result; status='COMPLETE'; message='Pemeriksaan selesai untuk '+h+'.'; host.clear(); ticket.clear(); });
    } catch(e) {
      if (!mounted) return;
      setState(() { status='NETWORK ERROR'; message='Tidak dapat menyelesaikan HTTPS request. Periksa koneksi internet, hostname, atau TLS. Detail: '+e.toString(); });
    } finally { client.close(); if (mounted) setState(()=>busy=false); }
  }

  @override
  void dispose() { host.dispose(); ticket.dispose(); super.dispose(); }

  String timeLabel(DateTime d) => d.day.toString().padLeft(2,'0')+'/'+d.month.toString().padLeft(2,'0')+' '+d.hour.toString().padLeft(2,'0')+':'+d.minute.toString().padLeft(2,'0');

  Widget sectionTitle(String n,String title,{Widget? trailing}) => Row(children:[
    Text(n,style:const TextStyle(color:neon,fontWeight:FontWeight.w800,letterSpacing:1.5)),
    const SizedBox(width:10),Expanded(child:Text(title,style:const TextStyle(fontSize:15,fontWeight:FontWeight.w700))),
    if(trailing!=null) trailing,
  ]);

  Widget input(TextEditingController c,String label,String hint,IconData icon) => TextField(
    controller:c,autocorrect:false,enableSuggestions:false,style:const TextStyle(fontSize:14),
    decoration:InputDecoration(labelText:label,hintText:hint,prefixIcon:Icon(icon,color:muted,size:19),
      filled:true,fillColor:const Color(0xff0a111b),
      border:OutlineInputBorder(borderRadius:BorderRadius.circular(13),borderSide:const BorderSide(color:Color(0xff263344))),
      enabledBorder:OutlineInputBorder(borderRadius:BorderRadius.circular(13),borderSide:const BorderSide(color:Color(0xff263344))),
      focusedBorder:OutlineInputBorder(borderRadius:BorderRadius.circular(13),borderSide:const BorderSide(color:neon,width:1.3))));

  Widget panelCard({required Widget child}) => Container(padding:const EdgeInsets.all(16),
    decoration:BoxDecoration(color:panel,borderRadius:BorderRadius.circular(19),border:Border.all(color:const Color(0xff202d3c))),child:child);

  @override
  Widget build(BuildContext context) => Scaffold(body:SafeArea(child:ListView(
    padding:const EdgeInsets.fromLTRB(18,14,18,28),children:[
      Row(children:[
        Container(width:43,height:43,decoration:BoxDecoration(color:neon.withOpacity(.12),borderRadius:BorderRadius.circular(13),border:Border.all(color:neon.withOpacity(.45))),child:const Icon(Icons.radar_rounded,color:neon,size:25)),
        const SizedBox(width:11),const Expanded(child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
          Text('FIPXEZ',style:TextStyle(fontSize:21,fontWeight:FontWeight.w900,letterSpacing:2)),
          Text('MOBILE SECURITY CONSOLE',style:TextStyle(fontSize:9,color:muted,letterSpacing:1.7,fontWeight:FontWeight.w700))])),
        Container(padding:const EdgeInsets.symmetric(horizontal:10,vertical:7),decoration:BoxDecoration(color:neon.withOpacity(.1),borderRadius:BorderRadius.circular(30),border:Border.all(color:neon.withOpacity(.35))),
          child:const Row(children:[Icon(Icons.circle,color:neon,size:7),SizedBox(width:6),Text('MOBILE',style:TextStyle(color:neon,fontSize:10,fontWeight:FontWeight.bold,letterSpacing:1))])),
      ]),
      const SizedBox(height:23),
      Container(padding:const EdgeInsets.all(19),decoration:BoxDecoration(borderRadius:BorderRadius.circular(22),
        gradient:const LinearGradient(begin:Alignment.topLeft,end:Alignment.bottomRight,colors:[Color(0xff18352f),Color(0xff10202a),Color(0xff111a29)]),border:Border.all(color:neon.withOpacity(.24))),
        child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
          const Text('PASSIVE SECURITY AUDIT',style:TextStyle(color:neon,fontSize:10,letterSpacing:2,fontWeight:FontWeight.w800)),
          const SizedBox(height:12),const Text('Your security\nworkspace.',style:TextStyle(fontSize:27,height:1.1,fontWeight:FontWeight.w900)),
          const SizedBox(height:10),const Text('Lightweight HTTPS configuration checks. Runs directly on this Android device.',style:TextStyle(color:Color(0xffb4c4c5),fontSize:12,height:1.5)),
          const SizedBox(height:18),Row(children:[
            const Icon(Icons.phone_android,color:neon,size:17),const SizedBox(width:7),
            const Text('PC NOT REQUIRED',style:TextStyle(color:neon,fontSize:10,letterSpacing:1.3,fontWeight:FontWeight.bold)),
            const Spacer(),Text(records.length.toString().padLeft(2,'0')+' CHECKS',style:const TextStyle(color:Colors.white70,fontSize:10,letterSpacing:1))]),
        ])),
      const SizedBox(height:15),
      Row(children:[
        Expanded(child:statCard(Icons.shield_outlined,'ENGINE','ON-DEVICE',neon)),
        const SizedBox(width:10),Expanded(child:statCard(Icons.fact_check_outlined,'CHECKS',records.length.toString(),Colors.lightBlueAccent)),
      ]),
      const SizedBox(height:21),
      panelCard(child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
        sectionTitle('01','REGISTER AUTHORIZED TARGET'),const SizedBox(height:7),
        const Text('Masukkan domain yang lu miliki atau yang sudah memberi izin tertulis.',style:TextStyle(color:muted,fontSize:11,height:1.4)),
        const SizedBox(height:15),input(host,'Hostname','contoh: domainmiliklu.com',Icons.language),
        const SizedBox(height:11),input(ticket,'Permission reference / ticket','Nomor tiket atau referensi izin',Icons.verified_user_outlined),
        const SizedBox(height:14),SizedBox(width:double.infinity,height:49,child:FilledButton.icon(
          onPressed:busy?null:runAudit,style:FilledButton.styleFrom(backgroundColor:neon,foregroundColor:const Color(0xff06130d),shape:RoundedRectangleBorder(borderRadius:BorderRadius.circular(13))),
          icon:Icon(busy?Icons.hourglass_top:Icons.radar),label:Text(busy?'CHECKING…':'START PASSIVE CHECK',style:const TextStyle(fontWeight:FontWeight.w900,letterSpacing:1,fontSize:11)))),
        if(busy) const Padding(padding:EdgeInsets.only(top:12),child:LinearProgressIndicator(color:neon,backgroundColor:panel2)),
        const SizedBox(height:12),const Text('One HTTPS GET • Security headers only • No crawling, exploit, brute force, or port scan.',style:TextStyle(color:muted,fontSize:10,height:1.5)),
      ])),
      const SizedBox(height:14),
      panelCard(child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
        sectionTitle('02','ENGINE STATUS',trailing:Icon(Icons.circle,color:status=='COMPLETE'?neon:status.contains('ERROR')?Colors.orangeAccent:Colors.blueGrey,size:9)),
        const SizedBox(height:13),Text(status,style:TextStyle(color:status=='COMPLETE'?neon:status.contains('ERROR')?Colors.orangeAccent:Colors.white,fontWeight:FontWeight.w800,letterSpacing:1.2,fontSize:12)),
        const SizedBox(height:6),Text(message,style:const TextStyle(color:muted,fontSize:11,height:1.5)),
      ])),
      const SizedBox(height:14),
      panelCard(child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
        sectionTitle('03','LATEST RESULTS',trailing:Text(records.length.toString()+' TOTAL',style:const TextStyle(color:muted,fontSize:9,letterSpacing:1))),
        const SizedBox(height:12),
        if(records.isEmpty) const Padding(padding:EdgeInsets.symmetric(vertical:15),child:Center(child:Column(children:[
          Icon(Icons.manage_search,color:muted,size:30),SizedBox(height:7),Text('No checks yet',style:TextStyle(color:muted,fontSize:12))])))
        else ...records.take(8).map((r)=>InkWell(onTap:()=>setState(()=>selected=r),
          child:Container(margin:const EdgeInsets.only(bottom:8),padding:const EdgeInsets.all(11),decoration:BoxDecoration(color:panel2,borderRadius:BorderRadius.circular(12)),
            child:Row(children:[const Icon(Icons.language,color:neon,size:18),const SizedBox(width:9),Expanded(child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
              Text(r.host,style:const TextStyle(fontSize:12,fontWeight:FontWeight.w700)),
              Text(timeLabel(r.date)+' • HTTP '+r.code.toString(),style:const TextStyle(color:muted,fontSize:10))])),
              const Icon(Icons.chevron_right,color:muted,size:18)])))),
        if(selected!=null)...[
          const Divider(color:Color(0xff283545),height:22),
          Text('DETAIL • '+selected!.host,style:const TextStyle(color:neon,fontWeight:FontWeight.bold,fontSize:11)),
          const SizedBox(height:7),Text(selected!.note,style:const TextStyle(color:muted,fontSize:10,height:1.4)),
          const SizedBox(height:8),
          if(selected!.headers.isEmpty) const Text('Tidak ada header terpilih pada respons ini.',style:TextStyle(color:muted,fontSize:10))
          else ...selected!.headers.entries.map((e)=>Padding(padding:const EdgeInsets.only(bottom:7),child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
            Text(e.key.toUpperCase(),style:const TextStyle(color:neon,fontSize:9,fontWeight:FontWeight.bold)),
            Text(e.value,style:const TextStyle(color:Colors.white70,fontSize:10))]))),
        ],
      ])),
      const SizedBox(height:17),const Center(child:Text('FIPXEZ • MOBILE EDITION • PASSIVE MODE',style:TextStyle(color:Color(0xff536274),fontSize:9,letterSpacing:1.1))),
    ])));

  Widget statCard(IconData icon,String label,String value,Color color)=>Container(padding:const EdgeInsets.all(13),
    decoration:BoxDecoration(color:panel,borderRadius:BorderRadius.circular(16),border:Border.all(color:const Color(0xff202d3c))),
    child:Row(children:[Icon(icon,color:color,size:21),const SizedBox(width:9),Expanded(child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
      Text(label,style:const TextStyle(color:muted,fontSize:9,letterSpacing:1)),const SizedBox(height:3),
      Text(value,style:TextStyle(color:color,fontSize:11,fontWeight:FontWeight.w900))]))]));
}
