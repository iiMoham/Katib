"""A tiny thread-safe LRU cache for correction/suggestion results.

Mirrors the spec's "cache the last 50 correction results" requirement so the
proxy can short-circuit identical requests without hitting Grok again.
"""

from __future__ import annotations

import threading
from collections import OrderedDict
from typing import Generic, Optional, TypeVar

K = TypeVar("K")
V = TypeVar("V")


class LRUCache(Generic[K, V]):
    def __init__(self, capacity: int = 50) -> None:
        self.capacity = max(1, capacity)
        self._store: "OrderedDict[K, V]" = OrderedDict()
        self._lock = threading.Lock()

    def get(self, key: K) -> Optional[V]:
        with self._lock:
            if key not in self._store:
                return None
            self._store.move_to_end(key)
            return self._store[key]

    def put(self, key: K, value: V) -> None:
        with self._lock:
            self._store[key] = value
            self._store.move_to_end(key)
            while len(self._store) > self.capacity:
                self._store.popitem(last=False)

    def clear(self) -> None:
        with self._lock:
            self._store.clear()

    def __len__(self) -> int:
        with self._lock:
            return len(self._store)
