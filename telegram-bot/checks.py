"""Infrastructure health check functions."""
import asyncio
import shutil
import socket
import subprocess
from datetime import datetime, timezone
from pathlib import Path


# ── Colima ────────────────────────────────────────────────────────────────────

def colima_status() -> dict:
    """Return {'running': bool, 'info': str}."""
    result = subprocess.run(
        ["colima", "status"],
        capture_output=True, text=True, timeout=10
    )
    running = result.returncode == 0
    info = (result.stdout or result.stderr).strip().splitlines()[0] if (result.stdout or result.stderr) else ""
    return {"running": running, "info": info}


def colima_start() -> bool:
    """Attempt to start Colima. Returns True on success."""
    result = subprocess.run(
        ["colima", "start"],
        capture_output=True, text=True, timeout=120
    )
    return result.returncode == 0


# ── Docker containers ─────────────────────────────────────────────────────────

def container_states() -> list[dict]:
    """Return list of {name, status, health} for all compose containers."""
    result = subprocess.run(
        ["docker", "ps", "--format", "{{.Names}}\t{{.Status}}"],
        capture_output=True, text=True, timeout=10
    )
    if result.returncode != 0:
        return []
    containers = []
    for line in result.stdout.strip().splitlines():
        if not line:
            continue
        parts = line.split("\t", 1)
        name = parts[0] if parts else "?"
        status = parts[1] if len(parts) > 1 else "?"
        healthy = "healthy" in status.lower() or (
            "(healthy)" not in status.lower() and "unhealthy" not in status.lower()
            and "starting" not in status.lower()
        )
        containers.append({"name": name, "status": status, "healthy": healthy})
    return containers


def restart_container(name: str) -> bool:
    result = subprocess.run(
        ["docker", "restart", name],
        capture_output=True, text=True, timeout=60
    )
    return result.returncode == 0


def compose_up() -> bool:
    project_dir = Path(__file__).parent.parent
    result = subprocess.run(
        ["docker", "compose", "up", "-d"],
        capture_output=True, text=True, timeout=180,
        cwd=str(project_dir)
    )
    return result.returncode == 0


def container_logs(name: str, lines: int = 30) -> str:
    result = subprocess.run(
        ["docker", "logs", "--tail", str(lines), name],
        capture_output=True, text=True, timeout=10
    )
    return (result.stdout + result.stderr).strip()


# ── Vault ─────────────────────────────────────────────────────────────────────

def vault_status() -> dict:
    """Return {'sealed': bool, 'ok': bool}."""
    result = subprocess.run(
        ["vault", "status", "-format=json"],
        capture_output=True, text=True, timeout=5,
        env={**__import__("os").environ, "VAULT_ADDR": "http://127.0.0.1:8200"}
    )
    import json
    try:
        data = json.loads(result.stdout)
        return {"sealed": data.get("sealed", True), "ok": True}
    except Exception:
        return {"sealed": True, "ok": False}


# ── nginx ─────────────────────────────────────────────────────────────────────

def nginx_responding(port: int = 443) -> bool:
    """TCP check on port 443 (nginx HTTPS)."""
    try:
        sock = socket.create_connection(("localhost", port), timeout=3)
        sock.close()
        return True
    except OSError:
        return False


# ── Disk ──────────────────────────────────────────────────────────────────────

def disk_usage() -> list[dict]:
    """Return usage for key mount points."""
    mounts = [
        ("Internal SSD", "/"),
        ("T7-NorthStar (Ollama)", "/Volumes/T7-NorthStar"),
        ("T9-NorthStar (DB data)", "/Volumes/T9-NorthStar"),
    ]
    results = []
    for label, path in mounts:
        if not Path(path).exists():
            continue
        u = shutil.disk_usage(path)
        pct = int(u.used / u.total * 100)
        results.append({
            "label": label,
            "path": path,
            "used_gb": u.used / 1024**3,
            "total_gb": u.total / 1024**3,
            "pct": pct,
        })
    return results


# ── SSL certificate ───────────────────────────────────────────────────────────

def ssl_days_remaining(cert_path: str = "/etc/letsencrypt/live/hagelbg.dyndns-ip.com/fullchain.pem") -> int | None:
    """Return days until SSL cert expires, or None if unreadable."""
    try:
        import ssl, datetime
        cert_data = Path(cert_path).read_bytes()
        # Use openssl CLI — simpler than parsing PEM in stdlib
        result = subprocess.run(
            ["openssl", "x509", "-noout", "-enddate"],
            input=cert_data, capture_output=True, timeout=5
        )
        line = result.stdout.decode().strip()  # notAfter=Apr 30 12:00:00 2025 GMT
        date_str = line.split("=", 1)[1]
        expiry = datetime.datetime.strptime(date_str, "%b %d %H:%M:%S %Y %Z").replace(tzinfo=timezone.utc)
        return (expiry - datetime.datetime.now(timezone.utc)).days
    except Exception:
        return None


# ── Ollama ────────────────────────────────────────────────────────────────────

def ollama_status() -> dict:
    """Return {'running': bool, 'model': str, 'processor': str}."""
    result = subprocess.run(
        ["ollama", "ps"],
        capture_output=True, text=True, timeout=5
    )
    if result.returncode != 0:
        return {"running": False, "model": "", "processor": ""}
    lines = result.stdout.strip().splitlines()
    if len(lines) < 2:
        return {"running": True, "model": "none loaded (idle)", "processor": ""}
    # Header: NAME  ID  SIZE  PROCESSOR  CONTEXT  UNTIL
    # Use header offsets — more reliable than splitting on whitespace
    # because SIZE contains a space ("5.1 GB").
    header = lines[0]
    name_start      = 0
    processor_start = header.index("PROCESSOR")
    context_start   = header.index("CONTEXT")
    data = lines[1]
    model     = data[name_start:processor_start].split()[0] if data else "?"
    processor = data[processor_start:context_start].strip() if len(data) > processor_start else "?"
    return {"running": True, "model": model, "processor": processor}
