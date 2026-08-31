"""Drop-in metrics client for Python services.

Usage:
    from metrics import Metrics

    m = Metrics(
        endpoint="https://panel.leclowndu93150.dev/api/ingest",
        service="bot",
        hmac_key=os.environ["PANEL_HMAC_KEY"],
    )
    m.track("command_used", user_key=str(message.author.id), props={"cmd": "emojis"})
    m.start()

Events are batched and POSTed in a background thread; nothing blocks your code path.
"""
import hashlib
import hmac
import json
import queue
import threading
import time
import urllib.error
import urllib.request


class Metrics:
    def __init__(self, endpoint: str, service: str, hmac_key: str, flush_interval: float = 5.0, batch_size: int = 100):
        self.endpoint = endpoint
        self.service = service
        self.hmac_key = hmac_key.encode("utf-8")
        self.flush_interval = flush_interval
        self.batch_size = batch_size
        self.q: queue.Queue = queue.Queue(maxsize=10000)
        self._stop = threading.Event()
        self._thread: threading.Thread | None = None

    def track(self, kind: str, user_key: str | None = None, props: dict | None = None):
        try:
            self.q.put_nowait({
                "ts": int(time.time()),
                "kind": kind,
                "user_key": user_key,
                "props": props,
            })
        except queue.Full:
            pass

    def start(self):
        if self._thread is not None:
            return
        self._thread = threading.Thread(target=self._run, daemon=True, name="metrics")
        self._thread.start()

    def stop(self):
        self._stop.set()
        if self._thread:
            self._thread.join(timeout=5)

    def _run(self):
        buf = []
        last_flush = time.time()
        while not self._stop.is_set():
            timeout = max(0.1, self.flush_interval - (time.time() - last_flush))
            try:
                buf.append(self.q.get(timeout=timeout))
            except queue.Empty:
                pass
            if len(buf) >= self.batch_size or (buf and time.time() - last_flush >= self.flush_interval):
                self._send(buf)
                buf = []
                last_flush = time.time()
        if buf:
            self._send(buf)

    def _send(self, events: list[dict]):
        body = json.dumps({"service": self.service, "events": events}, separators=(",", ":")).encode("utf-8")
        ts = str(int(time.time()))
        mac = hmac.new(self.hmac_key, digestmod=hashlib.sha256)
        mac.update(ts.encode("utf-8"))
        mac.update(b".")
        mac.update(body)
        req = urllib.request.Request(
            self.endpoint,
            data=body,
            method="POST",
            headers={
                "Content-Type": "application/json",
                "X-Panel-Ts": ts,
                "X-Panel-Sig": mac.hexdigest(),
            },
        )
        try:
            with urllib.request.urlopen(req, timeout=5) as r:
                r.read()
        except urllib.error.URLError as e:
            print(f"[metrics] send failed: {e}", flush=True)
        except Exception as e:
            print(f"[metrics] send error: {e}", flush=True)
