import os,time,socket,ipaddress,requests
API=os.getenv("FIPXEZ_API","http://127.0.0.1:8000").rstrip("/")
TOKEN=os.getenv("FIPXEZ_WORKER_TOKEN","")
def inspect(task):
    host=task["host"]
    for item in socket.getaddrinfo(host,443,type=socket.SOCK_STREAM):
        if not ipaddress.ip_address(item[4][0]).is_global: raise ValueError("Alamat target bukan IP publik")
    r=requests.get("https://"+host+"/",timeout=12,allow_redirects=False,headers={"User-Agent":"FIPXEZ-Passive-Audit/0.3"})
    h={k.lower():v for k,v in r.headers.items()}
    keys=["strict-transport-security","content-security-policy","x-content-type-options","x-frame-options","referrer-policy","permissions-policy"]
    return {"url":"https://"+host+"/","status_code":r.status_code,"headers":{k:h[k] for k in keys if k in h},"https_reachable":True,"method":"single HTTPS GET; no crawling, exploit, brute force, or port scan"}
def main():
    if not TOKEN: raise SystemExit("FIPXEZ_WORKER_TOKEN belum diatur")
    print("FIPXEZ worker aktif. Ctrl+C untuk berhenti.")
    while True:
        try:
            r=requests.post(API+"/worker/claim",headers={"X-Worker-Token":TOKEN},timeout=10); r.raise_for_status()
            task=r.json().get("task")
            if not task: time.sleep(3); continue
            try: data=inspect(task)
            except Exception as e: data={"error":str(e),"method":"passive HTTPS check"}
            r=requests.post(API+"/worker/result",headers={"X-Worker-Token":TOKEN},json={"scan_id":task["id"],"result":data},timeout=15); r.raise_for_status()
            print("Selesai:",task["host"])
        except KeyboardInterrupt: break
        except Exception as e: print("Worker error:",e); time.sleep(5)
if __name__=="__main__": main()
