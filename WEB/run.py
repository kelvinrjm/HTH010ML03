"""STOCKSENSE Platform Launcher.

Executes both the FastAPI backend server (port 8000) and the Vite frontend (port 5173).
Usage:
    python run.py
"""

import os
import subprocess
import sys
import time
import webbrowser

def main():
    root_dir = os.path.dirname(os.path.abspath(__file__))
    frontend_dir = os.path.join(root_dir, "frontend")

    print("=" * 60)
    print("STOCKSENSE — AI-Powered Inventory Intelligence Platform")
    print("=" * 60)
    print("[1/3] Checking environment...")

    # Start FastAPI backend
    print("[2/3] Starting FastAPI Backend on http://127.0.0.1:8000...")
    backend_cmd = [
        sys.executable,
        "-m",
        "uvicorn",
        "backend.main:app",
        "--host",
        "127.0.0.1",
        "--port",
        "8000",
        "--reload",
    ]
    backend_proc = subprocess.Popen(backend_cmd, cwd=root_dir)

    # Start Vite frontend
    print("[3/3] Starting Vite React Frontend on http://localhost:5173...")
    npm_cmd = "npm.cmd" if sys.platform == "win32" else "npm"
    frontend_proc = subprocess.Popen([npm_cmd, "run", "dev"], cwd=frontend_dir)

    time.sleep(2)
    print("\n" + "=" * 60)
    print("STOCKSENSE is now running!")
    print("Frontend UI:   http://localhost:5173")
    print("Backend Docs:  http://127.0.0.1:8000/docs")
    print("Demo Admin:    admin / Admin@123")
    print("Demo Staff:    staff / Staff@123")
    print("Press Ctrl+C to terminate both servers.")
    print("=" * 60 + "\n")

    try:
        backend_proc.wait()
        frontend_proc.wait()
    except KeyboardInterrupt:
        print("\nShutting down STOCKSENSE services...")
        backend_proc.terminate()
        frontend_proc.terminate()
        print("Shutdown complete.")

if __name__ == "__main__":
    main()
