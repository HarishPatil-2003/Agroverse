from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
import os

HOST = "localhost"
PORT = 3001

class SPAHandler(SimpleHTTPRequestHandler):
    def end_headers(self):
        # Prevent aggressive browser caching of local ES modules during dev
        self.send_header('Cache-Control', 'no-cache, no-store, must-revalidate')
        self.send_header('Pragma', 'no-cache')
        self.send_header('Expires', '0')
        super().end_headers()

    def do_GET(self):
        clean_path = self.path.split("?")[0].split("#")[0]
        file_path = clean_path.lstrip("/")

        # Static asset extensions that should NEVER return index.html if missing
        asset_exts = ('.js', '.css', '.png', '.jpg', '.jpeg', '.gif', '.svg', '.json', '.woff', '.woff2', '.ico', '.webp', '.map')
        
        if clean_path == "/" or clean_path == "":
            self.path = "/index.html"
            return super().do_GET()

        # Redirect legacy standalone page requests like /crop.html or /dashboard.html to SPA hash /#crop
        if clean_path.endswith(".html") and clean_path != "/index.html" and not clean_path.startswith("/pages/"):
            page_name = clean_path.strip("/").replace(".html", "")
            if page_name == "dashboard":
                page_name = "home"
            self.send_response(302)
            self.send_header("Location", f"/#{page_name}")
            self.end_headers()
            return

        # If file exists on disk, serve it directly
        if os.path.isfile(file_path):
            return super().do_GET()

        # If it's a known asset extension and missing, return 404
        if any(clean_path.lower().endswith(ext) for ext in asset_exts):
            self.send_error(404, f"File not found: {clean_path}")
            return

        # Fallback to SPA root for client-side routing
        self.path = "/index.html"
        return super().do_GET()

if __name__ == "__main__":
    os.chdir(os.path.dirname(os.path.abspath(__file__)))

    server = ThreadingHTTPServer((HOST, PORT), SPAHandler)
    server.daemon_threads = True
    print(f"AgroVerse frontend running at http://{HOST}:{PORT}")
    server.serve_forever()
