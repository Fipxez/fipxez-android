import json, os, re, socket, sqlite3, uuid, ipaddress
from datetime import datetime, timezone
from urllib.parse import urlparse
from fastapi import FastAPI, Header, HTTPException
from pydantic import BaseModel, Field

DB=os.getenv("FIPXEZ_DB","fipxez.db")
OWNER=os.getenv("FIPXEZ_OWNER_TOKEN","")
WORKER=os.getenv("FIPXEZ_WORKER_TOKEN","")
app=FastAPI(title="FIPXEZ API",version="0.3.0")

def now(): return datetime.now(timezone.utc).isoformat()
def connect():
    c=sqlite3.connect(DB); c.row_factory=sqlite3.Row; return c
def init():
    with connect() as c:
        c.execute("CREATE TABLE IF NOT EXISTS targets(id TEXT PRIMARY KEY,host TEXT UNIQUE,permission_ref TEXT,created_at TEXT)")
        c.execute("CREATE TABLE IF NOT EXISTS scans(id TEXT PRIMARY KEY,target_id TEXT,status TEXT,result TEXT,created_at TEXT,updated_at TEXT)")
        c.execute("CREATE TABLE IF NOT EXISTS audit(id INTEGER PRIMARY KEY,event TEXT,detail TEXT,created_at TEXT)")
@app.on_event("startup")
def startup(): init()
def log(event,detail):
    with connect() as c: c.execute("INSERT INTO audit(event,detail,created_at) VALUES(?,?,?)",(event,json.dumps(detail),now()))
def auth(token,expected):
    if not expected or token!=expected: raise HTTPException(401,"Token tidak valid")
def host_only(value):
    h=value.strip().lower().rstrip(".")
    if "://" in h: h=urlparse(h).hostname or ""
    if not h or "/" in h or ":" in h or "@" in h or not re.fullmatch(r"[a-z0-9.-]+",h) or "." not in h:
        raise HTTPException(400,"Masukkan hostname valid tanpa skema, port, atau path")
    try: answers=socket.getaddrinfo(h,None)
    except socket.gaierror: raise HTTPException(400,"Hostname tidak dapat di-resolve")
    for a in answers:
        if not ipaddress.ip_address(a[4][0]).is_global: raise HTTPException(400,"Target harus memakai IP publik")
    return h
class Target(BaseModel):
    host:str=Field(min_length=3,max_length=253)
    permission_ref:str=Field(min_length=3,max_length=120)
class Scan(BaseModel): target_id:str
class Result(BaseModel): scan_id:str; result:dict
@app.get("/health")
def health(): return {"status":"ok","service":"FIPXEZ API","version":"0.3.0"}
@app.post("/targets")
def add_target(p:Target,x_owner_token:str|None=Header(default=None)):
    auth(x_owner_token,OWNER); h=host_only(p.host); i=str(uuid.uuid4())
    try:
        with connect() as c: c.execute("INSERT INTO targets VALUES(?,?,?,?)",(i,h,p.permission_ref.strip(),now()))
    except sqlite3.IntegrityError: raise HTTPException(409,"Hostname sudah terdaftar")
    log("target.registered",{"id":i,"host":h})
    return {"id":i,"host":h,"status":"registered"}
@app.get("/targets")
def targets(x_owner_token:str|None=Header(default=None)):
    auth(x_owner_token,OWNER)
    with connect() as c: rows=c.execute("SELECT * FROM targets ORDER BY created_at DESC").fetchall()
    return [dict(r) for r in rows]
@app.post("/scans")
def add_scan(p:Scan,x_owner_token:str|None=Header(default=None)):
    auth(x_owner_token,OWNER); i=str(uuid.uuid4()); stamp=now()
    with connect() as c:
        if not c.execute("SELECT id FROM targets WHERE id=?",(p.target_id,)).fetchone(): raise HTTPException(404,"Target tidak ditemukan")
        c.execute("INSERT INTO scans(id,target_id,status,created_at,updated_at) VALUES(?,?,?,?,?)",(i,p.target_id,"queued",stamp,stamp))
    log("scan.queued",{"id":i}); return {"id":i,"status":"queued"}
@app.get("/scans")
def scans(x_owner_token:str|None=Header(default=None)):
    auth(x_owner_token,OWNER)
    with connect() as c: rows=c.execute("SELECT s.id,s.target_id,t.host,s.status,s.created_at FROM scans s JOIN targets t ON t.id=s.target_id ORDER BY s.created_at DESC LIMIT 100").fetchall()
    return [dict(r) for r in rows]
@app.get("/scans/{sid}")
def scan_status(sid:str,x_owner_token:str|None=Header(default=None)):
    auth(x_owner_token,OWNER)
    with connect() as c: r=c.execute("SELECT s.*,t.host FROM scans s JOIN targets t ON t.id=s.target_id WHERE s.id=?",(sid,)).fetchone()
    if not r: raise HTTPException(404,"Scan tidak ditemukan")
    d=dict(r); d["result"]=json.loads(d["result"]) if d["result"] else None; return d
@app.post("/worker/claim")
def claim(x_worker_token:str|None=Header(default=None)):
    auth(x_worker_token,WORKER)
    with connect() as c:
        r=c.execute("SELECT s.id,s.target_id,t.host FROM scans s JOIN targets t ON t.id=s.target_id WHERE s.status='queued' ORDER BY s.created_at LIMIT 1").fetchone()
        if not r: return {"task":None}
        c.execute("UPDATE scans SET status='running',updated_at=? WHERE id=? AND status='queued'",(now(),r["id"]))
    return {"task":dict(r)}
@app.post("/worker/result")
def result(p:Result,x_worker_token:str|None=Header(default=None)):
    auth(x_worker_token,WORKER)
    with connect() as c:
        cur=c.execute("UPDATE scans SET status='completed',result=?,updated_at=? WHERE id=? AND status='running'",(json.dumps(p.result),now(),p.scan_id))
        if cur.rowcount!=1: raise HTTPException(409,"Scan tidak ada atau bukan running")
    log("scan.completed",{"id":p.scan_id}); return {"status":"completed"}
@app.get("/scans/{sid}/analysis")
def analysis(sid:str,x_owner_token:str|None=Header(default=None)):
    auth(x_owner_token,OWNER)
    with connect() as c: r=c.execute("SELECT status,result FROM scans WHERE id=?",(sid,)).fetchone()
    if not r: raise HTTPException(404,"Scan tidak ditemukan")
    if r["status"]!="completed" or not r["result"]: raise HTTPException(409,"Hasil belum tersedia")
    data=json.loads(r["result"]); headers=data.get("headers",{}); advice=[]
    for key,msg in [("strict-transport-security","Tinjau HSTS setelah HTTPS dipastikan."),("content-security-policy","Tinjau Content-Security-Policy."),("x-content-type-options","Pertimbangkan X-Content-Type-Options: nosniff.")]:
        if key not in headers: advice.append(msg)
    return {"status":"indicative_only","observations":data,"recommendations":advice,"notice":"Pemeriksaan pasif, bukan bukti kerentanan."}
@app.get("/audit")
def audit(x_owner_token:str|None=Header(default=None)):
    auth(x_owner_token,OWNER)
    with connect() as c: rows=c.execute("SELECT * FROM audit ORDER BY id DESC LIMIT 200").fetchall()
    return [{**dict(r),"detail":json.loads(r["detail"])} for r in rows]
