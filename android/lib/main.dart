import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

const bg=Color(0xff070b12), panel=Color(0xff101824), neon=Color(0xff55f2a5), muted=Color(0xff8b9aab);
void main()=>runApp(const FipxezApp());

class FipxezApp extends StatelessWidget {
  const FipxezApp({super.key});
  @override Widget build(BuildContext context)=>MaterialApp(
    title:'FIPXEZ AI',debugShowCheckedModeBanner:false,
    theme:ThemeData(brightness:Brightness.dark,scaffoldBackgroundColor:bg,
      colorScheme:ColorScheme.fromSeed(seedColor:neon,brightness:Brightness.dark),useMaterial3:true),
    home:const ChatPage());
}

class ChatMessage {
  const ChatMessage(this.role,this.content,this.time);
  final String role,content; final DateTime time;
  Map<String,dynamic> toJson()=>{'role':role,'content':content,'time':time.toIso8601String()};
  factory ChatMessage.fromJson(Map<String,dynamic> j)=>ChatMessage(
    j['role'] as String? ?? 'assistant',j['content'] as String? ?? '',
    DateTime.tryParse(j['time'] as String? ?? '') ?? DateTime.now());
}

class ChatPage extends StatefulWidget {
  const ChatPage({super.key});
  @override State<ChatPage> createState()=>_ChatPageState();
}

class _ChatPageState extends State<ChatPage> {
  static const secure=FlutterSecureStorage();
  final input=TextEditingController(),scroll=ScrollController();
  final endpoint=TextEditingController(text:'https://api.openai.com/v1');
  final model=TextEditingController(text:'gpt-4o-mini');
  final apiKey=TextEditingController();
  final systemPrompt=TextEditingController(text:'You are FIPXEZ AI, a helpful, honest and capable personal assistant. Reply in the user language. Be clear about uncertainty and never claim actions you did not perform.');
  final messages=<ChatMessage>[];
  bool busy=false,ready=false;
  String provider='OPENAI-COMPATIBLE';
  String? lastError;

  @override void initState(){super.initState();loadData();}
  Future<void> loadData() async {
    try {
      final p=await SharedPreferences.getInstance();
      endpoint.text=p.getString('endpoint')??endpoint.text;
      model.text=p.getString('model')??model.text;
      systemPrompt.text=p.getString('system')??systemPrompt.text;
      final raw=p.getString('history');
      if(raw!=null){final list=jsonDecode(raw) as List;messages.addAll(list.map((e)=>ChatMessage.fromJson(Map<String,dynamic>.from(e as Map))));}
      apiKey.text=await secure.read(key:'provider_api_key')??'';
      provider=endpoint.text.contains('openai.com')?'OPENAI':'CUSTOM PROVIDER';
    } catch (_) {}
    if(mounted)setState(()=>ready=true);
  }
  Future<void> saveData() async {
    try {
      final p=await SharedPreferences.getInstance();
      await p.setString('endpoint',endpoint.text.trim());
      await p.setString('model',model.text.trim());
      await p.setString('system',systemPrompt.text);
      await p.setString('history',jsonEncode(messages.takeLast(60).map((m)=>m.toJson()).toList()));
    } catch (_) {}
  }
  void toast(String s){if(mounted)ScaffoldMessenger.of(context).showSnackBar(SnackBar(content:Text(s),behavior:SnackBarBehavior.floating));}
  String get chatUrl {
    final b=endpoint.text.trim().replaceAll(RegExp(r'/+$'),'');
    return b.endsWith('/chat/completions')?b:b+'/chat/completions';
  }
  Future<void> send() async {
    final prompt=input.text.trim();
    if(prompt.isEmpty||busy)return;
    if(apiKey.text.trim().isEmpty){toast('Hubungkan provider dan masukkan API key di Settings.');await settings();return;}
    final uri=Uri.tryParse(chatUrl);
    if(uri==null||!uri.hasAuthority||(uri.scheme!='https'&&uri.scheme!='http')){toast('API URL tidak valid.');return;}
    setState((){messages.add(ChatMessage('user',prompt,DateTime.now()));input.clear();busy=true;lastError=null;});
    await saveData();jumpBottom();
    try {
      final history=messages.where((m)=>m.role=='user'||m.role=='assistant').toList().takeLast(24);
      final body={
        'model':model.text.trim(),
        'messages':[
          {'role':'system','content':systemPrompt.text.trim()},
          ...history.map((m)=>{'role':m.role,'content':m.content})
        ],
        'temperature':0.7
      };
      final res=await http.post(uri,headers:{
        'Content-Type':'application/json',
        'Authorization':'Bearer '+apiKey.text.trim()
      },body:jsonEncode(body)).timeout(const Duration(seconds:75));
      if(res.statusCode<200||res.statusCode>=300){
        var detail='HTTP '+res.statusCode.toString();
        try{final d=jsonDecode(res.body);detail=d['error']?['message']?.toString()??detail;}catch(_){}
        throw Exception(detail);
      }
      final data=jsonDecode(res.body) as Map<String,dynamic>;
      final raw=data['choices']?[0]?['message']?['content'];
      final answer=raw is String?raw.trim():'';
      if(answer.isEmpty)throw Exception('Jawaban kosong atau format provider belum didukung.');
      if(!mounted)return;
      setState(()=>messages.add(ChatMessage('assistant',answer,DateTime.now())));
      await saveData();
    } catch(e) {
      if(!mounted)return;
      final detail=e.toString().replaceFirst('Exception: ','');
      setState(()=>lastError=detail);
      toast('Gagal menghubungi AI: '+detail);
    } finally {
      if(mounted){setState(()=>busy=false);jumpBottom();}
    }
  }
  void jumpBottom(){WidgetsBinding.instance.addPostFrameCallback((_){if(scroll.hasClients)scroll.animateTo(scroll.position.maxScrollExtent,duration:const Duration(milliseconds:250),curve:Curves.easeOut);});}

  Future<void> settings() async {
    final ep=TextEditingController(text:endpoint.text),mo=TextEditingController(text:model.text),
      key=TextEditingController(text:apiKey.text),sys=TextEditingController(text:systemPrompt.text);
    var reveal=false;
    await showModalBottomSheet<void>(context:context,isScrollControlled:true,backgroundColor:panel,
      builder:(ctx)=>StatefulBuilder(builder:(ctx,setSheet)=>Padding(
        padding:EdgeInsets.fromLTRB(18,18,18,MediaQuery.of(ctx).viewInsets.bottom+22),
        child:SingleChildScrollView(child:Column(mainAxisSize:MainAxisSize.min,crossAxisAlignment:CrossAxisAlignment.start,children:[
          Row(children:[const Expanded(child:Text('AI PROVIDER SETTINGS',style:TextStyle(fontWeight:FontWeight.w900,letterSpacing:1))),IconButton(onPressed:()=>Navigator.pop(ctx),icon:const Icon(Icons.close))]),
          const Text('Pakai endpoint API yang kompatibel dengan format chat completions.',style:TextStyle(color:muted,fontSize:11)),
          const SizedBox(height:14),
          field(ep,'API Base URL','https://api.openai.com/v1'),
          const SizedBox(height:10),field(mo,'Model ID','gpt-4o-mini'),
          const SizedBox(height:10),
          TextField(controller:key,obscureText:!reveal,autocorrect:false,decoration:dec('API Key','API key dari penyedia model',Icons.key).copyWith(
            suffixIcon:IconButton(onPressed:()=>setSheet(()=>reveal=!reveal),icon:Icon(reveal?Icons.visibility_off:Icons.visibility,color:muted)))),
          const SizedBox(height:10),field(sys,'System instructions','Instruksi perilaku AI',maxLines:4),
          const SizedBox(height:14),
          SizedBox(width:double.infinity,height:48,child:FilledButton(
            style:FilledButton.styleFrom(backgroundColor:neon,foregroundColor:bg),
            onPressed:() async {
              endpoint.text=ep.text.trim();model.text=mo.text.trim();apiKey.text=key.text.trim();systemPrompt.text=sys.text;
              if(apiKey.text.isEmpty){await secure.delete(key:'provider_api_key');}else{await secure.write(key:'provider_api_key',value:apiKey.text);}
              await saveData();
              if(ctx.mounted)Navigator.pop(ctx);
              if(mounted)setState(()=>provider=endpoint.text.contains('openai.com')?'OPENAI':'CUSTOM PROVIDER');
              toast('Pengaturan tersimpan. API key disimpan di secure storage Android.');
            },
            child:const Text('SAVE SETTINGS',style:TextStyle(fontWeight:FontWeight.w900,letterSpacing:1)))),
          const SizedBox(height:9),
          const Text('Jangan bagikan API key. Pemakaian model bisa dikenai biaya oleh provider. Gunakan HTTPS untuk endpoint online.',style:TextStyle(color:Colors.orangeAccent,fontSize:10)),
        ])))));
    ep.dispose();mo.dispose();key.dispose();sys.dispose();
  }
  Widget field(TextEditingController c,String label,String hint,{int maxLines=1})=>TextField(controller:c,maxLines:maxLines,autocorrect:false,decoration:dec(label,hint,Icons.tune));
  InputDecoration dec(String label,String hint,IconData icon)=>InputDecoration(
    labelText:label,hintText:hint,prefixIcon:Icon(icon,color:muted,size:18),filled:true,fillColor:const Color(0xff0a111b),
    border:OutlineInputBorder(borderRadius:BorderRadius.circular(12),borderSide:const BorderSide(color:Color(0xff263344))),
    enabledBorder:OutlineInputBorder(borderRadius:BorderRadius.circular(12),borderSide:const BorderSide(color:Color(0xff263344))),
    focusedBorder:OutlineInputBorder(borderRadius:BorderRadius.circular(12),borderSide:const BorderSide(color:neon,width:1.2)));
  Future<void> clearChat() async {setState((){messages.clear();lastError=null;});await saveData();toast('Riwayat chat lokal dihapus.');}
  @override void dispose(){input.dispose();scroll.dispose();endpoint.dispose();model.dispose();apiKey.dispose();systemPrompt.dispose();super.dispose();}

  @override Widget build(BuildContext context)=>Scaffold(
    appBar:AppBar(backgroundColor:bg,titleSpacing:16,title:Row(children:[
      Container(width:38,height:38,decoration:BoxDecoration(color:neon.withValues(alpha:.12),borderRadius:BorderRadius.circular(12),border:Border.all(color:neon.withValues(alpha:.4))),child:const Icon(Icons.auto_awesome,color:neon)),
      const SizedBox(width:10),const Expanded(child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
        Text('FIPXEZ AI',style:TextStyle(fontSize:17,fontWeight:FontWeight.w900,letterSpacing:1.4)),
        Text('PERSONAL INTELLIGENCE',style:TextStyle(color:muted,fontSize:8,letterSpacing:1.4,fontWeight:FontWeight.bold))
      ]))
    ]),actions:[
      IconButton(tooltip:'Clear chat',onPressed:messages.isEmpty?null:clearChat,icon:const Icon(Icons.add_comment_outlined)),
      IconButton(tooltip:'AI settings',onPressed:settings,icon:const Icon(Icons.tune,color:neon)),
      const SizedBox(width:4)
    ]),
    body:Column(children:[
      Container(margin:const EdgeInsets.fromLTRB(14,4,14,8),padding:const EdgeInsets.symmetric(horizontal:12,vertical:9),
        decoration:BoxDecoration(color:panel,borderRadius:BorderRadius.circular(12),border:Border.all(color:const Color(0xff202d3c))),
        child:Row(children:[const Icon(Icons.circle,color:neon,size:7),const SizedBox(width:8),
          Expanded(child:Text(ready?model.text:'Loading AI profile…',style:const TextStyle(color:Colors.white70,fontSize:10,fontWeight:FontWeight.bold))),
          Text(provider,style:const TextStyle(color:neon,fontSize:9,letterSpacing:.8,fontWeight:FontWeight.bold))])),
      Expanded(child:messages.isEmpty?welcome():ListView.builder(controller:scroll,padding:const EdgeInsets.fromLTRB(14,8,14,18),
        itemCount:messages.length+(busy?1:0),itemBuilder:(ctx,i)=>busy&&i==messages.length?typing():bubble(messages[i]))),
      if(lastError!=null)Padding(padding:const EdgeInsets.fromLTRB(15,0,15,7),child:Text('Last error: '+lastError!,maxLines:2,overflow:TextOverflow.ellipsis,style:const TextStyle(color:Colors.orangeAccent,fontSize:10))),
      composer()
    ]));
  Widget welcome()=>Center(child:SingleChildScrollView(padding:const EdgeInsets.all(24),child:Column(mainAxisAlignment:MainAxisAlignment.center,children:[
    Container(width:78,height:78,decoration:BoxDecoration(shape:BoxShape.circle,color:neon.withValues(alpha:.08),border:Border.all(color:neon.withValues(alpha:.4)),boxShadow:[BoxShadow(color:neon.withValues(alpha:.08),blurRadius:28)]),child:const Icon(Icons.auto_awesome,color:neon,size:36)),
    const SizedBox(height:22),const Text('What can we explore?',textAlign:TextAlign.center,style:TextStyle(fontSize:24,fontWeight:FontWeight.w900)),
    const SizedBox(height:9),const Text('Your personal AI workspace. Connect a model provider in settings to start chatting.',textAlign:TextAlign.center,style:TextStyle(color:muted,fontSize:12,height:1.6)),
    const SizedBox(height:22),suggestion(Icons.lightbulb_outline,'Explain something complicated simply'),
    suggestion(Icons.rocket_launch_outlined,'Help me plan a project'),suggestion(Icons.code_rounded,'Review or write some code'),
    const SizedBox(height:14),OutlinedButton.icon(onPressed:settings,icon:const Icon(Icons.link,color:neon),
      label:const Text('CONNECT AI PROVIDER',style:TextStyle(color:neon,fontWeight:FontWeight.bold,letterSpacing:.7)),
      style:OutlinedButton.styleFrom(side:BorderSide(color:neon.withValues(alpha:.45)),shape:RoundedRectangleBorder(borderRadius:BorderRadius.circular(12))))
  ])));
  Widget suggestion(IconData icon,String text)=>Container(width:double.infinity,margin:const EdgeInsets.only(bottom:8),padding:const EdgeInsets.all(12),
    decoration:BoxDecoration(color:panel,borderRadius:BorderRadius.circular(12),border:Border.all(color:const Color(0xff202d3c))),
    child:Row(children:[Icon(icon,color:neon,size:17),const SizedBox(width:10),Expanded(child:Text(text,style:const TextStyle(color:Colors.white70,fontSize:11)))]));
  Widget bubble(ChatMessage m){final user=m.role=='user';return Align(alignment:user?Alignment.centerRight:Alignment.centerLeft,
    child:Container(constraints:BoxConstraints(maxWidth:MediaQuery.of(context).size.width*.88),margin:const EdgeInsets.only(bottom:12),padding:const EdgeInsets.all(13),
      decoration:BoxDecoration(color:user?const Color(0xff17382f):panel,borderRadius:BorderRadius.circular(16),border:Border.all(color:user?neon.withValues(alpha:.2):const Color(0xff263344))),
      child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
        Text(user?'YOU':'FIPXEZ AI',style:TextStyle(color:user?neon:const Color(0xff9cb8ff),fontSize:9,fontWeight:FontWeight.w900,letterSpacing:1.1)),
        const SizedBox(height:7),SelectableText(m.content,style:const TextStyle(color:Colors.white,fontSize:13,height:1.55))
      ])));
  }
  Widget typing()=>Align(alignment:Alignment.centerLeft,child:Container(margin:const EdgeInsets.only(bottom:12),padding:const EdgeInsets.all(14),
    decoration:BoxDecoration(color:panel,borderRadius:BorderRadius.circular(15),border:Border.all(color:const Color(0xff263344))),
    child:const Row(mainAxisSize:MainAxisSize.min,children:[SizedBox(width:13,height:13,child:CircularProgressIndicator(strokeWidth:2,color:neon)),SizedBox(width:10),Text('FIPXEZ is thinking…',style:TextStyle(color:muted,fontSize:11))])));
  Widget composer()=>SafeArea(top:false,child:Container(padding:const EdgeInsets.fromLTRB(12,10,12,10),
    decoration:const BoxDecoration(color:panel,border:Border(top:BorderSide(color:Color(0xff202d3c)))),
    child:Row(crossAxisAlignment:CrossAxisAlignment.end,children:[
      Expanded(child:TextField(controller:input,minLines:1,maxLines:5,decoration:InputDecoration(hintText:'Message FIPXEZ AI…',filled:true,fillColor:const Color(0xff080e17),
        contentPadding:const EdgeInsets.symmetric(horizontal:15,vertical:12),border:OutlineInputBorder(borderRadius:BorderRadius.circular(18),borderSide:BorderSide.none)))),
      const SizedBox(width:8),SizedBox(width:47,height:47,child:FilledButton(onPressed:busy?null:send,
        style:FilledButton.styleFrom(backgroundColor:neon,foregroundColor:bg,padding:EdgeInsets.zero,shape:RoundedRectangleBorder(borderRadius:BorderRadius.circular(15))),
        child:Icon(busy?Icons.hourglass_top:Icons.arrow_upward_rounded)))
    ])));
}
extension<T> on List<T>{List<T> takeLast(int n)=>length<=n?List<T>.from(this):sublist(length-n);}
