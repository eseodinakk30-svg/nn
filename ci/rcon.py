"""Tiny Minecraft RCON client: python3 ci/rcon.py "command" [more commands...] prints each response."""
import socket
import struct
import sys
import time


def packet(sock, req_id, kind, body):
    data = struct.pack("<ii", req_id, kind) + body.encode("utf-8") + b"\x00\x00"
    sock.sendall(struct.pack("<i", len(data)) + data)
    length = struct.unpack("<i", recv_exact(sock, 4))[0]
    payload = recv_exact(sock, length)
    resp_id, _ = struct.unpack("<ii", payload[:8])
    return resp_id, payload[8:-2].decode("utf-8", "replace")


def recv_exact(sock, n):
    buf = b""
    while len(buf) < n:
        chunk = sock.recv(n - len(buf))
        if not chunk:
            raise ConnectionError("rcon closed")
        buf += chunk
    return buf


def connect(password="soak", port=25575, tries=60):
    for _ in range(tries):
        try:
            sock = socket.create_connection(("127.0.0.1", port), timeout=120)
            resp_id, _ = packet(sock, 1, 3, password)
            if resp_id == -1:
                raise RuntimeError("bad rcon password")
            return sock
        except (ConnectionError, OSError):
            time.sleep(2)
    raise RuntimeError("could not reach rcon")


if __name__ == "__main__":
    s = connect()
    for i, command in enumerate(sys.argv[1:]):
        print(packet(s, 10 + i, 2, command)[1])
