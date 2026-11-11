import hashlib,json,os,secrets,shutil
from datetime import datetime
from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse
HOST="0.0.0.0";PORT=int(os.getenv("FIPXEZ_PORT","8765"))
ROOT=Path(os.getenv("LOCALAPPDATA",str(Path.home())))/"FIPXEZ-CPM2";ROOT.mkdir(parents=True,exist_ok=True)
TOKEN_FILE=ROOT/"worker-token.txt"
if TOKEN_FILE.exists(): TOKEN=TOKEN_FILE.read_text(encoding="utf-8").strip()
else:
 TOKEN=secrets.token_urlsafe(32);TOKEN_FILE.write_text(TOKEN,encoding="utf-8")
SAVE=Path(os.getenv("CPM2_SAVE_PATH",str(Path.home()/"AppData/LocalLow/Olzhas/Car Parking 2/SaveFile.es3")))
BACKUPS=Path.home()/"Documents"/"FIPXEZ-CPM2-Backups";BACKUPS.mkdir(parents=True,exist_ok=True)
def inspect_save():
 if not SAVE.is_file(): return {"found":False,"path":str(SAVE),"message":"Save belum ditemukan. Atur CPM2_SAVE_PATH jika lokasi berbeda."}
 st=SAVE.stat();h=hashlib.sha256(SAVE.read_bytes()).hexdigest()
 return {"found":True,"path":str(SAVE),"size_bytes":st.st_size,"modified":datetime.fromtimestamp(st.st_mtime).isoformat(timespec="seconds"),"sha256":h,"message":"Read-only; file asli tidak diubah."}
class Handler(BaseHTTPRequestHandler):
 def send_json(self,status,data):
  raw=json.dumps(data,ensure_ascii=False).encode();self.send_response(status);self.send_header("Content-Type","application/json; charset=utf-8");self.send_header("Content-Length",str(len(raw)));self.end_headers();self.wfile.write(raw)
 def authorized(self): return self.headers.get("Authorization","")=="Bearer "+TOKEN
 def do_GET(self):
  route=urlparse(self.path).path
  if route=="/health": return self.send_json(200,{"ok":True,"service":"FIPXEZ CPM2 Worker","version":"0.1.0"})
  if not self.authorized(): return self.send_json(401,{"error":"Token salah"})
  if route=="/save/inspect": return self.send_json(200,inspect_save())
  return self.send_json(404,{"error":"Endpoint tidak ditemukan"})
 def do_POST(self):
  if not self.authorized(): return self.send_json(401,{"error":"Token salah"})
  if urlparse(self.path).path!="/save/backup": return self.send_json(404,{"error":"Endpoint tidak ditemukan"})
  info=inspect_save()
  if not info["found"]: return self.send_json(404,info)
  dest=BACKUPS/("SaveFile-"+datetime.now().strftime("%Y%m%d-%H%M%S")+".es3");shutil.copy2(SAVE,dest)
  return self.send_json(200,{"ok":True,"backup":str(dest),"size_bytes":dest.stat().st_size,"message":"Backup selesai; save asli tidak diubah."})
 def log_message(self,fmt,*args): print("[FIPXEZ]",fmt%args)
if __name__=="__main__":
 print("\nFIPXEZ CPM2 Worker aktif");print(f"Alamat: http://IP-PC:{PORT}");print(f"Token (jangan bagikan): {TOKEN}");print(f"Save: {SAVE}");print("Ctrl+C untuk berhenti. Inspeksi dan backup offline saja.")
 ThreadingHTTPServer((HOST,PORT),Handler).serve_forever()
