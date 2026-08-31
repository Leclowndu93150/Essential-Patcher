import threading
import time


class IpRateLimiter:
    def __init__(self, per_minute: int):
        self.per_minute = per_minute
        self._lock = threading.Lock()
        self._windows: dict[str, tuple[int, int]] = {}

    def try_acquire(self, ip: str) -> bool:
        if not ip:
            return True
        minute = int(time.time() // 60)
        with self._lock:
            entry = self._windows.get(ip)
            if entry is None or entry[0] != minute:
                count = 1
            else:
                count = entry[1] + 1
            self._windows[ip] = (minute, count)
            if len(self._windows) > 10_000:
                cutoff = minute - 1
                self._windows = {k: v for k, v in self._windows.items() if v[0] >= cutoff}
        return count <= self.per_minute
